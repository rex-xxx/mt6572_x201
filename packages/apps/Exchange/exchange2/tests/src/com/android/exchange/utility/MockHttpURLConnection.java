package com.android.exchange.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

public class MockHttpURLConnection extends HttpURLConnection {

    public MockHttpURLConnection(URL url) {
        super(url);
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public int getResponseCode() throws IOException {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new ByteArrayOutputStream(10 * 1024);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // Just return a Ping status with "1" No Change
        Serializer s = new Serializer();
        s.start(Tags.PING_PING).data(Tags.PING_STATUS, "1").end().done();
        byte[] bytes = s.toByteArray();
        return new ByteArrayInputStream(bytes);
    }

}
