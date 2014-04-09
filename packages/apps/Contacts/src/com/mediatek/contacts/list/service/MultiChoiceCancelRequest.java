package com.mediatek.contacts.list.service;

/**
 * Class representing one request for canceling multichoice handler.
 */
public class MultiChoiceCancelRequest {
    public final int jobId;

    public MultiChoiceCancelRequest(int id) {
        jobId = id;
    }
}
