#ifndef __DP_ISP_STREAM_H__
#define __DP_ISP_STREAM_H__

#include "DpDataType.h"
#include "tpipe_config.h"

#define ISP_MAX_OUTPUT_PORT_NUM     3

class DpStream;
class DpChannel;
class DpBasicBufferPool;

class DpIspStream
{
public:
    enum ISPStreamType
    {
        ISP_IC_STREAM,
        ISP_VR_STREAM,
        ISP_ZSD_STREAM,
        ISP_IP_STREAM,
        ISP_VSS_STREAM
    };

    DpIspStream(ISPStreamType type);

    ~DpIspStream();

    /**
     * Description:
     *     Set source buffer base address with size
     *
     * Parameter:
     *     pAddr: Buffer base address
     *     size: Ssource buffer size
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM registerSrcBuffer(void     *pAddr,
                                     uint32_t size);

    /**
     * Description:
     *     Set source buffer base address with size
     *
     * Parameter:
     *     pAddrList: Buffer address list
     *     pSizeList: Buffer size list
     *     planeNum: Buffer plane count
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM registerSrcBuffer(void     **pAddrList,
                                     uint32_t *pSizeList,
                                     int32_t  planeNum);

    /**
     * Description:
     *     Source source buffer address with size
     *
     * Parameter:
     *     fileDesc: ION file descriptor
     *     pSizeList: buffer size list
     *     planeNum: buffer plane count
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM registerSrcBuffer(int32_t  fileDesc,
                                     uint32_t *pSizeList,
                                     int32_t  planeNum);

    /**
     * Description:
     *     Set source buffer configuration information
     *
     * Parameter:
     *     srcFormat: Source buffer format
     *     srcWidth: Source buffer width
     *     srcHeight: Source buffer height
     *     srcPitch: Source buffer pitch
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM setSrcConfig(DpColorFormat srcFormat,
                                int32_t       srcWidth,
                                int32_t       srcHeight,
                                int32_t       srcPitch);

    /**
     * Description:
     *     Set source buffer crop window information
     *
     * Parameter:
     *     XStart: Source crop X start coordinate
     *     XSubpixel: Source crop X subpixel coordinate
     *     YStart: Source crop Y start coordinate
     *     YSubpixel: Source crop Y subpixel coordinate
     *     cropWidth: Source crop window width
     *     cropHeight: Source crop window height
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM setSrcCrop(int32_t XStart,
                              int32_t XSubpixel,
                              int32_t YStart,
                              int32_t YSubpixel,
                              int32_t cropWidth,
                              int32_t cropHeight);

    /**
     * Description:
     *     Set destination buffer information
     *
     * Parameter:
     *     port: Port index number
     *     pAddr: Buffer base address
     *     size: Buffer size list
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM registerDstBuffer(int32_t  portIndex,
                                     void     *pAddr,
                                     uint32_t size);

    /**
     * Description:
     *     Set destination buffer information
     *
     * Parameter:
     *     portIndex: Port index number
     *     pAddrList: Buffer address list
     *     pSizeList: Buffer size list
     *     planeNum: Buffer plane count
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM registerDstBuffer(int32_t  portIndex,
                                     void     **pAddrList,
                                     uint32_t *pSizeList,
                                     int32_t  planeNum);

    /**
     * Description:
     *     Set destination buffer information
     *
     * Parameter:
     *     portIndex: Port index number
     *     fileDesc: ION file descriptor
     *     pSizeList: buffer size list
     *     planeNum: buffer plane count 
     * 
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM registerDstBuffer(int32_t  portIndex,
                                     int32_t  fileDesc,
                                     uint32_t *pSizeList,
                                     int32_t  planeNum);

    /**
     * Description:
     *     Set destination buffer configuration information
     *
     * Parameter:
     *     format: Destination buffer format
     *     width: Destination buffer width
     *     height: Destination buffer height
     *     pitch: Destination buffer pitch
     *     port: Destination port number
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM setDstConfig(int32_t       portIndex,
                                DpColorFormat dstFormat,
                                int32_t       dstWidth,
                                int32_t       dstHeight,
                                int32_t       dstPitch);

    /**
     * Description:
     *     Set port desired rotation angles
     *
     * Parameter:
     *     portIndex: Port index number
     *     rotation: Desired rotation angle
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM setRotation(int32_t portIndex,
                               int32_t rotation);

    /**
     * Description:
     *     Set port desired flip status
     *
     * Parameter:
     *    portIndex: Port index number
     *    flipStatus: Desired flip status
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM setFlipStatus(int32_t  portIndex,
                                 bool     flipStatus);


    /**
     * Description:
     *     Set extra parameter for ISP
     *
     * Parameter:
     *     extraPara: ISP extra parameters
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM setParameter(ISP_TPIPE_CONFIG_STRUCT &extraPara);


    /**
     * Description:
     *     Start ISP stream processing (non-blocking)
     *
     * Parameter:
     *     None
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM startStream();


    /**
     * Description:
     *     Dequeue a source buffer for SW processing
     *
     * Parameter:
     *     pBufID: Pointer to the buffer ID
     *     base: buffer virtual base address
     *     waitBuf: true for the buffer is ready;
     *              else return immediately
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM dequeueSrcBuffer(int32_t    *pBufID,
                                    uint32_t   base[3],
                                    bool       waitBuf = true);

    /**
     * Description:
     *    Queue a source buffer for HW processing
     *
     * Parameter:
     *    bufID: The specified buffer ID
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM queueSrcBuffer(int32_t bufID);


    /**
     * Description:
     *     Acquire a destination buffer for HW processing
     *
     * Parameter:
     *     port: Output port index
     *     base: buffer virtual base address
     *     waitBuf: true for the buffer is ready;
     *              else return immediately
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM dequeueDstBuffer(int32_t  portIndex,
                                    int32_t  *pBufID,
                                    uint32_t base[3],
                                    bool     waitBuf = true);

    /**
     * Description:
     *     Release buffer to the output port
     *
     * Parameter:
     *     port: The specified output port
     *     bufID: The specified buffer ID
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM queueDstBuffer(int32_t portIndex,
                                  int32_t bufID);

    /**
     * Description:
     *     Stop ISP stream processing
     *
     * Parameter:
     *     None
     *
     * Return Value:
     *     Return DP_STATUS_RETURN_SUCCESS if the API succeeded,
     *     else the API will return the error code.
     */
    DP_STATUS_ENUM stopStream();

private:
    ISPStreamType           m_streamType;
    bool                    m_frameChange;
    DpStream                *m_pStream;
    DpChannel               *m_pChannel;
    int32_t                 m_channelID;

