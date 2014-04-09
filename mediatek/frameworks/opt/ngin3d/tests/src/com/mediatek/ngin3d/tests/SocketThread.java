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

package com.mediatek.ngin3d.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import com.mediatek.ngin3d.debugtools.messagepackage.Constants;
import com.mediatek.ngin3d.debugtools.messagepackage.NDBMessage;

public class SocketThread extends Thread {
    private static final int CHANNEL_BUFFER_SIZE = 4096;
    private Selector mSelector = null;
    private SocketChannel mChannel = null;
    private SelectionKey mClientKey = null;
    private SelectionKey mKey;
    private String mUserName;
    public boolean mClientWhile = true;
    private NDBMessage mFireMsg = new NDBMessage();
    private NDBMessage mReceMsg = new NDBMessage();

    /**
     * constructor of network IO thread
     *
     * @param username , assign a commander, for example "mediatek"
     */
    public SocketThread(String username) {
        try {
            System.out.println("SocketThread is create...");
            mSelector = Selector.open();
            mChannel = SocketChannel.open();
            mChannel.configureBlocking(false);
            mClientKey = mChannel.register(mSelector, SelectionKey.OP_CONNECT);
            mChannel.connect(new InetSocketAddress("localhost", 31286));
            mUserName = username;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (mClientWhile) {
                if (mSelector.select() > 0) {
                    System.out.println("SocketThread is running...");
                    for (Iterator<SelectionKey> it = mSelector.selectedKeys().iterator(); it
                            .hasNext();) {
                        mKey = it.next();
                        if (mKey.isConnectable()) {
                            doClinetSocketEvent(mKey);
                        } else if (mKey.isReadable()) {
                            doClientReadEvent(mKey);
                        }
                        it.remove();
                    }
                }
            }
        } catch (CancelledKeyException cke) {
            // key has been cancelled we can ignore that.
        } catch (ClosedSelectorException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } catch (java.util.ConcurrentModificationException e) {
            e.printStackTrace();
        }
        closeSocket();
        System.out.println("stop NIO thread");

    }

    private void doClinetSocketEvent(SelectionKey mkey) throws IOException {
        mChannel = (SocketChannel) mKey.channel();
        try {
            mChannel.finishConnect();
            mChannel.register(mSelector, SelectionKey.OP_READ);
            mChannel.write(ByteBuffer.wrap(mFireMsg.msgEncode(Constants.M_HANDSHAKE,
                    Constants.H_HANDSHAKE_SYNC, (mUserName).getBytes())));
        } catch (ConnectException e) {
            reConnect();
        } catch (ClosedChannelException e) {
            reConnect();
        } catch (NotYetConnectedException e) {
            reConnect();
        }

        // NotYetConnectedException, should we need to catch this exception??

    }

    private void doClientReadEvent(SelectionKey mkey) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(CHANNEL_BUFFER_SIZE);
        mChannel = (SocketChannel) mkey.channel();
        int mRead = 0;

