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

/** A logger that writes to standard out.
 *
 * @author tobias.gierke@code-sourcery.de
 */
class StdOutLogger( var debugMode : Boolean = true ) {

  def debug( msg : => String ) {
    println( "DEBUG: "+msg )
  }

  def info( msg : => String ) {
    println( "INFO: "+msg )
  }

  def warn( msg : => String ) {
    println( "WARN: "+msg )
  }

  def error( msg : => String ) {
    println( "ERROR: "+msg )
  }
}