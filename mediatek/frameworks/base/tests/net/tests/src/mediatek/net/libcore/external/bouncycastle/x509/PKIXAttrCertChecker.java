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

import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.util.Collection;
import java.util.Set;

public abstract class PKIXAttrCertChecker
    implements Cloneable
{

    /**
     * Returns an immutable <code>Set</code> of X.509 attribute certificate
     * extensions that this <code>PKIXAttrCertChecker</code> supports or
     * <code>null</code> if no extensions are supported.
     * <p>
     * Each element of the set is a <code>String</code> representing the
     * Object Identifier (OID) of the X.509 extension that is supported.
     * <p>
     * All X.509 attribute certificate extensions that a
     * <code>PKIXAttrCertChecker</code> might possibly be able to process
     * should be included in the set.
     * 
     * @return an immutable <code>Set</code> of X.509 extension OIDs (in
     *         <code>String</code> format) supported by this
     *         <code>PKIXAttrCertChecker</code>, or <code>null</code> if no
     *         extensions are supported
     */
    public abstract Set getSupportedExtensions();

    /**
     * Performs checks on the specified attribute certificate. Every handled
     * extension is rmeoved from the <code>unresolvedCritExts</code>
     * collection.
     * 
     * @param attrCert The attribute certificate to be checked.
     * @param certPath The certificate path which belongs to the attribute
     *            certificate issuer public key certificate.
     * @param holderCertPath The certificate path which belongs to the holder
     *            certificate.
     * @param unresolvedCritExts a <code>Collection</code> of OID strings
     *            representing the current set of unresolved critical extensions
     * @throws CertPathValidatorException if the specified attribute certificate
     *             does not pass the check.
     */
    public abstract void check(X509AttributeCertificate attrCert, CertPath certPath,
                                 CertPath holderCertPath, Collection unresolvedCritExts)
        throws CertPathValidatorException;

    /**
     * Returns a clone of this object.
     * 
     * @return a copy of this <code>PKIXAttrCertChecker</code>
     */
    public abstract Object clone();
}
