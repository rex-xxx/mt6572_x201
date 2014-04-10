#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <linux/ioctl.h>
/*
#include <linux/module.h>
#include <linux/kernel.h>
#include <linux/errno.h>
#include <linux/fs.h>
#include <linux/device.h>
#include <linux/spi/spi.h>

#include <asm/uaccess.h>
*/



#define MSR3110_DEV_MAGIC_ID       0xCD
#define MSR3110_IOCTL_FW_UPGRADE _IOW( MSR3110_DEV_MAGIC_ID, 0x00, int)
#define MSR3110_IOCTL_SET_VEN _IOW( MSR3110_DEV_MAGIC_ID, 0x01, int)
#define MSR3110_IOCTL_SET_RST _IOW( MSR3110_DEV_MAGIC_ID, 0x02, int)

#define MSR3110_IOCTL_ISP_READ_REG _IOW( MSR3110_DEV_MAGIC_ID, 0xA1, int)
#define MSR3110_IOCTL_ISP_WRITE_REG _IOW( MSR3110_DEV_MAGIC_ID, 0xA2, int)




typedef struct _msr3110fw_upgrade_info
{
	unsigned long FwBufLen;
	void* FwBuf;

} msr3110fw_upgrade_info;

typedef struct _msr3110_isp_info
{
	unsigned char isp_addrs;
	unsigned char reg_pos_1;
	unsigned char reg_pos_2;
	unsigned char write_val;
	unsigned char reg_val;
	
} msr3110_isp_info;


void main_help()
{
	printf( "Function List: \n");
	printf( "\n");
	printf( "a: VEN OFF \n");
	printf( "b: VEN ON \n");
	printf( "c: RST OFF \n");
	printf( "d: RST ON \n");
	printf( "e: FW(Firmware) Upgrade \n");
	printf( "\n");
	printf( "1: Echo Command \n");
	printf( "2: Get Version \n");
	printf( "3: ISP Reg Read (Reg Position: 0x 80 00) \n");
	printf( "4: ISP Reg Write (Reg Position: 0x 80 00) \n");
	printf( "5: RF Power On \n");
	printf( "6: RF Power Off \n");
	
}




