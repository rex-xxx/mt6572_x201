/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ngin3d.debugtools.android.serveragent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Iterator;

import com.mediatek.ngin3d.debugtools.messagepackage.Constants;
import com.mediatek.ngin3d.debugtools.messagepackage.NDBMessage;
import com.mediatek.ngin3d.debugtools.messagepackage.Utils;

public class SocketManager {
    private static final int CHANNEL_BUFFER_SIZE = 4096;
    private static final int SPLIT_SIZE = 4000;
    private static final int SOCKET_TIMEOUT = 50;
    private static final int IH_MORE_DATA = 4;

    private static final String CLIENT_NAME = "mediatek";

    private static int sGetDataInterval = 50;
    private static int sExtendHeaderId;
    private final IDebugee mIDebugee;
    private final StringBuilder mDeviceData = new StringBuilder();
    private byte[] mHugeData = new byte[SPLIT_SIZE + Constants.EXTEND_HEADER_SIZE];

    private Selector mSelector;
    private ServerSocketChannel mServerChannel;
    private final Hashtable<String, SocketChannel> mClients = new Hashtable<String, SocketChannel>();

    private final NDBMessage mNdbMsg = new NDBMessage();
    private final ParamObject mParamObject = new ParamObject();

    private boolean mKeepGetFrameAlive;
    private boolean mKeepGetDataAlive;
    private boolean mKeepRunServerAlive;

    private boolean mIsGettingCpu;
    private boolean mIsGettingFps;
    private boolean mIsGettingMem;

    public SocketManager(int serverPort, IDebugee dt) {
        try {
            openSocket(serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIDebugee = dt;
    }

    public void startListen() {
        new Thread() {
            public void run() {
                doListening();
            }
        }.start();
    }

    public void stopListen() {
        stopGetDeviceData();
        stopGetFrameInterval();
        mKeepRunServerAlive = false;
        // for if no client is connected
        if (!mClients.isEmpty()) {
            fireMsg(Constants.M_BYE, Constants.I_RP_SERVICE_ACTIVE_BYE);
            closeSocket();
        }
    }

    // create serversocket, use nio tech
    private void openSocket(int serverPort) throws IOException {
        mSelector = Selector.open();
        mServerChannel = ServerSocketChannel.open();
        mServerChannel.configureBlocking(false);
        mServerChannel.socket().bind(new InetSocketAddress(serverPort));
        mServerChannel.register(mSelector, SelectionKey.OP_ACCEPT);
        mKeepRunServerAlive = true;
    }

    private void closeSocket() {
        try {
            mSelector.close();
            mServerChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doListening() {
        ByteBuffer buffer = ByteBuffer.allocate(CHANNEL_BUFFER_SIZE);
        try {
            while (mKeepRunServerAlive) {

                if (mSelector.select() > 0) {
                    Iterator<SelectionKey> selectedKeys = mSelector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        SelectionKey key = selectedKeys.next();
                        selectedKeys.remove();

                        if (key.isAcceptable()) {
                            doAccept();
                        } else if (key.isReadable()) {
                            // because we don't register write event, so we use
                            // if/else if instead of two if
                            doClientReadEvent(key, buffer);
                        }
                    }
                }

            }
        } catch (ClosedSelectorException cse) {
            cse.printStackTrace();
        } catch (CancelledKeyException cke) {
            // key has been cancelled we can ignore that.
            cke.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSocket();
        }

    }

    private void doAccept() throws IOException {
        SocketChannel socketChannel = mServerChannel.accept();
        // set channel is nonblocking
        socketChannel.configureBlocking(false);
        // register the channel to readable for waiting receive data
        socketChannel.register(mSelector, SelectionKey.OP_READ);
    }

    private void doClientReadEvent(SelectionKey key, ByteBuffer buffer) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int mRead;
        buffer.clear();

        try {
            mRead = socketChannel.read(buffer);
        } catch (IOException e) {
            // The remote forcibly closed the connection, cancel
            // the selection key and close the channel.
            key.cancel();
            socketChannel.close();
            return;
        }

        if (mRead == -1) {
            // Remote entity shut the socket down cleanly. Do the
            // same from our end and cancel the channel.
            key.channel().close();
            key.cancel();
            return;
        }

        if (mNdbMsg.msgDecode(buffer.array())) {
            handleMSG(mNdbMsg, socketChannel);
        }
    }

    private void handleMSG(NDBMessage recvMsg, SocketChannel socketChannel) {
        switch (recvMsg.getMessageType()) {
        case Constants.M_HANDSHAKE:
            String msg = recvMsg.getMsgData();
            if (msg.equals(CLIENT_NAME)) {
                mClients.put(msg, socketChannel);
                fireMsg(Constants.M_HANDSHAKE, Constants.I_RP_HANDSHAKE_OK);
            } else {
                // if the handshake is wrong, we close this channel
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            break;

        case Constants.M_BYE:
            fireMsg(Constants.M_BYE, Constants.I_RP_BYE_OK);
            stopGetDeviceData();
            stopGetFrameInterval();
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case Constants.M_REQUEST:
            handleRequestEvent(recvMsg);
            break;

        case Constants.M_MODIFY:
            handleModifyEvent(recvMsg);
            break;

        case Constants.M_CONTROL:
            handleControlEvent(recvMsg);
            break;

        default:
            break;
        }
    }

    private void handleRequestEvent(NDBMessage recvMsg) {
        switch (recvMsg.getTypeItem()) {
        case Constants.I_RQ_STAGE_INFO_ALL:
            fireHugeMsg(Constants.M_RESPONSE, Constants.I_RP_GET_STAGE_DUMP_INFO_BYTEARRAY_OK,
                mIDebugee.dumpStage().getBytes());
            break;

        case Constants.I_RQ_STAGE_INFO_UNIT:
            String actorInfo = mIDebugee.dumpActorByID(Integer.parseInt(recvMsg.getMsgData()));
            if (actorInfo == null) {
                fireMsg(Constants.M_RESPONSE, Constants.I_RP_GET_STAGE_DUMP_INFO_FAIL);
            } else {
                // "{" compress message "}" is for client easy to handle
                fireHugeMsg(Constants.M_RESPONSE,
                    Constants.I_RP_GET_STAGE_DUMP_INFO_BYTEARRAY_OK,
                    ("{" + actorInfo + "}").getBytes());
            }
            break;

        case Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_ALL:
            startGetDeviceData();
            break;

        case Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_CPU:
            mIsGettingCpu = true;
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_SATRT_GET_DEVICE_CPU_OK);
            break;

        case Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_FPS:
            mIsGettingFps = true;
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_SATRT_GET_DEVICE_FPS_OK);
            break;

        case Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_MEM:
            mIsGettingMem = true;
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_SATRT_GET_DEVICE_MEM_OK);
            break;

        case Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_ALL:
            stopGetDeviceData();
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_STOP_GET_DEVICE_PERFORMANCE_OK);
            break;

        case Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_CPU:
            mIsGettingCpu = false;
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_STOP_GET_DEVICE_CPU_OK);
            break;

