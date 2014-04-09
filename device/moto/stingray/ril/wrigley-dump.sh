#!/system/bin/sh

# Don't waste time trying to dump logs for a device that doesn't exist.
isWrigleyFound=0
for dev in $(ls /sys/bus/usb/devices); do
    case $(cat /sys/bus/usb/devices/$dev/product 2>/dev/null) in
    "Motorola LTE Datacard")
        isWrigleyFound=1
        break
    ;;
    esac
done
case $isWrigleyFound in 0) echo "Skipping dump for non-existent Wrigley"; exit 1;; esac

echo "Dumping Wrigley logs"
echo "I am running as $(id)"
hostName=$(getprop ro.product.device)

# The host-side iptables are important for logging functionality, and
# they are not currently part of the AP bugreport.
echo "file:begin:txt:${hostName}-iptables.txt"
iptables -L
iptables -t nat -L
echo "file:end:txt:${hostName}-iptables.txt"

# The host-side IPv6 routes are important, and they are not currently
# part of the AP bugreport.
echo "file:begin:txt:/${hostName}-ipv6_route.txt"
cat /proc/net/ipv6_route
echo "file:end:txt:/${hostName}-ipv6_route.txt"

# Get the rest of the info from dumpd.  Specify a timeout to netcat to ensure
# we don't hang forever on BP panics.  The -w option in netcat is used both as
# a connect timeout and a timeout on the main select() loop when doing reads
# and writes.  Use a value that is not likely to false-positive, but also
# provides adequate protection against hanging for a long time.
for cmd in "state" "files" "atvc"; do
    echo "-o wrigley $cmd" | nc -w 10 192.168.20.2 3002
done
echo "file:begin:txt:wrigley/AdditionalExtractionInstructions.txt"
echo "To extract logs.gz and panic.gz, you must do the following:"
echo "    gunzip logs.gz"
echo "    extract-embedded-files logs"
echo "    gunzip panic.gz"
echo "    extract-embedded-files panic"
echo ""
echo "These additional steps are required, because we are dumping an enormous"
echo "amount of data into the bugreport.  Google demanded we reduce the overall"
echo "number of lines we are generating.  To accomplish the reduction, we are"
echo "doing compression prior to dumping into the bugreport.  We are sorry for"
echo "the inconvenience."
echo "file:end:txt:wrigley/AdditionalExtractionInstructions.txt"
for cmd in "logs" "panic"; do
    echo "file:begin:bin:wrigley/${cmd}.gz"
    echo "$cmd" | nc -w 10 192.168.20.2 3002 | gzip | base64 -e
    echo "file:end:bin:wrigley/${cmd}.gz"
done
