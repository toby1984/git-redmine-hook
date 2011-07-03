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

import scala.xml.XML
import java.net.URL
import scala.xml.Elem
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.client.methods.HttpPut
import org.apache.commons.codec.net.URLCodec
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.client.protocol.ClientContext
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.HttpResponseException
import org.apache.http.protocol.HttpContext
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils

/** Redmine API client.
 *
 * This class uses an Apache Commons HTTP client to access the REST API of
 * a Redmine instance with basic HTTP authentication.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
class RedmineClient( configuration : Configuration )( implicit LOG : StdOutLogger ) {

  implicit private def urlToURLHelper( url : URL ) : URLHelper = new URLHelper( url )
  implicit private def urlHelperToURL( helper : URLHelper ) : URL = helper.url

  private lazy val httpContext : HttpContext =
    {
      val authCache = new BasicAuthCache
      val basicAuth = new BasicScheme
      authCache.put( configuration.httpHost, basicAuth )

      val localcontext = new BasicHttpContext
      localcontext.setAttribute( ClientContext.AUTH_CACHE, authCache )

      localcontext
    }

  private lazy val httpClient : HttpClient =
    {
      val httpClient = new DefaultHttpClient

      httpClient.getCredentialsProvider().setCredentials(
        configuration.authScope,
        new UsernamePasswordCredentials( configuration.apiKey, "dummy" ) )

      httpClient
    }

  /**
   * Get issue by ID.
   *
   * @return redmine issue
   */
  def getIssue( ticketId : Int ) : Option[Issue] =
    {
      val document = sendGET( createIssueFetchURL( ticketId ) )

      document match {
        case Some( xml ) => {
          val subject = ( xml \ "subject" ).text
          val id = ( xml \ "id" ).text.toInt
          Some( new Issue( id, subject ) )
        }
        case None => {
          LOG.warn( "Commit seems to refer to unknown Redmine issue #"+ticketId )
          None
        }
      }
    }

  private def createIssueUpdateURL( issue : Issue, comment : String ) : URLHelper =
    {
      val encodedComment = new URLCodec().encode( comment )
      baseURL + ( "/issues/"+issue.ticketID.toString+".xml?notes="+encodedComment )
    }

  /**
   * Adds a comment to an existing Redmine issue.
   */
  def addComment( issue : Issue, comment : String ) {
    sendPUT( createIssueUpdateURL( issue, comment ) )
  }

  private def createIssueFetchURL( ticketId : Int ) : URLHelper = {
    baseURL + ( "/issues/"+ticketId.toString+".xml" )
  }

  private def sendGET( url : URL ) : Option[Elem] =
    {
      LOG.debug( "Sending GET to "+url )
      val response = executeRequest( new HttpGet( url.toString ) )

      response.getStatusLine.getStatusCode match {
        case status : Int if status == 200 => {
          LOG.debug( "GET returned HTTP status "+status )
          Some( XML.load( response.getEntity().getContent ) )
        }
        case status : Int => {
          LOG.error( "GET returned HTTP status "+status )
          None
        }
      }
    }

  private def sendPUT( url : URL ) {
    LOG.debug( "Sending PUT to "+url )
    val response = executeRequest( new HttpPut( url.toString ) )
    response.getStatusLine.getStatusCode match {
      case status : Int if status == 200 => LOG.debug( "PUT returned HTTP status "+status )
      case status : Int => LOG.error( "PUT returned HTTP status "+status )
    }
    // required to release TCP connection
    EntityUtils.consume( response.getEntity )
  }

  private def executeRequest( request : HttpUriRequest ) : HttpResponse = {
    httpClient.execute( configuration.httpHost, request, httpContext )
  }

  private def baseURL : URLHelper = configuration.serverBaseURL
}