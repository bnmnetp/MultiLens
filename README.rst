INTRODUCTION
============

MultiLens is a CVS repository containing code necessary to produce two
JARs:

#. multilens.jar
#. glutil.jar

The rest of this doc contains:

#. `The big picture <#bigpicture>`__
#. `Checking out MultiLens <#checkingout>`__
#. `External prerequisites <#prereqs>`__
#. `Configuring <#config>`__
#. `Read-only access <#readonly>`__

For more information:

#. If the javadoc is generated in this source tree ("ant javadoc"), it
   is `here <dist/docs/api/index.html>`__.
#. `Here <http://www.cs.umn.edu/Research/GroupLens/internal/java/MultiLens/dist/docs/api/index.html>`__
   is a global copy of the javadoc.
#. If the MultiLens cookbook is generated in this source tree ("ant
   cookbook-pdf"), it is `here <doc/Jrec.pdf>`__.
#. This repository's root is `here <.>`__.

THE BIG PICTURE
---------------

Here are the CVS repositories that contain code to run
`www.movielens.umn.edu <http://www.movielens.umn.edu>`__, and how they
depend on each other. An arrow indicates a dependency, with the tail
depending on the head.

::


           movielens3 ==> jrecserver ==> MultiLens (glutil.jar, glutil.pm)
              |                                        ^           ^
              |                                        |           |
              ======================================================

#. **MultiLens** contains code to build models, and use models and
   ratings to produce recommendations for users. It also contains
   org.grouplens.util.\* (which becomes glutil.jar) and glutil.pm, which
   contain some utilities used by all Java and Perl for reading
   configuration files, logging, etc. These jars and pms could live
   separately from MultiLens, but it was convenient to put them there
   for now.
#. **jrecserver** contains code to serve up MultiLens recommendations in
   Tomcat, and other low-level recommender operations, e.g., build
   models. This is nice because it may be run independently of a web
   site, e.g. be used by 0 or many web sites.
#. **movielens3** contains code for a movie recommender site that runs
   in Tomcat. It has searching, submitting ratings, getting
   recommendations, etc. It communicates to jrecserver through XML only.
   This makes it fairly independent of jrecserver. If someone wished to
   create another recommendation server and offer an XML API, it could
   use that instead.

Each of the 3 applications represented by these CVS repositories uses
glutil.jar and glutil.pm.

CHECKING OUT MultiLens
======================

Example for tcsh:

::

      setenv CVSROOT /project/Grouplens/cvs-repository/
      cvs co MultiLens

If you've got this doc, probably you've already done that.

EXTERNAL PREREQUISITES
======================

MultiLens requires the following from external sources before it is
ready to run:

+------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------+
| Prerequisite                                                                             | Example (on gibson, using tcsh)                                                       |
+------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------+
| Java 1.4                                                                                 | module load java/jdk-1.4                                                              |
+------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------+
| ant                                                                                      | module load java/ant                                                                  |
+------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------+
| GL\_CONFIG\_FILE environment variable set to the full pathname of a gl.properties file   | setenv GL\_CONFIG\_FILE \`pwd\`/MultiLens/src/org/grouplens/util/test/gl.properties   |
+------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------+

CONFIGURING
===========

All of the configuration changes necessary are in gl.properties. You
should copy an existing properties file and modify it.

Properties to look at carefully and modify as necessary:

#. dbUrl
#. dbUser
#. dbPassword
#. Anything starting with "multilens."

COOKING MODELS
==============

More needed here.

Read-only access
----------------

For read-only access to hugo, the production database, use the
'readonly' user (password is blank). This user has no permissions to
update tables. The application still works!

RUNNING
=======

NOTE #1: **Please don't run on our production machine (currently
hugo)!**. As long as you stay off that machine, it is very unlikely you
will accidentally shut down the production web site, even if your
configuration 'points' to the production site.

NOTE #2: If you wish to point to the production database, consider
`running read-only <#readonly>`__, so you can play without modifying the
contents of the production database.
