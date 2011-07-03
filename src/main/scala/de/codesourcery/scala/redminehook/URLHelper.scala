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

/** Immutable helper class that waps an URL and provides a '+' method for appending to URLs
 * in a natural way.
 *
 * @author tobias.gierke@code-sourcery.de
 */
final class URLHelper( val url : URL ) {

  require( url != null )

  def this( url : String ) = this( new URL( url ) )

  /**
   * Removes leading and trailing slashes and whitespace.
   */
  private def normalize( path : String ) : String =
    {
      def isInvalidChar( c : Char ) = c == '/' || c == ' '

      path.dropWhile( isInvalidChar ).reverse.dropWhile( isInvalidChar ).reverse
    }

  /** Returns a new URL with the given path appended.
   * 
   * This method will automatically strip leading/trailing slashes so
   * that the concatenated URL is always valid.
   * 
   * @param pathToAppend the path to append , must not be `NULL`
   */
  def +( pathToAppend : String ) : URLHelper = {

    require( pathToAppend != null )

    val protocol = url.getProtocol
    val host = url.getHost
    val port = url.getPort

    val newPath = normalize( url.getPath )+"/"+normalize( pathToAppend )

    new URLHelper( new URL( protocol, host, port, newPath ) )
  }

  override def toString() : String = url.toString
}