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
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import org.eclipse.jgit.storage.file.FileRepository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import scala.collection.mutable.ListBuffer

/**
 * Thin wrapper around the JGit client library.
 *
 * @author tobias.gierke@code-sourcery.de
 */
class GitClient( implicit LOG : StdOutLogger ) {

  private lazy val repository : FileRepository =
    {
      val builder = new FileRepositoryBuilder()
      val gitDir = new File( System.getProperty( "user.dir" ) )

      LOG.debug( "Using GIT working dir: "+gitDir.getAbsolutePath )

      builder.setWorkTree( gitDir ).readEnvironment.findGitDir.build
    }

  private def getCommit( hash : ObjectId ) : Option[RevCommit] = {
    revWalk { walk =>
      Some( walk.parseCommit( hash ) )
    }
  }

  /** Returns configuration data read from the `.git/config` file.
   */
  lazy val appConfig : Configuration = new Configuration() {
    protected def getProperty( key : ConfigProperties.Val ) : Option[String] =
      {
        val sectionName = key.section.sectionName
        val subSection = key.section.subSection;
        repository.getConfig.getString( sectionName, subSection, key.key ) match {
          case s : String => {
            LOG.debug( "[ CONFIG ] "+key.key+"="+s )
            Some( s )
          }
          case _ => {
            LOG.debug( "[ MISSING CONFIG ] "+key.key )
            None
          }
        }
      }
  }

  /** Returns all commits that lead from a given child to a given parent.
   *
   * The returned commits will at least contain of `child`and `parent`.
   * 
   * This method ignores any commit lookup failures and will only return
   * regular commits (read: no tags,branches, annotated tags etc.).
   *
   * @param child Child commit to start with
   * @param parent Parent commit to stop at
   * @return Commits from child to parent
   */
  def getCommits( child : ObjectId, parent : ObjectId ) : Seq[RevCommit] =
    {
      val latestCommit = getCommit( child ) match {
        case Some( x ) => x
        case None => return List[RevCommit]()
      }

      val result = new ListBuffer[RevCommit]()
      revWalk { walk =>
        {
          walk.markStart( latestCommit )

          var current = walk.next;
          do {
            result.append( current )
            current = if ( current.getId != parent ) walk.next else null
          } while ( current != null )
          Some( result )
        }
      }
      result
    }

  private def revWalk[T]( func : => ( RevWalk ) => Option[T] ) : Option[T] =
    {
      val walk = new RevWalk( repository )
      try {
        return func( walk )
      } catch {
        case ex : MissingObjectException => {
          LOG.error( "Unable to find commit "+ex.getObjectId )
          None
        }
        case ex : IncorrectObjectTypeException => {
          LOG.warn( "Ignoring refspec that does not refer to a commit" )
          None
        }
        case ex : Throwable => throw ex
      } finally {
        walk.dispose
      }
    }

}