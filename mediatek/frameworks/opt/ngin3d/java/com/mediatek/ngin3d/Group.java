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

import android.util.Log;
import com.mediatek.ngin3d.presentation.Presentation;
import com.mediatek.ngin3d.presentation.PresentationEngine;
import com.mediatek.util.JSON;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/**
 * Group is a abstract actor that contains other actors. The child management implementation is exported to its subclass only.
 * @hide
 */
public abstract class Group extends Actor {

    /**
     * It is important to name attached property as ATTACHED_PROP_*. If its name begins with
     * typical PROP_*, it will be treated as class-owned property and will not be dispatched
     * to property chain.
     */
    static final Property<Group> ATTACHED_PROP_PARENT = new Property<Group>("parent", null);
    /**
     * @hide
     */
    public static final int DEPTH_FIRST_SEARCH = 0;
    /**
     * @hide
     */
    public static final int BREADTH_FIRST_SEARCH = 1;

    /**
     * @hide
     */
    public static final int SEARCH_BY_TAG = 0;
    /**
     * @hide
     */
    public static final int SEARCH_BY_ID = 1;

    /**
     * @hide
     */
    protected List<Actor> mChildren = new ArrayList<Actor>();
    /**
     * @hide
     */
    protected List<Actor> mPendingRm = new ArrayList<Actor>();

    /**
     * @hide
     */
    @Override
    protected Presentation createPresentation(PresentationEngine engine) {
        return engine.createContainer();
    }

    /**
     * @hide
     */
    @Override
    public void realize(PresentationEngine presentationEngine) {
        synchronized (this) {
            super.realize(presentationEngine);

            int size = mChildren.size(); // Use indexing rather than iterator to prevent frequent GC
            for (int i = 0; i < size; ++i) {
                final Actor child = mChildren.get(i);

                if (!isRealized() || !child.isRealized()) {
                    // It is necessary to set
                    // +parent again to make the properties dirty if it was applied before the parent
                    // or child is re-realized.
                    child.setValue(ATTACHED_PROP_PARENT, this);
                }

                child.setOwner(this);
                child.realize(presentationEngine);
            }

            // Process all removed actors
            size = mPendingRm.size();
            for (int i = 0; i < size; ++i) {
                Actor actor = mPendingRm.get(i);
                actor.setValue(ATTACHED_PROP_PARENT, null);
                actor.setOwner(null);
                actor.setPropertyChain(null);
                actor.unrealize();
            }
            mPendingRm.clear();
        }
    }

