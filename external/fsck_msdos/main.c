/*
 * Copyright (C) 1995 Wolfgang Solfrank
 * Copyright (c) 1995 Martin Husemann
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by Martin Husemann
 *	and Wolfgang Solfrank.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#include <sys/cdefs.h>
#ifndef lint
__RCSID("$NetBSD: main.c,v 1.10 1997/10/01 02:18:14 enami Exp $");
static const char rcsid[] =
  "$FreeBSD: src/sbin/fsck_msdosfs/main.c,v 1.16 2009/06/10 19:02:54 avg Exp $";
#endif /* not lint */

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <stdarg.h>

#include "fsutil.h"
#include "ext.h"

int alwaysno;		/* assume "no" for all questions */
int alwaysyes;		/* assume "yes" for all questions */
int preen;		/* set when preening */
int rdonly;		/* device is opened read only (supersedes above) */
int skipclean;		/* skip clean file systems if preening */
int debugmessage;	/* print verbose message for debugging */

static void usage(void);

static void
usage(void)
{

	fprintf(stderr, "%s\n%s\n",
	    "usage: fsck_msdosfs -p [-f] filesystem ...",
	    "       fsck_msdosfs [-ny] filesystem ...");
	exit(1);
}

int
main(int argc, char **argv)
{
	int ret = 0, erg;
	int ch;

	skipclean = 1;
	debugmessage = 0;
	printf("The fsck_msdos options input by caller :\n") ;
	while ((ch = getopt(argc, argv, "CfFnpyd")) != -1) {
		switch (ch) {
		case 'C': /* for fsck_ffs compatibility */
			printf(" -C\n") ;
			break;
		case 'f':
			printf(" -f\n") ;
			skipclean = 0;
			break;
		case 'F':
			/*
			 * We can never run in the background.  We must exit
			 * silently with a nonzero exit code so that fsck(8)
			 * can probe our support for -F.  The exit code
			 * doesn't really matter, but we use an unusual one
			 * in case someone tries -F directly.  The -F flag
			 * is intentionally left out of the usage message.
			 */
			printf(" -F\n") ; 
			exit(5);
		case 'n':
			alwaysno = 1;
			alwaysyes = preen = 0;
			printf(" -n\n") ;
			break;
		case 'y':
			alwaysyes = 1;
			alwaysno = preen = 0;
			printf(" -y\n") ;
			break;

		case 'p':
			preen = 1;
			alwaysyes = alwaysno = 0;
			printf(" -p\n") ;
			break;
		case 'd':
			debugmessage = 1;
			printf(" -d\n") ;
			break;

		default:
			usage();
			break;
		}
	}
	argc -= optind;
	argv += optind;

	if (!argc)
		usage();

	start_count(&fsck_total_time) ;
	while (--argc >= 0) {
//		setcdevname(*argv, preen);
		erg = checkfilesys(*argv++);
		if (erg > ret)
			ret = erg;
	}
	end_count("Total fsck time", &fsck_total_time) ;

	printf("\n=== FSCK Time Usage ===\n") ;
	printf("Phase#1 tooks %lu usecs.\n", print_time(&fsck_p1_time)) ;
	printf("Phase#2 tooks %lu usecs.\n", print_time(&fsck_p2_time)) ;
	printf("Phase#3 tooks %lu usecs.\n", print_time(&fsck_p3_time)) ;
	printf("Phase#4 tooks %lu usecs.\n", print_time(&fsck_p4_time)) ;
	printf("Total fsck tooks %lu usecs.\n", print_time(&fsck_total_time)) ;

	return ret;
}


/*VARARGS*/
int
ask(int def, const char *fmt, ...)
{
	va_list ap;

	char prompt[256];
	int c;

	{
		va_list ap_tmp ;
		char prompt_tmp[256] ;
		va_start(ap_tmp, fmt);
		vsnprintf(prompt_tmp, sizeof(prompt_tmp), fmt, ap_tmp);
		va_end(ap_tmp);
		printf("ask if %s? ... ", prompt_tmp) ;
	}

	if (preen) {
		if (rdonly)
			def = 0;
		if (def)
			printf("FIXED\n");
		return def;
	}

	va_start(ap, fmt);
	vsnprintf(prompt, sizeof(prompt), fmt, ap);
	va_end(ap);
	if (alwaysyes || rdonly) {
		printf("%s? %s\n", prompt, rdonly ? "no" : "yes");
		return !rdonly;
	}
	do {
		printf("%s? [yn] ", prompt);
		fflush(stdout);
		c = getchar();
		while (c != '\n' && getchar() != '\n')
			if (feof(stdin))
				return 0;
	} while (c != 'y' && c != 'Y' && c != 'n' && c != 'N');
	return (c == 'y' || c == 'Y');
}
