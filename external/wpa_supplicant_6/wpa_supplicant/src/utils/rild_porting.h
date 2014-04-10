#ifndef RILD_PORTING_H
#define RILD_PORTING_H


#define uint8 unsigned char
#define uint32 unsigned int

#define SIM_RAND_LEN 	16
#define SIM_SRES_LEN	4
#define SIM_KC_LEN	8


#define AKA_RAND_LEN 16
#define AKA_AUTN_LEN 16
#define AKA_AUTS_LEN 14
#define RES_MAX_LEN 16
#define IK_LEN 16
#define CK_LEN 16

typedef enum {
	SCARD_GSM_SIM_ONLY,
	SCARD_USIM_ONLY,
	SCARD_TRY_BOTH
} scard_sim_type;

int  eapSimSetParam(int sock, int slotId, uint8 *rand);

int eapAkaSetParam(int sock, int slotId, uint8 *rand, uint8 *autn);

int eapSimQueryResult(int sock, uint8 *sres, uint8 *kc);

int eapAkaQueryResult(int sock, uint8 *res, size_t *res_len,
		     uint8 *ik, uint8 *ck, uint8 *auts);

struct scard_data * scard_init(scard_sim_type sim_type);
void scard_deinit(struct scard_data *scard);
int scard_gsm_auth(int slotId, const unsigned char *_rand,
		   unsigned char *sres, unsigned char *kc);
int scard_umts_auth(int slotId, const unsigned char *_rand,
		    const unsigned char *autn,
		    unsigned char *res, size_t *res_len,
		    unsigned char *ik, unsigned char *ck, unsigned char *auts);

#endif