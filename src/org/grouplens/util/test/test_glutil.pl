#!/opt/gnu/bin/perl -w

use strict;
use glutil;

print "ConfigVar:\n";
print "CATALINA_HOME is ", getConfigVar('CATALINA_HOME'), "\n";
print "jrecserver.CATALINA_OPTS is ", getConfigVar('CATALINA_OPTS', 'jrecserver'), "\n";
print "foo.CATALINA_OPTS is ", getConfigVar('CATALINA_OPTS', 'foo', 1), "\n";

print "Before loadEnv():\n";
system("echo CATALINA_HOME is \$CATALINA_HOME");

loadEnv('jrecserver');

print "After loadEnv(jrecserver):\n";
system("echo CATALINA_HOME is \$CATALINA_HOME");
system("echo CATALINA_OPTS is \$CATALINA_OPTS");

print "Substitute strings in file 'init_server.xml' ==> 'server.xml'\n";
substituteFile('init_server.xml', 'server.xml', 'movielens3');
