#!/soft/perl5.8/bin/perl -w

use strict;
use Getopt::Long;
my @options;

my $opt_request = 'getrecs';
push(@options, "request=s", \$opt_request);

my $opt_rating_file;
push(@options, "rating-file=s", \$opt_rating_file);

my $opt_user;
push(@options, "user=s", \$opt_user);

my $recEngUrl = 'http://gibson.cs.umn.edu:1613/jrec/servlet/Jrec';

die "Couldn't parse options" if !GetOptions(@options);

die "Must give -rating-file if -rectype is 'getbasketrecs'.\n"
    if ($opt_request eq 'getbasketrecs') && !defined($opt_rating_file);

die "Must give -user if -rectype is 'getrecs'\n"
    if ($opt_request eq 'getrecs') && !defined($opt_user);

my $itemRats = '';
if (defined($opt_rating_file)) {
    open(FOO, $opt_rating_file) or die "Couldn't open $opt_rating_file: $!\n";
    my $header = <FOO>;
    while ( <FOO> ) {
        s/[\r\n]//g;
        my @data = split;
        die "Expected 2 columns, found ", int(@data) if int(@data) != 2;
        my ($movie, $rating) = @data;
        $itemRats .= "\\\&item_rating=$movie,$rating";
    }
    close(FOO);
}

# Read users to get recs for
my $cmd;

if ($opt_request eq 'getbasketrecs') {
    $cmd = "wget -O - $recEngUrl?numrecs=10\\\&request=$opt_request$itemRats |";
}
elsif ($opt_request eq 'getrecs') {
    $cmd = "wget -O - $recEngUrl?userid=$opt_user\\\&numrecs=10\\\&request=$opt_request |";
}
else {
    die "Unknown request $opt_request\n";
}

open(CMD, $cmd) or die "Couldn't $cmd:\n";
while ( <CMD> ) {
    my $movie;
    my $pred;
#    if ( /movie=\"(\d+)\" pred=\"([\d\.]+)\"/ ) {
#        $movie = $1;
#        $pred = $2;
#        print "$opt_user\t$movie\t$pred\n";
#    }
        print;
}

print "\n";
