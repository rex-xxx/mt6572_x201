 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */


#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#include <fcntl.h>
#include <termios.h>

#include "common.h"
#include "masterclear_bp.h"


/* @ set usb in unsuspend mode */
int set_usb_unsuspend()
{
        int ret;
        int count = sizeof(setup_cmd)-1;

        FILE *fp = fopen(USB1_SETUP_FILE, "w");
        if (fp == NULL) {
            LOGE("Can't open %s\n", USB1_SETUP_FILE);
            fclose(fp);
            return 1;
        }
        else {
            if ((ret = fwrite(setup_cmd,1,count,fp))<count)
            {
              LOGE("Write %s failed.\n",USB1_SETUP_FILE);
              fclose(fp);
              return 1;
            }
        }
        fclose(fp);
        return 0;
}

/* @dump the  buff data to debug */
void CMD_DBG_data_dump(void* databuff, int len)
{
#ifdef TC_DEBUG
    int max_col = CMD_DBG_MAX_DUMP_COLS;
    int row = 0;
    int col = 0;
    int num_col;
    int buffer_index = 0;
    UINT8 value;

    char string_buffer[(TC_DBG_MAX_DUMP_COLS * 3) + 1];  /* Each column takes up 3 characters, plus NULL */
    for (row = 0; row <= (len / max_col); row++)
    {
        /* Reset string buffer for each new row */
        memset(string_buffer, 0, sizeof(string_buffer));

        /* For all rows, the number of columns is the max number, except for the last row */
        num_col = (row == (len / max_col)) ? (len % max_col) : max_col;
        if (num_col != 0)
        {
            for (col = 0; col < num_col; col++)
            {
                value = ((UINT8*)databuff)[buffer_index++];
                LOGE("%s%02x ", string_buffer, value);
            }
            LOGE("%s", string_buffer);
        }
    }

#endif
}

/*=============================================================================================*//**
@brief Initializes the communcation interface with the bp command engine

@return Status of initialization

@note
 - If no command engine is present, CMD_ENGINE_INIT_NOT_PRESENT must be returned
*//*==============================================================================================*/
CMD_ENGINE_INIT_T CMD_ENGINE_init(void)
{
    UINT8 try_count = 1;
    CMD_ENGINE_INIT_T status = CMD_ENGINE_INIT_FAIL;
    cmd_engine_fd = -1;
    /* Keep trying to connect to the command engine until we are successful, or we hit the retry max */
    usleep(4000000);
    while (cmd_engine_fd < 0)
    {
    LOGE("open dev\n");
        if ( (cmd_engine_fd = open (CMD_ENGINE_DEVICE, O_RDWR)) < 0 )
        {
            if (try_count == CMD_ENGINE_CONNECT_MAX_TRY)
            {
                LOGE("Reached max number of retries, giving up...\n");
                break;
            }
            else
            {
                /* Try to connect again, wait a bit before retry */
                try_count++;
                usleep(CMD_ENGINE_CONNECT_TRY_DELAY);
            }
        }
        else
        {
            struct termios tio;
            tcgetattr( cmd_engine_fd, &tio );

            /* Modify local options */
            tio.c_lflag &= ~( ECHO | ECHOE | ECHOK | ECHONL ); // None ECHO mode
            tio.c_lflag &= ~( ICANON | ISIG );    // raw data

            /* Modify input options */
            tio.c_iflag = IGNBRK | IGNPAR; //work code

            /* Modify output options */
            tio.c_oflag &= ~( OPOST );     // work code

            /* Modify control options */
            tio.c_cflag |= ( CLOCAL | CREAD | CRTSCTS );   //enable receiver & hardware flow control
            tio.c_cflag &= ~( CSIZE );     //disable bit mask for data bits
            tio.c_cflag |= CS8;            //set 8-bit characters
            tio.c_cflag &= ~( PARENB );    //disable parity bit*/

            /* Modify the Baud Rate */
            cfsetispeed( &tio, B115200 );
            cfsetospeed( &tio, B115200 );

            /* Clear the line and prepare to activate the new settings */
            tcflush( cmd_engine_fd, TCIFLUSH );

            /* Set the options */
            tcsetattr( cmd_engine_fd, TCSANOW, &tio );

            status = CMD_ENGINE_INIT_SUCCESS;
        }
    }
    return(status);
}

/*=========================================================================*//*
 brief read command response from bp
@param[in]  bytes_to_write - The number of bytes to read
@param[out] data           - Data to read

@return TRUE = success, FALSE = failure
*//*=================================================================================*/

