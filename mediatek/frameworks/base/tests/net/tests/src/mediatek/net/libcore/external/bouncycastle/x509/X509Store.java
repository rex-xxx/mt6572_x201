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
import org.bouncycastle.util.Store;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Collection;

public class X509Store
    implements Store
{
    public static X509Store getInstance(String type, X509StoreParameters parameters)
        throws NoSuchStoreException
    {
        try
        {
            X509Util.Implementation impl = X509Util.getImplementation("X509Store", type);

            return createStore(impl, parameters);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new NoSuchStoreException(e.getMessage());
        }
    }

    public static X509Store getInstance(String type, X509StoreParameters parameters, String provider)
        throws NoSuchStoreException, NoSuchProviderException
    {
        return getInstance(type, parameters, X509Util.getProvider(provider));
    }

    public static X509Store getInstance(String type, X509StoreParameters parameters, Provider provider)
        throws NoSuchStoreException
    {
        try
        {
            X509Util.Implementation impl = X509Util.getImplementation("X509Store", type, provider);

            return createStore(impl, parameters);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new NoSuchStoreException(e.getMessage());
        }
    }

    private static X509Store createStore(X509Util.Implementation impl, X509StoreParameters parameters)
    {
        X509StoreSpi spi = (X509StoreSpi)impl.getEngine();

        spi.engineInit(parameters);

        return new X509Store(impl.getProvider(), spi);
    }

    private Provider     _provider;
    private X509StoreSpi _spi;

    private X509Store(
        Provider provider,
        X509StoreSpi spi)
    {
        _provider = provider;
        _spi = spi;
    }

    public Provider getProvider()
    {
       return _provider;
    }

    public Collection getMatches(Selector selector)
    {
        return _spi.engineGetMatches(selector);
    }
}
