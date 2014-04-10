
#ifdef DBGOUT_LSI_DPRINTF


#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

#include "aacdec_debug.h"
#include "FDK_bitstream.h"
#include "FDK_tools_rom.h"

/*!
  \brief Print debug messages of aac fixed header

  The function prints all elements of the aac fixed header, if the
  debug switch AAC_DBG_HDR is set.

  \return  none
*/
void LSIdebug_display_fixed_header(unsigned int id,
                                   unsigned int layer,
                                   unsigned int protection_absent,
                                   unsigned int profile,
                                   unsigned int sampling_frequency_index,
                                   unsigned int private_bit,
                                   unsigned int channel_configuration,
                                   unsigned int original_copy,
                                   unsigned int home)
{
  LSI_dprintf(AAC_DBG_HDR, "\n  --- AAC-Core Fixed Header ---------------------------------------\n\n");
  LSI_dprintf(AAC_DBG_HDR, "  id .................................. %-4u\n",id);
  LSI_dprintf(AAC_DBG_HDR, "  layer ............................... %-4u\n",layer);
  LSI_dprintf(AAC_DBG_HDR, "  protection_absent ................... %-4u\n",protection_absent);
  LSI_dprintf(AAC_DBG_HDR, "  profile ............................. %-4u\n",profile);
  LSI_dprintf(AAC_DBG_HDR, "  sampling_frequency_index ............ %-4u\n",sampling_frequency_index);
  LSI_dprintf(AAC_DBG_HDR, "  private_bit ......................... %-4u\n",private_bit);
  LSI_dprintf(AAC_DBG_HDR, "  channel_configuration ............... %-4u\n",channel_configuration);
  LSI_dprintf(AAC_DBG_HDR, "  original_copy ....................... %-4u\n",original_copy);
  LSI_dprintf(AAC_DBG_HDR, "  home ................................ %-4u\n",home);
}

/*!
  \brief Print debug messages of aac variable header

  The function prints all elements of the aac variable header, if the
  debug switch AAC_DBG_HDR is set.

  \return  none
*/
void LSIdebug_display_variable_header(unsigned int copyright_identification_bit,
                                      unsigned int copyright_identification_start,
                                      unsigned int frame_length,
                                      unsigned int buffer_fullness,
                                      unsigned int number_of_raw_data_blocks_in_frame)
{
  LSI_dprintf(AAC_DBG_HDR, "\n  --- AAC-Core Variable Header ---------------------------------------\n\n");
  LSI_dprintf(AAC_DBG_HDR, "  copyright_identification_bit ........ %-4u\n",copyright_identification_bit);
  LSI_dprintf(AAC_DBG_HDR, "  copyright_identification_start ...... %-4u\n",copyright_identification_start);
  LSI_dprintf(AAC_DBG_HDR, "  frame_length ........................ %-4u\n",frame_length);
  LSI_dprintf(AAC_DBG_HDR, "  buffer_fullness ..................... %-4u\n",buffer_fullness);
  LSI_dprintf(AAC_DBG_HDR, "  number_of_raw_data_blocks_in_frame .. %-4u\n",number_of_raw_data_blocks_in_frame);
}

/*!
  \brief Print debug messages of CRC check

  The function prints the element crc_check, if the
  debug switch AAC_DBG_CRC is set.

  \return  none
*/
void LSIdebug_display_crc(unsigned char isCrcActive,
                          unsigned int crc_check)
{
  LSI_dprintf(AAC_DBG_CRC, "\n  --- AAC-Core CRC Info ---------------------------------------------\n\n");
  if (isCrcActive == 1)
    LSI_dprintf(AAC_DBG_CRC, "  crc_check ........................... %-4u\n",crc_check);
  else
    LSI_dprintf(AAC_DBG_CRC, "  crc_check ........................... %-7s\n","inactiv");
}

void LSIdebug_display_bitstream(void **vBs)
{
  HANDLE_FDK_BITSTREAM bs = (HANDLE_FDK_BITSTREAM)(*vBs);
  
  LSI_dprintf(AAC_DBG_RAWDATA, "\n  --- AAC RawData");

  // ps: FOR ADTS, the rawdata display here should be start from syncword FFFxxx
  LSI_dprintf(AAC_DBG_RAWDATA,"   [%02X ", FDKreadBits(bs,8));
  LSI_dprintf(AAC_DBG_RAWDATA,"%02X ", FDKreadBits(bs,8));
  LSI_dprintf(AAC_DBG_RAWDATA,"%02X ", FDKreadBits(bs,8));
  LSI_dprintf(AAC_DBG_RAWDATA,"%02X ", FDKreadBits(bs,8));
  LSI_dprintf(AAC_DBG_RAWDATA,"%02X ", FDKreadBits(bs,8));
  LSI_dprintf(AAC_DBG_RAWDATA,"%02X ", FDKreadBits(bs,8));
  LSI_dprintf(AAC_DBG_RAWDATA,"%02X ", FDKreadBits(bs,8));
  LSI_dprintf(AAC_DBG_RAWDATA,"%02X ]", FDKreadBits(bs,8));

  LSI_dprintf(AAC_DBG_RAWDATA, " --------------------\n");
  
  FDKpushBack(bs, 64);

}

