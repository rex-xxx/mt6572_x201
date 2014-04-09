import sys, os, fnmatch, re, shutil

###################################################
# Global Constants
###################################################
PROJECT_ROOT_DIR = "packages/apps/Phone"
PROJECT_BUILD_DIR = PROJECT_ROOT_DIR + "/build"
MANIFEST_TEMPLATE = "AndroidManifest.tpl"
MANIFEST_FILENAME = "AndroidManifest.xml"
ADD_ON_MANIFEST_FILENAME = "AndroidManifestAddOn.xml"
PLACEHOLDER_TEXT = "<!-- BLUEANGEL::PLACEHOLDER -->"
MODULE_BEG_PATTERN = re.compile(".*BLUEANGEL::IF\s*(\S+)\s*=\s*(\S+).*")
MODULE_END_PATTERN = re.compile(".*BLUEANGEL::FI.*")

ADDON_MODULE_BEG_PATTERN = re.compile(".*BLUEANGEL_ADDON::IF.*")
ADDON_MODULE_END_PATTERN = re.compile(".*BLUEANGEL_ADDON::FI.*")

###################################################
# Function
###################################################

# sarch file(s) under given directory (e.g. search for all "AndroidManifest.xml")
def find_files(directory, pattern):
	for root, dirs, files in os.walk(directory):
		for basename in files:
			if fnmatch.fnmatch(basename, pattern):
				filename = os.path.join(root, basename)
				yield filename

def copy_template_without_previous_addon(filename):
	moduleFile = open(filename, "r")
	moduleFileLines = moduleFile.readlines()
	moduleFile.close()
	moduleContent = []
	isInserting = True
	for line in moduleFileLines:
		# check if inserting mode beg
		if line.lstrip().startswith("<!--"):
			beg = ADDON_MODULE_BEG_PATTERN.match(line)
			if beg:
					isInserting = False
		# inserting and not to end
		if isInserting:
			moduleContent.append(line)
			continue
		# check if inserting mode end
		if (not isInserting) and line.lstrip().startswith("<!--"):
			checknext = ADDON_MODULE_END_PATTERN.match(line)
			if checknext:
				isInserting = True
	return ''.join(moduleContent)
	
# parsing module manifest xml file according feature-option and return activated content
def parse_module_file(filename):
	moduleFile = open(filename, "r")
	moduleFileLines = moduleFile.readlines()
	moduleFile.close()
	moduleContent = []
	isInserting = False
	for line in moduleFileLines:
		# check if inserting mode end
		if isInserting and line.lstrip().startswith("<!--"):
			end = MODULE_END_PATTERN.match(line)
			if end:
				isInserting = False
		# inserting and not to end
		if isInserting:
			moduleContent.append(line)
			continue
		# check if inserting mode beg
		if line.lstrip().startswith("<!--"):
			beg = MODULE_BEG_PATTERN.match(line)
			if beg:
			#	if featureOptions.get(beg.group(1)) == beg.group(2):
				if os.environ[beg.group(1)] == beg.group(2):
					isInserting = True
	return ''.join(moduleContent)

###################################################
# Main Script
###################################################
# 1. copy AndroidManifest.xml template
#if not os.path.exists(os.path.join(PROJECT_BUILD_DIR, MANIFEST_TEMPLATE)):
shutil.copyfile(os.path.join(PROJECT_ROOT_DIR, MANIFEST_FILENAME),os.path.join(PROJECT_BUILD_DIR, MANIFEST_TEMPLATE))

#Remove previously added component if any
templateContent = ""
templateContent = copy_template_without_previous_addon(os.path.join(PROJECT_BUILD_DIR, MANIFEST_TEMPLATE))

# 2. compose all modules' AndroidManifest.xml content according to feature option
moduleContent = []
for moduleFile in find_files(PROJECT_BUILD_DIR, ADD_ON_MANIFEST_FILENAME):
	moduleContent.append(parse_module_file(moduleFile))

# 3. read project manifest template
#templateFile = open(os.path.join(PROJECT_BUILD_DIR, MANIFEST_TEMPLATE), "r")
#templateContent = templateFile.read()
#templateFile.close()

# 4. compose project manifest content
manifestContent = templateContent.replace(PLACEHOLDER_TEXT, "".join(moduleContent))

# 5. remove & write out project manifest file
manifestPath = os.path.join(".", PROJECT_ROOT_DIR, MANIFEST_FILENAME)
os.remove(manifestPath)
manifestFile = open(manifestPath, "w")
manifestFile.write(manifestContent)
manifestFile.close()

###################################################
# End of File
###################################################