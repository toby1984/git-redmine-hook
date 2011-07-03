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

/** An issue in a trouble ticketing system.
 *
 * @author tobias.gierke@code-sourcery.de
 */
class Issue( val ticketID : Int, val subject : String ) {

  override def equals( obj : Any ) : Boolean = obj match {
    case x : Issue => x.ticketID == ticketID
    case _ => false
  }

  override def hashCode = ticketID.hashCode

  override def toString = "issue #"+ticketID
}