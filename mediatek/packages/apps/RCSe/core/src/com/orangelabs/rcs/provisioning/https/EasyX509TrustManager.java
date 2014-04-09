/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.provisioning.https;

import com.orangelabs.rcs.utils.logger.Logger;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * @author olamy
 * @version $Id: EasyX509TrustManager.java 765355 2009-04-15 20:59:07Z evenisse $
 * @since 1.2.3
 */
public class EasyX509TrustManager
    implements X509TrustManager
{

    private X509TrustManager standardTrustManager = null;

    /**
     * M: Added to make stack trust both the system default trusted CA
     * certificates and special certificates. @{
     */
    private static final String ALGORITHM = "PKIX";
    private static final String CERTIFICATE_TYPE = "X509";
    private PKIXParameters mCustomPKIXParameters = null;
    private static KeyStore sCustomTrustKeyStore = null;
    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(X509TrustManager.class.getName());
    /**
     * @}
     */
    
    /**
     * Constructor for EasyX509TrustManager.
     */
    public EasyX509TrustManager( KeyStore keystore )
        throws NoSuchAlgorithmException, KeyStoreException
    {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
        factory.init( keystore );
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if ( trustmanagers.length == 0 )
        {
            throw new NoSuchAlgorithmException( "no trust manager found" );
        }
        this.standardTrustManager = (X509TrustManager) trustmanagers[0];
        /**
         * M: Modified to make stack trust both the system default trusted CA
         * certificates and special certificates. @{
         */
        readCustomTrustedKeyStore();
        /**
         * @}
         */
    }

    /**
     * M: Modified to make stack trust both the system default trusted CA
     * certificates and special certificates. @{
     */
    /**
     * Constructor for EasyX509TrustManager.
     * 
     * @param keyStore The system default key store, always be null.
     * @param customKeyStore A custom trust key store.
     * @throws CertificateException
     */
    public EasyX509TrustManager(KeyStore keyStore, KeyStore customKeyStore)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException {
        super();
        logger.debug("EasyX509TrustManager(KeyStore keyStore, KeyStore customKeyStore) entry. customKeyStore = "
                + customKeyStore);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory
                .getDefaultAlgorithm());
        factory.init(keyStore);
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if (trustmanagers.length == 0) {
            logger.error("no trust manager found");
            throw new NoSuchAlgorithmException("no trust manager found");
        }
        this.standardTrustManager = (X509TrustManager) trustmanagers[0];
        readCustomTrustedKeyStore();
    }
    /**
     * @}
     */
    
    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],String authType)
     */
    public void checkClientTrusted( X509Certificate[] certificates, String authType )
        throws CertificateException
    {
        standardTrustManager.checkClientTrusted( certificates, authType );
    }

    /**
     * M: Added to make stack trust both the system default trusted CA
     * certificates and special certificates. @{
     */
    /**
     * Check whether the server is trusted. Firstly use system's trust key
     * store, if fail then use a custom trusted key store
     * 
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],
     *      String authType)
     */
    public void checkServerTrusted(X509Certificate[] certificates, String authType)
            throws CertificateException {
        logger.debug("checkServerTrusted() entry()");
        if (certificates != null && certificates.length > 0) {
            logger.debug("checkServerTrusted():certificates[0] = " + certificates[0].toString()
                    + ", authType = " + authType);
        }
        try {
            standardTrustManager.checkServerTrusted(certificates, authType);
            logger.debug("The server was verified by system.");
            return;
        } catch (CertificateException e) {
            logger.debug("The server was not verified by system, then check the custom keystore.");
        }
        // System key store verified failed, check custom trusted key store
        CertPathValidator certPathValidator;
        try {
            certPathValidator = CertPathValidator.getInstance(ALGORITHM);
            CertificateFactory certificateFactory = CertificateFactory
                    .getInstance(CERTIFICATE_TYPE);
            CertPath certPath = certificateFactory.generateCertPath(Arrays.asList(certificates));
            if (mCustomPKIXParameters == null) {
                // key store is empty
                logger.debug("Custom keystore is empty");
                throw new CertificateException("Custom keystore is empty");
            }
            try {
                certPathValidator.validate(certPath, mCustomPKIXParameters);
                logger.debug("The server's certificate is in our custom keystore. It means the server is secure.");
                return;
            } catch (CertPathValidatorException e) {
                logger.warn("CertPathValidatorException: " + e.getMessage());
            } catch (InvalidAlgorithmParameterException e) {
                logger.warn("InvalidAlgorithmParameterException: " + e.getMessage());
            }
            if (certificates.length >= 1) {
                byte[] questionable = certificates[0].getEncoded();
                Set<TrustAnchor> anchors = mCustomPKIXParameters.getTrustAnchors();
                logger.debug("ahchors:" + Integer.toString(anchors.size()));
                for (TrustAnchor anchor : anchors) {
                    byte[] trusted = anchor.getTrustedCert().getEncoded();
                    if (Arrays.equals(questionable, trusted)) {
                        logger.debug("trust manager verified, that is, the server is trusted by special key store");
                        return;
                    }
                }
                logger.debug("trust manager do not verified");
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException: " + e.getMessage());
            e.printStackTrace();
        }
        logger.error("checkServerTrusted failed, that is, the server is not secure.");
        /**
         * M: Modified for debugging VOdafone test server so delete the below
         * line. @{
         */
        logger.error("For debug vf test server, then trust the server.");
        //throw new CertificateException("Untrust server key");
        /**
         * @}
         */
    }
    /**
     * @}
     */
    
    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers()
    {
        return this.standardTrustManager.getAcceptedIssuers();
    }
    
    /**
     * M: Added to make stack trust both the system default trusted CA
     * certificates and special certificates. @{
     */
    /**
     * Load a custom key store which contains a special certificate such as
     * vodafone's CA certificate.
     */
    private void readCustomTrustedKeyStore() {
        logger.debug("readCustomTrustedKeyStore() entry");
        Set<TrustAnchor> trusted = new HashSet<TrustAnchor>();
        try {
            logger.debug("sCustomTrustKeyStore = " + sCustomTrustKeyStore);
            if(sCustomTrustKeyStore == null){
                logger.error("sExtraTrustKeyStore is null");
                return;
            }
            for (Enumeration<String> en = sCustomTrustKeyStore.aliases(); en.hasMoreElements();) {
                final String alias = en.nextElement();
                logger.debug("alias = " + alias);
                final X509Certificate cert = (X509Certificate) sCustomTrustKeyStore
                        .getCertificate(alias);
                if (cert != null) {
                    logger.debug("add certificate to trust");
                    trusted.add(new TrustAnchor(cert, null));
                }else{
                    logger.debug("certificate is null");
                }
            }
            mCustomPKIXParameters = new PKIXParameters(trusted);
            mCustomPKIXParameters.setRevocationEnabled(false);
        } catch (KeyStoreException e) {
            logger.error("KeyStoreException: " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("InvalidAlgorithmParameterException: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    /**
     * Add a extra custom trusted key store. {@link EasyX509TrustManager} allow
     * you to set only one custom trusted key store. Notice that please call
     * this method before construct a {@link EasyX509TrustManager} object.
     * 
     * @param keyStore A custom trusted key store.
     */
    public static void setCustomTrustedKeyStore(KeyStore keyStore){
        logger.debug("setCustomTrustedKeyStore(), keyStore = " + keyStore);
        sCustomTrustKeyStore = keyStore;
    }
    /**
     */
}