    /**
     * @hide
     */
    @Override
    public void unrealize() {
        synchronized (this) {
            for (Actor actor : mChildren) {
                actor.unrealize();
            }

            super.unrealize();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Base.PropertyChain

    @SuppressWarnings("PMD")
    class PropertyChainNode implements Base.PropertyChain {
        public boolean applyAttachedProperty(Base obj, Property property, Object value) {
            if (property.sameInstance(ATTACHED_PROP_PARENT)) {
                if (value != Group.this) {
                    throw new IllegalArgumentException("Unmatched child-parent!");
                }
                if (mPresentation == null) {
                    return false;
                }

                Actor actor = (Actor) obj;
                mPresentation.addChild(actor.getPresentation());
                return true;
            }

            return false;
        }

        public Object getInheritedProperty(Property property) {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parent-child relationship

    /**
     * Add the actors into this group.
     *
     * @param actors actors to add as children
     */
    protected void addChild(Actor... actors) {
        synchronized (this) {
            for (Actor child : actors) {
                if (child == null) {
                    continue;
                }
                // Avoid adding duplicated child
                if (mChildren.contains(child)) {
                    Log.w(TAG, "The actor is already in the group");
                    continue;
                }
                mChildren.add(child);

                onChildAdded(child);

                if (!mPendingRm.remove(child)) {
                    // Attach PARENT property to the child
                    child.setValue(ATTACHED_PROP_PARENT, this);

                    // Listen for unhandled property so that we can apply PARENT
                    child.setPropertyChain(new PropertyChainNode());
                }
            }

            requestRender();
        }
    }

    /**
     * Hook method to know a child is added.
     *
     * @param child the added child
     */
    protected abstract void onChildAdded(Actor child);

    /**
     * Hook method to know a child is removed.
     *
     * @param child the removed child
     */
    protected abstract void onChildRemoved(Actor child);

    /**
     * Remove the actor from this container.
     *
     * @param child child to remove
     */
    protected void removeChild(Actor child) {
        synchronized (this) {
            if (mChildren.remove(child)) {
                onChildRemoved(child);
                mPendingRm.add(child);
            }
        }
    }

    protected void removeAllChildren() {
        synchronized (this) {
            mPendingRm.addAll(mChildren);
            mChildren.clear();
        }
    }

    /**
     * Gets the number of children in Group.
     * @return  the number of actors
     */
    protected int getChildrenCount() {
        synchronized (this) {
            return mChildren.size();
        }
    }

    /**
     * Gets the number of descendant in Group.
     * @return  the number of actors
     */
    protected int getDescendantCount() {
        synchronized (this) {
            int count = mChildren.size();
            for (Actor actor : mChildren) {
                if (actor instanceof Group) {
                    count += ((Group) actor).getDescendantCount();
                }
            }
            return count;
        }
    }

    /**
     * Gets the child in Group with index.
     * @return  the child with specific index
     */
    protected <T> T getChildByIndex(int index) {
        synchronized (this) {
            return (T) mChildren.get(index);
        }
    }

    /**
     * Gets the all children in Group.
     * @return  the List of children
     */
    protected List<Actor> getAllChildren() {
        synchronized (this) {
            return new ArrayList<Actor>(mChildren);
        }
    }

    /**
     * Gets the specific actor with the name from children of Group.
     * @param childName  name of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    protected Actor findChildByName(CharSequence childName) {
        synchronized (this) {
            for (Actor actor : mChildren) {
                if (actor.getName().equals(childName)) {
                    return actor;
                }
            }
            return null;
        }
    }

    /**
     * Gets the specific actor with the name from descendant of Group.
     * @param childName  name of actor.
     * @param searchMode  0 is depth first search and 1 is breadth first search, otherwise search first level only.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    protected Actor findChildByName(CharSequence childName, int searchMode) {
        synchronized (this) {
            if (searchMode == BREADTH_FIRST_SEARCH) {
                return  findChildByBFS(childName);
            } else if (searchMode == DEPTH_FIRST_SEARCH) {
                return findChildByDFS(childName);
            } else {
                return findChildByName(childName);
            }
        }
    }

    /**
     * Gets the specific actor with the tag from children of Group.
     * @param tag  tag of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    protected Actor findChildByTag(int tag) {
        synchronized (this) {
            for (Actor actor : mChildren) {
                if (actor.getTag() == tag) {
                    return actor;
                }
            }
            return null;
        }
    }

    /**
     * Gets the specific actor with the tag from descendant of Group.
     * @param tag  tag of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    protected Actor findChildByTag(int tag, int searchMode) {
        synchronized (this) {
            if (searchMode == BREADTH_FIRST_SEARCH) {
                return findChildByBFS(tag, SEARCH_BY_TAG);
            } else if (searchMode == DEPTH_FIRST_SEARCH) {
                return findChildByDFS(tag, SEARCH_BY_TAG);
            } else {
                return findChildByTag(tag);
            }
        }
    }


    /**
     * Gets the specific actor with the id from children of Group.
     * @param id  id of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    protected Actor findChildById(int id) {
        synchronized (this) {
            for (Actor actor : mChildren) {
                if (actor.getId() == id) {
                    return actor;
                }
            }
            return null;
        }
    }

    /**
     * Gets the specific actor with the id from descendant of Group.
     * @param id  id of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    protected Actor findChildById(int id, int searchMode) {
        synchronized (this) {
            if (searchMode == BREADTH_FIRST_SEARCH) {
                return findChildByBFS(id, SEARCH_BY_ID);
            } else if (searchMode == DEPTH_FIRST_SEARCH) {
                return findChildByDFS(id, SEARCH_BY_ID);
            } else {
                return findChildById(id);
            }
        }
    }


    /**
     * Use BFS way to gets the specific actor with the tag from descendant of Group.
     * @param tag  tag of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    private Actor findChildByBFS(int tag, int attribute) {
        Queue<Group> queue = new ArrayDeque<Group>();
        queue.add(this);
        while (queue.size() > 0) {
            Group group = queue.remove();
            List<Actor> list = group.getAllChildren();
            for (Actor actor : list) {
                if (attribute == SEARCH_BY_TAG) {
                    if (actor.getTag() == tag) {
                        return actor;
                    }
                } else if (attribute == SEARCH_BY_ID) {
                    if (actor.getId() == tag) {
                        return actor;
                    }
                }
                if (actor instanceof Group) {
                    queue.add((Group) actor);
                }
            }
        }
        return null;
    }

    /**
     * Use BFS way to gets the specific actor with the name from descendant of Group.
     * @param childName  name of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    private Actor findChildByBFS(CharSequence childName) {
        Queue<Group> queue = new ArrayDeque<Group>();
        queue.add(this);
        while (queue.size() > 0) {
            Group group = queue.remove();
            List<Actor> list = group.getAllChildren();
            for (Actor actor : list) {
                if (actor.getName().equals(childName)) {
                    return actor;
                }
                if (actor instanceof Group) {
                    queue.add((Group) actor);
                }
            }
        }
        return null;
    }

    /**
     * Use DFS way to gets the specific actor with the tag from descendant of Group.
     * @param tag  tag of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    private Actor findChildByDFS(int tag, int attribute) {
        Stack<Actor> stack = new Stack<Actor>();
        stack.push(this);
        while (stack.size() > 0) {
            Actor popped = stack.pop();
            if (attribute == SEARCH_BY_TAG) {
                if (popped.getTag() == tag) {
                    return popped;
                }
            } else if (attribute == SEARCH_BY_ID) {
                if (popped.getId() == tag) {
                    return popped;
                }
            }
            if (popped instanceof Group) {
                List<Actor> list = ((Group) popped).getAllChildren();
                for (Actor actor : list) {
                    stack.push(actor);
                }
            }
        }
        return null;
    }

    /**
     * Use DFS way to gets the specific actor with the name from descendant of Group.
     * @param childName  name of actor.
     * @return  actor object, or null if the specific actor is not existed in this Group.
     */
    private Actor findChildByDFS(CharSequence childName) {
        Stack<Actor> stack = new Stack<Actor>();
        stack.push(this);
        while (stack.size() > 0) {
            Actor popped = stack.pop();
            if (popped.getName().equals(childName)) {
                return popped;
            }
            if (popped instanceof Group) {
                List<Actor> list = ((Group) popped).getAllChildren();
                for (Actor actor : list) {
                    stack.push(actor);
                }
            }
        }
        return null;
    }

    protected void raiseChild(Actor actor, Actor sibling) {
        synchronized (this) {
            if (sibling != null && !mChildren.contains(sibling)) {
                throw new IllegalArgumentException("sibling does not exist in the list");
            }
            if (actor == null || !mChildren.contains(actor)) {
                throw new IllegalArgumentException("actor does not exist in the list");
            }

            mChildren.remove(actor);
            int pos = (sibling == null) ? mChildren.size() : mChildren.indexOf(sibling) + 1;
            mChildren.add(pos, actor);
        }
    }

    protected void lowerChild(Actor actor, Actor sibling) {
        synchronized (this) {
            if (sibling != null && !mChildren.contains(sibling)) {
                throw new IllegalArgumentException("sibling does not exist in the list");
            }
            if (actor == null || !mChildren.contains(actor)) {
                throw new IllegalArgumentException("actor does not exist in the list");
            }

            mChildren.remove(actor);
            int pos = (sibling == null) ? 0 : mChildren.indexOf(sibling);
            mChildren.add(pos, actor);
        }
    }

    @Override
    public String dump() {
        StringBuffer buffer = new StringBuffer();
        synchronized (this) {
            buffer.append(super.dump());
            int count = mChildren.size();
            for (int i = 0; i < count; i++) {
                Actor actor = mChildren.get(i);
                buffer.append(",");
                buffer.append(actor.getClass().getSimpleName() + i + ":");
                buffer.append(JSON.wrap(actor.dump()));
            }
            JSON.wrap(buffer);
            buffer.insert(0, this.getClass().getSimpleName() + ":");
        }
        return buffer.toString();
    }

    /**
     * This method can only be called when all children are Plane or container of Planes.
     *
     * @param opacity opacity value
     */
    public void setOpacity(int opacity) {
        int n = getChildrenCount();
        for (int i = 0; i < n; i++) {
            Actor child = getChildByIndex(i);
            if (child instanceof Group) {
                ((Group) child).setOpacity(opacity);
            } else {
                ((Plane) child).setOpacity(opacity);
            }
        }
    }

    /**
     * Stop animations of this container and animation of all its children recursively.
     */
    @Override
    public void stopAnimations() {
        super.stopAnimations();

        for (Actor actor : mChildren) {
            actor.stopAnimations();
        }
    }

    /**
     * Check the container and all of its children are dirty or not.
     */
    public boolean isDirty() {
        synchronized (this) {
            if (super.isDirty()) {
                return true;
            }
            int size = mChildren.size();
            for (int i = 0; i < size; ++i) {
                if (mChildren.get(i).isDirty()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Check the animations that are applied to container are running or not.
     */
    public boolean isAnimationStarted() {
        synchronized (this) {
            if (super.isAnimationStarted()) {
                return true;
            }
            int size = mChildren.size();
            for (int i = 0; i < size; ++i) {
                if (mChildren.get(i).isAnimationStarted()) {
                    return true;
                }
            }
            return false;
        }
    }

    public void reloadBitmapTexture(PresentationEngine presentationEngine) {
        int size = getChildrenCount(); // Use indexing rather than iterator to prevent frequent GC
        for (int i = 0; i < size; ++i) {
            Actor child = (Actor) getChildByIndex(i);
            if (child instanceof Group) {
                ((Group) child).reloadBitmapTexture(presentationEngine);
            } else if (child instanceof Image) {
                ((Image) child).loadAsync();
            }
        }
    }

    /**
     * Touch property and make it dirty.
     * @param propertyName  selected property name
     * @hide
     */
    public <T> void touchProperty(String propertyName) {
        synchronized (this) {
            super.touchProperty(propertyName);
            int size = mChildren.size();
            for (int i = 0; i < size; ++i) {
                mChildren.get(i).touchProperty(propertyName);
            }
        }
    }

}
