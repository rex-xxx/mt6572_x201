#ifndef _CFG_DFO_FILE_H
#define _CFG_DFO_FILE_H

// the record structure define of dfo nvram file
typedef struct
{
    int count;
    char name[1][32];
    int value[1];
} ap_nvram_dfo_config_struct;

//the record size and number of dfo nvram file
#define CFG_FILE_DFO_CONFIG_SIZE    sizeof(ap_nvram_dfo_config_struct)
#define CFG_FILE_DFO_CONFIG_TOTAL   1

#endif /* _CFG_DFO_FILE_H */
