#!/opt/gnu/bin/perl -w

use strict;
use glutil;
use File::Copy;

use Getopt::Long;
my @options;

my $reload_request = 0;
push(@options, "reload-request!", \$reload_request);

# Turn on Java's built-in profiler, that spits out a java.hprof.txt
my $opt_profile = 0;
push(@options, "profile!", \$opt_profile);

die "Couldn't parse options\n" if !GetOptions(@options);

my $varContext = 'modelbuild';

my $modelFile = getConfigVar('modelFile', $varContext);
my $avgModelFile = getConfigVar('avgModelFile', $varContext);

# Set CLASSPATH

$ENV{'CLASSPATH'}=
    getConfigVar('multilensJar', $varContext)
    . ':' . getConfigVar('jdbcJar', $varContext)
    . ':' . getConfigVar('glutilJar', $varContext)
    . ':' . getConfigVar('jrecJar', $varContext);

runCmd("echo CLASSPATH is \$CLASSPATH");
glLog('INFO', 'Building model');

my $tmpModelFile = 'newmodel.bin';
my $tmpAvgModelFile = 'newavgmodel.bin';

my $profileStr = '';
if ($opt_profile) {
    $profileStr = '-Xrunhprof:cpu=samples,depth=6';
}

my $cmd = getConfigVar('JAVA_HOME', $varContext) . ' '
    . '-DGL_CONFIG_FILE=' . getConfigVar('GL_CONFIG_FILE') . ' '
    . getConfigVar('JAVA_OPTS', $varContext)
    . " $profileStr jre.ModelBuilder $tmpModelFile $tmpAvgModelFile";

runCmd($cmd);
glLog('INFO', 'Done building model');

# Move the model into place now that building went well
move($tmpModelFile, $modelFile);
move($tmpAvgModelFile, $avgModelFile);

if ($reload_request) {
    # Call the engine to reload the model
    glLog('INFO', 'Calling jrec to load model');

    my $cmd = "wget -O - " . getConfigVar('recEngineUrl', 'jrecserver') . "?request=loadmodel\\\&modfile=$modelFile\\\&avgmodfile=$avgModelFile";
    runCmd($cmd);

    glLog('INFO', 'Done calling jrec to load model');
}

