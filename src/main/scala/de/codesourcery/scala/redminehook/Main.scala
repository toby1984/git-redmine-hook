/**
 * Copyright 2004-2011 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.scala.redminehook

import scala.Enumeration
import scala.xml._
import java.net._
import java.io._
import java.text._
import scala.io.BufferedSource
import scala.collection.mutable.ListBuffer
import java.util.regex.Pattern
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

/**
 * Command-line application to be run as a GIT pre-commit or post-receive hook.
 *
 * This application serves two purposes:
 *
 * <ul>
 * <li>When run as a commit-msg hook and header line expansion is configured, it will
 *   try to enrich the first line of the commit message with data from the referenced Redmine issue</li>
 * <li>When run as a post-receive hook, it will extract issue references from the
 *   commit message and update the associated issues with a note containing the GIT commit hashes</li>
 * </ul>
 *
 * Required configuration data needs to be present in the `.git/config` file ,
 * see [[de.codesourcery.scala.redminehook.ConfigProperties]] for details.
 *
 * Command-line switches:
 *
 * You might want to pass the option '-d' ( or '--debug' ) to see what's going on.
 *
 * @author tobias.gierke@code-sourcery.de
 */
object Main 
{
  private implicit val LOG = new StdOutLogger( debugMode = false )

  private lazy val gitClient = new GitClient
  private lazy val appConfig = gitClient.appConfig
  private lazy val redmineClient = new RedmineClient( appConfig )

  private implicit def stringToObjectId( s : String ) : ObjectId = ObjectId.fromString( s )

  private def sha1( id : ObjectId ) : String = ObjectId.toString( id )

  private final class FileHelper( val file : File ) {
    def this( path : String ) = this( new File( path ) )

    def appendSuffix( suffix : String ) : FileHelper =
      {
        val absFile = new File( file.getAbsolutePath )
        new FileHelper( absFile.getParentFile+"/"+file.getName + suffix )
      }
  }

  private implicit def file2FileHelper( file : File ) : FileHelper = new FileHelper( file )
  private implicit def fileHelper2File( helper : FileHelper ) : File = helper.file

  def main( args : Array[String] ) {
    val remainingArgs = args.filterNot( s => s == "-d" || s == "--debug" )
    LOG.debugMode = args.size != remainingArgs.size

    if ( remainingArgs.isEmpty ) {
      calledAsGitReceiveHook( System.in )
    } else {
      LOG.debug( "Checking commit message in "+remainingArgs( 0 ) )
      expandCommitMessage( new File( remainingArgs( 0 ) ) )
    }
  }

  private def expandCommitMessage( commitMessage : File ) 
  {
    // do nothing if no pattern is configured in .git/config
    val placeholderExpression = appConfig.expandTicketIdPattern match {
      case Some( expression ) => expression
      case None => {
        LOG.debug( "Header line expansion not enabled in configuration." )
        return
      }
    }

    // read lines from commit message and check
    // whether the first line contains our pattern
    val source = new BufferedSource( new FileInputStream( commitMessage ) )
    val lines = source.getLines.toSeq
    
    if ( lines.isEmpty ) {
      return 
    }

    // replace TICKET_ID placeholder with regex for matching an integer number
    val substitutedExpression = placeholderExpression.expandToRegExPattern { 
      case "TICKET_ID" => "([0-9]++)"
    }

    if ( substitutedExpression.isContainedIn( lines.head ) ) 
    {
      LOG.debug( "Commit message needs expanding." )

      val tmpFile = commitMessage appendSuffix ".tmp"
      val writer = new BufferedWriter( new FileWriter( tmpFile ) )

      // copy all lines except the first, this one gets expanded
      try {
        lines.zipWithIndex.foreach( _ match {
          case ( line, 0 ) => writer.write( expandTicketReference( line )+"\n" )
          case ( line, _ ) => writer.write( line+"\n" )
        } )
      } finally {
        writer.close()
      }

      replaceFile( origFile = commitMessage, newFile = tmpFile )
    } else {
      LOG.debug( "Header line expansion not triggered ( pattern: "+placeholderExpression+")" )
    }
  }

