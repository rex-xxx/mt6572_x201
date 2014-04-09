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

package org.bouncycastle.asn1.isismtt;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

public interface ISISMTTObjectIdentifiers
{

    static final ASN1ObjectIdentifier id_isismtt = new ASN1ObjectIdentifier("1.3.36.8");

    static final ASN1ObjectIdentifier id_isismtt_cp = id_isismtt.branch("1");

    /**
     * The id-isismtt-cp-accredited OID indicates that the certificate is a
     * qualified certificate according to Directive 1999/93/EC of the European
     * Parliament and of the Council of 13 December 1999 on a Community
     * Framework for Electronic Signatures, which additionally conforms the
     * special requirements of the SigG and has been issued by an accredited CA.
     */
    static final ASN1ObjectIdentifier id_isismtt_cp_accredited = id_isismtt_cp.branch("1");

    static final ASN1ObjectIdentifier id_isismtt_at = id_isismtt.branch("3");

    /**
     * Certificate extensionDate of certificate generation
     * 
     * <pre>
     *                DateOfCertGenSyntax ::= GeneralizedTime
     * </pre>
     */
    static final ASN1ObjectIdentifier id_isismtt_at_dateOfCertGen = id_isismtt_at.branch("1");

    /**
     * Attribute to indicate that the certificate holder may sign in the name of
     * a third person. May also be used as extension in a certificate.
     */
    static final ASN1ObjectIdentifier id_isismtt_at_procuration = id_isismtt_at.branch("2");

    /**
     * Attribute to indicate admissions to certain professions. May be used as
     * attribute in attribute certificate or as extension in a certificate
     */
    static final ASN1ObjectIdentifier id_isismtt_at_admission = id_isismtt_at.branch("3");

    /**
     * Monetary limit for transactions. The QcEuMonetaryLimit QC statement MUST
     * be used in new certificates in place of the extension/attribute
     * MonetaryLimit since January 1, 2004. For the sake of backward
     * compatibility with certificates already in use, SigG conforming
     * components MUST support MonetaryLimit (as well as QcEuLimitValue).
     */
    static final ASN1ObjectIdentifier id_isismtt_at_monetaryLimit = id_isismtt_at.branch("4");

    /**
     * A declaration of majority. May be used as attribute in attribute
     * certificate or as extension in a certificate
     */
    static final ASN1ObjectIdentifier id_isismtt_at_declarationOfMajority = id_isismtt_at.branch("5");

    /**
     * 
     * Serial number of the smart card containing the corresponding private key
     * 
     * <pre>
     *                 ICCSNSyntax ::= OCTET STRING (SIZE(8..20))
     * </pre>
     */
    static final ASN1ObjectIdentifier id_isismtt_at_iCCSN = id_isismtt_at.branch("6");

    /**
     * 
     * Reference for a file of a smartcard that stores the public key of this
     * certificate and that is used as �security anchor�.
     * 
     * <pre>
     *      PKReferenceSyntax ::= OCTET STRING (SIZE(20))
     * </pre>
     */
    static final ASN1ObjectIdentifier id_isismtt_at_PKReference = id_isismtt_at.branch("7");

    /**
     * Some other restriction regarding the usage of this certificate. May be
     * used as attribute in attribute certificate or as extension in a
     * certificate.
     * 
     * <pre>
     *             RestrictionSyntax ::= DirectoryString (SIZE(1..1024))
     * </pre>
     * 
     * @see org.bouncycastle.asn1.isismtt.x509.Restriction
     */
    static final ASN1ObjectIdentifier id_isismtt_at_restriction = id_isismtt_at.branch("8");

    /**
     * 
     * (Single)Request extension: Clients may include this extension in a
     * (single) Request to request the responder to send the certificate in the
     * response message along with the status information. Besides the LDAP
     * service, this extension provides another mechanism for the distribution
     * of certificates, which MAY optionally be provided by certificate
     * repositories.
     * 
     * <pre>
     *        RetrieveIfAllowed ::= BOOLEAN
     *       
     * </pre>
     */
    static final ASN1ObjectIdentifier id_isismtt_at_retrieveIfAllowed = id_isismtt_at.branch("9");

    /**
     * SingleOCSPResponse extension: The certificate requested by the client by
     * inserting the RetrieveIfAllowed extension in the request, will be
     * returned in this extension.
     * 
     * @see org.bouncycastle.asn1.isismtt.ocsp.RequestedCertificate
     */
    static final ASN1ObjectIdentifier id_isismtt_at_requestedCertificate = id_isismtt_at.branch("10");

    /**
     * Base ObjectIdentifier for naming authorities
     */
    static final ASN1ObjectIdentifier id_isismtt_at_namingAuthorities = id_isismtt_at.branch("11");

    /**
     * SingleOCSPResponse extension: Date, when certificate has been published
     * in the directory and status information has become available. Currently,
     * accrediting authorities enforce that SigG-conforming OCSP servers include
     * this extension in the responses.
     * 
     * <pre>
     *      CertInDirSince ::= GeneralizedTime
     * </pre>
     */
    static final ASN1ObjectIdentifier id_isismtt_at_certInDirSince = id_isismtt_at.branch("12");

    /**
     * Hash of a certificate in OCSP.
     * 
     * @see org.bouncycastle.asn1.isismtt.ocsp.CertHash
     */
    static final ASN1ObjectIdentifier id_isismtt_at_certHash = id_isismtt_at.branch("13");

    /**
     * <pre>
     *          NameAtBirth ::= DirectoryString(SIZE(1..64)
     * </pre>
     * 
     * Used in
     * {@link org.bouncycastle.asn1.x509.SubjectDirectoryAttributes SubjectDirectoryAttributes}
     */
    static final ASN1ObjectIdentifier id_isismtt_at_nameAtBirth = id_isismtt_at.branch("14");

    /**
     * Some other information of non-restrictive nature regarding the usage of
     * this certificate. May be used as attribute in atribute certificate or as
     * extension in a certificate.
     * 
     * <pre>
     *               AdditionalInformationSyntax ::= DirectoryString (SIZE(1..2048))
     * </pre>
     * 
     * @see org.bouncycastle.asn1.isismtt.x509.AdditionalInformationSyntax
     */
    static final ASN1ObjectIdentifier id_isismtt_at_additionalInformation = id_isismtt_at.branch("15");

    /**
     * Indicates that an attribute certificate exists, which limits the
     * usability of this public key certificate. Whenever verifying a signature
     * with the help of this certificate, the content of the corresponding
     * attribute certificate should be concerned. This extension MUST be
     * included in a PKC, if a corresponding attribute certificate (having the
     * PKC as base certificate) contains some attribute that restricts the
     * usability of the PKC too. Attribute certificates with restricting content
     * MUST always be included in the signed document.
     * 
     * <pre>
     *                   LiabilityLimitationFlagSyntax ::= BOOLEAN
     * </pre>
     */
    static final ASN1ObjectIdentifier id_isismtt_at_liabilityLimitationFlag = new ASN1ObjectIdentifier("0.2.262.1.10.12.0");
}
