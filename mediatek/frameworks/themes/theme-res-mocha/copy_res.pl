#!/usr/local/bin/perl
use Cwd;
my @ignores = ("assets");
my $root = shift @ARGV;
my @checks = ("drawable",);

opendir(DIR, "$root") or die "Cannot open: $!\n";

my @folders = readdir(DIR);
closedir(DIR);

chdir($root) or die "Cannot chdir : $!\n";
$root = getcwd();


 foreach $folder (@folders) {
    if ($folder eq "res") {
    opendir(DIR, "$folder") or die "Cannot open: $!\n"; 
    @checks = readdir(DIR);    
    closedir(DIR);
    shift(@checks);
    shift(@checks);      
    }
 }

 foreach $folder (@folders) {
    if (substr($folder, 0, 1) eq ".") {next;}
    $sign = 0;
    foreach $ignore (@ignores) {if ($ignore eq $folder) {$sign = 1; last;}}
    if ($sign eq 1) {next;}
    if ($folder eq "res") {next;}
    if ($folder eq "copy_res.pl") {next;}
    if ($folder eq "AndroidManifest.xml") {next;}
    if ($folder eq "Android.mk") {next;}
    my $mkdir = $folder."/"."res";
    opendir(DIR, "$mkdir") or die "Cannot open: $!\n";
    @mkdirinres = readdir(DIR);
    foreach $mkdirinres (@mkdirinres){
        if (substr($mkdirinres, 0, 1) eq ".") {next;}
        if (substr($mkdirinres, 0, 8) ne "drawable") {next;}
        my $checkinres = (grep/^$mkdirinres$/,@checks);       
        if(!$checkinres){
        push(@checks,$mkdirinres);
        $command="mkdir ".$root."/"."res"."/".$mkdirinres;     
        readpipe($command);
        }
     } 
    closedir(DIR);   
  }

my $delete = $root."/"."res";
foreach $check (@checks) {
  my $delete = $root."/"."res"."/".$check;
  opendir(DIR, "$delete") or die "Cannot open: $!\n";
  @files = readdir(DIR);
  foreach $folder (@folders) {
    if (substr($folder, 0, 1) eq ".") {next;}
    $sign = 0;
    foreach $ignore (@ignores) {if ($ignore eq $folder) {$sign = 1; last;}}
    if ($sign eq 1) {next;}
    foreach $file (@files) {
      if (substr($file, 0, length($folder) + 1) eq $folder."_") {
 #       print $delete."/".$file."\n";
        $command = "rm -rf ".$delete."/".$file;
        readpipe($command);
      }
    }
  }
  closedir(DIR);
}

foreach $check (@checks) {
  my $destdir = $root."/res/".$check;
  foreach $folder (@folders) {
    if (substr($folder, 0, 1) eq ".") {next;}
    $sign = 0;
    foreach $ignore (@ignores) {if ($ignore eq $folder) {$sign = 1; last;}}
    if ($sign eq 1) {next;}
    if (-d $folder."/res/".$check) {
      #chdir($root) or die "Cannot chdir : $!\n";
      my $current = $root."/".$folder."/res/".$check;
      opendir(DIR, "$current") or die "Cannot open: $!\n";
      @files = readdir(DIR);
      foreach $file (@files) {
        if (substr($file, 0, 1) eq ".") {next;}
        $copy = $current."/".$file;
        $dest = $destdir."/".$folder."_".$file;
        $command = "cp ".$copy." ".$dest;
        readpipe($command);
      }
      closedir(DIR);
    }
  }
}
