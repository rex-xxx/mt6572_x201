#!/usr/local/bin/perl
#
#
use strict;

my $ORG_SCATTER = $ARGV[0];
my $NEW_SCATTER = $ARGV[1];
my $style = 0;
my $flag = 0;


open(READ_FP, "< $ORG_SCATTER") or die "Can not open $ORG_SCATTER";
open(WRITE_FP, "> $NEW_SCATTER") or die "Can not open $NEW_SCATTER";

while (<READ_FP>) {
    if ($_ =~ /^{/) {
        $style++;
        next;
    }
    if ($_ =~ /^}/) {
        $style++;
        next;
    }
    if ($_ =~ /^__NODL_BMTPOOL /) {
        next;
    }
    if ($_ =~ /^__NODL_RSV_BMTPOOL /) {
        next;
    }
    if ($_ =~ /^__NODL_RSV_OTP /) {
        next;
    }
    if ($_ =~ /^\n/) {
        next;
    }
    print WRITE_FP "$_";
}

close(READ_FP);
close(WRITE_FP);

#new scatter format
if ($style == 0){
    open(READ_FP, "< $ORG_SCATTER") or die "Can not open $ORG_SCATTER";
    open(WRITE_FP, "> $NEW_SCATTER") or die "Can not open $NEW_SCATTER";

    while (<READ_FP>) {
		chomp $_;
		if (($_ =~ m/BMTPOOL(.*)/) || ($_ =~ m/OTP(.*)/)){
		    $flag++;
			next;
		}
		else{
			if ($_ =~ m/partition_name:(.*)/) {
				my $position = rindex($_, " ")+1;
				my $p_partition =  substr($_, $position)." ";
				print WRITE_FP $p_partition;
				next;
			}
			if ($_ =~ m/linear_start_addr:(.*)/) {
				if($flag==1) {
				    $flag--;
					next;
				}
				else{
					my $position = rindex($_, " ")+1;
					my $p_add =  substr($_, $position)."\n";
					print WRITE_FP $p_add;
					next;
				}
			}
		}
	}
    close(READ_FP);
    close(WRITE_FP);
}
