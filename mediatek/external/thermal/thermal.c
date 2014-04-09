#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <sys/file.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <cutils/xlog.h>
#include <netutils/ifc.h>

static int debug_on = 0;

#define TM_LOG_TAG "thermal_repeater"
#define TM_DBG_LOG(_fmt_, args...) \
    do { \
        if (1 == debug_on) { \
            sxlog_printf(ANDROID_LOG_INFO, TM_LOG_TAG, _fmt_, ##args); \
        } \
    } while(0)

#define TM_INFO_LOG(_fmt_, args...) \
    do { sxlog_printf(ANDROID_LOG_INFO, TM_LOG_TAG, _fmt_, ##args); } while(0)

#define ONE_MBITS_PER_SEC 1000
#define PROCFS_TM_PID "/proc/wmt_tm/tm_pid"
#define COOLER_NUM 3

#define WLAN_IFC_PATH "/sys/class/net/wlan0/operstate"
#define AP_IFC_PATH "/sys/class/net/ap0/operstate"
#define P2P_IFC_PATH "/sys/class/net/p2p0/operstate"

enum {
	WLAN_IFC = 0,
	AP_IFC = 1,
	P2P_IFC = 2,
	IFC_NUM /*Last one*/
};

static char IFC_NAME[IFC_NUM][10] = {"wlan0","ap0","p2p0"};
static char IFC_PATH[IFC_NUM][50] = {WLAN_IFC_PATH, AP_IFC_PATH, P2P_IFC_PATH};

#ifdef NEVER
static char THROTTLE_SCRIPT_PATH[] = "/system/etc/throttle.sh";

static void exe_cmd(int wifi_ifc, int level)
{
	if (0 == access(THROTTLE_SCRIPT_PATH, R_OK | X_OK) && wifi_ifc >= 0) {
		char cmd[256] = {0};

		sprintf(cmd, "%s %s %d %d", THROTTLE_SCRIPT_PATH, IFC_NAME[wifi_ifc], level * ONE_MBITS_PER_SEC, level * ONE_MBITS_PER_SEC);

		TM_INFO_LOG("cmd=%s", cmd);

		/*Need to execute twice to effect the command*/
		int ret = system(cmd);
		if ((-1 == ret) || (0 != WEXITSTATUS(ret))) {
			TM_INFO_LOG("1. executing %s failed: %s", THROTTLE_SCRIPT_PATH, strerror(errno));
		}

		ret = system(cmd);
		if ((-1 == ret) || (0 != WEXITSTATUS(ret))) {
			TM_INFO_LOG("2. executing %s failed: %s", THROTTLE_SCRIPT_PATH, strerror(errno));
		}
	} else {
		TM_INFO_LOG("failed to access %s", THROTTLE_SCRIPT_PATH);
	}
}
#endif /* NEVER */

static void set_wifi_throttle(int level)
{
	int i = 0;
	for ( i=0; i<IFC_NUM; i++) {
		TM_DBG_LOG("checking %s", IFC_PATH[i]);
		if (0 == access(IFC_PATH[i], R_OK)) {
			char buf[80];
			int fd = open(IFC_PATH[i], O_RDONLY);
			if (fd < 0) {
				TM_INFO_LOG("Can't open %s: %s", IFC_PATH[i], strerror(errno));
				continue;
			}

			int len = read(fd, buf, sizeof(buf) - 1);
			if (len < 0) {
				TM_INFO_LOG("Can't read %s: %s", IFC_PATH[i], strerror(errno));
				continue;
			}
			close(fd);
			if(!strncmp (buf, "up", 2)) {
				ifc_set_throttle(IFC_NAME[i], level * ONE_MBITS_PER_SEC, level * ONE_MBITS_PER_SEC);

				#ifdef NEVER
			 	exe_cmd(i, level);
				#endif /* NEVER */
			} else
				TM_DBG_LOG("%s is down!", IFC_NAME[i]);
		}
	}
}

static void signal_handler(int signo, siginfo_t *si, void *uc)
{
	static int cur_thro = 0;
	int set_thro = si->si_code;

	switch(si->si_signo) {
		case SIGIO:
			TM_DBG_LOG("cur=%d, set=%d\n", cur_thro, set_thro);
			if(cur_thro != set_thro) {
				set_thro = set_thro?:1; /*If set_thro is 0, set 1Mb/s*/
				set_wifi_throttle(set_thro);
				cur_thro = set_thro;
			}
		break;
		default:
			TM_INFO_LOG("what!!!\n");
		break;
	}
}

int main(int argc, char *argv[])
{
	if(argc == 3) {
		char ifc[16] = {0};
		char tmp[16] = {0};
		int thro = 0;
		int i = 0;

		strncpy(ifc, argv[1], sizeof(char)*16);
		strncpy(tmp, argv[2], sizeof(char)*16);
		thro = atoi(tmp);

		TM_INFO_LOG("CMD MODE %s %d", ifc, thro);

		for ( i=0; i<IFC_NUM; i++) {
			if(!strncmp (IFC_NAME[i], ifc, 2)) {
				ifc_set_throttle(IFC_NAME[i], thro * ONE_MBITS_PER_SEC, thro * ONE_MBITS_PER_SEC);
				#ifdef NEVER
			 	exe_cmd(i, thro);
				#endif
			} else
				TM_DBG_LOG("NOT %s!", IFC_NAME[i]);
		}
	} else {
		int fd = open(PROCFS_TM_PID, O_RDWR);
		int pid = getpid();
		int ret = 0;
		char pid_string[32] = {0};

		struct sigaction act;

		TM_INFO_LOG("START+++++++++ %d", getpid());

		/* Create signal handler */
		memset(&act, 0, sizeof(act));
		act.sa_flags = SA_SIGINFO;
		//act.sa_handler = signal_handler;
		act.sa_sigaction = signal_handler;
		sigemptyset(&act.sa_mask);

		sigaction(SIGIO, &act, NULL);

		/* Write pid to procfs */
		sprintf(pid_string, "%d", pid);

		ret = write(fd, pid_string, sizeof(char) * strlen(pid_string));
		if (ret <= 0)	{
			TM_INFO_LOG("Fail to write %d to %s %x\n", pid, PROCFS_TM_PID, ret);
		} else {
			TM_INFO_LOG("Success to write %d to %s\n", pid, PROCFS_TM_PID);
		}
		close(fd);

#ifdef NEVER
		/* Check throttl.sh */
		if (0 == access(THROTTLE_SCRIPT_PATH, R_OK | X_OK)) {
			ret = chmod(THROTTLE_SCRIPT_PATH, S_ISUID | S_ISVTX | S_IRUSR | S_IXUSR);
			if (ret == 0)	{
				TM_INFO_LOG("Success to chomd\n");
			} else {
				TM_INFO_LOG("Fail to chmod %x\n", ret);
			}
		} else {
			TM_INFO_LOG("failed to access %s", THROTTLE_SCRIPT_PATH);
		}
#endif /* NEVER */

		TM_INFO_LOG("Enter infinite loop");

		while(1) {
			sleep(100);
		}

		TM_INFO_LOG("END-----------");
	}

	return 0;
}
