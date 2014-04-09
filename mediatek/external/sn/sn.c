#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/file.h>
#include <cutils/xlog.h>

#define DEBUG 0

#define SN_LOG_TAG "sn"
#define SN_DBG_LOG(_fmt_, args...) \
    do { \
        if (DEBUG) { \
            sxlog_printf(ANDROID_LOG_INFO, SN_LOG_TAG, _fmt_, ##args); \
        } \
    } while(0)

#define SN_INFO_LOG(_fmt_, args...) \
    do { sxlog_printf(ANDROID_LOG_INFO, SN_LOG_TAG, _fmt_, ##args); } while(0)

#define FILENAME "opensesame"

#define PATH1 "/sdcard/" FILENAME
#define PATH2 "/storage/sdcard0/" FILENAME
#define PATH3 "/storage/sdcard1/" FILENAME

#define BUF_SZ 256

FILE *chk_file(const char *path)
{
	int ret = 0;
	FILE *fp = NULL;

	/*Check file*/
	if (0 == access(path, R_OK)) {
		if ((fp = fopen(path, "r")) != NULL) {
			 SN_DBG_LOG("Open %s\n", path);
		} else {
			 SN_INFO_LOG("Fail to open err=%s\n", strerror(errno));
		}
	} else {
		SN_INFO_LOG("Fail to access err=%s", strerror(errno));
	}

	return fp;
}

int main(int argc, char *argv[])
{
	int retry = 0;

	while(retry < 10) {
		/*READ Serial Number from file first*/
		char buf[BUF_SZ] = {0};
		char sn[BUF_SZ] = {0};
		int ret, len;
		FILE *fp = NULL;
		int sys_fp;

		SN_INFO_LOG("Retry %d\n", retry);

		if( (fp = chk_file(PATH1)) != NULL) {
			goto read;
		} else if( (fp = chk_file(PATH2)) != NULL) {
			goto read;
		} else if( (fp = chk_file(PATH3)) != NULL) {
			goto read;
		} else {
			SN_INFO_LOG("Check all possible paths\n");
			goto fail;
		}
read:
		/*Read file*/
		len = fread(buf, 1, sizeof(char)*BUF_SZ, fp);
		if (ferror(fp) || len == 0) {
			SN_INFO_LOG("err=%s: fread fail ret=%x\n", strerror(errno), len);
			goto fail;
		}

		SN_DBG_LOG("Length=%x, Content=%s\n", len, buf);

		/*Write data to sysfs*/
		sys_fp = open("/sys/class/android_usb/android0/iSerial", O_RDWR);
		strncpy(sn, buf, sizeof(char)*BUF_SZ);

		ret = write(sys_fp, sn, sizeof(char) * strlen(sn));

		if (ret <= 0)	{
			SN_INFO_LOG("Fail to write %s ret=%x\n", sn, ret);
			goto fail;
		} else {
			SN_DBG_LOG("Success to write %s\n", sn);
			if(fp) fclose(fp);
			if(sys_fp) close(sys_fp);
			break;
		}

fail:
		if(fp) fclose(fp);
		if(sys_fp) close(sys_fp);
		retry++;
		sleep(10);
	}
	return 0;
}
