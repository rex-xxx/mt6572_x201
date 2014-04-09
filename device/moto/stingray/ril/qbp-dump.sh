#!/system/bin/sh
#set -x

###### KNOWN ISSUES ######
# - qbpfs pull is returning $?=0 when it fails
# - AP needs a more deterministic way of detecting the max dumps the BP will
#   store.  Otherwise the script will fail to calculate the rollover back
#   to index 0.

###### FUNCTIONS ######

setupDirs() {
    case $parentExists in
    1)
        outDirRoot=$1
        outDirBranch=$2
        ;;
    0)
        outDirRoot="/data/misc/ril"
        outDirBranch="bp-dump/${timestamp}/qcom"
        cd $outDirRoot
        case $? in 0) ;; *) echo "Couldn\'t cd to $outDirRoot"; exit 1;; esac
        # Set delimiter to slash for separating directories
        OIFS=$IFS; IFS='/'
        for dir in $outDirBranch; do
            mkdir "$dir" 2>/dev/null
            cd "$dir" 2>&1
            case $? in 0) ;; *) echo "Couldn\'t cd to $dir"; exit 1;; esac
            outDirBranchUnwind="$dir $outDirBranchUnwind"
        done
        IFS=$OIFS
        ;;
    esac
    outDir="${outDirRoot}/${outDirBranch}"
}

processLogs() {
    # Initialize communication with the BP
    qbpfs hello
    case $? in 0) ;; *) echo "qbpfs hello failed"; return;; esac

    local maxIndexes=$1
    local nextErrIndex=""
    local indexesToPull=""
    case $maxIndexes in
    0)
        echo "Nothing to do for indexes=0"
        return
        ;;
    1)
        # If we only support one dump, then we can detect when it has finished
        # by first removing the err_index.txt and then watching for it to
        # reappear.
        qbpfs rm "/err/err_index.txt"
        indexesToPull="00"
        nextErrIndex="00"
        ;;
    *)
        # If we support more than one dump, we can watch the index to change.
        qbpfs pull "/err/err_index.txt" "${outDir}/err_index.txt" 2>&1
        local lastErrIndex=`cat ${outDir}/err_index.txt`
        rm "${outDir}/err_index.txt"
        case $maxIndexes in
        2)
            indexesToPull="00 01"
            case $lastErrIndex in
                "") nextErrIndex="00";;
                00) nextErrIndex="01";;
                01) nextErrIndex="00";;
            esac
            ;;
        3)
            indexesToPull="00 01 02"
            case $lastErrIndex in
                "") nextErrIndex="00";;
                00) nextErrIndex="01";;
                01) nextErrIndex="02";;
                02) nextErrIndex="00";;
            esac
            ;;
        4)
            indexesToPull="00 01 02 03"
            case $lastErrIndex in
                "") nextErrIndex="00";;
                00) nextErrIndex="01";;
                01) nextErrIndex="02";;
                02) nextErrIndex="03";;
                03) nextErrIndex="00";;
            esac
            ;;
        *)
            echo "No support for indexes=$maxIndexes"
            return
            ;;
        esac
        ;;
    esac

    # Trigger dump from RAM to EFS
    echo dump_log > /sys/class/radio/mdm6600/command

    local errIndex=""
    # Loop until err_index.txt changes
    for i in 1 2 3 4 5 ; do
        qbpfs ls # This defeats a dir caching bug in the BP
        qbpfs pull "/err/err_index.txt" "${outDir}/err_index.txt" 2>&1
        errIndex=$(cat ${outDir}/err_index.txt)
        rm "${outDir}/err_index.txt"
        case $errIndex in
        $nextErrIndex)
            break
            ;;
        *)
            case $i in
                5) echo "Could not find BP log after $i attempts"; return;;
                *) echo "No change to err_index.txt: $errIndex"; sleep 1;;
            esac
            ;;
        esac
    done

    # Dump err_index.txt
    echo "file:begin:txt:${outDirBranch}/err_index.txt"
    echo "$errIndex"
    echo "file:end:txt:${outDirBranch}/err_index.txt"

    # Do NOT redirect stderr to stdout for bin files.  It may confuse the base64 decoder.
    for i in $indexesToPull ; do
        dataIndexFile="err_data_index${i}_log00.txt"
        qbpfs pull "/err/${dataIndexFile}" "${outDir}/${dataIndexFile}" 2>&1
        case $? in 0) ;; *) echo "qbpfs pull failed for /err/${dataIndexFile}"; continue;; esac
        echo "file:begin:txt:${outDirBranch}/${dataIndexFile}"
        cat "${outDir}/${dataIndexFile}"
        catRes=$?
        echo "file:end:txt:${outDirBranch}/${dataIndexFile}"
        rm "${outDir}/${dataIndexFile}"
        case $catRes in 0) ;; *) echo "cat failed for /err/${dataIndexFile}"; continue;; esac

        binFileExtensions="crs f3 id"
        for ext in $binFileExtensions; do
            binFile="err_f3_index${i}.${ext}"
            qbpfs pull "/err/${binFile}" "${outDir}/${binFile}" 2>&1
            case $? in 0) ;; *) echo "qbpfs pull failed for /err/${binFile}"; continue;; esac
            base64 -e "${outDir}/${binFile}" "${outDir}/${binFile}-base64" 2>&1
            case $? in
            0)
                ;;
            *)
                echo "base64 failed for ${outDir}/${binFile}"
                rm "${outDir}/${binFile}"
                continue
                ;;
            esac
            echo "file:begin:bin:${outDirBranch}/${binFile}"
            cat "${outDir}/${binFile}-base64"
            catRes=$?
            echo "file:end:bin:${outDirBranch}/${binFile}"
            case $catRes in 0) ;; *) "cat failed for ${outDir}/${binFile}-base64";; esac
            rm "${outDir}/${binFile}" "${outDir}/${binFile}-base64"
        done
    done
}

cleanupDirs() {
    case $parentExists in
    0)
        # Cleanup output dirs
        cd $outDir
        case $? in 0) ;; *) echo "Couldn\'t cd to $outDir"; exit 1;; esac
        for dir in $outDirBranchUnwind; do
            cd ..
            rmdir $dir
        done
        ;;
    esac
}

###### MAIN ROUTINE ######

# Generate a timestamp if none passed in from parent script
case $1 in
    "") timestamp=$(date +'%Y-%m-%d_%H-%M-%S'); parentExists=0;;
    *) timestamp=$1; parentExists=1;;
esac

echo "Dumping QCOM BP logs @ $timestamp"
setupDirs $2 $3
processLogs 2
cleanupDirs