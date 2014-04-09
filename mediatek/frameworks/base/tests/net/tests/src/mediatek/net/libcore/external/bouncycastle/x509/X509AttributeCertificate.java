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

package org.bouncycastle.x509;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Extension;
import java.util.Date;

/**
 * Interface for an X.509 Attribute Certificate.
 */
public interface X509AttributeCertificate
    extends X509Extension
{   
    /**
     * Return the version number for the certificate.
     * 
     * @return the version number.
     */
    public int getVersion();
    
    /**
     * Return the serial number for the certificate.
     * 
     * @return the serial number.
     */
    public BigInteger getSerialNumber();
    
    /**
     * Return the date before which the certificate is not valid.
     * 
     * @return the "not valid before" date.
     */
    public Date getNotBefore();
    
    /**
     * Return the date after which the certificate is not valid.
     * 
     * @return the "not valid afer" date.
     */
    public Date getNotAfter();
    
    /**
     * Return the holder of the certificate.
     * 
     * @return the holder.
     */
    public AttributeCertificateHolder getHolder();
    
    /**
     * Return the issuer details for the certificate.
     * 
     * @return the issuer details.
     */
    public AttributeCertificateIssuer getIssuer();
    
    /**
     * Return the attributes contained in the attribute block in the certificate.
     * 
     * @return an array of attributes.
     */
    public X509Attribute[] getAttributes();
    
    /**
     * Return the attributes with the same type as the passed in oid.
     * 
     * @param oid the object identifier we wish to match.
     * @return an array of matched attributes, null if there is no match.
     */
    public X509Attribute[] getAttributes(String oid);
    
    public boolean[] getIssuerUniqueID();
    
    public void checkValidity()
        throws CertificateExpiredException, CertificateNotYetValidException;
    
    public void checkValidity(Date date)
        throws CertificateExpiredException, CertificateNotYetValidException;
    
    public byte[] getSignature();
    
    public void verify(PublicKey key, String provider)
            throws CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException, SignatureException;
    
    /**
     * Return an ASN.1 encoded byte array representing the attribute certificate.
     * 
     * @return an ASN.1 encoded byte array.
     * @throws IOException if the certificate cannot be encoded.
     */
    public byte[] getEncoded()
        throws IOException;
}
