#!/usr/bin/perl
# serviceSecurity.pl. Output basic starting serviceSecurityInterceptor objectDefinitionSource list.
# Copyright 2005 The authors
# $Id$

use strict;

# run this script from the top of the source tree.
my $line = `/usr/bin/find . -name *Service.java`;
my @files = split /\n/g, $line;
foreach my $file (@files) {
  # get the package
  my $package = `cat $file | grep package`;
  chomp $package;
  $package =~ s/package //;
  $package =~ s/;//;


  # get the class
  my $class = `cat $file | grep "public interface"`;
  chomp $class;
  $class =~ s/public interface //;
  $class =~ s/ {//;

  next unless $class;

  my $method = `cat $file | grep public`;
  my @methods = split /\n/, $method;
  foreach my $method (@methods) {
    $method =~ s/\(.+//; # remove paramters
    $method =~ s/^.+\s+//; # get just the method
    print "$package.$class.$method=user,admin\n";
  }
}