/*!
  \brief Print debug messages of channel info

  The function prints the element id_syn_ele, if the
  debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_dump_buffer(void **vBs, unsigned int size)
{
    HANDLE_FDK_BITSTREAM bs = (HANDLE_FDK_BITSTREAM)(*vBs);
    unsigned int i = 0;
    
    LSI_dprintf(AAC_DBG_CI, " == Dump Buffer == ");

    for (i = 0; i < size; i++)
    {
      if (i % 16 == 0)
      {
          LSI_dprintf(AAC_DBG_CI, "\n    [%04X]",i);
      }
      LSI_dprintf(AAC_DBG_CI, " %02X", FDKreadBits(bs, 8));
    }

    FDKpushBack(bs, 8*size);
}

void LSIdebug_dump_whole_bsbuffer(void **vBs)
{
    HANDLE_FDK_BITSTREAM bs = (HANDLE_FDK_BITSTREAM)(*vBs);
    UCHAR *pBuf = bs->hBitBuf.Buffer;
    unsigned int i = 0;
    
    LSI_dprintf(AAC_DBG_CI, " == Dump Buffer == ");

    for (i = 0; i < bs->hBitBuf.bufSize ; i++)
    {
      if (i % 16 == 0)
      {
          LSI_dprintf(AAC_DBG_CI, "\n    [%04X]",i);
      }
      LSI_dprintf(AAC_DBG_CI, " %02X", *pBuf++);
    }
}


/*!
  \brief Print debug messages of channel info

  The function prints the element id_syn_ele, if the
  debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_display_id_syn_ele(unsigned int id_syn_ele, void **vBs)
{
  HANDLE_FDK_BITSTREAM bs = (HANDLE_FDK_BITSTREAM)(*vBs);

  //LSI_dprintf(AAC_DBG_CI, "\n\n  --- AAC-Core Channel Info ------------------------------------------\n\n");
  LSI_dprintf(AAC_DBG_HDR, "\n  id_syn_ele [0x%04X][0x%04X][0x%04X]....... %d", FDKgetBitCnt(bs), bs->hBitBuf.ValidBits, bs->hBitBuf.BitNdx, id_syn_ele);
  switch (id_syn_ele)
  {
    case ID_SCE:
      LSI_dprintf(AAC_DBG_HDR, " (single_channel_element)");
      break;
    case ID_CPE:
      LSI_dprintf(AAC_DBG_HDR, " (channel_pair_element)");
      break;
    case ID_CCE:
      LSI_dprintf(AAC_DBG_HDR, " (coupling_channel_element)");
      break;
    case ID_LFE:
      LSI_dprintf(AAC_DBG_HDR, " (lfe_channel_element)");
      break;
    case ID_DSE:
      LSI_dprintf(AAC_DBG_HDR, " (data_stream_element)");
      break;
    case ID_PCE:
      LSI_dprintf(AAC_DBG_HDR, " (program_config_element)");
      break;
    case ID_FIL:
      LSI_dprintf(AAC_DBG_HDR, " (fill_element)");
      break;
    case ID_END:
      LSI_dprintf(AAC_DBG_HDR, " (end_element)");
      break;
  }
}

/*!
  \brief Print debug messages of channel info

  The function prints the element element_instance_tag, if the
  debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_display_element_instance_tag(unsigned int element_instance_tag)
{
  LSI_dprintf(AAC_DBG_CI, "  element_instance_tag ................ %-2u\n",element_instance_tag);
}

char *sElementToString(unsigned int channel_element)
{
    switch (channel_element) 
    {
        case element_instance_tag :
            return "element_instance_tag";
            
        case common_window:
            return "common_window";
            
        case global_gain:
            return "global_gain";
        
        case ics_info: 
            return "ics_info";
        
        case max_sfb:
            return "max_sfb";
        
        case ms:              /* ms_mask_present, ms_used */
            return "ms";
        
        case ltp_data_present:
            return "ltp_data_present";
        
        case ltp_data:
            return "ltp_data";
        
        case section_data:
            return "section_data";
        
        case scale_factor_data:
            return "scale_factor_data";
        
        case pulse:
            return "pulse";

        case tns_data_present:
            return "tns_data_present";

        
        case tns_data:
            return "tns_data";
        
        case gain_control_data_present:
            return "gain_control_data_present";
        
        case gain_control_data:
            return "gain_control_data";
        
        case esc1_hcr:
            return "esc1_hcr";
        
        case esc2_rvlc:
            return "esc2_rvlc";
        
        case spectral_data:
            return "spectral_data";
        
        case scale_factor_data_usac:
            return "scale_factor_data_usac";
        
        case core_mode:
            return "core_mode";
        
        case common_tw:
            return "common_tw";
        
        case lpd_channel_stream:
            return "lpd_channel_stream";
        
        case tw_data:
            return "tw_data";
        
        case noise:
            return "noise";
        
        case ac_spectral_data:
            return "ac_spectral_data";
        
        case fac_data:
            return "fac_data";
        
        case tns_active:
            return "tns_active";
        
        case tns_data_present_usac:
            return "tns_data_present_usac";
        
        case common_max_sfb:
            return "common_max_sfb";
        
        case adtscrc_start_reg1:
            return "adtscrc_start_reg1";
        
        case adtscrc_start_reg2:
            return "adtscrc_start_reg2";
        
        case adtscrc_end_reg1:
            return "adtscrc_end_reg1";
        
        case adtscrc_end_reg2:
            return "adtscrc_end_reg2";
        
        case drmcrc_start_reg:
            return "drmcrc_start_reg";
        
        case drmcrc_end_reg:
            return "drmcrc_end_reg";
        
        case next_channel:
            return "next_channel";
        
        case next_channel_loop:
            return "next_channel_loop";
        
        case link_sequence:
            return "link_sequence";
        
        case end_of_sequence:
            return "end_of_sequence";
        
        default:
			return "NULL";
            break;
    }            
  
} rbd_id_t;

