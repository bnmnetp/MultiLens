#!/soft/perl5.8/bin/perl -w

use strict;

# Apply this script to catalina.out of a current jrecserver to get:
#
# - # items
# - # cosine model cells
# - # M of memory used
#

my $cells;
my $used_mem;
my $num_items;
while ( <> ) {
    if ( m@Read (\d+) cells, \d+ seconds \(\d+ cells/s\),  total memory \d+M  free memory \d+M  used memory (\d+)M@o ) {
        $cells = $1;
        $used_mem = $2;
    }
    elsif ( /^Read (\d+) cells/ ) {
        $num_items = $1;
    }
    elsif ( /Done loading average rec model/ ) {
        print "$num_items $cells $used_mem\n";
        $num_items = 0;
        $cells = 0;
        $used_mem = 0;
    }
}