BOOL CMD_ENGINE_read(UINT32 bytes_to_read, UINT8 *data)
{
    BOOL   is_success = FALSE;
    UINT32 total_bytes_read = 0;
    UINT32 bytes_read = 0;

    /* Return error if the aux engine handle is not init'd */
    if (cmd_engine_fd == CMD_ENGINE_FD_NOT_INIT)
    {
        LOGE(" engine device is not open!\n");
    }
    else
    {
        while( total_bytes_read != bytes_to_read )
        {
            bytes_read = read(cmd_engine_fd, &data[total_bytes_read], bytes_to_read - total_bytes_read);
            LOGE("Attempted to read %d bytes and read %d bytes.\n", bytes_to_read - total_bytes_read, bytes_read);

            if( bytes_read <= 0 )
            {
                LOGE("Failed to read  engine device.\n");
                break;
            }
            total_bytes_read += bytes_read;
        }
        if( total_bytes_read == bytes_to_read )
        {
            LOGE("Successfully read %d bytes.\n", total_bytes_read);
            is_success = TRUE;
        }
    }

    return (is_success);
}

/*=============================================================================================*//**
@brief Writes the specified number of bytes to the command engine

@param[in]  bytes_to_write - The number of bytes to write
@param[out] data           - Data to write

@return TRUE = success, FALSE = failure

@note
 - The write is synchronous, the function will block until the requested number of bytes are written
*//*==============================================================================================*/
BOOL CMD_ENGINE_write(UINT32 bytes_to_write, UINT8 *data)
{
    BOOL   is_success  = FALSE;
    UINT32 bytes_wrote = 0;

    /* Return error if the aux engine handle is not init'd */
    if (cmd_engine_fd == CMD_ENGINE_FD_NOT_INIT)
    {
        LOGE("engine device is not open!\n");
    }
    else
    {
        bytes_wrote = write(cmd_engine_fd, data, bytes_to_write);
        if (bytes_wrote != bytes_to_write)
        {
            LOGE("Failed to write to engine device, attempted to write %d bytes, but wrote %d.\n",bytes_to_write, bytes_wrote);
        }
        else
        {
            LOGE("Successfully wrote %d bytes.\n", bytes_wrote);
            is_success = TRUE;
        }
    }

    return (is_success);
}



/*=============================================================================================*//**
@brief Convert network byte order to host byte order for command request headers

@param[in]  hdr_in  - Network byte order command request header
@param[out] hdr_out - Host byte order command request header
*//*==============================================================================================*/
void CMD_ENGINE_UTIL_hdr_req_ntoh(CMD_DEFS_CMD_REQ_HDR_T* hdr_in,CMD_DEFS_CMD_REQ_HDR_T* hdr_out)
{
    memcpy(hdr_out, hdr_in, sizeof(CMD_DEFS_CMD_REQ_HDR_T));
    hdr_out->opcode = ((hdr_in->opcode & 0x00FF) << 8) | ((hdr_in->opcode & 0xFF00) >> 8);
    hdr_out->length = ((hdr_in->length & 0x000000FF) << 24) |
        ((hdr_in->length & 0x0000FF00) << 8) |
        ((hdr_in->length & 0x00FF0000) >> 8) |
        ((hdr_in->length & 0xFF000000) >> 24);

}

/*================================================================*//**
@ brief change bp from flash mode to normal mode
*//*=================================================================*/
int bp_flashmode_to_normalmode(void)
{
    int fd;
    ssize_t result;
    fd = open(MDM_CTRL_DEVICE, O_WRONLY);
    if (fd < 0)
    {
      LOGE("failed open mdm_ctrl\n");
      return fd;
    }
    LOGE("open mdm_ctrl ok\n");

    //shutdown BP
    // echo shutdown > /sys/class/radio/mdm6600/command
    result = write(fd, MDM_CMD_SHUTDONW, sizeof(MDM_CMD_SHUTDONW)-1);
    if (result < (ssize_t)(sizeof(MDM_CMD_SHUTDONW)-1))
    {
        LOGE("Failed to shutdown BP\n");
        return -1;
    }
    usleep(1000000);

    //set BP power up mode
    // echo bootmode_normal > /sys/class/radio/mdm6600/command
    result = write(fd, MDM_CMD_NORMAL_MODE, sizeof(MDM_CMD_NORMAL_MODE)-1);
    if (result < (ssize_t)(sizeof(MDM_CMD_NORMAL_MODE)-1))
    {
        LOGE("Failed to set BP boot mode\n");
        return -1;
    }

    //power up BP
    // echo powerup > /sys/class/radio/mdm6600/command
    result = write(fd, MDM_CMD_POWERUP, sizeof(MDM_CMD_POWERUP)-1);
    if (result < (ssize_t)(sizeof(MDM_CMD_POWERUP)-1))
    {
        LOGE("Failed to powerup BP\n");
        return -1;
    }
    usleep(2000000);
    close(fd);

    LOGE("Finished boot BP to normal mode\n");
    return 0;
}

