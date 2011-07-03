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

import java.util.regex.Pattern
import scala.collection.mutable.HashSet
import java.util.regex.Matcher

/**
 * Helper class to ease matching and placeholder substitution in string patterns.  
 * 
 * <p>This class wraps strings that may contain placeholder
 * expressions. Since this class uses regex matching internally , it's important
 * to indicate when the expression itself is already a regular expression (otherwise
 * all regex-special characters will automatically be escaped whenever the
 * expression is used in a regex match).
 * 
 * Sample usage:
 * {{{
 *   val expr = PlaceholderExpression("The answer to all questions is {PLACEHOLDER}.")
 *   println( expr.expand( { case "PLACEHOLDER" => "42" } ) )
 * }}}
 * 
 * will print 
 * {{{
 *      The answer to all questions is 42.
 * }}}
 * </p>
 * @author tobias.gierke@code-sourcery.de
 */
class PlaceholderExpression(val pattern : String, isRegExPattern : Boolean = false) {
  
  private sealed case class Variable( val name : String )
  
	private val VARIABLE_PATTERN = Pattern.compile("\\{(.*?)\\}")
	    
	private def quoteRegEx( s : String ) : String =
	{
	  val specialCharacters = "{}\\.*[]+-?"
	
	  s.foldLeft( "" ) { ( previousString, currentChar ) =>
	    {
	      if ( specialCharacters.contains( currentChar ) ) {
	        previousString+"\\"+currentChar.toString
	      } else { previousString + currentChar.toString }
	    }
	  }
	}   
  
  /**
   * Checks whether a given string contains this expression.
   */
  def isContainedIn( string : String ) : Boolean =  
  {
    string != null && toRegExPattern.matcher( string ).find()
  }
  
  /**
   * Traverses all matched occurances of this pattern.
   */
  def foreachMatch( input : String )( func : Matcher => Unit ) 
  {
     val matcher = toRegExPattern.matcher( input )
     while ( matcher.find ) {
       func( matcher )
     }
  }
  
  override def toString = pattern
  
  /**
   * Converts this pattern into a CASE-INSENSITIVE [[java.util.regex.Pattern]].
   */
  def toRegExPattern : Pattern = Pattern.compile( "(?i)"+toRegEx )
  
  /**
   * Converts this pattern into a regular expression (string).
   */
  def toRegEx : String = if ( ! isRegExPattern ) quoteRegEx( pattern ) else pattern 
  
  /**
   * Substitutes placeholders and treats the resulting pattern as a regular expression.
   */
  def expandToRegExPattern(replacementFunction : PartialFunction[String , String]) : PlaceholderExpression = {
	  new PlaceholderExpression( internalReplaceVariables( pattern ,  replacementFunction ) , true)
  }  
  
  /**
   * Substitutes placeholders in this pattern.
   */
  def expand(replacementFunction : PartialFunction[String , String]) : PlaceholderExpression = {
	  new PlaceholderExpression( internalReplaceVariables( pattern ,  replacementFunction ) , isRegExPattern )
  }
  
  protected[redminehook] def replaceVariables( input : String)(replacementFunction : PartialFunction[String , String] ) : String = 
  {
	  internalReplaceVariables( input , replacementFunction )
  }
  
  private def internalReplaceVariables( input : String , replacementFunction : PartialFunction[String , String] ) : String = 
  {
    var result = input
    for ( variable <- extractVariables( input ) ) 
    {
        if ( ! replacementFunction.isDefinedAt( variable.name ) ) {
          throw new RuntimeException("Failed to expand unknown property '"+variable.name+"' in '"+input+"'" )
        }
    	result = replaceAll( result , variable , replacementFunction( variable.name ) )
    }
    result
  }
  
  private def variableExpression( variable : Variable ) : String = 
    quoteRegEx( "{"+variable.name+"}" )
  
  private def replaceAll( input : String , variable : Variable , replacement : String ) : String = {
     input.replaceAll( variableExpression( variable ) , replacement )
  }
  
  private def extractVariables( input : String ) : Set[Variable] = 
  {
    val result = new HashSet[Variable]
    val matcher = VARIABLE_PATTERN.matcher( input )
    while( matcher.find() ) {
      result.add( Variable( matcher.group(1) ) )
    }
    result.toSet
  }
  
}

object PlaceholderExpression {
    def apply(s : String ) : PlaceholderExpression = new PlaceholderExpression(s)
    def apply(s : String , isRegExPattern : Boolean ) : PlaceholderExpression = new PlaceholderExpression(s,isRegExPattern)
}