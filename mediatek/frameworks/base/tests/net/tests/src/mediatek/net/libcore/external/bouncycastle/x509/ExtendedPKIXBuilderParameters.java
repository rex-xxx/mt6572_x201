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

import org.bouncycastle.util.Selector;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains extended parameters for PKIX certification path builders.
 * 
 * @see java.security.cert.PKIXBuilderParameters
 * @see org.bouncycastle.jce.provider.PKIXCertPathBuilderSpi
 */
public class ExtendedPKIXBuilderParameters extends ExtendedPKIXParameters
{

    private int maxPathLength = 5;

    private Set excludedCerts = Collections.EMPTY_SET;

    /**
     * Excluded certificates are not used for building a certification path.
     * <p>
     * The returned set is immutable.
     * 
     * @return Returns the excluded certificates.
     */
    public Set getExcludedCerts()
    {
        return Collections.unmodifiableSet(excludedCerts);
    }

    /**
     * Sets the excluded certificates which are not used for building a
     * certification path. If the <code>Set</code> is <code>null</code> an
     * empty set is assumed.
     * <p>
     * The given set is cloned to protect it against subsequent modifications.
     * 
     * @param excludedCerts The excluded certificates to set.
     */
    public void setExcludedCerts(Set excludedCerts)
    {
        if (excludedCerts == null)
        {
            excludedCerts = Collections.EMPTY_SET;
        }
        else
        {
            this.excludedCerts = new HashSet(excludedCerts);
        }
    }

    /**
     * Creates an instance of <code>PKIXBuilderParameters</code> with the
     * specified <code>Set</code> of most-trusted CAs. Each element of the set
     * is a {@link TrustAnchor TrustAnchor}.
     * 
     * <p>
     * Note that the <code>Set</code> is copied to protect against subsequent
     * modifications.
     * 
     * @param trustAnchors a <code>Set</code> of <code>TrustAnchor</code>s
     * @param targetConstraints a <code>Selector</code> specifying the
     *            constraints on the target certificate or attribute
     *            certificate.
     * @throws InvalidAlgorithmParameterException if <code>trustAnchors</code>
     *             is empty.
     * @throws NullPointerException if <code>trustAnchors</code> is
     *             <code>null</code>
     * @throws ClassCastException if any of the elements of
     *             <code>trustAnchors</code> is not of type
     *             <code>java.security.cert.TrustAnchor</code>
     */
    public ExtendedPKIXBuilderParameters(Set trustAnchors,
            Selector targetConstraints)
            throws InvalidAlgorithmParameterException
    {
        super(trustAnchors);
        setTargetConstraints(targetConstraints);
    }

    /**
     * Sets the maximum number of intermediate non-self-issued certificates in a
     * certification path. The PKIX <code>CertPathBuilder</code> must not
     * build paths longer then this length.
     * <p>
     * A value of 0 implies that the path can only contain a single certificate.
     * A value of -1 does not limit the length. The default length is 5.
     * 
     * <p>
     * 
     * The basic constraints extension of a CA certificate overrides this value
     * if smaller.
     * 
     * @param maxPathLength the maximum number of non-self-issued intermediate
     *            certificates in the certification path
     * @throws InvalidParameterException if <code>maxPathLength</code> is set
     *             to a value less than -1
     * 
     * @see org.bouncycastle.jce.provider.PKIXCertPathBuilderSpi
     * @see #getMaxPathLength
     */
    public void setMaxPathLength(int maxPathLength)
    {
        if (maxPathLength < -1)
        {
            throw new InvalidParameterException("The maximum path "
                    + "length parameter can not be less than -1.");
        }
        this.maxPathLength = maxPathLength;
    }

    /**
     * Returns the value of the maximum number of intermediate non-self-issued
     * certificates in the certification path.
     * 
     * @return the maximum number of non-self-issued intermediate certificates
     *         in the certification path, or -1 if no limit exists.
     * 
     * @see #setMaxPathLength(int)
     */
    public int getMaxPathLength()
    {
        return maxPathLength;
    }

    /**
     * Can alse handle <code>ExtendedPKIXBuilderParameters</code> and
     * <code>PKIXBuilderParameters</code>.
     * 
     * @param params Parameters to set.
     * @see org.bouncycastle.x509.ExtendedPKIXParameters#setParams(java.security.cert.PKIXParameters)
     */
    protected void setParams(PKIXParameters params)
    {
        super.setParams(params);
        if (params instanceof ExtendedPKIXBuilderParameters)
        {
            ExtendedPKIXBuilderParameters _params = (ExtendedPKIXBuilderParameters) params;
            maxPathLength = _params.maxPathLength;
            excludedCerts = new HashSet(_params.excludedCerts);
        }
        if (params instanceof PKIXBuilderParameters)
        {
            PKIXBuilderParameters _params = (PKIXBuilderParameters) params;
            maxPathLength = _params.getMaxPathLength();
        }
    }

    /**
     * Makes a copy of this <code>PKIXParameters</code> object. Changes to the
     * copy will not affect the original and vice versa.
     * 
     * @return a copy of this <code>PKIXParameters</code> object
     */
    public Object clone()
    {
        ExtendedPKIXBuilderParameters params = null;
        try
        {
            params = new ExtendedPKIXBuilderParameters(getTrustAnchors(),
                    getTargetConstraints());
        }
        catch (Exception e)
        {
            // cannot happen
            throw new RuntimeException(e.getMessage());
        }
        params.setParams(this);
        return params;
    }

    /**
     * Returns an instance of <code>ExtendedPKIXParameters</code> which can be
     * safely casted to <code>ExtendedPKIXBuilderParameters</code>.
     * <p>
     * This method can be used to get a copy from other
     * <code>PKIXBuilderParameters</code>, <code>PKIXParameters</code>,
     * and <code>ExtendedPKIXParameters</code> instances.
     * 
     * @param pkixParams The PKIX parameters to create a copy of.
     * @return An <code>ExtendedPKIXBuilderParameters</code> instance.
     */
    public static ExtendedPKIXParameters getInstance(PKIXParameters pkixParams)
    {
        ExtendedPKIXBuilderParameters params;
        try
        {
            params = new ExtendedPKIXBuilderParameters(pkixParams
                    .getTrustAnchors(), X509CertStoreSelector
                    .getInstance((X509CertSelector) pkixParams
                            .getTargetCertConstraints()));
        }
        catch (Exception e)
        {
            // cannot happen
            throw new RuntimeException(e.getMessage());
        }
        params.setParams(pkixParams);
        return params;
    }
}
