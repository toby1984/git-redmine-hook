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

/** Holds names of supported configuration file properties.
  *
  * A full-blown configuration (post-receive issue updates and
  * pre-commit header line expansion) might look like this:
  * 
  * {{{
  * [redmine]
  *       apikey = 5d9e0f25eb5540970f817143581a56047a08e794
  *       baseurl = http://localhost
  * [redmine "post-receive"]
  *       ticketidpattern = "Ticket #{TICKET_ID}"
  * [redmine "pre-commit" ]
  *       expandpattern = "Fixed #{TICKET_ID}"
  *       expandto = "Ticket #{TICKET_ID}: {ISSUE_SUBJECT}"
  * }}}
  * 
  * When used in a pattern the expression `{TICKET_ID}` will be used to match an integer number,
  * when used in the `expandto` property it will be expanded to a Redmine issue ID.
  * 
  *  The expression `{ISSUE_SUBJECT}` will be expanded to the corresponding Redmine issue's subject.
  * 
  * The configuration properties in detail:
  * 
  * <ul>
  * <li> `apikey`  : Redmine REST API key used to authenticate with the server</li>
  * <li> `baseurl` : URL to access the Redmine instance</li>
  * <li> `ticketidpattern` : Expression to use for extracting Redmine issue IDs when ran as a post-receive hook</li>
  * <li> `expandpattern` : Expression the first line of the commit needs to match in order to be expanded</li>
  * <li> `expandto`: Expression that is used to expand the first match of 'expandpattern'</li>
  * </ul> 
  * @author tobias.gierke@code-sourcery.de
  */
object ConfigProperties {

  private val GIT_CONFIG_SECTION = "redmine"
  private val GIT_CONFIG_SUBSECTION : String = null
  
  /** Holds section and subsection for our config properties in `.git/config`.
   * @author tobias.gierke@code-sourcery.de
   */
  sealed case class GitConfigSection( val sectionName : String , val subSection : String = null )

  private val SECTION_REDMINE = GitConfigSection( "redmine" )
  private val SECTION_PRE_COMMIT = GitConfigSection( "redmine" , "commit-msg" )
  private val SECTION_POST_RECEIVE = GitConfigSection( "redmine" , "post-receive" )
  
  sealed case class Val( val key : String, val section : GitConfigSection )

  /** Redmine HTTP server base URL.
   */
  val PROP_REDMINE_BASEURL = Val( "baseurl" , SECTION_REDMINE )

  /** API key to use for accessing the Redmine system.
   */
  val PROP_API_KEY = Val( "apikey" , SECTION_REDMINE )

  /** Regular expression to look for when parsing commit messages.
   */
  val PROP_TICKETID_PATTERN = Val( "ticketidpattern" , SECTION_POST_RECEIVE )
  
  /** Pattern to use for creating the Redmine comment.
   */
  val PROP_COMMENT_PATTERN = Val( "comment" , SECTION_POST_RECEIVE )  

  /** Expression to match when expanding commit message header lines.
   */
  val PROP_EXPAND_TICKET_PATTERN = Val( "expandpattern" , SECTION_PRE_COMMIT )

  /** Expression to expand [[de.codesourcery.scala.redminehook.PROP_EXPAND_TICKET_PATTERN]] into.
   * 
   * The following placeholders may be used:
   * 
   * - `{TICKET_ID}` will be expanded to the Redmine issue ID 
   * - `{ISSUE_SUBJECT}` will be expanded to the subject of the Redmine issue 
   */
  val PROP_EXPAND_TICKET_PATTERN_TO = Val( "expandto" , SECTION_PRE_COMMIT )
}