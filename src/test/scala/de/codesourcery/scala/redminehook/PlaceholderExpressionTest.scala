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
import junit.framework.TestCase
import junit.framework.Assert._

class PlaceholderExpressionTest extends TestCase {

  def test1() {
      val expr = PlaceholderExpression("Ticket #1")
      
      assertTrue( expr.isContainedIn("Ticket #1" ) )
      assertTrue( expr.isContainedIn("ticket #1" ) )
      assertFalse( expr.isContainedIn("ticket  #1" ) )
      assertEquals( "Ticket #1"  , expr.replaceVariables("Ticket #1" ){ case x : String => x } )    
  }
  
  def test2() {
      val expr2 = PlaceholderExpression("Fixed issue {TICKET}")
      assertTrue( expr2.isContainedIn("Fixed issue {TICKET}" ) )
      assertEquals( "Fixed issue 1234"  , 
          expr2.replaceVariables("Fixed issue {TICKET}" ) { case "TICKET" => "1234" } )      
  }

  def test3() {
      val expr3 = PlaceholderExpression("Ticket #{TICKET_ID}")
      val expanded1 = expr3.expand{ case "TICKET_ID" => "12" }
      assertTrue( expanded1.isContainedIn("Ticket #12") )
      
      val expanded2 = expr3.expandToRegExPattern{ case "TICKET_ID" => "([0-9]+)" }
      assertEquals( "Ticket #([0-9]+)" , expanded2.pattern )
      assertTrue( expanded2.isContainedIn("Ticket #12") )         
  }
  
  def testForeachMatch() {
      val toTest = "ticket #1 and Ticket #2 and ticket #3"
      var patternToTest = PlaceholderExpression("Ticket #{TICKET_ID})")
      patternToTest = patternToTest.expandToRegExPattern{ case "TICKET_ID" => "([0-9]+" }
      
      var result = Set[Int]()
      patternToTest.foreachMatch( toTest ) {
         matcher => result = result + matcher.group(1).toInt
      }
      
      List(1,2,3).foreach( ticketId => assertTrue( result.contains( ticketId ) ) )    
  }
  
  def test4() {
      val expr4 = PlaceholderExpression("Fixed #{TICKET_ID}")
      val regExpr4 = expr4.expandToRegExPattern{ case "TICKET_ID" => "([0-9]++)" }
      assertTrue( regExpr4.isContainedIn( "Fixed #3.") )
  }

}
