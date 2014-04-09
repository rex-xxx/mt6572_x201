#!/system/bin/sh

mprTempDir="$1"
case $mprTempDir in
    "") echo "Usage: $0 <destination dir>"; exit 1;;
esac
for cmd in "state" "-d logs" "files" "-d panic" "atvc"; do
    echo "$cmd" | nc 192.168.20.2 3002 | extract-embedded-files -o "$mprTempDir"
done