  private def replaceFile( origFile : FileHelper, newFile : FileHelper ) {
    val backupFile = origFile appendSuffix ".bak"

    if ( origFile.renameTo( backupFile ) ) {
      if ( newFile.renameTo( origFile ) ) {
        backupFile.delete()
      } else if ( !backupFile.renameTo( origFile ) ) {
        LOG.error( "Failed to restore backed-up commit msg file "+backupFile+" -> "+origFile )
      }
    }
  }

  private def expandTicketReference( line : String ) : String = {

    val placeholderExpression = appConfig.expandTicketIdPattern.get

    // replace TICKET_ID with regex for integer number
    val substitutedExpression = placeholderExpression.expandToRegExPattern {
      case "TICKET_ID" => "([0-9]++)"
    }

    // substitute the first match (ONLY!)
    substitutedExpression.foreachMatch( line ) { matcher =>
      {
        val ticketId = matcher.group( 1 )

        redmineClient.getIssue( ticketId.toInt ) match 
        {
          case Some( issue ) => {

            // substitute ticket ID and issue subject placeholders
            val replacement = appConfig.headerReplacePattern.expand {
              case "TICKET_ID" => ticketId
              case "ISSUE_SUBJECT" => issue.subject
            }.toString
            
            // replace TICKET_ID with actual ticket number
            val withTicketNumber = placeholderExpression.expand {
              case "TICKET_ID" => ticketId
            }

            // String#replaceAll() requires a regex pattern
            val toReplace = "(?i)"+withTicketNumber.toRegEx // CASE-INSENSITIVE            

            val result = line.replaceAll( toReplace, replacement )
            LOG.info( "Expanded commit message header line: "+result )
            return result /* Exits method */
          }
          case None =>
        }
      }
    }
    line
  }

  private def calledAsGitReceiveHook( stream : InputStream ) {
    val input = new BufferedSource( stream )

    val GitReceiveLine = "(.*?) (.*?) (.*)".r /* <parent> <child> <refspec> */

    for ( line <- input.getLines ) {
      val GitReceiveLine( parentHash, childHash, ref ) = line
      handleCommit( childHash, parentHash )
    }
  }

  private def handleCommit( childCommit : ObjectId, parentCommit : ObjectId ) {

    val commits = gitClient.getCommits( childCommit, parentCommit )

    LOG.debug( "Visiting refs "+sha1( childCommit )+" -> "+sha1( parentCommit )+
      "( "+commits.size+" commits )" )

    // associate commits with ticket IDs parsed from commit messages
    // while making sure that each commit will be mentioned at most once
    // per ticket
    val commitsByTicketId = commits.foldLeft( new HashMap[Int, HashSet[RevCommit]] ) { ( commitsByTicketId, commit ) =>
      {
        for ( ticketId <- extractTicketIdsFrom( commit ) ) {
          commitsByTicketId.getOrElseUpdate( ticketId, new HashSet[RevCommit]() ).add( commit )
        }
        commitsByTicketId
      }
    }

    for ( ( ticketId, commitsForThisTicket ) <- commitsByTicketId ) {
      LOG.debug( "Trying to fetch issue #"+ticketId )
      redmineClient.getIssue( ticketId ) match {
        case Some( issue ) => updateIssue( issue, commitsForThisTicket )
        case _ =>
      }
    }
  }

  private def extractTicketIdsFrom( commit : RevCommit ) : Set[Int] =
    {
      LOG.debug( "Scanning commit "+sha1( commit ) )
      
      val result = new scala.collection.mutable.HashSet[Int]
      appConfig.ticketIdPattern.foreachMatch( commit.getFullMessage ) {
        matcher => {
          result.add( matcher.group(1).toInt )
        }
      }
      result.toSet
    }

  private def updateIssue( issue : Issue, commits : Iterable[RevCommit] ) 
  {
    require( !commits.isEmpty )

    LOG.info( "GIT commit(s) referring to "+issue+" : "+commits.map( sha1 ).mkString( "," ) )

    val message = commits.foldLeft( "" ) { ( previousLine, commit ) => {
        previousLine+"\n"+sha1( commit )+" by "+commit.getAuthorIdent.getEmailAddress
      }
    }
    
    val pattern = appConfig.commentPattern.expand( { case "COMMITS" => message } ) 
    redmineClient.addComment( issue, pattern.toString )
  }
}