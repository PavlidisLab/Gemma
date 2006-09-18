#!/usr/bin/perl
=head1 NAME

chopSoft.pl - reduce a set of SOFT files down to something we can
handle quickly for testing. Original files are renamed *.bak.

=head1 SYNOPSIS

chopsoft  -n 100 -s GSE1632_family.soft.txt.gz GDS244.soft.gz

Important limitation: This cannot correctly deal with many-to-one GDS to
 GSE relationships (though this could be fixed).
 
Requires a "gzip" on the path.

=head1 INPUTS

A "family" file (GPL, GSE) or GDS file (-s) and, if using GSE, the corresponding GDS files. 
This script is too dumb to figure out which files go with which so you have to tell it.

=over 4

=item B n

How many probes to preserve

=item B s <file>

GSE file

=item B k

File with probes to keep (-n is ignored)

=back

=head2 Version

$Id$

=cut

use strict;
# given a GEO SOFT file, remove all but selected probe ids. This is used to make files for testing.

use Getopt::Long;
use Pod::Usage;
my ($opt_h, $opt_s, $opt_n, $opt_k);
$opt_n = 20;
my $result = GetOptions("n=i" => \$opt_n,
			 "s=s" => \$opt_s,   
			 "h" => \$opt_h);
			 
if ($opt_h || !$opt_s) {
   pod2usage(-verbose=>1, -exitval=>2);
}

print STDERR "Opening $opt_s\n";

open (IN, "gzip -dc $opt_s |") or die "Could not open $opt_s";

# initialize keepers.
my %keepers;
$keepers{"ID_REF"}++;
$keepers{"ID"}++;


my $sbase = $opt_s;

$sbase =~ s/\.soft\.gz//;
my $outputSeriesFile = "${sbase}.soft.gz.tmp";
open (OUT, "| gzip -c > $outputSeriesFile");

my $i = 0;
my $n = 0;
print STDERR "Keeping $opt_n probes\n";
my $inPlatform = 0;
while(<IN>) {
  $i++;
  if ($i > 0 && $i % 20000 == 0) {
    print STDERR "$i lines processed\n";
  }
  if ($_=~ /^!Platform_sample_id/) {
    next;  # missable
  } elsif ($_=~ /^!Platform_series_id/) {
    next; # missable
  } elsif ($_ =~ /[\^!\#]/) {

    if ($_ =~ /^!platform_table_begin/) {
      $inPlatform  = 1;
      print STDERR "Starting platform\n";
    } elsif ($_ =~ /^!platform_table_end/) {
      $inPlatform = 0;
      print STDERR "Ending platform\n";
    }

    print OUT; # normal annotation lines
  } elsif ($_ =~ /^ID/) {
    print OUT;
  } else {
    my @vals = split "\t", $_;
    my $probe = $vals[0];

    if ($inPlatform && $n < $opt_n && !$keepers{$probe}) {
      $keepers{$probe}++;
      $n++;
    } else { # done
      $inPlatform = 0;
      $n = 0; # start over on another platform.
    }

    if ($keepers{$probe}) {
      print OUT; # includes sample data lines.
    }
  }
}
close IN;
close OUT;
rename($opt_s, "${opt_s}.bak");
rename($outputSeriesFile, $opt_s);

for(my $i = 0; $i < scalar @ARGV; $i++) {
  open (IN, "gzip -dc $ARGV[$i] |") or die "Could not open $ARGV[$i]";  
  my $sbase = $ARGV[$i];
  $sbase =~ s/\.soft\.gz//;
  $outputSeriesFile = "${sbase}.soft.gz.tmp";
  open (OUT, "| gzip -c > $outputSeriesFile");
  my $j = 0;
  while(<IN>) {
    $j++;
    if ($j > 0 && $j % 5000 == 0) {
      print STDERR "$j lines processed\n";
    }
    if ($_ =~ /[\^!\#]/) {
      print OUT;
    } else {
      my @vals = split "\t", $_;
      my $probe = $vals[0];
      if ($keepers{$probe}) {
	print OUT;
      }
    }
  }
  close IN;
  close OUT;
  rename($ARGV[$i], "$ARGV[$i].bak");
  rename($outputSeriesFile, $ARGV[$i]);
}
