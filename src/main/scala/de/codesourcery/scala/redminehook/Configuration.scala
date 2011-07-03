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
import java.net.URL
import java.io.File
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import java.util.regex.Pattern

/**
 * Application configuration.
 *
 * @author tobias.gierke@code-sourcery.de
 */
trait Configuration 
{
  /** Placeholder expression that will either be substituted with a Redmine
   * ticket ID or (depending on where it's used) with a regular expressing
   * matching an integer number.
   */
  val TICKET_ID_PLACEHOLDER = "{TICKET_ID}"
    
  /** Placeholder that gets substituted with the subject of a Redmine issue. 
   */
  val ISSUE_SUBJECT_PLACEHOLDER = "{ISSUE_SUBJECT}"

  protected def getProperty( key : ConfigProperties.Val ) : Option[String]

  private def getPropertyOrElse( prop : ConfigProperties.Val, func : => String ) : String = {
    getProperty( prop ) match {
      case Some( string ) => string
      case _ => func
    }
  }
  
  private def getPropertyOrFail( prop : ConfigProperties.Val ) : String =
    {
      getProperty( prop ) match {
        case Some( string ) => string
        case None => throw new RuntimeException( ".git/config lacks configuration "+prop )
      }
    }  

  /**
   * Returns the Redmine API key to use.
   * @return
   */
  lazy val apiKey : String = getPropertyOrFail( ConfigProperties.PROP_API_KEY )

  /**
   * Returns the HTTP base URL to be used when accessing the Redmine system.
   * @return
   */
  lazy val serverBaseURL : URLHelper = {
    new URLHelper(
      getPropertyOrFail( ConfigProperties.PROP_REDMINE_BASEURL ) )
  }

  /**
   * Returns the regex pattern to use when extracting ticket IDs from GIT commit messages.
   *
   * @return
   */
  lazy val ticketIdPattern : PlaceholderExpression =
    {
      val pattern = getPropertyOrFail( ConfigProperties.PROP_TICKETID_PATTERN) 
      PlaceholderExpression( pattern ).expandToRegExPattern { case "TICKET_ID" => "([0-9]++)"} 
    }

  /** Returns the regex pattern to look for when expanding ticket references in commit messages.
   *
   * @return regex pattern or `None` if the `.git/config` file did not contain this config property
   */
  lazy val expandTicketIdPattern : Option[PlaceholderExpression] = { 
    getProperty( ConfigProperties.PROP_EXPAND_TICKET_PATTERN ) match {
      case Some(string) => Some( PlaceholderExpression( string ) )
      case None => None
    }
  }
  
  lazy val commentPattern : PlaceholderExpression = {
    PlaceholderExpression( getPropertyOrFail( ConfigProperties.PROP_COMMENT_PATTERN ) )
  }

  def headerReplacePattern : PlaceholderExpression = {
    PlaceholderExpression( getPropertyOrFail( ConfigProperties.PROP_EXPAND_TICKET_PATTERN_TO ) )
  }

  /**
   * Returns the scope of HTTP basic authentication.
   */
  lazy val authScope : AuthScope = new AuthScope( httpHost.getHostName, httpHost.getPort )

  /**
   * Returns the Redmine HTTP host.
   */
  lazy val httpHost : HttpHost =
    {
      val url : URL = serverBaseURL.url

      val port = if ( url.getPort != -1 ) {
        url.getPort
      } else {
        url.getProtocol match {
          case "http" => 80
          case "https" => 443
          case x => throw new RuntimeException( "Internal error, unknown protocol "+x )
        }
      }
      new HttpHost( url.getHost, port, url.getProtocol )
    }
}