void LSIdebug_display_channel_element_usedBtis(unsigned int channel_element, unsigned int usedBits)
{
  LSI_dprintf(AAC_DBG_CI, "\n     channel_element ........... %26s [0x%x]",sElementToString(channel_element), usedBits);
}

/*!
  \brief Print debug messages of channel info

  The function prints the element global_gain, if the
  debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_display_global_gain(unsigned int global_gain)
{
  LSI_dprintf(AAC_DBG_CI, "  global_gain ......................... %-2u\n",global_gain);
}


/*!
  \brief Print debug messages of channel info

  The function prints the ics_info, if the
  debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_display_ics(unsigned int ics_reserved_bit,
                          unsigned int window_sequence,
                          unsigned int window_shape,
                          unsigned int max_sfb,
                          unsigned int scale_factor_grouping,
                          unsigned int is_long_block)
{
  LSI_dprintf(AAC_DBG_CI, "  ics_reserved_bit .................... %-2u\n",ics_reserved_bit);
  LSI_dprintf(AAC_DBG_CI, "  window_sequence ..................... %-2u\n",window_sequence);
  LSI_dprintf(AAC_DBG_CI, "  window_shape ........................ %-2u\n",window_shape);

  if (is_long_block)
  {
    LSI_dprintf(AAC_DBG_CI, "  max_sfb ............................. %-2u\n",max_sfb);
  }
  else
  {
    LSI_dprintf(AAC_DBG_CI, "  max_sfb ............................. %-2u\n",max_sfb);
    LSI_dprintf(AAC_DBG_CI, "  scale_factor_grouping ............... 0x%-2x\n",scale_factor_grouping);
  }
}

/*!
  \brief Print debug messages of channel info

  The function prints the tns_data_present and
  gain_control_data_present, if the debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_display_tools_present(unsigned int pulse_data_present,
                                    unsigned int tns_data_present,
                                    unsigned int gain_control_data_present)
{
  LSI_dprintf(AAC_DBG_CI, "  pulse_data_present .................. %-2u\n",pulse_data_present);
  LSI_dprintf(AAC_DBG_CI, "  tns_data_present .................... %-2u\n",tns_data_present);
  LSI_dprintf(AAC_DBG_CI, "  gain_control_data_present ........... %-2u\n",gain_control_data_present);
}




/*!
  \brief Print debug messages of channel info

  The function prints the common_window, if the
  debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_display_common_window(unsigned int common_window)
{
  LSI_dprintf(AAC_DBG_CI, "  common_window ....................... %-2u\n",common_window);
}


/*!
  \brief Print debug messages of channel info

  The function prints the ms_mask_present, if the
  debug switch AAC_DBG_CI is set.

  \return  none
*/
void LSIdebug_display_ms_mask_present(unsigned int ms_mask_present)
{
  LSI_dprintf(AAC_DBG_CI, "  ms_mask_present ..................... %-2u\n",ms_mask_present);
}

