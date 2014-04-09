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

package com.mediatek.ngin3d;

import com.mediatek.ngin3d.animation.BasicAnimation;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.presentation.PresentationEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * A mechanism for batching multiple model-tree operations into atomic updates to the render tree. Besides, it will also help
 * to build animations for property change.
 */
public abstract class Transaction {
    /**
     * @hide
     */
    protected int mAnimationDuration = BasicAnimation.DEFAULT_DURATION;
    /**
     * @hide
     */
    protected Mode mAlphaMode = Mode.EASE_IN_OUT_QUAD;
    /**
     * @hide
     */
    protected Runnable mCompletion;

    /**
     * @hide
     */
    protected abstract class Modification {
        /**
         * Override to apply the modifications.
         */
        protected abstract void apply();
    }

    private static PresentationEngine.RenderCallback sRenderCallback;

    ///////////////////////////////////////////////////////////////////////////
    // static Transaction states

    private static ThreadLocal sTransactionStack = new ThreadLocal();
    private static List<Modification> sCommittedOperations = Collections.synchronizedList(new ArrayList<Modification>());
    private static List<Modification> sOperationsToApply = Collections.synchronizedList(new ArrayList<Modification>());

    private static Stack<Transaction> getTransactionStack() {
        Stack<Transaction> stack = (Stack<Transaction>) sTransactionStack.get();
        if (stack == null) {
            stack = new Stack<Transaction>();
            sTransactionStack.set(stack);
        }
        return stack;
    }

    /**
     * @hide
     */
    protected static List<Modification> getModificationList() {
        return sCommittedOperations;

    }

    /**
     * Begin a new transaction.
     */
    private static void begin(Transaction transaction) {
        Stack<Transaction> stack = getTransactionStack();
        stack.push(transaction);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Transaction API

    /**
     * @return active transaction or null if no transaction is active.
     */
    public static Transaction getActive() {
        Stack<Transaction> stack = getTransactionStack();
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }

    public static ImplicitAnimation beginImplicitAnimation() {
        ImplicitAnimation animation = new ImplicitAnimation();
        begin(animation);
        return animation;
    }

    public static BatchPropertyModification beginPropertiesModification() {
        BatchPropertyModification modification = new BatchPropertyModification();
        begin(modification);
        return modification;
    }

    /**
     * Change animation duration in active transaction.
     *
     * @param duration in milliseconds
     */
    public static void setAnimationDuration(int duration) {
        Transaction transaction = getActive();
        if (transaction != null) {
            transaction.mAnimationDuration = duration;
        }
    }

    /**
     * Change the alpha mode in active transaction.
     *
     * @param mode alpha mode, such as Mode.LINEAR.
     */
    public static void setAlphaMode(Mode mode) {
        Transaction transaction = getActive();
        if (transaction != null) {
            transaction.mAlphaMode = mode;
        }
    }

    public static void setCompletion(Runnable completion) {
        Transaction transaction = getActive();
        if (transaction != null) {
            transaction.mCompletion = completion;
        }
    }

    /**
     * Commit current transaction.
     */
    public static void commit() {
        Stack<Transaction> stack = getTransactionStack();
        stack.pop();
        if (stack.isEmpty()) {
            sOperationsToApply.addAll(sCommittedOperations);
            sCommittedOperations.clear();
            if (sRenderCallback != null) {
                sRenderCallback.requestRender();
            }
        }
    }

    /**
     * Commit all transactions.
     */
    public static void commitAll() {
        Stack<Transaction> stack = getTransactionStack();
        stack.clear();
        sOperationsToApply.addAll(sCommittedOperations);
        sCommittedOperations.clear();

        if (sRenderCallback != null) {
            sRenderCallback.requestRender();
        }
    }

    public static void applyOperations() {
        Stack<Transaction> stack = getTransactionStack();
        if (stack.isEmpty()) {
            synchronized (sOperationsToApply) {
                for (Modification modification : sOperationsToApply) {
                    modification.apply();
                }
                sOperationsToApply.clear();
            }
        }
    }

    public static void setRenderCallback(PresentationEngine.RenderCallback renderCallback) {
        sRenderCallback = renderCallback;
    }

    /**
     * Override to add property modification.
     *
     * @param target   target actor
     * @param property property to change
     * @param value    the new value
     */
    public abstract void addPropertyModification(Actor target, Property property, Object value);
}

