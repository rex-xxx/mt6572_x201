#ifndef __OPEN_NFC_MSTAR_INTERFACE_H
#define __OPEN_NFC_MSTAR_INTERFACE_H

int ms_interfaceInit(int i);
void ms_interfaceClose( void );
//int ms_interfaceSend(unsigned char *buf, int len);
//int ms_interfaceSend(struct spidev_data *spidev, unsigned char *buf, int len);
//int ms_interfaceSend(struct spidev_data *spidev, int len);
int ms_interfaceSend(tNALMsrComInstance  *dev, int len);
//int ms_interfaceRecv(unsigned char *buf, int maxlen, int timeout);
int ms_interfaceRecv(tNALMsrComInstance  *dev, int maxlen, int timeout);
int ms_interfaceTranseive( unsigned int len, unsigned char *buf, int timeout );

int MS_GetRFStatus();
int ms_chk_dev_IRQ();
int ms_IRQ_abort();

int SPI_RECV(int len,unsigned char *buf, int timeout );
int SPI_SEND(int len,unsigned char *buf);
int SPI_Init( int index );
int SPI_State( void );
void SPI_Reset(void);
void ms_DeviceReset( void);
unsigned char Checksum_is_correct( unsigned char *buf, unsigned short len );
int MS_PowerOff( void );
int MS_PowerOn( void );
void I2C_Reset(void);

#endif