        case Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_FPS:
            mIsGettingFps = false;
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_STOP_GET_DEVICE_FPS_OK);
            break;

        case Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_MEM:
            mIsGettingMem = false;
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_STOP_GET_DEVICE_MEM_OK);
            break;

        case Constants.I_RQ_STOP_GET_FRAME_INTERVAL:
            stopGetFrameInterval();
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_NGIN3D_STOP_GET_FRAME_INTERVAL_OK);
            break;

        case Constants.I_RQ_SATRT_GET_FRAME_INTERVAL:
            startGetFrameInterval();
            break;

        case Constants.I_RQ_SET_DEVICE_PERFORMANCE_INTERVAL:
            sGetDataInterval = Integer.parseInt(recvMsg.getMsgData());
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_SET_DEVICE_PERFORMANCE_INTERVAL_OK);
            break;

        case Constants.I_RQ_ANIMATION_INFO_UNIT:

            String animationInfo = mIDebugee
                .animationDumpToJSONByID(Integer.parseInt(recvMsg.getMsgData()));
            if (animationInfo == null) {
                fireMsg(Constants.M_RESPONSE, Constants.I_RP_GET_ANIMATION_DUMP_INFO_FAIL);
            } else {
                fireHugeMsg(Constants.M_RESPONSE,
                    Constants.I_RP_GET_ANIMATION_DUMP_INFO_BYTEARRAY_OK,
                    animationInfo.getBytes());
            }
            break;

        default:
            break;
        }
    }

    private void handleModifyEvent(NDBMessage recvMsg) {
        boolean isModify = false;
        if (recvMsg.getTypeItem() == Constants.I_MD_MODIFY_STAGE_UNIT_BY_ID) {
            String[] items = recvMsg.getMsgData().split(" ");
            // items[0] is useless
            if (mParamObject.setParameters(items[2], items[3])) {
                isModify = mIDebugee.setParameterByID(Integer.parseInt(items[1]), mParamObject);
            }
        }

        fireMsg(Constants.M_RESPONSE, isModify
            ? Constants.I_RP_MODIFY_STAGE_OK : Constants.I_RP_MODIFY_STAGE_FAIL);
    }

    private void handleControlEvent(NDBMessage recvMsg) {
        switch (recvMsg.getTypeItem()) {
        case Constants.I_C_NGIN3D_PASUE_RENDERING:
            mIDebugee.pauseRender();
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_NGIN3D_PASUE_RENDERING_OK);
            break;

        case Constants.I_C_NGIN3D_RESUME_RENDERING:
            mIDebugee.resumeRender();
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_NGIN3D_RESUME_RENDERING_OK);
            break;

        case Constants.I_C_NGIN3D_TICKTIME_RENDERING:
            mIDebugee.tickRender(Integer.parseInt(recvMsg.getMsgData()));
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_NGIN3D_TICKTIME_RENDERING_OK);
            break;

        case Constants.I_C_NGIN3D_REMOVE_ACTOR:
            mIDebugee.removeActorByID(Integer.parseInt(recvMsg.getMsgData()));
            fireMsg(Constants.M_RESPONSE, Constants.I_RP_NGIN3D_REMOVE_ACTOR_BY_ID_OK);
            break;

        default:
            break;
        }
    }

    private void startGetDeviceData() {
        // We start the thread which read the values.
        mKeepGetDataAlive = true;
        startGetDataThread();
    }

    private void stopGetDeviceData() {
        mIsGettingCpu = false;
        mIsGettingFps = false;
        mIsGettingMem = false;
        mKeepGetDataAlive = false;
    }

    private void startGetDataThread() {
        new Thread() {
            public void run() {
                while (mKeepGetDataAlive) {
                    fireDeviceDataEvent();
                    try {
                        Thread.sleep(sGetDataInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void fireDeviceDataEvent() {
        // fire the whole device info data
        if (mIsGettingCpu) {
            mDeviceData.append(mIDebugee.getCpuUsage());
        } else {
            mDeviceData.append(-1);
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mIsGettingFps) {
            mDeviceData.append(" " + mIDebugee.getFPS());
        } else {
            mDeviceData.append(" " + -1);
        }
        if (mIsGettingMem) {
            mDeviceData.append(" " + mIDebugee.getMemoryInfo());
        } else {
            mDeviceData.append(" " + "null");
        }

        fireMsg(Constants.M_RESPONSE, Constants.I_RP_SATRT_GET_DEVICE_PERFORMANCE_OK, mDeviceData
            .toString().getBytes());
        mDeviceData.delete(0, mDeviceData.length());

    }

    private void startGetFrameInterval() {
        mKeepGetFrameAlive = true;
        startFrameIntervalThread();
    }

    private void startFrameIntervalThread() {
        new Thread() {
            public void run() {
                while (mKeepGetFrameAlive) {
                    fireMsg(Constants.M_RESPONSE,
                        Constants.I_RP_NGIN3D_START_GET_FRAME_INTERVAL_OK,
                        Utils.writeInt(mIDebugee.getFrameInterval()));
                }
            }
        }.start();
    }

    private void stopGetFrameInterval() {
        mKeepGetFrameAlive = false;
    }

    /**
     * write the huge data
     *
     * @param type
     * @param item
     * @param data
     */
    private void fireHugeMsg(byte type, byte item, byte[] data) {
        // is sExtendHeaderId necessary??
        // should we need to combine this with firemsg method?
        int offset = data.length;
        int count = 0;
        while ((offset -= SPLIT_SIZE) > 0) {
            Utils.writeInt(mHugeData, 0, sExtendHeaderId);
            // 1 means there is more data
            mHugeData[IH_MORE_DATA] = 1;
            System.arraycopy(data, SPLIT_SIZE * count, mHugeData, Constants.EXTEND_HEADER_SIZE,
                SPLIT_SIZE);
            fireMsg(type, item, mHugeData);
            count++;
        }
        Utils.writeInt(mHugeData, 0, sExtendHeaderId);
        mHugeData[IH_MORE_DATA] = 0;
        System.arraycopy(data, SPLIT_SIZE * count, mHugeData, Constants.EXTEND_HEADER_SIZE, offset
            + SPLIT_SIZE);
        fireMsg(type, item, mHugeData);
        sExtendHeaderId++;
    }

    /**
     * write the data to socket
     *
     * @param type
     * @param item
     */
    private void fireMsg(byte type, byte item) {
        try {
            mClients.get(CLIENT_NAME).write(ByteBuffer.wrap(mNdbMsg.msgEncode(type, item, null)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write the data to socket
     *
     * @param type
     * @param item
     * @param byte[] data
     */
    private void fireMsg(byte type, byte item, byte[] data) {
        try {
            mClients.get(CLIENT_NAME).write(
                ByteBuffer.wrap(mNdbMsg.msgEncode(type, item, data, true)));
        } catch (IOException e) {
            stopGetFrameInterval();
            stopGetDeviceData();
            e.printStackTrace();
        }
    }
}
