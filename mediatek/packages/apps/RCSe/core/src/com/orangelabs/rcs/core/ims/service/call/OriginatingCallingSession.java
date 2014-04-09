/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.orangelabs.rcs.core.ims.service.call;

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.sip.OriginatingSipSession;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating calling session for call USSD & star code
 * 
 */
public class OriginatingCallingSession extends OriginatingSipSession {
	
	/**
     * The logger
     */
    private final Logger mLogger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Dialed USSD & star codes phone number
     */
    
    private final String mDialedPhoneNumber;

    /**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param contact Remote contact
	 * @param featureTag Feature tag
	 * @param sdp SDP
	 * @param dialedPhoneNumber the dialed phone number
	 */
	public OriginatingCallingSession(ImsService parent, String contact, String featureTag, String sdp, String dialedPhoneNumber) {
		super(parent, contact, featureTag, sdp);
		mDialedPhoneNumber = dialedPhoneNumber;
	}

	/**
	 * Create a SIP INVITE request
	 * 
	 * @return SipRequest a SIP INVITE request
	 */
	protected SipRequest createSipInvite() {
        mLogger.debug("createSipInvite()");
        // Set the local SDP part in the dialog path
        String sdp = createSDPPart();
        setLocalSdp(sdp);
        getDialogPath().setLocalContent(getLocalSdp());

        // Create an INVITE request
        if (mLogger.isActivated()) {
            mLogger.info("Send INVITE");
        }
        try {
            SipRequest invite;
            // Set the Request-URI
            String sipURI = PhoneUtils.formatUSSDCodeToSipUri(mDialedPhoneNumber);
            getDialogPath().setTarget(sipURI);
            
            invite = SipMessageFactory.createInvite(getDialogPath(), new String[] {
                getFeatureTag()
            }, getLocalSdp());
            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);
            return invite;
        } catch (SipException e) {
            e.printStackTrace();
        }
        mLogger.error("Create sip invite failed, return null.");
        return null;
    }
	
	/**
	 * Send INVITE message
	 * 
	 * @param invite SIP INVITE
	 * @throws SipException
	 */
	@Override
	protected void sendInvite(SipRequest invite) throws SipException {
		// Send INVITE request
		SipTransactionContext ctx = getImsService().getImsModule().getSipManager().sendSipMessageAndWait(invite);
		
        // Wait response
        ctx.waitResponse(getResponseTimeout());

		// Analyze the received response 
        if (ctx.isSipResponse()) {
        	int statusCode = ctx.getStatusCode();
    		if (mLogger.isActivated()) {
        		mLogger.debug("Received INVITE response: " + statusCode);
        	}
    		SipResponse sipResponse = ctx.getSipResponse();
    		// A response has been received
            if (200 == statusCode) {
    	        // 200 OK
            	handle200OK(sipResponse);
            } else if (403 == statusCode) {
            	// 403 sent if rejected(duo to missing permissions of setting up INVITE)
            	handle403Reject(sipResponse);
            } else if (488 == statusCode) {
            	// 488 sent for success for any SS code activation using INVITE
            	handle488SuccessActivateSSCode(sipResponse);
            }else if (500 == statusCode) {
                // 500 sent if there are any problems related to setting up the facility
            	handle500FacilityProblem(sipResponse);
            } else {
                // Other error response
                handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED,
                        ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
            }
        } else {
            if (mLogger.isActivated()) {
                mLogger.debug("No response received for INVITE");
            }
            // No response received: failed
            handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED));
        }
	}
	
	/**
	 * Handle 200 0K response 
	 * 
	 * @param resp 200 OK response
	 */
	@Override
	public void handle200OK(SipResponse resp) {
		super.handle200OK(resp);
		
	}
	
	/**
	 * Handle 488 not acceptable here response 
	 * 
	 * @param resp 488 not acceptable here response
	 */
	private void handle488SuccessActivateSSCode(SipResponse resp) {
		// TODO It should to be completed in the future
		
	}

	/**
	 * Handle 500 problem setting up the facility response
	 * 
	 * @param resp 500 problem setting up the facility response
	 */
	private void handle500FacilityProblem(SipResponse resp) {
		// TODO It should to be completed in the future.
		
	}

	/**
	 * Handle 403 missing permissions for subscriber response
	 * 
	 * @param resp 403 missing permissions for subscriber response
	 */
	private void handle403Reject(SipResponse resp) {
		// TODO It should to be completed in the future.
		
	}
	
	/**
	 * Create the SDP part of the SIP INVITE request 
	 * 
	 * @return A SDP string
	 */
	private String createSDPPart() {
		// TODO It should to be completed in the future
        String sdp = "";
        return sdp;
	}

}
