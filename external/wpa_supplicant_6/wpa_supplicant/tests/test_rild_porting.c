#include "rild_porting.c"


void dumpMenu()
{
	printf("EAP_SIM_AKA_FIXED_TEST slect 0:\n");	

	
	printf("EAP_SIM_TEST_FLOW\n");
	printf("step: eapSimSetParam eapSimQueryResult slect 1:\n");
	
	printf("EAP_AKA_TEST_FLOW SUCCESS\n");
	printf("step: eapAkaSetParam eapAkaQueryResult slect 2:\n");

}


//sim input string, EAP_SIM,0,5a01d5224f14222c5554102a10df5896
//sim output string 8944F433373A7603DE84C000
//aka input string, EAP_AKA,0,5a01d5224f14222c5554102a10df5896, 5a01d5224f14222c5554102a10df5896 
//aka success string DB083F7D8D419277AE1C1080BB43519D0175C139187861DC01A31A10AC98E8D86AF2D92CA0CAC30B5C50E88A08B5F110E377A2E77D
//aka failure string  DC0E8944F438944F438944F438944F43


int main(int argc, char *argv[])
{
	int ret = 0, input = -1;
	
	android_printf(MSG_DEBUG, "%s", "Hello rild");

	while(1){
		dumpMenu();
		scanf("%d", &input);
		printf("Input %d\n", input);

		
		switch (input)
		{
			case 0:
				{
					char *pSimResult1 = "8944F433373A7603DE84C000";
					char *pSimResult2 = "ERROR,rild is not setup up";
					
					char *pAkaSuccess = "DB083F7D8D419277AE1C1080BB43519D0175C139187861DC01A31A10AC98E8D86AF2D92CA0CAC30B5C50E88A08B5F110E377A2E77D";
					char *pAkaFailure = "DC0E8944F438944F438944F438944F43";
					u8  i = 0xcd, ia;
					char *a = "f4", ai[2];
					u8   ii[2] = {0x44, 0xfb}, aaii[2];
					char *aa = "ffee", iiaa[4];

					wpa_printf(MSG_DEBUG, "FIXED TEST\n");
					
					hextoa(&i, ai);		
					wpa_printf(MSG_DEBUG, "%x -> %c%c", i, ai[0], ai[1]);
					
					atohex(a, &ia);
					wpa_printf(MSG_DEBUG, "%s -> %x", a, ia);
					
					hextostr(ii, sizeof(ii)/sizeof(ii[0]), iiaa);
					wpa_printf(MSG_DEBUG, "%x%x -- > %c%c%c%c", ii[0], ii[1],
							iiaa[0], iiaa[1], iiaa[2], iiaa[3]);
					
					strtohex(aa, strlen(aa), aaii);
					wpa_printf(MSG_DEBUG, "%s --> %x%x", aa,
							aaii[0], aaii[1]);

					uint8 *strParm = (uint8 *)pSimResult1, strLen = strlen(pSimResult1);
					uint8 sres[SIM_SRES_LEN], kc[SIM_KC_LEN];
					uint8 res[50];
					size_t res_len = 0;
					uint8 ck[50], ik[50], auts[50];
					 
					parseSimResult(strParm, strLen, sres, kc);
					
					strParm = (uint8 *)pSimResult2, strLen = strlen(pSimResult2);
					parseSimResult(strParm, strLen, sres, kc);
					
					parseAkaResult(pAkaSuccess, strlen(pAkaSuccess),
						res, &res_len, ck, ik, auts);
					parseAkaResult(pAkaFailure, strlen(pAkaFailure),
						res, &res_len, ck, ik, auts);
	
					
				}	
				break;
			case 1:	
				{
					int sock = -1;
					int i = 0;
					char *simInput = "EAP_SIM,0,5a01d5224f14222c5554102a10df5896";
					uint8 sres[SIM_SRES_LEN], kc[SIM_KC_LEN];
					uint8 rand[] = {0x5a, 0x01, 0xd5, 0x22, 0x4f, 0x14, 0x22, 0x2c, 0x55, 0x5, 0x4, 0x10, 0x2a, 0x10, 0xdf, 0x58, 0x96};					
					uint8 randLeon[3][16] = {{0x89, 0xab, 0xcb, 0xee, 0xf9, 0xab, 0xcd, 0xef, 
																0x89, 0xab, 0xcd, 0xef, 0x89, 0xab, 0xcd, 0xef}, 
																{0x9a, 0xbc, 0xde, 0xf8, 0x9a, 0xbc, 0xde, 0xf8, 
																	0x9a, 0xbc, 0xde, 0xf8, 0x9a, 0xbc, 0xde, 0xf8},
																{0xab, 0xcd, 0xef, 0x89, 0xab, 0xcd, 0xef, 0x89, 
																	0xab, 0xcd, 0xef, 0x89, 0xab, 0xcd, 0xef, 0x89}};
					
					for(i = 0; i < 3; i++){
						sock = connectToRild();
						eapSimSetParam(sock, 0, randLeon[i]);
						eapSimQueryResult(sock, sres, kc); 
						disconnectRild(sock);
					}	
				}
				break;
			case 2:
				{
					int sock = -1;
					char *akaInput = "EAP_AKA,0, 8090a966d69c5dd3b5fb0ae975596961,d7ac9c65801a3412af8d47b4ce54ee1e";
					uint8 rand[] = {0x80, 0x90, 0xa9, 0x66, 0xd6, 0x9c, 0x5d, 0xd3, 
							0xb5, 0xfb, 0x0a, 0xe9, 0x75, 0x59, 0x69, 0x61};
					uint8 autn[] = {0xd7,0xac,0x9c,0x65,0x80,0x1a,0x34,0x12,
							0xaf,0x8d,0x47,0xb4,0xce,0x54,0xee,0x1e};
					uint8 res[100], ik[100], ck[100], auts[100];
					size_t res_len;
					
					
					sock = connectToRild();
					eapAkaSetParam(sock, 0, rand, autn);
					eapAkaQueryResult(sock, res, &res_len,
		     					ik, ck, auts);
					disconnectRild(sock);	
					
				}
				break;
			default:
				printf("wrong input %d\n", input);
				break;
		}

			
	}	

	return 0;
}