        try {
            mRead = mChannel.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mRead > 0) {
            buffer.flip();
            if (mReceMsg.msgDecode(buffer.array())) {
                handelReceiveEvent(mReceMsg);
            }

        } else if (mRead == -1) {
            // the server is closed
            System.out.println("mRead == -1..");
            mChannel.close();

            return;
        } else {
            System.out.println("mRead else..");
            mChannel.close();

        }
        buffer.clear();
    }

    public static String unCompressByteArrayToString(byte[] b) {
        if (b == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(b);// "ISO-8859-1"
        GZIPInputStream gunzip;
        try {
            gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[512];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }

    public byte mConstantsState;
    private final Object mReceiveEvent = new Object();

    public void waitForEventReceived() {
        try {
            synchronized (mReceiveEvent) {
                mReceiveEvent.wait(10000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handelReceiveEvent(NDBMessage mMessage) {
        mConstantsState = mMessage.getTypeItem();
        switch (mMessage.getMessageType()) {
            case Constants.M_HANDSHAKE:
                if (mConstantsState == Constants.I_RP_HANDSHAKE_OK) {

                }
                break;

            case Constants.M_BYE:
                if (mConstantsState == Constants.I_RP_BYE_OK) {
                    closeSocket();
                    System.exit(0);
                } else if (mConstantsState == Constants.I_RP_SERVICE_ACTIVE_BYE) {
                    closeSocket();
                }
                break;

            case Constants.M_RESPONSE:

                if (mConstantsState == Constants.I_RP_GET_STAGE_DUMP_INFO_BYTEARRAY_OK) {

                } else if (mConstantsState == Constants.I_RP_GET_STAGE_DUMP_INFO_STRING_OK) {

                } else if (mConstantsState == Constants.I_RP_MODIFY_STAGE_OK) {

                } else if (mConstantsState == Constants.I_RP_GET_STAGE_DUMP_INFO_FAIL) {

                } else if (mConstantsState == Constants.I_RP_MODIFY_STAGE_FAIL) {

                } else if (mConstantsState == Constants.I_RP_SATRT_GET_DEVICE_PERFORMANCE_OK) {

                    System.out.println("start get perf!!!!!!!!!!!!!!!!!!!!");
                } else if (mConstantsState == Constants.I_RP_STOP_GET_DEVICE_PERFORMANCE_OK) {

                } else if (mConstantsState == Constants.I_RP_GET_ANIMATION_DUMP_INFO_BYTEARRAY_OK) {

                } else if (mConstantsState == Constants.I_RP_GET_ANIMATION_DUMP_INFO_FAIL) {

                } else if (mConstantsState == Constants.I_RP_NGIN3D_START_GET_FRAME_INTERVAL_OK) {

                } else if (mConstantsState == Constants.I_RP_NGIN3D_STOP_GET_FRAME_INTERVAL_OK) {

                } else if (mConstantsState == Constants.I_RP_NGIN3D_REMOVE_ACTOR_BY_ID_OK) {

                } else if (mConstantsState == Constants.I_RP_NGIN3D_PASUE_RENDERING_OK) {

                } else if (mConstantsState == Constants.I_RP_NGIN3D_RESUME_RENDERING_OK) {

                } else if (mConstantsState == Constants.I_RP_NGIN3D_TICKTIME_RENDERING_OK) {

                } else if (mConstantsState == Constants.I_RP_SATRT_GET_DEVICE_CPU_OK) {

                } else if (mConstantsState == Constants.I_RP_SATRT_GET_DEVICE_MEM_OK) {

                } else if (mConstantsState == Constants.I_RP_SATRT_GET_DEVICE_FPS_OK) {

                } else if (mConstantsState == Constants.I_RP_STOP_GET_DEVICE_CPU_OK) {

                } else if (mConstantsState == Constants.I_RP_STOP_GET_DEVICE_MEM_OK) {

                } else if (mConstantsState == Constants.I_RP_STOP_GET_DEVICE_FPS_OK) {

                }
                break;

            default:
        }
        synchronized (mReceiveEvent) {
            mReceiveEvent.notifyAll();
        }
    }

    /**
     * send debug message
     *
     * @param cmd , the major command string, for example
     *            "msgsync or msgget or msgbye"
     */
    public void sendmsg(String readline) {
        String[] cmd = readline.split(" ");

        if (cmd[0].equals("sync")) {
            writeBufferToSend(Constants.M_HANDSHAKE, Constants.H_HANDSHAKE_SYNC);
        } else if (cmd[0].equals("get") && cmd.length == 1) {
            System.out.println("get all->");
            writeBufferToSend(Constants.M_REQUEST, Constants.I_RQ_STAGE_INFO_ALL);
        } else if (cmd[0].equals("get") && cmd.length == 2) {
            System.out.println("get unit->" + cmd[1]);
            writeBufferToSend(Constants.M_REQUEST, Constants.I_RQ_STAGE_INFO_UNIT,
                    cmd[1]);
        } else if (cmd[0].equals("modi") && cmd.length > 1) {
            writeBufferToSend(Constants.M_MODIFY,
                    Constants.I_MD_MODIFY_STAGE_UNIT_BY_ID, readline);
        } else if (cmd[0].equals("bye") && cmd.length == 1) {
            writeBufferToSend(Constants.M_BYE, Constants.I_B_BYE);
        } else if (readline.equals("pause ngin")) {
            writeBufferToSend(Constants.M_CONTROL,
                    Constants.I_C_NGIN3D_PASUE_RENDERING);
        } else if (readline.equals("resume ngin")) {
            writeBufferToSend(Constants.M_CONTROL,
                    Constants.I_C_NGIN3D_RESUME_RENDERING);
        } else if (cmd[0].equals("tick") && cmd.length == 1) {
            writeBufferToSend(Constants.M_CONTROL,
                    Constants.I_C_NGIN3D_TICKTIME_RENDERING, "1000");
        } else if (cmd[0].equals("tick") && cmd.length > 1) {
            writeBufferToSend(Constants.M_CONTROL,
                    Constants.I_C_NGIN3D_TICKTIME_RENDERING, cmd[1]);
        } else if (cmd[0].equals("get") && cmd[1].equals("animation") && cmd.length == 3) {
            writeBufferToSend(Constants.M_REQUEST, Constants.I_RQ_ANIMATION_INFO_UNIT,
                    cmd[2]);
        } else if (cmd[0].equals("start") && cmd[1].equals("perf") && cmd.length == 3) {
            if (cmd[2].equals("all")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_ALL);
            } else if (cmd[2].equals("cpu")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_CPU);
            } else if (cmd[2].equals("fps")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_FPS);
            } else if (cmd[2].equals("mem")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_SATRT_GET_DEVICE_PERFORMANCE_MEM);
            }
        } else if (cmd[0].equals("stop") && cmd[1].equals("perf") && cmd.length == 3) {
            if (cmd[2].equals("all")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_ALL);
            } else if (cmd[2].equals("cpu")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_CPU);
            } else if (cmd[2].equals("fps")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_FPS);
            } else if (cmd[2].equals("mem")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_STOP_GET_DEVICE_PERFORMANCE_MEM);
            }
        } else if (cmd[0].equals("start") && cmd.length == 2) {
            if (cmd[1].equals("frame")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_SATRT_GET_FRAME_INTERVAL);
            }
        } else if (cmd[0].equals("stop") && cmd.length == 2) {
            if (cmd[1].equals("frame")) {
                writeBufferToSend(Constants.M_REQUEST,
                        Constants.I_RQ_STOP_GET_FRAME_INTERVAL);
            }
        } else if (cmd[0].equals("search") && cmd.length == 2) {
            // DebugUI.pushEvent(event);
        } else if (cmd[0].equals("remove") && cmd.length == 2) {
            writeBufferToSend(Constants.M_CONTROL, Constants.I_C_NGIN3D_REMOVE_ACTOR,
                    cmd[1]);
        } else if (cmd[0].equals("set") && cmd.length == 2) {
            writeBufferToSend(Constants.M_REQUEST,
                    Constants.I_RQ_SET_DEVICE_PERFORMANCE_INTERVAL, cmd[1]);
        }
    }

    private void writeBufferToSend(byte message, byte item) {
        if (!mClientWhile || !mChannel.isConnected()) {
            return;
        }

        synchronized (mChannel) {
            try {
                mChannel = (SocketChannel) mClientKey.channel();

                mChannel.write(ByteBuffer.wrap(mFireMsg.msgEncode(message, item, null)));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeBufferToSend(byte message, byte item, String data) {
        if (!mClientWhile || !mChannel.isConnected()) {
            return;
        }
        synchronized (mChannel) {
            try {
                mChannel = (SocketChannel) mClientKey.channel();

                mChannel.write(ByteBuffer.wrap(mFireMsg.msgEncode(message, item,
                        data.getBytes())));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * inform to phone, the debug tools will disconnect.
     */
    public void informClose() {
        writeBufferToSend(Constants.M_BYE, Constants.I_B_BYE, null);
    }

    /**
     * inform to phone, the debug tools will disconnect and then close the PC
     * socket.
     */
    public void closeSocket() {
        try {
            if (mChannel != null) {
                mChannel.close();
            }
            if (mSelector != null) {
                mSelector.close();
            }

            mClientWhile = false;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void reConnect() {
        mClientWhile = true;

    }
}