    // Source information
    DpBasicBufferPool       *m_pSrcPool;
    int32_t                 m_srcBuffer;
    DpColorFormat           m_srcFormat;
    int32_t                 m_srcWidth;
    int32_t                 m_srcHeight;
    int32_t                 m_srcPitch;
    bool                    m_cropChange;

    // Destination information
    DpBasicBufferPool       *m_pDstPool[ISP_MAX_OUTPUT_PORT_NUM];
    int32_t                 m_dstBuffer[ISP_MAX_OUTPUT_PORT_NUM];
    DpColorFormat           m_dstFormat[ISP_MAX_OUTPUT_PORT_NUM];
    int32_t                 m_dstWidth[ISP_MAX_OUTPUT_PORT_NUM];
    int32_t                 m_dstHeight[ISP_MAX_OUTPUT_PORT_NUM];
    int32_t                 m_dstPitch[ISP_MAX_OUTPUT_PORT_NUM];
    int32_t                 m_rotation[ISP_MAX_OUTPUT_PORT_NUM];
    bool                    m_flipStatus[ISP_MAX_OUTPUT_PORT_NUM];
    bool                    m_dstEnable[ISP_MAX_OUTPUT_PORT_NUM];

    // Crop information
    int32_t                 m_srcXStart;
    int32_t                 m_srcXSubpixel;
    int32_t                 m_srcYStart;
    int32_t                 m_srcYSubpixel;
    int32_t                 m_cropWidth;
    int32_t                 m_cropHeight;

    bool                    m_parameterSet;
    ISP_TPIPE_CONFIG_STRUCT *m_pParameter;
};

#endif  // __DP_ISP_STREAM_H__
