(C) 2011-07-03 , tobias.gierke@code-sourcery.de

This is a little Scala program I wrote for 
GIT / Redmine integration (tested against Redmine 1.2.0).

*RANT ON*

Yeah, I know that I could've achieved the same thing
with a Perl (or even Bash) script ... but I 
desperately wanted to write some Scala code 
(considering that do Java programming for a living),
so Scala it is... 

*RANT OFF*

This tool is intended to be run as a GIT commit hook
and provides the following two modes of operation: 

1. (when run as commit-msg hook)

   Checks whether the FIRST line of a commit message matches a given
   pattern and enriches the commit message with properties from 
   the referenced Redmine issue according to a given pattern.

   Currently only the issue's subject line is available
   for substitution.

2. (when run as a post-receive hook)

   Tries to extract Redmine issue IDs from 
   commit messages and updates each issue 
   with a note saying which GIT commits refer to it.
   
BUILDING
========

To build the project I use

Apache Maven 2.2.1
Java version: 1.6.0_26 (x86,32-bit)
Scala library : 2.9.0-1

Running 'mvn package' will create the executable JAR /target/git-redmine-hook-1.0.0-exe.jar

INSTALLATION
============

Put the following lines into the .git/config file inside the 
GIT repository you want to enable this commit hook for. 

[redmine]
        apikey = 5d9e0f25eb5540970f817143581a56047a08e794
        baseurl = http://localhost
[redmine "post-receive"]
        ticketidpattern = "Ticket #{TICKET_ID}"
        comment = Mentioned in GIT commit(s): \n{COMMITS}
[redmine "commit-msg"]
        expandpattern = "Fixed #{TICKET_ID}"
        expandto = "Ticket #{TICKET_ID}: {ISSUE_SUBJECT}"

You only need to have the subsections ("post-receive" or "commit-msg")
when you have enabled the corresponding GIT hook to run 
this program.

Make sure the API key belongs to a Redmine user that has
permissions to read (and update) issues in the corresponding project.

Available placeholders
----------------------

TICKET_ID     : Matches an integer number (when used in pattern matching) and
                is substituted with the ID of a Redmine issue (when substituting text)
ISSUE_SUBJECT : Gets replaced with the subject line from the corresponding
                Redmine issue 
COMMITS       : Gets replaced with a list of GIT SHA1 hashes along with the author's email 

GIT CONFIGURATION
=================

To run this program as a GIT hook, do the following:

1. Make sure you have the Java JRE (runtime) installed (run 'java -version' to check)

2.1 To run as a "post-receive" hook

Edit/create the file hooks/post-receive to look like

java -jar <path to executable JAR> [--debug]

2.2 To run as a "commit-msg" hook

Edit/create the file hooks/commit-msg to look like

java -jar <path to executable JAR> [--debug] "$@"

HINT: The '--debug' option will print out very verbose messages about what's going on. 
