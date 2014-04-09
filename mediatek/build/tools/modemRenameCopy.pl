#!/usr/bin/perl

my ($input_path, $project) = @ARGV;

usage() if ($#ARGV < 1);

die "Cannot find modem path $input_path\n" if (!-d $input_path);

my $dest_folder = $input_path . "/temp_modem";

print "[LOG] Delete and re-make directory $dest_folder...\n\n";

`rm -r $dest_folder` and die "Cannot delete folder $dest_folder\n" if (-d $dest_folder);
`mkdir -p $dest_folder` and die "Cannot create folder $dest_folder\n" if (!-d $dest_folder);

# Check whether "build" folder exists or not
die "[FAIL] $input_path/build doesn't exist!" if (! -e "$input_path/build");

my $flavor = "DEFAULT"; # default flavor is "DEFAULT"

if ($project =~ /(.*)\((.*)\)/) {
	$flavor = $2;
	$projectNoFlavor = $1;
}

my $flavorUC = uc ($flavor);
my $projectNoFlavorUC = uc ($projectNoFlavor);
my $projectUC = uc ($project);

my $modem_bin_file = `find ${input_path}/build/${projectNoFlavorUC}/${flavorUC}/bin/ -name "*_PCB01_*.bin"`;
my $database_file = `find ${input_path}/build/${projectNoFlavorUC}/${flavorUC}/tst/database/ -name "BPLGUInfoCustom*"`;
my $dbginfo_file = `find ${input_path}/build/${projectNoFlavorUC}/${flavorUC}/tst/database/ -name "DbgInfo*"`;
my $modem_make_file = "${input_path}/build/${projectNoFlavorUC}/${flavorUC}/bin/${projectUC}.mak";
my $catcher_filter_bin_file = "${input_path}/build/${projectNoFlavorUC}/${flavorUC}/tst/database/catcher_filter.bin";
my $mcddll_dll_file = "${input_path}/build/${projectNoFlavorUC}/${flavorUC}/tst/database/mcddll.dll";

chomp ($modem_bin_file);
chomp ($database_file);
chomp ($dbginfo_file);
chomp ($modem_make_file);
chomp ($catcher_filter_bin_file);
chomp ($mcddll_dll_file);

my $debugFlag = 0;

if ($debugFlag == 1) {
	print "[DEBUG] \$flavorUC = $flavorUC\n";
	print "[DEBUG] \$projectUC = $projectUC\n";
	print "[DEBUG] \$modem_bin_file = $modem_bin_file\n";
	print "[DEBUG] \$database_file = $database_file\n";
	print "[DEBUG] \$dbginfo_file = $dbginfo_file\n";
	print "[DEBUG] \$modem_make_file = $modem_make_file\n";
	print "[DEBUG] \$catcher_filter_bin_file = $catcher_filter_bin_file\n";
	print "[DEBUG] \$mcddll_dll_file = $mcddll_dll_file\n";
}

my $length1 = 188; # modem image rear
my $length2 = 172;
my $whence = 0;

my $suffix = &Calc_Suffix ("$modem_bin_file");

print "[INFO] The renaming suffix is $suffix!\n\n";

sub Get_Basename {
	my ($tmpName) = @_;
	my $baseName = $1 if ($tmpName =~ /.*\/(.*)/);
	return $baseName;
}

my $tmp = &Get_Basename ("$modem_make_file");

my $database_file_name = &Get_Basename ("$database_file");
my $dbginfo_file_name = &Get_Basename ("$dbginfo_file");
my $modem_make_file_after = &Bracket_Handle($modem_make_file);

my %mapping_table = (
	"$modem_bin_file" => "modem${suffix}.img",
	"$database_file" => "${database_file_name}${suffix}",
	"$dbginfo_file" => "${dbginfo_file_name}${suffix}",
	"$catcher_filter_bin_file" => "catcher_filter${suffix}.bin",
	"$modem_make_file_after" => "modem${suffix}.mak",
	"$mcddll_dll_file" => "mcddll${suffix}.dll",
);

foreach my $curFile (keys %mapping_table) {
	print "[Info] Copy ${curFile} as ${dest_folder}/$mapping_table{$curFile}\n";
	`cp -f ${curFile} ${dest_folder}/$mapping_table{$curFile} 2>/dev/null` and warn "[Warning] Cannot copy ${input_path}/${curFile}\n";
}


print "\n\n[NOTICE]\n";
print "The modem files were renamed and copied to folder [\"${dest_folder}\"].\n";
print "You can start using them in your Smartphone developement.\n\n";

exit;

sub Bracket_Handle {
	my ($tmp_name) = @_;
	$tmp_name =~ s/\(/\\\(/g; # ( => \(
	$tmp_name =~ s/\)/\\\)/g; # ) => \)
	return $tmp_name;
}

sub Calc_Suffix {
	my ($modem_file) = @_;
	my $naming_string = "";
	die "The file \"$modem_file\" does NOT exist!\n" if (!-e $modem_file);

	my ($MD_IMG_MODE, $MD_HEADER_VERNO, $MD_IMG_PLATFORM, $MD_IMG_SERIAL_NO) = &Parse_MD($modem_file);
	$MD_IMG_PLATFORM =~ s/_(.*)//;

	if ($MD_HEADER_VERNO == 2) { # "*_x_yy_z"
		if ($MD_IMG_SERIAL_NO == 1) {
			$naming_string = "_1";
		} elsif ($MD_IMG_SERIAL_NO == 2) {
			$naming_string = "_2";
		} else {
			die "Invalid modem serial number! Should be 1 or 2!\n";
		}
		$naming_string .= &Mode_Trans($MD_IMG_MODE);
		$naming_string .= "_n";	# Temp: all images are "n"	
	} elsif ($MD_HEADER_VERNO == 1) {
		$naming_string = "_sys2" if ($MD_IMG_SERIAL_NO == 2);
	} else {
		die "Invalid modem header verno: $MD_HEADER_VERNO!\n";
	}
	return $naming_string;	 
}

sub Mode_Trans {
	my ($tmp_mode) = @_;
	my $tmp_string = "";

	if ($tmp_mode == 1) {
		$tmp_string = "_2g";
	} elsif ($tmp_mode == 3) {
		$tmp_string = "_wg";
	} elsif ($tmp_mode == 4) {
		$tmp_string = "_tg";
	} else {
		die "Invalide modem mode! Should be 1 / 3 / 4!\n";
	}
	return $tmp_string;
}

sub Parse_MD {
	my $tmp_header_str;
	my $tmp_inutility1;
	my $tmp_debug_mode;
	my $tmp_mode;
	my $tmp_platform;
	my $tmp_inutility2;
	my $tmp_project_id;
	my $tmp_serial_num;
	
	my $buffer;

	my ($parse_md_file) = @_;
	my $md_file_size = -s $parse_md_file;

	open(MODEM, "< $parse_md_file") or die "Can NOT open file $parse_md_file\n";
	binmode(MODEM);
	seek(MODEM, $md_file_size - $length1, $whence) or die "Can NOT seek to the position of modem image rear in \"$parse_md_file\"!\n";
	read(MODEM, $buffer, $length2) or die "Failed to read the rear of the file \"$parse_md_file\"!\n";
	($tmp_header_str, $tmp_inutility1, $tmp_debug_mode, $tmp_mode, $tmp_platform, $tmp_inutility2, $tmp_project_id, $tmp_serial_num) = unpack("A12 L L L A16 A64 A64, L", $buffer);
	die "Reading from MODEM failed! No CHECK_HEADER info!\n" if ($tmp_header_str ne "CHECK_HEADER") ;
	close(MODEM);
	return ($tmp_mode, $tmp_inutility1, $tmp_platform, $tmp_serial_num);
}


sub usage
{
	print <<"__EOFUSAGE";

Usage:
	$0 [Modem Codebase Path] [Modem Project Makefile Name] 

Description:

	The script is to automatically copy modem files which are needed in Smartphone development (including modem image, database, makefile, etc.) with defined renaming rule.
	The files will be copied to a designated folder for you to use. 

Example:
	$0 ~/currUser/Modem/mcu MT6572_DEMO_HSPA

__EOFUSAGE
	exit 1;
}

