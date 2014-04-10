#ifndef DEBUG_LVL_H
#define DEBUG_LVL_H

/*!
  \page debug03 Debugging Switches

  Debugging messages are controlled by one bit per debugging level.
  I.e. to get all debugging output, specify -LSIdebug 0x00003000 on the
  command line.


  \li \c 0x00001000 Bit Buffer Overflow
  \li \c 0x00002000 Bit Buffer Underflow

  Values below 0x00001000 specify messages from the LSIlib and LSI_sbrdeclib.
  Please refer to the LSIlib and LSI_sbrdeclib documentation for those switches.
*/
#define LSI_DEBUG_LEV_MEM 0x4 /*!< Common debug level for memory
                                   requirements */

#define LSI_INTENTIONAL_SUPPRESS_OF_LSI_INFO 0x8 /*!< Intentional suppress of arithmetic 
                                                      related info */

#define LSI_DEBUG_LEV_MIN 0x1 /*!< Minimum debug level for other
                                   application must reside _above_ this
                                   value */

#define AAC_DBG_HDR     (LSI_DEBUG_LEV_MIN << 0) /*!< Header data */
#define AAC_DBG_RAWDATA (LSI_DEBUG_LEV_MIN << 1) /*!< Raw data block */ 

#define BITBUF_DBG_OFLOW  (LSI_DEBUG_LEV_MIN << 9)  /*!< Bit Buffer Overflow */
#define BITBUF_DBG_UFLOW  (LSI_DEBUG_LEV_MIN << 10) /*!< Bit Buffer Underflow */
#define AAC_DBG_COEF   (LSI_DEBUG_LEV_MIN << 12) /*!< Spectrial Coefficient */
#define AAC_DBG_CRC    (LSI_DEBUG_LEV_MIN << 14) /*!< CRC data */
#define AAC_DBG_CI     (LSI_DEBUG_LEV_MIN << 15) /*!< Channel info */
#define AAC_DBG_SD     (LSI_DEBUG_LEV_MIN << 16) /*!< Section data */
#define AAC_DBG_SF     (LSI_DEBUG_LEV_MIN << 17) /*!< Scalefactor data */
#define AAC_DBG_PU     (LSI_DEBUG_LEV_MIN << 18) /*!< Pulse data */
#define AAC_DBG_TNS    (LSI_DEBUG_LEV_MIN << 19) /*!< Tns data */
#ifdef MPEG4
#define AAC_DBG_PNS    (LSI_DEBUG_LEV_MIN << 19) /*!< Pns data */
#endif
#define AAC_DBG_QSP    (LSI_DEBUG_LEV_MIN << 20) /*!< Quantized spectrum */
#define AAC_DBG_RSP    (LSI_DEBUG_LEV_MIN << 21) /*!< Requantized spectrum */
#define AAC_DBG_TI     (LSI_DEBUG_LEV_MIN << 22) /*!< Time data */
#define AAC_DBG_FATAL  (LSI_DEBUG_LEV_MIN << 23) /*!< Fatal errors */
#define AAC_DBG_BF     (LSI_DEBUG_LEV_MIN << 24) /*!< Buffer fullness */
#define AAC_DBG_AB     (LSI_DEBUG_LEV_MIN << 25) /*!< Average bitrate */
#define AAC_DBG_SYNC   (LSI_DEBUG_LEV_MIN << 26) /*!< LOAS synchronisation */
#define AAC_DBG_CONCL  (LSI_DEBUG_LEV_MIN << 27) /*!< Concealment */

extern void LSI_dprintf(int level, char *format, ...);

extern void LSIdebug_display_fixed_header(unsigned int id,
                                   unsigned int layer,
                                   unsigned int protection_absent,
                                   unsigned int profile,
                                   unsigned int sampling_frequency_index,
                                   unsigned int private_bit,
                                   unsigned int channel_configuration,
                                   unsigned int original_copy,
                                   unsigned int home);

extern void LSIdebug_display_variable_header(unsigned int copyright_identification_bit,
                                      unsigned int copyright_identification_start,
                                      unsigned int frame_length,
                                      unsigned int buffer_fullness,
                                      unsigned int number_of_raw_data_blocks_in_frame);

