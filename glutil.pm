#!/opt/gnu/bin/perl -w

package glutil;

use strict;
use POSIX;
use File::Copy;
use IO::Handle;
use File::Basename;

use Mail::Send;
use MIME::Base64;

BEGIN {
    use Exporter ();

    @glutil::ISA     = qw(Exporter);
    @glutil::EXPORT      = qw(checkConfigVar
                              dropTables
                              getGlutilDir
                              getConfigVar
                              getTables
                              glLog
                              loadEnv
                              runCmd
                              setDebug
                              setEmailAttachment
                              setLogToEmail
                              setLogToStderr
                              setEmailSubjects
                              setEmailRecipientsLists
                              setSimulateFlag
                              substituteFile);
    @glutil::EXPORT_OK   = qw();
}

# Private, but shared
my @emailRecipientsLists;
my $emailRecipients;
my @emailSubjects;
my $emailSubject;
my $logToEmail = 0;

# If the email flag is set and we have pre-reqs, we should send email
END {
    my $i = 0;
    if ($?) {
        # We are die-ing or otherwise sad
        $i = 1;
    }

    if ($logToEmail && (int(@emailSubjects) == 2)
        && (int(@emailRecipientsLists) == 2)) {
        $emailSubject = $emailSubjects[$i];
        $emailRecipients = $emailRecipientsLists[$i];
        # There's at least one email address in there
        if ( length($emailRecipients) > 0 ) {
            _sendEmail();
        }
    }
}

my $varsLoaded = 0;
my %var;
my $debug = 0;

sub setDebug {
    $debug = shift;
}

# This is the directory of glutil.pm, suitable for perl -I <dir>
sub getGlutilDir() {
    my $fullpath = $INC{'glutil.pm'};

    my ($name, $path, $suffix) = fileparse($fullpath, '.pm');
    return $path;
}

