#!/soft/perl5.8/bin/perl -w

#
# This script parses the email of the model building cron job
# in order to pull out:
#
# date #-cosine-model-cells #-average-model-cells
#
# Note that #-average-model-cells == #-items
#
# USAGE: ./cells.pl /var/mail/a_user > cells.txt
#

use strict;

my %month_map = ( 'Jan' => '01',
                  'Feb' => '02',
                  'Mar' => '03',
                  'Apr' => '04',
                  'May' => '05',
                  'Jun' => '06',
                  'Jul' => '07',
                  'Aug' => '08',
                  'Sep' => '09',
                  'Oct' => '10',
                  'Nov' => '11',
                  'Dec' => '12' );

my $the_date;
my $printed_cos_cells = 0;
my $cos_cells;

while ( <> ) {
    # This text appears in the intermediate and final cosine rec cook
    if (/Wrote out (\d+)/ ) {
        my $num = $1;
        if (($num % 1000000) != 0) {
            # This is not an even multiple of 1,000,000, hence probably
            # the final calculation
            #print "$the_date $1 ";
            $cos_cells = $1;
        }
    }

    # This text appears in the final average cook
    if (/Wrote (\d+) cells/) {
        if ($1 > 100000) {
#            print "$the_date $1 ";
            $cos_cells = $1;
        }
        if ($1 < 100000) {
            if ($cos_cells > 0) {
                print "$the_date $cos_cells $1\n";
            }
            $cos_cells = 0;
        }
    }

    if ( m#(\d\d/\d\d/\d\d) (\d\d:\d\d:\d\d)# ) {
         $the_date = $1;
    }

    if ( /(Mon|Tue|Wed|Thu|Fri|Sat|Sun) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Nov|Dec) (\d\d) \S+ \S+ \d\d(\d\d)/ ) {
        $the_date = $month_map{$2} . '/' . $3 . '/'. $4;
        #print "$the_date\n";
    }
}