/*!
  \brief Print debug messages of section data

  The function prints the section_data for long blocks, if the
  debug switch AAC_DBG_SD is set.

  \return  none
*/
void LSIdebug_display_section_data_long(char *code_book,
                                        unsigned int bands)
{
  unsigned int i;

  LSI_dprintf(AAC_DBG_SD, "\n      --- AAC-Core Section data------------------------------------------\n\n");
  LSI_dprintf(AAC_DBG_SD, "      groups 1    sfb %2u\n\n",bands);
  for (i=0; i< bands; i++)
    LSI_dprintf(AAC_DBG_SD, "      code_book[%2d] ...................... %d\n",i,code_book[i]);
  LSI_dprintf(AAC_DBG_SD, "\n");
}

/*!
  \brief Print debug messages of section data

  The function prints the section_data for short blocks, if the
  debug switch AAC_DBG_SD is set.

  \return  none
*/
void LSIdebug_display_section_data_short(char *code_book,
                                         unsigned int bands,
                                         unsigned int groups)
{
  unsigned int g,i;

  LSI_dprintf(AAC_DBG_SD, "\n      --- AAC-Core Section data------------------------------------------\n\n");
  LSI_dprintf(AAC_DBG_SD, "      groups %2d    sfb %2u\n\n",groups,bands);
  for (g=0; g<groups; g++)
  {
    for (i=0; i<bands; i++)
    {
      LSI_dprintf(AAC_DBG_SD, "      code_book[%2d][%2d] ...................... %-2u\n",g,i,code_book[g*16+i]);
    }
    LSI_dprintf(AAC_DBG_SD, "\n");
  }
}

/*!
  \brief Print debug messages of scalefactor data

  The function prints the scalefactor_data for long blocks, if the
  debug switch AAC_DBG_SF is set.

  \return  none
*/
void LSIdebug_display_scalefactor_data_long(short int *scalefactor,
                                            int bands)
{
  int i;

  LSI_dprintf(AAC_DBG_SF, "\n      --- AAC-Core Scalefactor data------------------------------------------\n\n");
  LSI_dprintf(AAC_DBG_SF, "      groups 1    sfb %2d\n\n",bands);
  for (i=0; i< bands; i++)
    LSI_dprintf(AAC_DBG_SF, "      scalefactor[%2d] .................... %-2u\n",i,scalefactor[i]);
  LSI_dprintf(AAC_DBG_SF, "\n");

}

/*!
  \brief Print debug messages of scalefactor data

  The function prints the scalefactor_data for short blocks, if the
  debug switch AAC_DBG_SF is set.

  \return  none
*/
void LSIdebug_display_scalefactor_data_short(short int *scalefactor,
                                             int bands,
                                             int groups)
{
  int g,i;

  LSI_dprintf(AAC_DBG_SF, "\n      --- AAC-Core Scalefactor data------------------------------------------\n\n");
  LSI_dprintf(AAC_DBG_SF, "      groups %2d    sfb %2d\n\n",groups,bands);
  for (g=0; g<groups; g++)
  {
    for (i=0; i<bands; i++)
    {
      LSI_dprintf(AAC_DBG_SF, "      scalefactor[%2d][%2d] ................ %-2u\n",g,i,scalefactor[g*16+i]);
    }
    LSI_dprintf(AAC_DBG_SF, "\n");
  }
}

/*!
  \brief Print debug messages of pulse data

  The function prints the pulse_data, if the
  debug switch AAC_DBG_PU is set.

  \return  none
*/
void LSIdebug_display_pulse_data(unsigned int pulse_data_present,
                                 unsigned int number_pulse,
                                 char pulse_start_band,
                                 char *pulse_offset,
                                 char *pulse_amp)
{
  unsigned int i;

  LSI_dprintf(AAC_DBG_PU, "\n      --- AAC-Core Pulse data------------------------------------------------\n\n");

  if (pulse_data_present)
  {
    LSI_dprintf(AAC_DBG_PU, "      number_pulse ....................... %d\n",number_pulse);
    LSI_dprintf(AAC_DBG_PU, "      pulse_start_band ................... %d\n",pulse_start_band);

    for (i=0; i<=number_pulse; i++)
    {
      LSI_dprintf(AAC_DBG_PU, "      pulse_offset[%1u] .................... %d\n",i,pulse_offset[i]);
      LSI_dprintf(AAC_DBG_PU, "      pulse_amp[%1u] ....................... %d\n",i,pulse_amp[i]);
    }
  }
  else
  {
    LSI_dprintf(AAC_DBG_PU, "      pulse data inactiv\n");
  }
  LSI_dprintf(AAC_DBG_PU, "\n");
}