sub loadConfig {
    my $config_file = shift;

    $config_file = $ENV{'GL_CONFIG_FILE'} if !defined($config_file);

    die "GL_CONFIG_FILE not defined\n" if !defined($config_file);
    die "Couldn't read $config_file\n" if ! -r $config_file;
    die "$config_file is not a file\n" if ! -f $config_file;

    open(FOO, $config_file) or die "Couldn't open $config_file: $!\n";
    while ( <FOO> ) {
	s/[\r\n]//g;

	# The '#' is a comment character
	next if ( /^#/ );

        if ( /([^=]+)=(.*)/ ) {
            # Variable
            $var{$1} = $2;
            die "You can't set GL_CONFIG_FILE in the properties file\n" if ($1 eq 'GL_CONFIG_FILE');
#	    print STDERR "Read var $1 value $2\n";
        }
    }
    close(FOO);

    # Set this variable specially
    $var{'GL_CONFIG_FILE'}=$config_file;
    $varsLoaded = 1;
}

sub getConfigVar {
    my $varname = shift;
    my $subCat = shift;
    my $optional = shift;

    loadConfig() if !$varsLoaded;

    my $val = $var{$varname};
    my $the_name = $varname;

    if (defined($subCat)) {
        my $new_name = $subCat . '.' . $varname;
#	print "getConfigVar: looking for $subCat.$varname ..\n";
        my $subVal = $var{$new_name};
        if (defined($subVal)) {
            $the_name = $new_name;
            $val = $subVal;
        }
#	print "getConfigVar: found $subCat.$varname\n" if defined($subVal);
    }

    # This var is not optional, so die if we don't find it
    if (!defined($optional) || !$optional) {
        die "Config variable $varname not defined" . (defined($subCat) ? " (for subcat $subCat)" : "") if !defined($val);
    }

    if ($debug) {
        glLog('INFO', "glutil::getConfigVar: $the_name=" . (defined($val) ? $val : ''));
    }

    return $val;
}

sub checkConfigVar {
    my $varname = shift;
    my $subCat = shift;

    getConfigVar($varname, $subCat);
}

#
# This loads the %ENV hash (inherited by all sub-processes) with
# all non-subcat variables.  For example:
#
# loadEnv('movielens3') will
#   - load TOMCAT_OPTS as "TOMCAT_OPTS"        (global)
#   - load movielens3.TOMCAT_OPTS as "TOMCAT_OPTS" (overrides global)
#   - NOT load foo.TOMCAT_OPTS at all
#
# loadEnv() will
#   - load TOMCAT_OPTS
#   - not load any variable that is only in subcats, not in global space
#
# Theoretically, one could load movielens3.TOMCAT_OPTS as both
# movielens3.TOMCAT_OPTS and TOMCAT_OPTS, but Bourne shell doesn't seem to like
# .s in variable names.
#
sub loadEnv {
    my $subCat = shift;

    loadConfig() if !$varsLoaded;

    # Gather up all possible keys for this subCat
    my @these_vars;
    foreach my $avar (keys %var) {
	if ( $avar =~ /([^\.]+)\.(.*)/ ) {
	    # Var with subcat
	    my $this_subcat = $1;
	    my $var = $2;
#	    print "this_subcat: $this_subcat   var $var\n";
	    if (defined($subCat) && ($subCat eq $this_subcat)) {
		push(@these_vars, $var);
	    }
	}
	else {
	    push(@these_vars, $avar);
	}
    }

    foreach my $avar (@these_vars) {
#	print "Put $avar into the environment .. \n";
	$ENV{$avar} = getConfigVar($avar, $subCat);
    }
}

sub substituteFile {
    my $old_filename = shift;
    my $new_filename = shift;
    my $subCat = shift;

    loadConfig() if !$varsLoaded;

    open(FOO, $old_filename) or die "Couldn't open file $old_filename: $!\n";

    my $temp_filename;
    do { 
        $temp_filename = tmpnam(); 
    } until sysopen(TEMPFH, $temp_filename, O_RDWR|O_CREAT|O_EXCL, 0666); 

    while ( <FOO> ) {
	my $newLine = $_;
	if ( /@[^@]+@/g ) {
	    $newLine = substituteString($_, $subCat);
	}
	print TEMPFH $newLine;
    }
    close(FOO);
    close(TEMPFH);

#    print STDERR "Rename $temp_filename to $new_filename\n";
    copy($temp_filename, $new_filename) or die "Couldn't rename $temp_filename to $new_filename: $!\n";
}

sub substituteString {
    my $line = shift;
    my $subCat = shift;

    my @vars = ($line =~ m/@([^@]+)@/g);
    my @newConfigVars = @vars;
    foreach (@newConfigVars) {
	# Maybe undefined var should not be fatal, but make it so for now
	checkConfigVar($_, $subCat);

	my $newVal = getConfigVar($_, $subCat);
#	print STDERR "Substitute '$newVal' for '$_'\n";
	$_ = $newVal if defined($newVal);
    }
#    print "Old line: $line\n";
    for (my $i=0; $i<=$#vars; $i++) {
#	print STDERR "Substitute $vars[$i]  $newConfigVars[$i]\n";
	$line =~ s/\@$vars[$i]\@/$newConfigVars[$i]/;
    }
#    print "New line: $line\n";
    return $line;
}

sub dateStr {
    my $date = `date +"%D %H:%M:%S"`;
    chomp $date;
    return $date;
}

# Keep around logging to email if desired
my $logger_contents = '';
sub setLogToEmail {
    $logToEmail = $_[0];
}

# $_[0] is the subject upon successful exit
# $_[1] is the subject upon unsuccessful exit
sub setEmailSubjects {
    die "Must pass setEmailSubjects an array of two subjects" if int(@_) != 2;
    @emailSubjects = @_;
}

# $_[0] is the recipientsList upon successful exit,
# $_[1] is the recipientsList upon unsuccessful exit
sub setEmailRecipientsLists {
    die "Must pass setEmailRecipientsLists an array of two subjects" if int(@_) != 2;
    @emailRecipientsLists = @_;
}

my $emailAttachment;
sub setEmailAttachment {
    $emailAttachment = $_[0];
}

# This uses previously set variables to send the log
sub _sendEmail {
    if ($logToEmail) {
        glLog ('INFO', "setEmailRecipients() not called\n") if !defined($emailRecipients);
        glLog ('INFO', "setEmailSubject() not called\n") if !defined($emailSubject);
        if (defined($emailRecipients) && defined($emailSubject)) {
            sendEmail($emailRecipients, $emailSubject, $logger_contents, $emailAttachment);
        }
    }
}

# This sends an email
# Parameters:
# recipients   - e.g., 'dfrankow@cs.umn.edu,grphack@cs.umn.edu'
# subject      - e.g., 'hello'
# body         - e.g., 'body of a message'
# attachment   - e.g., 'file.txt' (this is optional)
sub sendEmail {
    my ($recipients, $subject, $body, $attachment) = @_;

    my $msg = new Mail::Send;
    $msg->subject($subject);
    $msg->to($recipients);

    if (defined($attachment)) {
        # Read in attachment
        my $boundary = "====" . time() . "====";
        $msg->set("Content-type", "multipart/mixed; boundary=\"$boundary\"");

        open (F, $attachment) or die "Cannot read $attachment: $!";
        binmode F;
        undef $/;
        my $attachment_body = encode_base64(<F>);
        close F;
	
        $boundary = '--'.$boundary;
        $body = <<END_OF_BODY;
$boundary
Content-Type: text/plain; charset="iso-8859-1"
Content-Transfer-Encoding: quoted-printable

$body
$boundary
Content-Type: application/octet-stream; name="$attachment"
Content-Transfer-Encoding: base64
Content-Disposition: attachment; filename="$attachment"

$attachment_body
$boundary--
END_OF_BODY

    }

#    print "sendEmail: recipients='$recipients', subject='$subject', attachment='$attachment', body:\n $body\n";

    my $fh = $msg->open('sendmail') or print "Couldn't open mailer: $!\n";
    print $fh $body;
    $fh->close() or print "Couldn't close mailer: $!\n";;
}

my $logToStderr = 1;
sub setLogToStderr {
    $logToStderr = $_[0];
}

sub glLog {
    my $level = shift;
    my $logStr = dateStr() . ': ' . "@_" . "\n";
    print STDERR $logStr if $logToStderr;
    $logger_contents .= $logStr if $logToEmail;
}

my $simulateFlag = 0;
sub setSimulateFlag {
    $simulateFlag = shift;
}

sub runCmd {
    my $cmd = shift;
    glLog('INFO', $cmd);
    if (!$simulateFlag) {
        system($cmd);
        my $exit_val = $? >> 8;
        if ($exit_val) {
            my $str = dateStr() . ": Couldn't run $cmd";
            glLog('INFO', $str);
            die $str;
        }
    }
}

#
# Drop tables passed in a ref to an array
#
sub dropTables {
    my ($opt_host, $opt_user, $opt_password, $opt_database, $mysqlbindir, $tablesRef) = @_;

    my $cmd = "|$mysqlbindir/mysql -h $opt_host -u $opt_user --password=$opt_password $opt_database";
    open(FOO, $cmd) or die "Couldn't $cmd: $!\n";
    foreach my $table (@$tablesRef) {
        print FOO "drop table $table;\n";
    }
    close(FOO) or die "Couldn't close $cmd: $!\n";
}

#
#  Get the tables in a particular database
#
sub getTables {
    my ($opt_host, $opt_user, $opt_password, $opt_database, $mysqlbindir) = @_;
    my $cmd = "$mysqlbindir/mysql -h $opt_host -u $opt_user --password=$opt_password -e 'show tables' $opt_database|";
    open(FOO, $cmd) or die "Couldn't $cmd: $!\n";
    my @tables;
    my $skip_header = <FOO>;
    while ( <FOO> ) {
        s/[\r\n]//g;
        push(@tables, $_);
    }
    close(FOO) or die "Couldn't close $cmd: $!\n";
    return @tables;
}


1;