int main( int argc, char *arg[])
{
    int retVal = 0;
    int i,retry;
    int fd;
    unsigned char resBuf[256];

    //unsigned char cmd_echo[] = {0x00, 0xf0, 0x01, 0x08, 0x11,0x22,0x33,0x44,0x55,0x66,0x77,0x88};
    unsigned char cmd_echo[] = { 0x01, 0x02, 0x00, 0x00, 0x03, 0x00};    
    unsigned char cmd_get_version[] = { 0x05, 0x01, 0x00, 0x06, 0x00};    
	unsigned char cmd_rf_power_on[] = { 0x42, 0x02, 0x80, 0x05, 0xC9, 0x00};    
	unsigned char cmd_rf_power_off[] = { 0x42, 0x02, 0x80, 0x04, 0xC8, 0x00}; 
    msr3110fw_upgrade_info msr3110FwInfo;
    FILE *fp = NULL;
    unsigned long fileLen = 0;
    unsigned char *fwBuf = NULL;
    unsigned int fileRead = 0;
    int pinVal = 0;
    char inputCmd = arg[1][0];

	msr3110_isp_info ispInfo;
    
    
    printf("msr3110_dev_test - test 03 \n");
    
    printf("msr3110_dev_test start... \n");
    
    printf( "command: %c \n", inputCmd);
    
    fd = open( "/dev/msr3110", O_RDWR);
    if( fd < 0)
    {
    	printf( "FAIL: open msr3110, fd: %d \n", fd);
    	retVal = -1;
    	goto end;
    }    
    printf( "SUCCESS: open msr3110, fd: %d \n", fd);
    
    switch( arg[1][0])
    {
    	case 'a':

    		//ioctl test: VEN on
    		pinVal = 0;
    		retVal = ioctl( fd, MSR3110_IOCTL_SET_VEN, ( int)&pinVal);
    		printf( "MSR3110_IOCTL_SET_VEN retVal: %d \n", retVal);
    		
    		goto end;

    		break;
    		
    	case 'b':
    		//ioctl test: VEN off
    		pinVal = 1;
    		retVal = ioctl( fd, MSR3110_IOCTL_SET_VEN, ( int)&pinVal);
    		printf( "MSR3110_IOCTL_SET_VEN retVal: %d \n", retVal);
    		
    		goto end;
    		break;
    	
    	case 'c':

    		//ioctl test: RST on
    		pinVal = 0;
    		retVal = ioctl( fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);
    		printf( "MSR3110_IOCTL_SET_RST retVal: %d \n", retVal);
    		
    		goto end;

    		break;
    		
    	case 'd':
    		//ioctl test: RST off
    		pinVal = 1;
    		retVal = ioctl( fd, MSR3110_IOCTL_SET_RST, ( int)&pinVal);
    		printf( "MSR3110_IOCTL_SET_RST retVal: %d \n", retVal);
    		
    		goto end;
    		break;
    			
    	case 'e':
    		printf( "MSR3110_IOCTL_FW_UPGRADE \n");
    		
    		memset( &msr3110FwInfo, 0, sizeof( msr3110fw_upgrade_info));
				fp = fopen( "/data/MSR3110_U03.BIN", "rb");
				if( fp == NULL)
				{
					printf( "FAIL: open msr3110 firmware bin file, fp: %d \n", fp);
					retVal = -1;
    			goto end;			
				}
				
				fseek( fp, 0, SEEK_SET);
				fseek( fp, 0, SEEK_END);
				fileLen = ftell( fp);
				printf( "file len: %lu \n", fileLen);
				
				fseek( fp, 0, SEEK_SET);
				fwBuf = ( unsigned char*)malloc( fileLen);
				if( fwBuf == NULL)
				{
					printf( "FAIL: fwBuf malloc \n");
					retVal = -1;
    			goto end;	
				}
				
				memset( fwBuf, 0, fileLen);
				fileRead = fread( fwBuf, fileLen, 1, fp);
				
				printf( "fileRead: %d \n", fileRead);
				msr3110FwInfo.FwBufLen = fileLen;
				msr3110FwInfo.FwBuf = fwBuf;	
    		
				retVal = ioctl( fd, MSR3110_IOCTL_FW_UPGRADE, ( unsigned long)&msr3110FwInfo);
				printf( "MSR3110_IOCTL_FW_UPGRADE retVal: %d \n", retVal);
				
				
				goto end;
				break;
		
			
		case '1':
    		retVal = write( fd, cmd_echo, sizeof( cmd_echo));
    		printf( "cmd_echo retVal: %d \n", retVal);
    		if( retVal != sizeof( cmd_echo))
    		{
    			printf( "FAIL: write msr3110, retVal: %d \n", retVal);
    			goto end;
    		}
    		goto read;
    		break;
    	
    	case '2':
    		retVal = write( fd, cmd_get_version, sizeof( cmd_get_version));
    		printf( "cmd_get_version retVal: %d \n", retVal);
    		if( retVal != sizeof( cmd_get_version))
    		{
    			printf( "FAIL: write msr3110, retVal: %d \n", retVal);
    			goto end;
    		}
    		goto read;
    		break;

		case '3':
			printf( "ioctl, read isp reg val \n");
			ispInfo.isp_addrs = 0x59;
			ispInfo.reg_pos_1 = 0x08;
			ispInfo.reg_pos_2 = 0x00;
			ispInfo.reg_val = 0x00;
    		retVal = ioctl( fd, MSR3110_IOCTL_ISP_READ_REG, ( unsigned long)&ispInfo);
			printf( "retVal: %02X \n", ispInfo.reg_val);
    		printf( "retVal: %d \n", retVal);
    		goto end;
    		
    		break;

		case '4':
			printf( "ioctl, write isp reg val \n");
			ispInfo.isp_addrs = 0x59;
			ispInfo.reg_pos_1 = 0x08;
			ispInfo.reg_pos_2 = 0x00;
			ispInfo.write_val = 0x79;
			ispInfo.reg_val = 0x00;
    		retVal = ioctl( fd, MSR3110_IOCTL_ISP_WRITE_REG, ( unsigned long)&ispInfo);
			
    		printf( "retVal: %d \n", retVal);
    		goto end;
    		
    		break;
    	
		case '5':
			printf( "RF Power On \n");

			retVal = write( fd, cmd_rf_power_on, sizeof( cmd_rf_power_on));
    		printf( "cmd_rf_power_on retVal: %d \n", retVal);
    		if( retVal != sizeof( cmd_echo))
    		{
    			printf( "FAIL: write msr3110, retVal: %d \n", retVal);
    			goto end;
    		}
    		goto read;
    		break;

		case '6':
			printf( "RF Power off \n");

			retVal = write( fd, cmd_rf_power_off, sizeof( cmd_rf_power_off));
    		printf( "cmd_rf_power_off retVal: %d \n", retVal);
    		if( retVal != sizeof( cmd_echo))
    		{
    			printf( "FAIL: write msr3110, retVal: %d \n", retVal);
    			goto end;
    		}
    		goto read;
    		break;


		case 'h':
			printf( "[User Manual]  \n");

			main_help();

			goto end;			
    		break;
    		
    	default:
    		printf( "FAIL: unknown command: %c \n", inputCmd);
    		goto end;
    		break;
    }






    
// read
//
#if 1    
    
read:    
    // read 2 bytes
    //
    retVal = read( fd, resBuf, 2);
    if( retVal < 0)
    {
    	printf( "FAIL: read fail. retVal: %d \n", retVal);
    	goto end;
    }
    
    printf( "SUCCESS. RCV BUF:  ");
    for( i = 0; i < retVal; i++)
    {
    	printf( "%02X ", resBuf[i]);
    }
    printf( " \n");
    
    // read data bytes
    //
    
    retVal = read( fd, &resBuf[2], resBuf[1] + 2);
    if( retVal < 0)
    {
    	printf( "FAIL: read fail. retVal: %d \n", retVal);
    	goto end;
    }
    
    printf( "SUCCESS. RCV BUF:  ");
    for( i = 0; i < retVal + 2; i++)
    {
    	printf( "%02X ", resBuf[i]);
    }
    printf( " \n");   

#endif
    
end:    
		close( fd);
		if( fp)
		{
			fclose( fp);
		}
		if( fwBuf)
		{
			free( fwBuf);
		}
    printf("msr3110_dev_test end... \n");

		return retVal;

}