#if defined(MPEG4)
/*!
  \brief Print debug messages of PNS data

  The function prints the PNS data if the
  debug switch AAC_DBG_PNS is set.

  \return  none
*/
void LSIdebug_display_pns_data(unsigned int   bands,
                               unsigned int   groups,
                               unsigned char  pns_active,
                               unsigned char *pns_used,
                               short int     *scalefactor)
{
  unsigned int g,i;

  LSI_dprintf(AAC_DBG_PNS, "\n      --- AAC-Core Pns data--------------------------------------------------\n\n");

  if (pns_active) {
    LSI_dprintf(AAC_DBG_PNS, "      groups %2d    sfb %2u\n\n", groups, bands);
    LSI_dprintf(AAC_DBG_PNS, "                                         used  energy\n");

    for (g=0; g<groups; g++)
    {
      for (i=0; i<bands; i++)
      {
        unsigned int  pns_band  = g*MaximumScaleFactorBandsShort+i;
        unsigned char band_used = (pns_used[pns_band >> PNS_BAND_FLAGS_SHIFT] >> (pns_band & PNS_BAND_FLAGS_MASK)) & (unsigned char)1;

        LSI_dprintf(AAC_DBG_PNS, "          pns used/energy[%2u][%2u] ......... %1d", g, i, band_used);
        if (band_used) {
          LSI_dprintf(AAC_DBG_PNS, "      %-6d\n", scalefactor[pns_band]);
        }
        else {
          LSI_dprintf(AAC_DBG_PNS, "\n");
        }
      }
    }
  }
  else
  {
    LSI_dprintf(AAC_DBG_PNS, "      PNS inactive\n");
  }
}
#endif


/*!
  \brief Print debug messages of quantized spectrum

  The function prints the quantized spectrum for long blocks, if the
  debug switch AAC_DBG_QSP is set.

  \return  none
*/
void LSIdebug_display_quantized_spectrum_long(int *quantized_coef,
                                              int num_coef)
{
  int i;

  LSI_dprintf(AAC_DBG_QSP, "\n      --- AAC-Core Quantized Spectrum----------------------------------------\n\n");

  for (i=0; i<num_coef; i++)
  {
    if (i%16 == 0)
      LSI_dprintf(AAC_DBG_QSP,"\n      %4d ",i);

    LSI_dprintf(AAC_DBG_QSP, "%5d ",quantized_coef[i]);
  }
  LSI_dprintf(AAC_DBG_QSP, "\n");
}

/*!
  \brief Print debug messages of quantized spectrum

  The function prints the quantized spectrum for short blocks, if the
  debug switch AAC_DBG_QSP is set.

  \return  none
*/
void LSIdebug_display_quantized_spectrum_short(int *quantized_coef,
                                               int num_coef)
{
  int w,i;

  LSI_dprintf(AAC_DBG_QSP, "\n      --- AAC-Core Quantized Spectrum----------------------------------------");

  for (w=0; w<8; w++)
  {
    LSI_dprintf(AAC_DBG_QSP, "\n\n      window %1d",w);
    for (i=0; i<num_coef; i++)
    {
      if (i%16 == 0)
        LSI_dprintf(AAC_DBG_QSP,"\n      %4d ",i);

      LSI_dprintf(AAC_DBG_QSP, "%5d ",quantized_coef[w*num_coef+i]);
    }
  }
  LSI_dprintf(AAC_DBG_QSP, "\n");
}

/*!
  \brief Print debug messages of requantized spectrum

  The function prints the requantized spectrum for long blocks, if the
  debug switch AAC_DBG_RSP is set.

  \return  none
*/
void LSIdebug_display_requantized_spectrum_long(int *requantized_coef,
                                                int num_coef)
{
  int i;

  LSI_dprintf(AAC_DBG_RSP, "\n      --- AAC-Core Requantized Spectrum--------------------------------------\n\n");

  for (i=0; i<num_coef; i++)
  {
    if (i%16 == 0)
      LSI_dprintf(AAC_DBG_RSP,"\n      %4d ",i);

    LSI_dprintf(AAC_DBG_RSP, "0x%08lx ", requantized_coef[i]);
  }
  LSI_dprintf(AAC_DBG_RSP, "\n\n");
}

/*!
  \brief Print debug messages of requantized spectrum

  The function prints the requantized spectrum for short blocks, if the
  debug switch AAC_DBG_RSP is set.

  \return  none
*/
void LSIdebug_display_requantized_spectrum_short(int *requantized_coef,
                                                int num_coef)
{
  int w,i;

  LSI_dprintf(AAC_DBG_RSP, "\n      --- AAC-Core Requantized Spectrum--------------------------------------");

  for (w=0; w<8; w++)
  {
    LSI_dprintf(AAC_DBG_RSP, "\n\n      window %1d",w);

    for (i=0; i<num_coef; i++)
    {
      if (i%16 == 0)
        LSI_dprintf(AAC_DBG_RSP,"\n      %4d ",i);

      LSI_dprintf(AAC_DBG_RSP, "0x%08lx ", requantized_coef[w*1024/8+i]);
    }
  }
  LSI_dprintf(AAC_DBG_RSP, "\n\n");
}

