#!/opt/gnu/bin/perl -w

use strict;
use glutil;

my $varContext = 'jrecserver';

# Check we have the vars we need
checkConfigVar('CATALINA_HOME', $varContext);
checkConfigVar('CATALINA_BASE', $varContext);
checkConfigVar('CATALINA_OPTS', $varContext);
checkConfigVar('TOMCAT_OPTS', $varContext);

# Load environment with config vars
loadEnv($varContext);

# Add info to find glutil.jar
my $config_file = getConfigVar('GL_CONFIG_FILE');

# This won't work on Windows .. for now
die "GL_CONFIG_FILE must be a full path" if ( ! ($config_file =~ m%^/%) );
my $glutil_opt = ' -DGL_CONFIG_FILE=' . getConfigVar('GL_CONFIG_FILE');
$ENV{'CATALINA_OPTS'} .= $glutil_opt;
$ENV{'TOMCAT_OPTS'}   .= $glutil_opt;

# start the server
print "*** Starting $varContext ***\n";
print "CATALINA_OPTS is $ENV{'CATALINA_OPTS'}\n";
print "TOMCAT_OPTS is $ENV{'CATALINA_OPTS'}\n";
print "\n";
runCmd("\$CATALINA_HOME/bin/startup.sh");
