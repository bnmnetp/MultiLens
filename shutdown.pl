#!/opt/gnu/bin/perl -w

use strict;
use glutil;

my $varContext = 'jrecserver';

# Check we have the vars we need
checkConfigVar('CATALINA_HOME', $varContext);
checkConfigVar('CATALINA_BASE', $varContext);

# Load environment with config vars
loadEnv($varContext);

# shutdown the server
print "*** Stopping $varContext ***\n";
runCmd("\$CATALINA_HOME/bin/shutdown.sh");
