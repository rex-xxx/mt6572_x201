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

import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.Mode;
import com.mediatek.ngin3d.animation.PropertyAnimation;

import java.util.ArrayList;
import java.util.List;

public class Transition extends AnimationGroup {

    public static final int FADE = 1;
    public static final int MOVE = 2;
    public static final int FLY = 3;
    public static final int PUSH = 4;
    public static final int FOLD = 5;

    public static final int REP_FOLD = 6;
    public static final int REP_FADE = 7;
    public static final int REP_ROTATE = 8;

    public static final int SUB_TOP = 101;
    public static final int SUB_BOTTOM = 102;
    public static final int SUB_RIGHT = 103;
    public static final int SUB_LEFT = 104;

    public static final int FOLD_SUB_HORIZONTAL = 201;
    public static final int FOLD_SUB_VERTICAL = 202;

    public static final int ANI_TYPE_IN = 1000;
    public static final int ANI_TYPE_OUT = 1001;

    private int mScreenWidth;
    private int mScreenHeight;
    private static final int DEFAULT_TRANSITION_DURATION = 500;
    private Animation mGoInAnimation;
    private Animation mGoOutAnimation;
    private AnimationGroup mGoInAnimationGroup;
    private AnimationGroup mGoOutAnimationGroup;
    private Mode mGoInMode;
    private Mode mGoOutMode;
    private int mGoInType;
    private int mGoInSubType;
    private int mGoOutType;
    private int mGoOutSubType;

    List<Actor> mInProgressAnimation = new ArrayList<Actor>();