/*=============================================================================================*//**
@brief BP master clear

*//*==============================================================================================*/

int bp_master_clear(void)
{
    UINT8                  bp_ver_len;
    const UINT8            *bp_rsp_data_ptr;
    int                    write_len  = 0;
    int                    read_len = 0;
    UINT8                  *write_buff = NULL;
    UINT8                  *read_buff = NULL;

    CMD_ENGINE_INIT_T  aux_status;
    CMD_DEFS_CMD_REQ_HDR_T cmd_header = {0};
    CMD_DEFS_CMD_RSP_HDR_T  c_rsp_hdr;

    write_len = sizeof(CMD_DEFS_CMD_REQ_HDR_T);
    /* Build up the CMD request */
    cmd_header.cmd_rsp_flag     = CMD_DEFS_HDR_FLAG_CMD_RSP_COMMAND;
    cmd_header.opcode           = CMD_CMN_DRV_BP_MASTERCLEAR_OPCODE;
    cmd_header.no_rsp_reqd_flag = CMD_DEFS_HDR_FLAG_RESPONSE_EXPECTED;
    cmd_header.length           = CMD_BP_MASTER_RESET_DATALENTH;

    /* cancel the usb suspend mode so that usb devices can be detected when bp power up */
    if(set_usb_unsuspend()!=0)
    {
       LOGE("USB is suspended, master clear is ignored.\n");
       return 1;
    }
    LOGE("finished unsuspend\n");

    /* change bp from flash mode to normal mode*/
    bp_flashmode_to_normalmode();
    LOGE("from flash to normal mode\n");
    /* Send the command and receive the response */
    aux_status = CMD_ENGINE_init();
    if (aux_status == CMD_ENGINE_INIT_NOT_PRESENT)
    {
        LOGE("Aux engine is not present, skipping  engine setup.\n");
        return 1;
    }
    else if (aux_status != CMD_ENGINE_INIT_SUCCESS)
    {
        LOGE("Failed to init the engine! aux_status = %d.\n", aux_status);
        return 1;
    }
    LOGE("engine init finished\n");
    write_len = sizeof(CMD_DEFS_CMD_REQ_HDR_T)+cmd_header.length;
    if ( (write_buff = (UINT8 *)malloc(write_len)) == NULL)
    {
        LOGE("Out of memory - malloc failed on write_buff, length = %d.\n", write_len);
        return 1;
    }

    CMD_ENGINE_UTIL_hdr_req_ntoh(&cmd_header, (CMD_DEFS_CMD_REQ_HDR_T *) write_buff);
    write_buff[write_len-1] = CMD_BP_MASTER_CLEAR;

    CMD_DBG_data_dump(write_buff, write_len);

    if (CMD_ENGINE_write(write_len, write_buff) != TRUE)
    {
           LOGE("Write data to aux engine failed!\n");
           return 1;
    }
    else
    {
       LOGE("Transferred %d byte(s) CMD opcode = 0x%04x to aux engine succeeded.\n",write_len, cmd_header.opcode);
    }
    LOGE("write finished\n");
    free(write_buff);
    /* Verify BP response was not a failure */
    if (CMD_ENGINE_read(sizeof(c_rsp_hdr), (UINT8 *) &c_rsp_hdr) != TRUE)
    {
            LOGE("Reading header failed!\n");
            return 1;
    }
    else
    {
            /* Network byte order to host byte order... */
           CMD_DBG_data_dump(&c_rsp_hdr, sizeof(c_rsp_hdr));
    }

    if ( (c_rsp_hdr.fail_flag & CMD_DEFS_RSP_FLAG_FAIL) ||
    ( (c_rsp_hdr.rsp_code != CMD_RSP_CODE_CMD_RSP_GENERIC) &&
         (c_rsp_hdr.rsp_code != CMD_RSP_CODE_NOT_SET) ) )
    {
         return 1;
    }

    close(cmd_engine_fd);
    return 0;
}
