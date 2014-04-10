#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <cutils/sockets.h>
#include "includes.h"
#include "rild_porting.h"
#include <assert.h>

#include "common.h"
#include "config.h"

static uint8  atou8(const char *a)
{
	uint8 ret = 0;
	
	if(*a <= '9' && *a >= '0')
		ret = *a - '0';
	else if(*a >= 'a' && *a <= 'f')
		ret = *a - 'a' + 10;
	else if(*a >= 'A' && *a <= 'F')
		ret = *a - 'A' + 10;

	return ret;
}


//char to uint_8
static  void atohex(const char *a, uint8 *hex)
{
	uint8 tmp = atou8(a);

	tmp <<= 4;
	tmp += atou8(a + 1);

	*hex = tmp;
	
}

static  void strtohex(char *a, uint32 len, uint8 *hex)
{
	int i = 0;
	
	for (i = 0; i < len/2; i++)
		atohex(a + i * 2, hex + i);
}


//uint8 to char
static void hextoa(uint8 *hex, char *a)
{
	sprintf(a, "%2x", *hex);
	
	if(*hex < 0x10){
		*a = '0';
	}
}

static void hextostr(uint8 *hex, uint32 len,
			char *a)
{
	int i = 0;
	
	for(i = 0; i < len; i++)
		hextoa(hex + i, a + i * 2);
}


//Initialization
//create the socket
int connectToRild()
{
	int sock = socket_local_client("rild-debug", 
			ANDROID_SOCKET_NAMESPACE_RESERVED, SOCK_STREAM);
	
	if(sock < 0)
		wpa_printf(MSG_ERROR, "connectToRild %s", strerror(errno));
	
	return sock;
}


//Input functionName
//1 EAP-SIM
//IN  SLOD ID,RAND(len 16)
int  eapSimSetParam(int sock, int slotId, uint8 *rand)
{
	int ret = -1, strLen = 0, count = 1;
	char *strParm = NULL, *pTmp = NULL;
		
	assert(sock > 0);
	
	wpa_printf(MSG_DEBUG, "%s sock %d slotId %d\n", __FUNCTION__, sock, slotId);
	wpa_hexdump(MSG_DEBUG, "rand: ", rand, SIM_RAND_LEN);

	strLen = strlen("EAP_SIM") + 2 + SIM_RAND_LEN * 2 + 1 + 1;
	strParm	= (char *)malloc(strLen); 
	strParm[strLen - 1] = '\0';
	strcpy(strParm, "EAP_SIM");
	strcat(strParm, ",");
	sprintf(strParm + strlen(strParm), "%d", slotId);
	strcat(strParm, ",");
	//strncpy(strParm + strlen(strParm), rand, SIM_RAND_LEN);
	hextostr(rand, SIM_RAND_LEN, strParm + strlen(strParm));
	wpa_printf(MSG_DEBUG, "%d %s will sent to rild\n", strLen, strParm);

	ret = send(sock, (int)&count, sizeof(int), 0);
	if(sizeof(int) == ret)
		ret = send(sock, &strLen, sizeof(strLen), 0);
	else{	
		ret = -4;
		goto failed;
	}

	if(sizeof(strLen) == ret){
		ret = send(sock, strParm, strLen, 0);
		if(strLen == ret){
			ret = 0;
			wpa_printf(MSG_DEBUG, "%s ok\n", __FUNCTION__);
		}else{
			ret = -5;
			goto failed;	
		}	
	}
	else{
		ret = -3;	
	}

failed:	
	free(strParm);
	wpa_printf(MSG_DEBUG, "oh, %s (%d)%s\n", __FUNCTION__, ret, strerror(errno));

	return ret;
}



//2  EAP-AKA
//IN  SLOD ID, RAND(16), AUTN(16)
int eapAkaSetParam(int sock, int slotId, uint8 *rand, uint8 *autn)
{
	int ret = -1, strLen = 0, count = 1;
	char *strParm = NULL, *pTmp = NULL;
		
	assert(sock > 0);
	
	wpa_printf(MSG_DEBUG, "%s slotId %d\n", __FUNCTION__, slotId);
	wpa_hexdump(MSG_DEBUG, "rand: ", rand, AKA_RAND_LEN);
	wpa_hexdump(MSG_DEBUG, "autn: ", autn, AKA_AUTN_LEN);

	strLen = strlen("EAP_AKA") + 3 + AKA_RAND_LEN * 2 + 1 + AKA_AUTN_LEN * 2 + 1;
	strParm	= (char *)malloc(strLen); 
	strParm[strLen - 1] = '\0';
	strcpy(strParm, "EAP_AKA");
	strcat(strParm, ",");
	sprintf(strParm + strlen(strParm), "%d", slotId);
	strcat(strParm, ",");
	hextostr(rand, AKA_RAND_LEN, strParm + strlen(strParm));
	strcat(strParm, ",");
	hextostr(autn, AKA_AUTN_LEN, strParm + strlen(strParm));
	wpa_printf(MSG_DEBUG, "%d %s will sent to rild\n", strLen, strParm);

	ret = send(sock, &count, sizeof(int), 0);
	if(sizeof(int) == ret )
		ret = send(sock, &strLen, sizeof(strLen), 0);
	else
		goto failed;

	if(sizeof(strLen) == ret)
		ret = send(sock, strParm, strLen, 0);

	if(strLen == ret){
		ret = 0;
		wpa_printf(MSG_DEBUG, "%s ok\n", __FUNCTION__);
	}else{
		ret = -1;
		wpa_printf(MSG_DEBUG, "%s failed\n", __FUNCTION__);
	}
	

failed:	
	free(strParm);
	wpa_printf(MSG_DEBUG, "oh, %s (%d)%s\n", __FUNCTION__, ret, strerror(errno));

	return ret;
}