/*!
  \brief Print debug messages of time output

  The function prints the time output for long blocks, if the
  debug switch AAC_DBG_TI is set.

  \return  none
*/
void LSIdebug_display_time_output_long(short *time_output,
                                       int num_coef,
                                       int ch,
                                       int frameno)
{
  int i;

  LSI_dprintf(AAC_DBG_TI, "\n      --- [%d] AAC-Core Time Output CH%d-----------------------------------------------\n\n",frameno, ch);

#if 1  // reorder LRLRLR to LLLLLRRRRR
  for (i=0; i<num_coef; i+=2)
  {
    if (i%32 == 0)
      LSI_dprintf(AAC_DBG_TI,"\n      %4d ",i/2);

    LSI_dprintf(AAC_DBG_TI, "%04hX ",time_output[i]);
  }

#else
  for (i=0; i<num_coef; i++)
  {
    if (i%16 == 0)
      LSI_dprintf(AAC_DBG_TI,"\n      %4d ",i);

    LSI_dprintf(AAC_DBG_TI, "%6d ",time_output[i]);
  }
#endif

  LSI_dprintf(AAC_DBG_TI, "\n");
}

void LSIdebug_display_frame_no(int framecnt)
{
    LSI_dprintf(AAC_DBG_HDR, "\n=== AAC-Core Frame %d ===================================================\n\n", framecnt);
}

void LSIdebug_display_frame_size(int framecnt, unsigned int framesize, unsigned int totalbytes)
{
    LSI_dprintf(AAC_DBG_HDR, "\n\n  --- Decode Frame [%04d] -- size = 0x%x[%d]", framecnt,  framesize, totalbytes);
}

void LSIdebug_display_ltp_ft_doing(int exp, int *pFreqInfo)
{
    int i;
    
    LSI_dprintf(AAC_DBG_TI, "\n  --- AAC-Core LTP F/T Time Doing-----------------------------------------------\n\n");
    
    LSI_dprintf(AAC_DBG_TI, "\n      exp  : %d\n", exp);
    LSI_dprintf(AAC_DBG_TI, "\n        pFreqInfo \n");

    for (i = 0; i < 1024/8; i++)
    {
       LSI_dprintf(AAC_DBG_TI, "            [0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx]  \n",
               pFreqInfo[8*i],
               pFreqInfo[8*i + 1],
               pFreqInfo[8*i + 2],
               pFreqInfo[8*i + 3],
               pFreqInfo[8*i + 4],
               pFreqInfo[8*i + 5],
               pFreqInfo[8*i + 6],
               pFreqInfo[8*i + 7]);
    }


}

void LSIdebug_display_ltp_ft_in(int ch,
                       int *pSpectralCoefficient_int,
                       int *time_quant,
                       int qFormatNorm,
                       int               abs_max_per_window[])
{
   int i;
   
   LSI_dprintf(AAC_DBG_TI, "\n  --- AAC-Core LTP F/T Time Input-----------------------------------------------\n\n");

   LSI_dprintf(AAC_DBG_TI, "\n      Channel : %d\n", ch);
   LSI_dprintf(AAC_DBG_TI, "\n      qFormatNorm : 0x%lx\n", qFormatNorm);
   LSI_dprintf(AAC_DBG_TI, "\n      abs_max_per_window \n");
      LSI_dprintf(AAC_DBG_TI, "            [0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx]  \n",
              abs_max_per_window[0],
              abs_max_per_window[1],
              abs_max_per_window[2],
              abs_max_per_window[3],
              abs_max_per_window[4],
              abs_max_per_window[5],
              abs_max_per_window[6],
              abs_max_per_window[7]);

   
   LSI_dprintf(AAC_DBG_TI, "\n        SpectralCoefficient \n");
   for (i = 0; i < 24/8; i++)
   {
      LSI_dprintf(AAC_DBG_TI, "            [0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx]  \n",
              pSpectralCoefficient_int[8*i],
              pSpectralCoefficient_int[8*i + 1],
              pSpectralCoefficient_int[8*i + 2],
              pSpectralCoefficient_int[8*i + 3],
              pSpectralCoefficient_int[8*i + 4],
              pSpectralCoefficient_int[8*i + 5],
              pSpectralCoefficient_int[8*i + 6],
              pSpectralCoefficient_int[8*i + 7]);
   }

  
  LSI_dprintf(AAC_DBG_TI, "\n        Time_Quant \n");
  for (i = 0; i < 24/8; i++)
  {
     LSI_dprintf(AAC_DBG_TI, "            [0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx]  \n",
             time_quant[8*i],
             time_quant[8*i + 1],
             time_quant[8*i + 2],
             time_quant[8*i + 3],
             time_quant[8*i + 4],
             time_quant[8*i + 5],
             time_quant[8*i + 6],
             time_quant[8*i + 7]);
  }
  
}

