#!/usr/bin/perl
# serviceSecurityCheck.pl. Check for meths missing from serviceSecurityInterceptor objectDefinitionSource list.
# Copyright 2008 The authors
# $Id$

use strict;

# run this script from the top of gemma-mda source tree.

# Get currently mapped meths.
my $securityConfigs = `/usr/bin/find ../gemma-core -name applicationContext-security.xml`;
my @configs = split /\n/g, $securityConfigs;
my $securityConfig = $configs[0];
open(IN, "<$securityConfig");
my %mappedmeths;
while(<IN>) {
	chomp;
	if (/ubic\.gemma\..+\=.+/) {
		$_ =~ s/^\s+//;
		$_ =~ s/\=.*$//;
		#print STDERR "$_\n";
		$mappedmeths{$_}++;
	}
}
close IN;

my $line = `/usr/bin/find . -name *Service.java`;



my @files = split /\n/g, $line;
foreach my $file (@files) {
  # get the package
  my $pkg = `cat $file | grep package`;
  chomp $pkg;
  $pkg =~ s/\cM//;
  $pkg =~ s/package //;
  $pkg =~ s/;//;
 
  next unless $pkg;
  
  # get the clazz
  my $clazz = `cat $file | grep "public interface"`;
  chomp $clazz;
  $clazz =~ s/\cM//;
  $clazz =~ s/public interface //;
  $clazz =~ s/\s{.*//;
  $clazz =~ s/\s*extends.+//;
  next unless $clazz;
  
  my $meth = `cat $file | grep public`;
  chomp $meth;
  $meth =~ s/\cM//;
  my @meths = split /\n/, $meth;
  
  my %seenMethods;
  foreach my $meth (@meths) {
    $meth =~ s/\(.+//; # remove parameters
    $meth =~ s/^.+\s+//; # get just the meth

	next if $meth eq "{";

    next unless $meth;
    
    next if $seenMethods{$meth};
    $seenMethods{$meth}++;
    
    if (!$mappedmeths{"${pkg}.${clazz}.${meth}"}) {
    	print "${pkg}\.${clazz}\.${meth}\=user,admin\n";
    } else {
    	#print STDERR "OK: ${pkg}.${clazz}.${meth}\n";
    } 
  }
}