/*
    int fd = open("/dev/nfcc", O_RDWR);
    if( fd < 0){
        printf("failed to open nfcc\n");
        exit(-1);
    }
    printf("open nfcc ok\n");
    
    //ioctl(fd, OPEN_NFC_IOC_RESET);

    cmd[0] = NAL_SERVICE_ADMIN;
    cmd[1] = NAL_CMD_GET_PARAMETER;
    cmd[2] = NAL_PAR_PERSISTENT_POLICY;

#if 0   //enabling echo service
#define TSIZE 20
    cmd[0] = NAL_SERVICE_ADMIN;
    cmd[1] = 0xF0;  //raw command
    cmd[2] = 0x01;  //firmware echo service
    cmd[3] = TSIZE;
    for(i=0;i<TSIZE;i++)
       cmd[4+i] = i;
    if( write(fd, cmd, 4+TSIZE) < 0 ){
        printf("faileddd to write\r\n");
        close(fd);
        exit(-1);
    }
#else
#define CMD cmd_detection

    if( write(fd, CMD, sizeof(CMD) ) < 0 ){
        printf("faileddd to write\r\n");
        close(fd);
        exit(-1);
    }

    //read back first response.
    retry=0;
    while(retry<500){
        ret = read(fd, rsp, 256);
        if( ret > 0)
            break;
        retry++;
        usleep(10000); //sleep 10ms
    }
    printf("read nfcc ok, ret =%d\r\n", ret);
    if( ret > 0){
        printf("buf:");
        for(i=0;i<ret;i++){
            printf("%02X ", rsp[i]);
        }
        printf("\r\n");
    }
    else{
        printf("failed to read\n");
        //close(fd);
    }

    //wait for detection tag result
    retry=0;
    while(retry<1000){
        printf(".");
        fflush(stdout);
        ret = read(fd, rsp, 256);
        if( ret > 0)
            break;
        retry++;
        usleep(10000); //sleep 10ms
    }
    if( ret > 0){
        printf("target found buf:");
        for(i=0;i<ret;i++){
            printf("%02X ", rsp[i]);
        }
        printf("\n");
    }
    else{
        printf("failed to detect tag\n");
    }
#endif
    close(fd);
    printf("nfcc_tester done...\r\n");
    return 0;
    
*/

