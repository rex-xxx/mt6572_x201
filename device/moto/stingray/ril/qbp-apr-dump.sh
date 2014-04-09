#!/system/bin/sh
#set -x

###### KNOWN ISSUES ######
# - qbpfs pull is returning $?=0 when it fails

###### FUNCTIONS ######

cleanupDirs() {
    # Cleanup output dirs
    cd $tmpOutDirRoot
    case $? in 0) ;; *) echo "Couldn\'t cd to $tmpOutDirRoot"; exit 2;; esac
    rm -r $outDirBranch
}

setupDirs() {
    cd $tmpOutDirRoot
    case $? in 0) ;; *) echo "Couldn\'t cd to $tmpOutDirRoot"; exit 4;; esac

    mkdir "$outDirBranch" 2>/dev/null
    cd "$outDirBranch"
    case $? in 0) ;; *) echo "Couldn\'t cd to $outDirBranch"; cleanupDirs ; exit 5;; esac

    tmpOutDir="${tmpOutDirRoot}/${outDirBranch}"

    bp_dump_tmp_file="${tmpOutDirRoot}/bp-dump_${timestamp}.txt"
    bp_dump_dest_file="${outDirRoot}/bp-dump_${timestamp}.txt"
}

processLogs() {
    # Initialize communication with the BP
    qbpfs hello
    case $? in 0) ;; *) echo "qbpfs hello failed"; cleanupDirs ; exit 6;; esac

    local errIndex=""

    # Retry until we have an error index to retrieve
    for i in 1 2 3 4 5 ; do
        qbpfs ls # This defeats a dir caching bug in the BP
        qbpfs pull "/err/err_index.txt" "${tmpOutDir}/err_index.txt"

        case $? in 0) ;; *) echo "qbpfs pull failed"; cleanupDirs ; exit 7;; esac

        errIndex=$(cat ${tmpOutDir}/err_index.txt)
        rm "${tmpOutDir}/err_index.txt"

        case $errIndex in
        "")
            case $i in
                5) echo "Could not find BP log after $i attempts"; cleanupDirs ; exit 8;;
                *) echo "Could not find BP log. Retrying...";;
            esac
            ;;
        *)
            # We found an error index
            break
            ;;
        esac
    done

    qbpfs rm "/err/err_index.txt"

    # Dump err_index.txt
    echo "file:begin:txt:${outDirBranch}/err_index.txt" > ${bp_dump_tmp_file}
    echo "$errIndex" >> ${bp_dump_tmp_file}
    echo "file:end:txt:${outDirBranch}/err_index.txt" >> ${bp_dump_tmp_file}

    # Do NOT redirect stderr to stdout for bin files.  It may confuse the base64 decoder.
    dataIndexFile="err_data_index${errIndex}_log00.txt"
    qbpfs pull "/err/${dataIndexFile}" "${tmpOutDir}/${dataIndexFile}" 2>&1
    case $? in
    0)
        echo "file:begin:txt:${outDirBranch}/${dataIndexFile}" >> ${bp_dump_tmp_file}
        cat "${tmpOutDir}/${dataIndexFile}" >> ${bp_dump_tmp_file}
        catRes=$?
        echo "file:end:txt:${outDirBranch}/${dataIndexFile}" >> ${bp_dump_tmp_file}
        rm "${tmpOutDir}/${dataIndexFile}"
        case $catRes in 0) ;; *) echo "cat failed for /err/${dataIndexFile}";; esac
        ;;
    *)
        echo "qbpfs pull failed for /err/${dataIndexFile}";;
    esac

    qbpfs rm "/err/${dataIndexFile}"

    binFileExtensions="crs f3 id"
    for ext in $binFileExtensions; do
        binFile="err_f3_index${errIndex}.${ext}"
        qbpfs pull "/err/${binFile}" "${tmpOutDir}/${binFile}" 2>&1
        case $? in 0) ;; *) echo "qbpfs pull failed for /err/${binFile}"; continue;; esac

        base64 -e "${tmpOutDir}/${binFile}" > "${tmpOutDir}/${binFile}-base64" 2>&1
        case $? in
        0)
            ;;
        *)
            echo "base64 failed for ${tmpOutDir}/${binFile}"
            rm "${tmpOutDir}/${binFile}"
            continue
            ;;
        esac

        echo "file:begin:bin:${outDirBranch}/${binFile}" >> ${bp_dump_tmp_file}
        cat "${tmpOutDir}/${binFile}-base64" >> ${bp_dump_tmp_file}
        catRes=$?
        echo "file:end:bin:${outDirBranch}/${binFile}" >> ${bp_dump_tmp_file}
        case $catRes in 0) ;; *) "cat failed for ${tmpOutDir}/${binFile}-base64";; esac

        qbpfs rm "/err/${binFile}"
        rm "${tmpOutDir}/${binFile}" "${tmpOutDir}/${binFile}-base64"
    done

    # Grant read permissions to the bug report file
    chmod 644 ${bp_dump_tmp_file}

    # Move the bp_dump file from tmp location (/cache) to the correct location
    # Note: "mv" doesn't work here -- produces "Cross-device link" error
    cat ${bp_dump_tmp_file} >> ${bp_dump_dest_file}
    chmod 644 ${bp_dump_dest_file}
    rm ${bp_dump_tmp_file}
}

###### MAIN ROUTINE ######

# Generate a timestamp if none passed in from parent script
timestamp=$(date +'%Y-%m-%d_%H-%M-%S')
tmpOutDirRoot="/cache"
outDirRoot="/data/panicreports"
outDirBranch="${timestamp}"

cd $outDirRoot
case $? in 0) ;; *) echo "Couldn\'t cd to $outDirRoot"; exit 9;; esac # Do not execute if /data/panicreports does not exist

echo "Dumping QCOM BP logs @ $timestamp"

setupDirs
processLogs
cleanupDirs
