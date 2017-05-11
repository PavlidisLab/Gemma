#!/usr/bin/perl

=head1 NAME

chopSoft.pl - reduce a set of SOFT files down to something we can
handle quickly for testing. Original files are renamed *.bak.

=head1 SYNOPSIS

chopsoft  -n 100 -s GSE1632_family.soft.txt.gz GDS244.soft.gz GDS245.soft.gz
 
Requires a "gzip" on the path.

=head1 INPUTS

A "family" file (GPL, GSE) or GDS file (-s) and, if using GSE, the corresponding GDS files. 
This script is too dumb to figure out which files go with which so you have to tell it.

=over 4

=item n

How many probes to preserve

=item s <file>

GSE file

=item k

File with probes to keep (-n is ignored)

=back

=head2 Version

$Id$

=cut

use strict;

# given a GEO SOFT file, remove all but selected probe ids. This is used to make files for testing.

use Getopt::Long;
use Pod::Usage;
my ( $opt_h, $opt_s, $opt_n, $opt_k );
$opt_n = 20;
my $result = GetOptions(
	"n=i" => \$opt_n,
	"s=s" => \$opt_s,
	"h"   => \$opt_h
);

if ( $opt_h || !$opt_s ) {
	pod2usage( -verbose => 1, -exitval => 2 );
}

print STDERR "Opening $opt_s\n";

open( IN, "/usr/bin/gzip -dc $opt_s |" ) or die "Could not open $opt_s";

# initialize keepers.
my %keepers;
$keepers{"ID_REF"}++;
$keepers{"ID"}++;

my $sbase = $opt_s;

$sbase =~ s/\.soft\.gz//;

my $outputSeriesFile = "${sbase}.soft.gz.tmp";
open( OUT, "| /usr/bin/gzip -c > $outputSeriesFile" );

#open (OUT, ">$outputSeriesFile");
my $i = 0;
my $n = 0;
print STDERR "Keeping $opt_n probes\n";
my $inPlatform = 0;
my $printed    = 0;
while (<IN>) {
	if ( ++$i > 0 && $i % 100000 == 0 ) {
		print STDERR "$i lines processed\n";
	}
	if ( $_ =~ /^!Platform_sample_id/ ) {
		next;    # missable
	} elsif ( $_ =~ /^!Platform_series_id/ ) {
		next;    # missable
	} elsif ( $_ =~ /^[\^!\#]/ ) {
		if ( $_ =~ /^!platform_table_begin/ ) {
			$inPlatform++;
			print STDERR "Starting platform at line $i\n";
		} elsif ( $_ =~ /^!platform_table_end/ ) {
			$inPlatform = 0;
			print STDERR "Ending platform at line $i\n";
		}
		print OUT;    # normal annotation lines
		$printed++;
	} elsif ( $_ =~ /^ID/ ) {
		print OUT;
		$printed++;
	} else {
		my @vals  = split "\t", $_;
		my $probe = $vals[0];

		if ($inPlatform) {
			if ( $n < $opt_n && !$keepers{$probe} ) {
				$keepers{$probe}++;
				print STDERR "Keeping $probe, number $n, on line $i, printed $printed lines so far\n";
				$n++;
			} elsif ( $keepers{$probe} ) {
				print STDERR "Seen $probe already \n";
			} else {    # done
				print STDERR "Done taking probes for platform at line $i\n";
				$inPlatform = 0;
				$n          = 0;    # start over on another platform.
			}

		} else {
			$n = 0;
		}

		# data lines.
		if ( $keepers{$probe} ) {
			print OUT;              # includes sample data lines.
			$printed++;
		} elsif ($keepers{uc($probe)}) {
			print OUT;
			$printed++;
		} elsif ($keepers{lc($probe)}) {
			print OUT;
			$printed++;
		}	
		 
		if ( $printed % 1000 == 0 ) {
			print STDERR "Printed $printed lines\n";
		}
	}
}
print STDERR "Printed $printed lines in total\n";
close IN;
close OUT;
rename( $opt_s,            "${opt_s}.bak" );
rename( $outputSeriesFile, $opt_s );

for ( my $i = 0 ; $i < scalar @ARGV ; $i++ ) {
	open( IN, "/usr/bin/gzip -dc $ARGV[$i] |" )
	  or die "Could not open $ARGV[$i]";
	my $sbase = $ARGV[$i];
	$sbase =~ s/\.soft\.gz//;
	$outputSeriesFile = "${sbase}.soft.gz.tmp";
	open( OUT, "| /usr/bin/gzip -c > $outputSeriesFile" );
	my $j = 0;
	while (<IN>) {
		$j++;
		if ( $j > 0 && $j % 20000 == 0 ) {
			print STDERR "$j lines processed\n";
		}
		if ( $_ =~ /^[\^!\#]/ ) {
			print OUT;
		} else {
			my @vals = split "\t", $_;
			my $probe = $vals[0];
			if ( $keepers{$probe} ) {
				print OUT;
			} elsif ($keepers{uc($probe)}) {
				print OUT;
			} elsif ($keepers{lc($probe)}) {
				print OUT;
			}
		}
	}
	close IN;
	close OUT;
	rename( $ARGV[$i], "$ARGV[$i].bak" );
	rename( $outputSeriesFile, $ARGV[$i] );
}
