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

package org.bouncycastle.jce;

import java.io.IOException;
import java.security.Principal;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.X509Name;

/**
 * a general extension of X509Name with a couple of extra methods and
 * constructors.
 * <p>
 * Objects of this type can be created from certificates and CRLs using the
 * PrincipalUtil class.
 * </p>
 * @see org.bouncycastle.jce.PrincipalUtil
 */
public class X509Principal
    extends X509Name
    implements Principal
{
    private static ASN1Sequence readSequence(
        ASN1InputStream aIn)
        throws IOException
    {
        try
        {
            return ASN1Sequence.getInstance(aIn.readObject());
        }
        catch (IllegalArgumentException e)
        {
            throw new IOException("not an ASN.1 Sequence: " + e);
        }
    }

    /**
     * Constructor from an encoded byte array.
     */
    public X509Principal(
        byte[]  bytes)
        throws IOException
    {
        super(readSequence(new ASN1InputStream(bytes)));
    }

    /**
     * Constructor from an X509Name object.
     */
    public X509Principal(
        X509Name  name)
    {
        super((ASN1Sequence)name.getDERObject());
    }

    /**
     * constructor from a table of attributes.
     * <p>
     * it's is assumed the table contains OID/String pairs.
     */
    public X509Principal(
        Hashtable  attributes)
    {
        super(attributes);
    }

    /**
     * constructor from a table of attributes and a vector giving the
     * specific ordering required for encoding or conversion to a string.
     * <p>
     * it's is assumed the table contains OID/String pairs.
     */
    public X509Principal(
        Vector      ordering,
        Hashtable   attributes)
    {
        super(ordering, attributes);
    }

    /**
     * constructor from a vector of attribute values and a vector of OIDs.
     */
    public X509Principal(
        Vector      oids,
        Vector      values)
    {
        super(oids, values);
    }

    /**
     * takes an X509 dir name as a string of the format "C=AU,ST=Victoria", or
     * some such, converting it into an ordered set of name attributes.
     */
    public X509Principal(
        String  dirName)
    {
        super(dirName);
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU,ST=Victoria", or
     * some such, converting it into an ordered set of name attributes. If reverse
     * is false the dir name will be encoded in the order of the (name, value) pairs 
     * presented, otherwise the encoding will start with the last (name, value) pair
     * and work back.
     */
    public X509Principal(
        boolean reverse,
        String  dirName)
    {
        super(reverse, dirName);
    }

    /**
     * Takes an X509 dir name as a string of the format "C=AU, ST=Victoria", or
     * some such, converting it into an ordered set of name attributes. lookUp 
     * should provide a table of lookups, indexed by lowercase only strings and
     * yielding a DERObjectIdentifier, other than that OID. and numeric oids
     * will be processed automatically.
     * <p>
     * If reverse is true, create the encoded version of the sequence starting
     * from the last element in the string.
     */
    public X509Principal(
        boolean     reverse,
        Hashtable   lookUp,
        String      dirName)
    {
        super(reverse, lookUp, dirName);
    }

    public String getName()
    {
        return this.toString();
    }

    /**
     * return a DER encoded byte array representing this object
     */
    public byte[] getEncoded()
    {
        try
        {
            return this.getEncoded(ASN1Encodable.DER);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.toString());
        }
    }
}