    public Transition(int screenWidth, int screenHeight, int goIn, int goOut) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        setGoInType(goIn);
        setGoOutType(goOut);
    }

    public Transition(int screenWidth, int screenHeight, int goIn, int goInSub, int goOut, int goOutSub) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        setGoInType(goIn, goInSub);
        setGoOutType(goOut, goOutSub);
    }

    public Transition(int screenWidth, int screenHeight, int effect) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        setGoInType(effect);
    }

    public Transition(AnimationGroup goIn, AnimationGroup goOut) {
        mGoInAnimationGroup = goIn;
        mGoOutAnimationGroup = goOut;
    }

    private void setGoInType(int goInType) {
        mGoInType = goInType;
        if (mGoInSubType == 0) {
            if (mGoInType == FOLD) {
                mGoInSubType = FOLD_SUB_HORIZONTAL;
            } else {
                mGoInSubType = SUB_LEFT;
            }
        }

        if (mGoInMode == null) {
            switch (mGoInType) {
            case MOVE:
                mGoInMode = Mode.LINEAR;
                break;
            case FLY:
                mGoInMode = Mode.EASE_IN_CUBIC;
                break;
            case PUSH:
                mGoInMode = Mode.EASE_IN_OUT_CUBIC;
                break;
            default:
                mGoInMode = Mode.LINEAR;
            }
        }

    }

    private void setGoInType(int goInType, int goInSubType) {
        mGoInType = goInType;
        mGoInSubType = goInSubType;

        if (mGoInMode == null) {
            switch (mGoInType) {
            case MOVE:
                mGoInMode = Mode.LINEAR;
                break;
            case FLY:
                mGoInMode = Mode.EASE_IN_CUBIC;
                break;
            case PUSH:
                mGoInMode = Mode.EASE_IN_OUT_CUBIC;
                break;
            default:
                mGoInMode = Mode.LINEAR;
            }
        }
    }

    private void setGoOutType(int goOutType) {
        mGoOutType = goOutType;
        if (mGoInSubType == 0) {
            if (mGoOutType == FOLD) {
                mGoOutSubType = FOLD_SUB_HORIZONTAL;
            } else {
                mGoOutSubType = SUB_LEFT;
            }
        }

        if (mGoOutMode == null) {
            switch (mGoOutType) {
            case MOVE:
                mGoOutMode = Mode.LINEAR;
                break;
            case FLY:
                mGoOutMode = Mode.EASE_IN_CUBIC;
                break;
            case PUSH:
                mGoOutMode = Mode.EASE_IN_OUT_CUBIC;
                break;
            default:
                mGoOutMode = Mode.LINEAR;
            }
        }
    }

    private void setGoOutType(int goOutType, int goOutSubType) {
        mGoOutType = goOutType;
        mGoOutSubType = goOutSubType;

        if (mGoOutMode == null) {
            switch (mGoOutType) {
            case MOVE:
                mGoOutMode = Mode.LINEAR;
                break;
            case FLY:
                mGoOutMode = Mode.EASE_IN_CUBIC;
                break;
            case PUSH:
                mGoOutMode = Mode.EASE_IN_OUT_CUBIC;
                break;
            default:
                mGoOutMode = Mode.LINEAR;
            }
        }
    }

    private void applyGoInAnimation(Actor actor) {
        if (mGoInType == 0) {
            mGoInType = MOVE;
            mGoInSubType = SUB_LEFT;
            mGoInMode = Mode.LINEAR;
        }

        switch (mGoInType) {
        case FADE:
            if (actor instanceof Plane) {
                mGoInAnimation = new PropertyAnimation(actor, "color", new Color(0, 0, 0), new Color(255, 255, 255)).setDuration(DEFAULT_TRANSITION_DURATION);
            }
            break;
        case FOLD:
            if (mGoInSubType == FOLD_SUB_HORIZONTAL) {
                mGoInAnimation = new PropertyAnimation(actor, "rotation", new Rotation(-90, 0, 0), new Rotation(0, 0, 0)).setDuration(DEFAULT_TRANSITION_DURATION);
            } else if (mGoInSubType == FOLD_SUB_VERTICAL) {
                mGoInAnimation = new PropertyAnimation(actor, "rotation", new Rotation(0, -90, 0), new Rotation(0, 0, 0)).setDuration(DEFAULT_TRANSITION_DURATION);
            } else {
                throw new IllegalArgumentException("Illegal animation sub type.");
            }
            break;
        case MOVE:
            goInPositionAnimation(actor);
            break;
        case FLY:
            goInPositionAnimation(actor);
            break;
        case PUSH:
            goInPositionAnimation(actor);
            break;
        default:
            throw new IllegalArgumentException("No such animation type.");
        }
    }

    private void goInPositionAnimation(Actor actor) {
        if (mGoInSubType == SUB_TOP) {
            mGoInAnimation = new PropertyAnimation(actor, "position",
                new Point(actor.getPosition().x, -400), actor.getPosition()).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoInMode);
        } else if (mGoInSubType == SUB_BOTTOM) {
            mGoInAnimation = new PropertyAnimation(actor, "position",
                new Point(actor.getPosition().x, mScreenHeight + 10), actor.getPosition()).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoInMode);
        } else if (mGoInSubType == SUB_RIGHT) {
            mGoInAnimation = new PropertyAnimation(actor, "position",
                new Point(mScreenWidth + 10, actor.getPosition().y), actor.getPosition()).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoInMode);
        } else if (mGoInSubType == SUB_LEFT) {
            mGoInAnimation = new PropertyAnimation(actor, "position",
                new Point(-400, actor.getPosition().y), actor.getPosition()).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoInMode);
        } else {
            throw new IllegalArgumentException("Illegal animation sub type.");
        }
    }

    private void applyGoOutAnimation(Actor actor) {
        if (mGoOutType == 0) {
            mGoOutType = mGoInType;
            mGoOutSubType = mGoInSubType;
            mGoOutMode = mGoInMode;
        }

        switch (mGoOutType) {
        case FADE:
            if (actor instanceof Plane) {
                mGoOutAnimation = new PropertyAnimation(actor, "color", new Color(255, 255, 255), new Color(0, 0, 0)).setDuration(DEFAULT_TRANSITION_DURATION);
            }
            break;
        case FOLD:
            if (mGoOutSubType == FOLD_SUB_HORIZONTAL) {
                mGoOutAnimation = new PropertyAnimation(actor, "rotation", new Rotation(0, 0, 0), new Rotation(90, 0, 0)).setDuration(DEFAULT_TRANSITION_DURATION);
            } else if (mGoOutSubType == FOLD_SUB_VERTICAL) {
                mGoOutAnimation = new PropertyAnimation(actor, "rotation", new Rotation(0, 0, 0), new Rotation(0, 90, 0)).setDuration(DEFAULT_TRANSITION_DURATION);
            } else {
                throw new IllegalArgumentException("Illegal animation sub type.");
            }
            break;
        case MOVE:
            goOutPositionAnimation(actor);
            break;
        case FLY:
            goOutPositionAnimation(actor);
            break;
        case PUSH:
            goOutPositionAnimation(actor);
            break;
        default:
            throw new IllegalArgumentException("No such animation type.");
        }
    }

    private void goOutPositionAnimation(Actor actor) {
        if (mGoOutSubType == SUB_TOP) {
            mGoOutAnimation = new PropertyAnimation(
                actor,
                Actor.PROP_POSITION,
                actor.getPosition(),
                new Point(actor.getPosition().x, -400)).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoOutMode);
        } else if (mGoOutSubType == SUB_BOTTOM) {
            mGoOutAnimation = new PropertyAnimation(
                actor,
                Actor.PROP_POSITION,
                actor.getPosition(),
                new Point(actor.getPosition().x, mScreenHeight + 10)).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoOutMode);
        } else if (mGoOutSubType == SUB_RIGHT) {
            mGoOutAnimation = new PropertyAnimation(
                actor,
                Actor.PROP_POSITION,
                actor.getPosition(),
                new Point(mScreenWidth + 10, actor.getPosition().y)).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoOutMode);
        } else if (mGoOutSubType == SUB_LEFT) {
            mGoOutAnimation = new PropertyAnimation(
                actor,
                Actor.PROP_POSITION,
                actor.getPosition(),
                new Point(-400, actor.getPosition().y)).setDuration(DEFAULT_TRANSITION_DURATION).setMode(mGoOutMode);
        } else {
            throw new IllegalArgumentException("Illegal animation sub type.");
        }
    }

    public interface TransitionListener {
        /**
         * Notify that an actor should appear.
         *
         * @param actor actor to appear
         */
        void onActorAppear(Actor actor);

        /**
         * Notify that an actor should disappear.
         *
         * @param actor actor to disappear
         */
        void onActorDisappear(Actor actor);
    }

    public void startTransition(Actor actor, int aniType) {
        startTransition(actor, aniType, null);
    }

    public void startTransition(Actor actor, int aniType, final TransitionListener transitionListener) {
        if (aniType == ANI_TYPE_IN) {
            final Actor tmpChild = actor;
            Animation.Listener listener1 = new Animation.Listener() {
                public void onCompleted(Animation animation) {
                    mInProgressAnimation.remove(tmpChild);
                }
            };
            if (!mInProgressAnimation.contains(actor)) {
                mInProgressAnimation.add(actor);
                if (mGoInAnimationGroup == null) {
                    applyGoInAnimation(actor);
                    add(mGoInAnimation);
                    mGoInAnimation.addListener(listener1);
                } else {
                    add(mGoInAnimationGroup);
                    mGoInAnimationGroup.addListener(listener1);
                }
                start();
            }

        } else if (aniType == ANI_TYPE_OUT) {
            final Actor thisActor = actor;
            Animation.Listener aniListener = new Listener() {
                public void onCompleted(Animation animation) {
                    if (transitionListener != null) {
                        transitionListener.onActorDisappear(thisActor);
                    }
                    mInProgressAnimation.remove(thisActor);
                }
            };
            if (!mInProgressAnimation.contains(actor)) {
                mInProgressAnimation.add(actor);
                if (mGoOutAnimationGroup == null) {
                    applyGoOutAnimation(actor);
                    add(mGoOutAnimation);
                    mGoOutAnimation.addListener(aniListener);
                } else {
                    add(mGoOutAnimationGroup);
                    mGoOutAnimationGroup.addListener(aniListener);
                }
                start();
            }
        } else {
            throw new IllegalArgumentException("No transition animation setting.");
        }
    }

    public void startTransition(Actor from, Actor to) {
        startTransition(from, to, null);
    }

    public void startTransition(final Actor from, final Actor to, final TransitionListener transitionListener) {
        Animation.Listener listener1 = new Listener() {
            public void onStarted(Animation animation) {
                if (transitionListener != null) {
                    transitionListener.onActorAppear(to);
                }
            }

            public void onCompleted(Animation animation) {
                if (transitionListener != null) {
                    transitionListener.onActorDisappear(from);
                }
                mInProgressAnimation.remove(from);
            }
        };

        Animation.Listener listener2 = new Animation.Listener() {
            public void onCompleted(Animation animation) {
                mInProgressAnimation.remove(to);
            }
        };

        if (!mInProgressAnimation.contains(from) && !mInProgressAnimation.contains(to)) {
            mInProgressAnimation.add(from);
            mInProgressAnimation.add(to);
            if (mGoOutAnimationGroup == null) {
                applyGoOutAnimation(from);
                add(mGoOutAnimation);
                mGoOutAnimation.addListener(listener1);
            } else {
                add(mGoOutAnimationGroup);
                mGoOutAnimationGroup.addListener(listener1);
            }

            if (mGoInAnimationGroup == null) {
                applyGoInAnimation(to);
                add(mGoInAnimation);
                mGoInAnimation.addListener(listener2);
            } else {
                add(mGoInAnimationGroup);
                mGoInAnimationGroup.addListener(listener2);
            }

            start();
        }
    }

    public boolean isAnimationInProgress(Actor actor) {
        return mInProgressAnimation.contains(actor);
    }

}
