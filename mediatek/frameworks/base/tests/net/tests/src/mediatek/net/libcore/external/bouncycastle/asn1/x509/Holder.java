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

package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * The Holder object.
 * <p>
 * For an v2 attribute certificate this is:
 * 
 * <pre>
 *            Holder ::= SEQUENCE {
 *                  baseCertificateID   [0] IssuerSerial OPTIONAL,
 *                           -- the issuer and serial number of
 *                           -- the holder's Public Key Certificate
 *                  entityName          [1] GeneralNames OPTIONAL,
 *                           -- the name of the claimant or role
 *                  objectDigestInfo    [2] ObjectDigestInfo OPTIONAL
 *                           -- used to directly authenticate the holder,
 *                           -- for example, an executable
 *            }
 * </pre>
 * 
 * <p>
 * For an v1 attribute certificate this is:
 * 
 * <pre>
 *         subject CHOICE {
 *          baseCertificateID [0] IssuerSerial,
 *          -- associated with a Public Key Certificate
 *          subjectName [1] GeneralNames },
 *          -- associated with a name
 * </pre>
 */
public class Holder
    extends ASN1Encodable
{
    IssuerSerial baseCertificateID;

    GeneralNames entityName;

    ObjectDigestInfo objectDigestInfo;

    private int version = 1;

    public static Holder getInstance(Object obj)
    {
        if (obj instanceof Holder)
        {
            return (Holder)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new Holder((ASN1Sequence)obj);
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            return new Holder((ASN1TaggedObject)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    /**
     * Constructor for a holder for an v1 attribute certificate.
     * 
     * @param tagObj The ASN.1 tagged holder object.
     */
    public Holder(ASN1TaggedObject tagObj)
    {
        switch (tagObj.getTagNo())
        {
        case 0:
            baseCertificateID = IssuerSerial.getInstance(tagObj, false);
            break;
        case 1:
            entityName = GeneralNames.getInstance(tagObj, false);
            break;
        default:
            throw new IllegalArgumentException("unknown tag in Holder");
        }
        version = 0;
    }

    /**
     * Constructor for a holder for an v2 attribute certificate. *
     * 
     * @param seq The ASN.1 sequence.
     */
    public Holder(ASN1Sequence seq)
    {
        if (seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        for (int i = 0; i != seq.size(); i++)
        {
            ASN1TaggedObject tObj = ASN1TaggedObject.getInstance(seq
                .getObjectAt(i));

            switch (tObj.getTagNo())
            {
            case 0:
                baseCertificateID = IssuerSerial.getInstance(tObj, false);
                break;
            case 1:
                entityName = GeneralNames.getInstance(tObj, false);
                break;
            case 2:
                objectDigestInfo = ObjectDigestInfo.getInstance(tObj, false);
                break;
            default:
                throw new IllegalArgumentException("unknown tag in Holder");
            }
        }
        version = 1;
    }

    public Holder(IssuerSerial baseCertificateID)
    {
        this.baseCertificateID = baseCertificateID;
    }

    /**
     * Constructs a holder from a IssuerSerial.
     * @param baseCertificateID The IssuerSerial.
     * @param version The version of the attribute certificate. 
     */
    public Holder(IssuerSerial baseCertificateID, int version)
    {
        this.baseCertificateID = baseCertificateID;
        this.version = version;
    }
    
    /**
     * Returns 1 for v2 attribute certificates or 0 for v1 attribute
     * certificates. 
     * @return The version of the attribute certificate.
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Constructs a holder with an entityName for v2 attribute certificates or
     * with a subjectName for v1 attribute certificates.
     * 
     * @param entityName The entity or subject name.
     */
    public Holder(GeneralNames entityName)
    {
        this.entityName = entityName;
    }

    /**
     * Constructs a holder with an entityName for v2 attribute certificates or
     * with a subjectName for v1 attribute certificates.
     * 
     * @param entityName The entity or subject name.
     * @param version The version of the attribute certificate. 
     */
    public Holder(GeneralNames entityName, int version)
    {
        this.entityName = entityName;
        this.version = version;
    }
    
    /**
     * Constructs a holder from an object digest info.
     * 
     * @param objectDigestInfo The object digest info object.
     */
    public Holder(ObjectDigestInfo objectDigestInfo)
    {
        this.objectDigestInfo = objectDigestInfo;
    }

    public IssuerSerial getBaseCertificateID()
    {
        return baseCertificateID;
    }

    /**
     * Returns the entityName for an v2 attribute certificate or the subjectName
     * for an v1 attribute certificate.
     * 
     * @return The entityname or subjectname.
     */
    public GeneralNames getEntityName()
    {
        return entityName;
    }

    public ObjectDigestInfo getObjectDigestInfo()
    {
        return objectDigestInfo;
    }

    public DERObject toASN1Object()
    {
        if (version == 1)
        {
            ASN1EncodableVector v = new ASN1EncodableVector();

            if (baseCertificateID != null)
            {
                v.add(new DERTaggedObject(false, 0, baseCertificateID));
            }

            if (entityName != null)
            {
                v.add(new DERTaggedObject(false, 1, entityName));
            }

            if (objectDigestInfo != null)
            {
                v.add(new DERTaggedObject(false, 2, objectDigestInfo));
            }

            return new DERSequence(v);
        }
        else
        {
            if (entityName != null)
            {
                return new DERTaggedObject(false, 1, entityName);
            }
            else
            {
                return new DERTaggedObject(false, 0, baseCertificateID);
            }
        }
    }
}
