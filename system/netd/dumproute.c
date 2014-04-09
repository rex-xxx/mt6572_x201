/* vi: set sw=4 ts=4: */
/* route
 *
 * Similar to the standard Unix route, but with only the necessary
 * parts for AF_INET and AF_INET6
 *
 * Bjorn Wesen, Axis Communications AB
 *
 * Author of the original route:
 *              Fred N. van Kempen, <waltje@uwalt.nl.mugnet.org>
 *              (derived from FvK's 'route.c     1.70    01/04/94')
 *
 * Licensed under GPLv2 or later, see file LICENSE in this source tree.
 *
 *
 * displayroute() code added by Vladimir N. Oleynik <dzo@simtreas.ru>
 * adjustments by Larry Doolittle  <LRDoolittle@lbl.gov>
 *
 * IPV6 support added by Bart Visscher <magick@linux-fan.com>
 */

/* 2004/03/09  Manuel Novoa III <mjn3@codepoet.org>
 *
 * Rewritten to fix several bugs, add additional error checking, and
 * remove ridiculous amounts of bloat.
 */

#include <netinet/in.h>
#include <netdb.h>
#include <sys/socket.h>
#include <net/if.h>
#include <stdio.h>

#define LOG_TAG "CommandListener_"

#include <cutils/log.h>




#ifndef RTF_UP
/* Keep this in sync with /usr/src/linux/include/linux/route.h */
#define RTF_UP          0x0001	/* route usable                 */
#define RTF_GATEWAY     0x0002	/* destination is a gateway     */
#define RTF_HOST        0x0004	/* host entry (net otherwise)   */
#define RTF_REINSTATE   0x0008	/* reinstate route after tmout  */
#define RTF_DYNAMIC     0x0010	/* created dyn. (by redirect)   */
#define RTF_MODIFIED    0x0020	/* modified dyn. (by redirect)  */
#define RTF_MTU         0x0040	/* specific MTU for this route  */
#ifndef RTF_MSS
#define RTF_MSS         RTF_MTU	/* Compatibility :-(            */
#endif
#define RTF_WINDOW      0x0080	/* per route window clamping    */
#define RTF_IRTT        0x0100	/* Initial round trip time      */
#define RTF_REJECT      0x0200	/* Reject route                 */
#endif

static const unsigned flagvals[] = { /* Must agree with flagchars[]. */
	RTF_GATEWAY,
	RTF_HOST,
	RTF_REINSTATE,
	RTF_DYNAMIC,
	RTF_MODIFIED,
#if ENABLE_FEATURE_IPV6
	RTF_DEFAULT,
	RTF_ADDRCONF,
	RTF_CACHE
#endif
};

#define IPV4_MASK (RTF_GATEWAY|RTF_HOST|RTF_REINSTATE|RTF_DYNAMIC|RTF_MODIFIED)
#define IPV6_MASK (RTF_GATEWAY|RTF_HOST|RTF_DEFAULT|RTF_ADDRCONF|RTF_CACHE)

/* Must agree with flagvals[]. */
static const char flagchars[] =
	"GHRDM"
#if ENABLE_FEATURE_IPV6
	"DAC"
#endif
;

static void set_flags(char *flagstr, int flags)
{
	int i;

	*flagstr++ = 'U';

	for (i = 0; (*flagstr = flagchars[i]) != 0; i++) {
		if (flags & flagvals[i]) {
			++flagstr;
		}
	}
}

int displayRoutes( )
{
    char devname[64], flags[16];
    unsigned long d, g, m;
    int flgs, ref, use, metric, mtu, win, ir;
    struct in_addr mask, dest, gw;
	FILE *fp =NULL ;

   ALOGI("DumpRoute ++");
   fp = fopen("/proc/net/route", "r");
        if(fp == NULL) {           
			ALOGE("DumpRoute -- : open file fail");
			return -2 ;
        }	

	ALOGI( "Destination     Gateway         Genmask         Flags %s Iface\n",
		   "Metric Ref    Use");

	if (fscanf(fp, "%*[^\n]\n") < 0) { /* Skip the first line. */
		goto ERROR;		   /* Empty or missing line, or read error. */
	}
	while (1) {
		int r;
		r = fscanf(fp, "%63s%lx%lx%X%d%d%d%lx%d%d%d\n",
                   devname, &d, &g, &flgs, &ref, &use, &metric, &m, &mtu, &win, &ir);
		if (r != 11) {
			if ((r < 0) && feof(fp)) { /* EOF with no (nonspace) chars read. */
				break;
            ALOGE("[displayRoutes] fscanf incorrect bytes");
			}
 ERROR:
            fclose(fp);
			ALOGE("DumpRoute -- :fscanf I/O Error");
			return -1 ;
		}

		if (!(flgs & RTF_UP)) { /* Skip interfaces that are down. */
			continue;
		}

		set_flags(flags, (flgs & IPV4_MASK));
#ifdef RTF_REJECT
		if (flgs & RTF_REJECT) {
			flags[0] = '!';
		}
#endif

        dest.s_addr = d;
        gw.s_addr = g;
        mask.s_addr = m;
	char *dest_ptr = strdup(inet_ntoa(dest));
	char *gw_ptr =  strdup(inet_ntoa(gw));
        char *mask_prt = strdup(inet_ntoa(mask));
        /* "%15.15s" truncates hostnames, do we really want that? */        
        ALOGI("%-15.15s %-15.15s %-16s%-6s%-6d %-2d %7d %s", 
                dest_ptr, gw_ptr, mask_prt, flags, metric, ref, use, devname);

        if(NULL != dest_ptr)
            free(dest_ptr);
        if(NULL != gw_ptr)
            free(gw_ptr);
        if(NULL != mask_prt)
            free(mask_prt);

    }
    fclose(fp);
	ALOGI("DumpRoute --");
	return 0 ;
}


