package com.android.providers.drm;

import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

public class Ntp {
    private static final String TAG = "Ntp";
    // if ntp encoutered exception, set offset as INVALID_OFFSET
    private static final int INVALID_OFFSET = 0x7fffffff;

    public static int sync(String host) {
        int retry = 2;
        int port = 123;
        int timeout = 3000;

        // get the address and NTP address request
        //
        InetAddress ipv4Addr = null;
        try {
            Log.v(TAG, "get address from host: " + host);
            ipv4Addr = InetAddress.getByName(host);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }

        int serviceStatus = -1;
        DatagramSocket socket = null;
        long responseTime = -1;
        int offset = 0;
        try {
            Log.v(TAG, "create datagram socket");
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout); // will force the InterruptedIOException

            for (int attempts = 0; attempts <= retry && serviceStatus != 1; attempts++) {
                try {
                    // Send NTP request
                    //
                    byte[] data = new NtpMessage().toByteArray();
                    DatagramPacket outgoing =
                        new DatagramPacket(data, data.length, ipv4Addr, port);
                    long sentTime = System.currentTimeMillis();
                    socket.send(outgoing);
                    Log.v(TAG, "sent via datagram socket");

                    // Get NTP Response
                    //
                    // byte[] buffer = new byte[512];
                    DatagramPacket incoming =
                        new DatagramPacket(data, data.length);
                    socket.receive(incoming);
                    responseTime = System.currentTimeMillis() - sentTime;
                    double destinationTimestamp =
                        (System.currentTimeMillis() / 1000.0) + 2208988800.0;

                    // Validate NTP Response
                    // IOException thrown if packet does not decode as expected.
                    NtpMessage msg = new NtpMessage(incoming.getData());
                    double localClockOffset =
                        ((msg.mReceiveTimestamp - msg.mOriginateTimestamp)
                         + (msg.mTransmitTimestamp - destinationTimestamp)) / 2;
                    offset = (int)localClockOffset;

                    Log.d(TAG, "local clock offset: " + offset);
                    serviceStatus = 1;
                } catch (InterruptedIOException ex) {
                    // Ignore, no response received.
                    Log.d(TAG, "InterruptedIOException caught, set offset as " + INVALID_OFFSET);
                    offset = INVALID_OFFSET;
                }
            }
        } catch (NoRouteToHostException e) {
            Log.e(TAG, "No route to host exception for address: " + ipv4Addr + ", set offset as " + INVALID_OFFSET);
            offset = INVALID_OFFSET;
        } catch (ConnectException e) {
            // Connection refused. Continue to retry.
            e.fillInStackTrace();
            Log.e(TAG, "Connection exception for address: " + ipv4Addr + ", set offset as " + INVALID_OFFSET);
            offset = INVALID_OFFSET;
        } catch (IOException ex) {
            ex.fillInStackTrace();
            Log.e(TAG, "IOException while polling address: " + ipv4Addr + ", set offset as " + INVALID_OFFSET);
            offset = INVALID_OFFSET;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return offset;
    }
}