extern void LSIdebug_display_crc(unsigned char isCrcActive,
                          unsigned int crc_check);

extern void LSIdebug_display_id_syn_ele(unsigned int id_syn_ele, void **vBs);

extern void LSIdebug_dump_buffer(void **vBs, unsigned int size);

extern void LSIdebug_dump_whole_bsbuffer(void **vBs);

extern void LSIdebug_display_bitstream(void **vBs);

extern void LSIdebug_display_element_instance_tag(unsigned int element_instance_tag);

extern void LSIdebug_display_channel_element_usedBtis(unsigned int channel_element, unsigned int usedBits);

extern void LSIdebug_display_global_gain(unsigned int global_gain);

extern void LSIdebug_display_ics(unsigned int ics_reserved_bit,
                          unsigned int window_sequence,
                          unsigned int window_shape,
                          unsigned int max_sfb,
                          unsigned int scale_factor_grouping,
                          unsigned int is_long_block);

extern void LSIdebug_display_tools_present(unsigned int pulse_data_present,
                                    unsigned int tns_data_present,
                                    unsigned int gain_control_data_present);

extern void LSIdebug_display_common_window(unsigned int common_window);

extern void LSIdebug_display_ms_mask_present(unsigned int ms_mask_present);

extern void LSIdebug_display_section_data_long(char *code_book,
                                        unsigned int bands);

extern void LSIdebug_display_section_data_short(char *code_book,
                                         unsigned int bands,
                                         unsigned int groups);

extern void LSIdebug_display_scalefactor_data_long(short int *scalefactor,
                                            int bands);

extern void LSIdebug_display_scalefactor_data_short(short int *scalefactor,
                                             int bands,
                                             int groups);

extern void LSIdebug_display_pulse_data(unsigned int pulse_data_present,
                                 unsigned int number_pulse,
                                 char pulse_start_band,
                                 char *pulse_offset,
                                 char *pulse_amp);


#if (defined(MPEG4) && !(defined(DRM_BSFORMAT)))
extern void LSIdebug_display_pns_data(unsigned int   bands,
                               unsigned int   groups,
                               unsigned char  pns_active,
                               unsigned char *pns_used,
                               short int     *scalefactor);
#endif

extern void LSIdebug_display_quantized_spectrum_long(int *quantized_coef,
                                              int num_coef);

extern void LSIdebug_display_quantized_spectrum_short(int *quantized_coef,
                                               int num_coef);

extern void LSIdebug_display_requantized_spectrum_long(int *requantized_coef,
                                                int num_coef);

extern void LSIdebug_display_requantized_spectrum_short(int *requantized_coef,
                                                 int num_coef);

extern void LSIdebug_display_time_output_long(short *time_output,
                                       int num_coef,
                                       int ch,
                                       int frameno);

extern void LSIdebug_display_ltp_ft_in(int ch,
                       int *pSpectralCoefficient_int,
                       int *time_quant,
                       int qFormatNorm,
                       int               abs_max_per_window[]);

extern void LSIdebug_display_ltp_ft_doing(int exp, int *pFreqInfo);

extern void LSIdebug_display_CoreAAC_ft_in(int ch,
                                      int *pSpectralCoefficient_int,
                                      short *pOverlapBuffer);

extern void LSIdebug_display_ApplyMS_Coeff(int *pSpectralCoefficient_int0,
                                      int *pSpectralCoefficient_int1);


extern void LSIdebug_display_ApplyIS_Coeff(int *pSpectralCoefficient_int0,
                                       int *pSpectralCoefficient_int1);

extern void LSIdebug_display_buffer_status (int frameno,
                             int data_used,
                             unsigned char *pRdPtr,
                             unsigned char *pWrPtr);

extern void LSIdebug_display_ltp_ft_out(int ch,
                       int *time_quant,
                       short *pTimeData);

extern void LSIdebug_display_frame_no(int framecnt);

extern void LSIdebug_display_frame_size(int framecnt, unsigned int framesize, unsigned int totalbytes);

#endif /* #ifndef DEBUG_LVL_H */