void LSIdebug_display_ltp_ft_out(int ch,
                       int *time_quant,
                       short *pTimeData)
{
	
   int i;
   
   LSI_dprintf(AAC_DBG_TI, "\n  --- AAC-Core LTP F/T Time Output-----------------------------------------------\n\n");

   LSI_dprintf(AAC_DBG_TI, "\n      Channel : %d\n", ch);

  
  LSI_dprintf(AAC_DBG_TI, "\n        Time_Quant \n");
  for (i = 0; i < 24/8; i++)
  {
     LSI_dprintf(AAC_DBG_TI, "            [0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx]  \n",
             time_quant[8*i],
             time_quant[8*i + 1],
             time_quant[8*i + 2],
             time_quant[8*i + 3],
             time_quant[8*i + 4],
             time_quant[8*i + 5],
             time_quant[8*i + 6],
             time_quant[8*i + 7]);
  }

  
  LSI_dprintf(AAC_DBG_TI, "\n        Time Data \n");
  for (i = 0; i < 24/8; i++)
  {
     LSI_dprintf(AAC_DBG_TI, "            [0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx][0x%08lx]  \n",
             pTimeData[8*i],
             pTimeData[8*i + 1],
             pTimeData[8*i + 2],
             pTimeData[8*i + 3],
             pTimeData[8*i + 4],
             pTimeData[8*i + 5],
             pTimeData[8*i + 6],
             pTimeData[8*i + 7]);
  }
}