static int parseSimResult(uint8 *strParm, int strLen, uint8 *sres, uint8 *kc)
{
	int ret = -1;
	
	wpa_printf(MSG_DEBUG, "%s (%d) %s\n", __FUNCTION__, strLen, (char *)strParm);
	
	if(0 == strncmp(strParm, "ERROR", strlen("ERROR")))
		wpa_printf(MSG_DEBUG, "%s\n", strParm);
	else{
		//strncpy(sres, strParm, 4);
		//strncpy(kc, strParm + 4, 8);
		strtohex(strParm, SIM_SRES_LEN * 2, sres);
		strtohex(strParm + SIM_SRES_LEN * 2, SIM_KC_LEN * 2, kc);
		wpa_printf(MSG_DEBUG, "parseSimResult ok\n");
		wpa_hexdump(MSG_DEBUG, "parseSimResult kc", kc, SIM_KC_LEN);
		wpa_hexdump(MSG_DEBUG, "parseSimResult sres", sres, SIM_SRES_LEN);
		ret = 0;
	}
		
	return ret;	
}


static void parseAkaSuccess(const char* str, uint8 *res, size_t *res_len,
			uint8 *ck, uint8 *ik)
{
	uint8 tmpLen = 0;
	uint32 index = 0;
	uint8 kc[16];	

	atohex(str, &tmpLen);
	index += 2;
	*res_len = tmpLen;
	strtohex(str + index, tmpLen * 2, res);
	wpa_hexdump(MSG_DEBUG, "parseAkaSuccess res", res, tmpLen);

	index += tmpLen * 2;
	atohex(str + index, &tmpLen);
	index += 2;
	strtohex(str + index, tmpLen * 2, ck);
	wpa_hexdump(MSG_DEBUG, "parseAkaSuccess ck", ck, tmpLen);
	
	index += tmpLen * 2;
	atohex(str + index, &tmpLen);
	index += 2;
	strtohex(str + index, tmpLen * 2, ik);
	wpa_hexdump(MSG_DEBUG, "parseAkaSuccess ik", ik, tmpLen);

	index += tmpLen * 2;
	atohex(str + index, &tmpLen);
	index += 2;
	strtohex(str + index, tmpLen * 2, kc);
	wpa_hexdump(MSG_DEBUG, "parseAkaSuccess kc", kc, tmpLen);

}

static void parseAkaFailure(const char* str, uint8 *auts)
{
	uint8 tmpLen = 0;
	uint32 index = 0;

	atohex(str, &tmpLen);
	index += 2;
	strtohex(str + index, tmpLen * 2, auts);
	
	wpa_hexdump(MSG_DEBUG, "parseAkaFailure auts ", auts, tmpLen);
}


static int parseAkaResult(uint8 *strParm, int strLen,
			uint8 *res, size_t *res_len,
		     	uint8 *ik, uint8 *ck, uint8 *auts)
{
	int ret = -1;

	wpa_printf(MSG_DEBUG, "%s %s\n", __FUNCTION__, strParm);	
	
	if(0 == strncmp(strParm, "DB", strlen("DB"))){
		parseAkaSuccess(strParm + 2, res, res_len,
				ck, ik);		
		return 0;
	}else if(0 == strncmp(strParm, "DC", strlen("DC"))){
		parseAkaFailure(strParm + 2, auts);
		
	}else{
		wpa_printf(MSG_DEBUG, "%s unknow string. %s\n", 
			__FUNCTION__, strParm);
	}

	return -1;	

}


//output function

//1 3G security_parameters context
int eapSimQueryResult(int sock, uint8 *sres, uint8 *kc)
{
	int ret = -1, strLen = 0;
	uint8 *strParm = NULL;

	assert(sres);
	assert(kc);
	assert(sock > 0);

	wpa_printf(MSG_DEBUG,"%s\n", __FUNCTION__);	
	ret = recv(sock, (uint8 *)&strLen, sizeof(strLen), 0);
	
	if(sizeof(strLen) == ret){
		strParm = (uint8 *)malloc(strLen + 1);
		memset(strParm, 0xcc, strLen + 1);
		strParm[strLen] = '\0';
		ret = recv(sock, strParm, strLen, 0);
		if(strLen == ret){
			ret = parseSimResult(strParm, strLen, sres, kc);
		}
		free(strParm);		
	}

	return ret;
}




//2 GSM security_parameters context
int eapAkaQueryResult(int sock, uint8 *res, size_t *res_len,
		     uint8 *ik, uint8 *ck, uint8 *auts)
{
	int ret = -1, strLen = 0;
	uint8 *strParm = NULL;

	assert(sres);
	assert(kc);
	assert(sock > 0);

	wpa_printf(MSG_DEBUG,"%s ret %d  strLen %d\n", __FUNCTION__, ret, strLen);	
	ret = recv(sock, (uint8 *)&strLen, sizeof(strLen), 0);
		
	if(sizeof(strLen) == ret){
		strParm = (uint8 *)malloc(strLen + 1);
		memset(strParm, 0xcc, strLen + 1);
		strParm[strLen] = '\0';
		ret = recv(sock, strParm, strLen, 0);
		if(strLen == ret){
			ret = parseAkaResult(strParm, strLen,
						res, res_len,
						ik, ck, auts);
		}
		free(strParm);		
	}

	return ret;
}

//uninitilization;
int disconnectRild(int sock)
{
	int ret;
	
	assert(sock > 0);
	ret = close(sock);
	
	return ret;
}
