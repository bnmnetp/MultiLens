#!/usr/bin/perl -w

use strict;
use glutil;

my $varContext = 'jrecserver';

my $file = 'server.xml';
substituteFile("conf/init/init_$file", "conf/$file", $varContext);
print "conf/init/init_$file ==> conf/$file  *** SUCCESSFUL\n";

$file = 'jrecserver';
substituteFile("conf/init/init_$file", "conf/$file", $varContext);
runCmd("chmod +x conf/$file");
print "conf/init/init_$file ==> conf/$file  *** SUCCESSFUL\n";

print "To start/stop the server use: \n";
print "perl -I" . getGlutilDir() . " startup.pl\n";
print "perl -I" . getGlutilDir() . " shutdown.pl\n";
print "\n";