#if 0
void LSIdebug_display_AacDecdoerChanneInfo_Addr(int ch, CAacDecoderChannelInfo *pAacDecoderChannelInfo)
{

    LSI_dprintf(AAC_DBG_COEF, "\n  --- AAC Channel %d Info Address ----------------------------", ch);

    LSI_dprintf(AAC_DBG_COEF, "\n      pAacDecoderChannelInfo  = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo));
    LSI_dprintf(AAC_DBG_COEF, "\n        pIcsInfo              = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo->pIcsInfo));
    LSI_dprintf(AAC_DBG_COEF, "\n        pTnsData              = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo->pTnsData));
    LSI_dprintf(AAC_DBG_COEF, "\n        pPulseData            = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo->pPulseData));
    LSI_dprintf(AAC_DBG_COEF, "\n        pRawDataInfo          = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo->pRawDataInfo));
    LSI_dprintf(AAC_DBG_COEF, "\n        pJointStereoData      = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo->pJointStereoData));
    LSI_dprintf(AAC_DBG_COEF, "\n        pPnsData              = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo->pPnsData));
    LSI_dprintf(AAC_DBG_COEF, "\n        pPnsInterChannelData  = 0x%lx ", (unsigned int)(pAacDecoderChannelInfo->pPnsInterChannelData));
}
#endif

void LSIdebug_display_ApplyMS_Coeff_Detail(int LeftCoefficientint, int RightCoefficientint, int accu64)
{
    LSI_dprintf(AAC_DBG_COEF, "\n  --- AAC-Core Apply MS Detail -----------------------------------------------");

    LSI_dprintf(AAC_DBG_COEF, "\n      Left Coef  = 0x%lx ", LeftCoefficientint);
    LSI_dprintf(AAC_DBG_COEF, "\n      Right Coef = 0x%lx ", RightCoefficientint);
    LSI_dprintf(AAC_DBG_COEF, "\n      accu64     = 0x%lx ", accu64);
}

void LSIdebug_display_ApplyMS_Coeff_In(int window, int band, int index, int LCoef, int RCoef)
{
    LSI_dprintf(AAC_DBG_COEF, "\n  --- AAC-Core Apply MS Input -----------------------------------------------");
    LSI_dprintf(AAC_DBG_COEF, "\n      window = %d ", window);
    LSI_dprintf(AAC_DBG_COEF, "\n      band = %d",  band);
    LSI_dprintf(AAC_DBG_COEF, "\n      index = %d ", index);
    LSI_dprintf(AAC_DBG_COEF, "\n      LCoef = 0x%lx ", LCoef);
    LSI_dprintf(AAC_DBG_COEF, "\n      RCoef = 0x%lx", RCoef);    
}

void LSIdebug_display_ApplyMS_Coeff(int *pSpectralCoefficient_int0,
                             int *pSpectralCoefficient_int1)
{
   int i;
   
   LSI_dprintf(AAC_DBG_COEF, "\n  --- AAC-Core Apply MS Output -----------------------------------------------\n\n");

   LSI_dprintf(AAC_DBG_COEF, "\n      L: SpectralCoefficient \n");
   for (i = 0; i < 1024; i++)
   {
       if (i%32 == 0)
         LSI_dprintf(AAC_DBG_COEF,"\n      %4d ",i);
       
       LSI_dprintf(AAC_DBG_COEF, "%08lX ",pSpectralCoefficient_int0[i]);
   }

   
   LSI_dprintf(AAC_DBG_COEF, "\n\n      R: SpectralCoefficient \n");
   for (i = 0; i < 1024; i++)
   {
       if (i%32 == 0)
         LSI_dprintf(AAC_DBG_COEF,"\n      %4d ",i);
       
       LSI_dprintf(AAC_DBG_COEF, "%08lX ",pSpectralCoefficient_int1[i]);
   }
}

void LSIdebug_display_buffer_status (int frameno,
                             int data_used,
                             unsigned char *pRdPtr,
                             unsigned char *pWrPtr)
{
   LSI_dprintf(AAC_DBG_BF, "\n  --- [%04d] Buffer Rd = [0x%08lx] Wr = [0x%08lx] Used = [%4d] ",
                                   frameno, 
                                   (unsigned int)(pRdPtr),
                                   (unsigned int)(pWrPtr),
                                   data_used);
}

void LSIdebug_display_ApplyIS_Coeff(int *pSpectralCoefficient_int0,
                             int *pSpectralCoefficient_int1)
{
   int i;
   
   LSI_dprintf(AAC_DBG_COEF, "\n  --- AAC-Core Apply IS Output -----------------------------------------------\n\n");

   LSI_dprintf(AAC_DBG_COEF, "\n      L: SpectralCoefficient \n");
   for (i = 0; i < 1024; i++)
   {
       if (i%32 == 0)
         LSI_dprintf(AAC_DBG_COEF,"\n      %4d ",i);
       
       LSI_dprintf(AAC_DBG_COEF, "%08lX ",pSpectralCoefficient_int0[i]);
   }

   
   LSI_dprintf(AAC_DBG_COEF, "\n\n      R: SpectralCoefficient \n");
   for (i = 0; i < 1024; i++)
   {
       if (i%32 == 0)
         LSI_dprintf(AAC_DBG_COEF,"\n      %4d ",i);
       
       LSI_dprintf(AAC_DBG_COEF, "%08lX ",pSpectralCoefficient_int1[i]);
   }
}

void LSIdebug_display_CoreAAC_ft_in(int ch,
                       int *pSpectralCoefficient_int,
                     short *pOverlapBuffer)
{
   int i;
   
   LSI_dprintf(AAC_DBG_COEF, "\n  --- AAC-Core F/T Time Input-----------------------------------------------\n\n");

   LSI_dprintf(AAC_DBG_COEF, "\n      Channel : %d\n", ch);
   LSI_dprintf(AAC_DBG_COEF, "\n        SpectralCoefficient \n");
   for (i = 0; i < 1024; i++)
   {
       if (i%32 == 0)
         LSI_dprintf(AAC_DBG_COEF,"\n      %4d ",i);
       
       LSI_dprintf(AAC_DBG_COEF, "%08lX ",pSpectralCoefficient_int[i]);
   }

  
  LSI_dprintf(AAC_DBG_COEF, "\n\n        OverlapBuffer \n");
  for (i = 0; i < 512; i++)
  {
  
      if (i%32 == 0)
        LSI_dprintf(AAC_DBG_COEF,"\n      %4d ",i);
      
      LSI_dprintf(AAC_DBG_COEF, "%04hX ",pOverlapBuffer[i]);

  }

  LSI_dprintf(AAC_DBG_COEF, "\n\n");

}

#ifdef DEBUG_TO_CONSOLE
void LSI_dprintf(int level,          /*!< Debugging level */ 
                 char *format,...     /*!< Variable number of arguments */ 
                 )
{
  va_list ap;

   if ((level == AAC_DBG_HDR))
//    || (level == AAC_DBG_CI))
   {
       va_start(ap, format);
       vprintf(format, ap);
       va_end(ap);
    }       
}
#else
                             
FILE *pLsiDbgFile;

void LSI_dbgOpen(void)
{
#ifdef __ANDROID__
  if( (pLsiDbgFile = fopen("/sdcard/AACDec_LSIdbg.txt","aw")) == NULL)   
#else
  if( (pLsiDbgFile = fopen("AACDec_LSIdbg.txt","aw")) == NULL) 
#endif  
  {
    printf(" Error for Open LSI debug file \n");
  }

}

void LSI_dbgClose(void)
{
  fclose(pLsiDbgFile);
}

void LSI_dprintf(int level,          /*!< Debugging level */ 
                 char *format,...     /*!< Variable number of arguments */ 
                 )
{
  va_list ap;
 
  LSI_dbgOpen();

  if (1) { //(level == AAC_DBG_TI) { //(LSI_debugLev & level){
        va_start(ap, format);
        vfprintf(pLsiDbgFile, format, ap);
        va_end(ap);
      }

  LSI_dbgClose();

}
#endif

#endif

