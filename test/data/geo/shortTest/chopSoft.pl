#!/usr/bin/perl
use strict;
# given a GEO SOFT file, remove all but selected probe ids. This is used to make files for testing.

# todo: make this customizeable outside code.
my %keepers = (
	       "1007_s_at" => 1,
	       "1053_at" => 1,
	       "117_at" => 1,
	       "121_at" => 1,
	       "1255_g_at" => 1,
	       "1294_at" => 1,
	       "1316_at" => 1,
	       "1320_at" => 1,
	       "1405_i_at" => 1,
	       "1431_at" => 1,
	       "1438_at" => 1,
	       "1487_at" => 1,
	       "1494_f_at" => 1,
	       "1598_g_at" => 1,
	       "160020_at" => 1,
	       "1729_at" => 1,
	       "1773_at" => 1,
	       "177_at" => 1,
	       "179_at" => 1,
	       "1861_at" => 1,
	       "200000_s_at" => 1,
	       "200001_at" => 1,
	       "200002_at" => 1,
	       "200003_s_at" => 1,
	       "200004_at" => 1,
	       "200005_at" => 1,
	       "200006_at" => 1,
	       "200007_at" => 1,
	       "200008_s_at" => 1,
	       "200009_at" => 1,
	       "200010_at" => 1,
	       "ID_REF" =>1,
	       "ID" =>1
	      );


while (<>) {
  if ($_=~ /^!Platform_sample_id/) {
    next;
  } elsif ($_=~ /^!Platform_series_id/) {
    next;
  } elsif ($_ =~ /[\^!\#]/) {
    print;
  } else {
    my @vals = split "\t", $_;
#    print STDERR $vals[0];
    if ($keepers{$vals[0]}) {
      print;
    }
  }
}

