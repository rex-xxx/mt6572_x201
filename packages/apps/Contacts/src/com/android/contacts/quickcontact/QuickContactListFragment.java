/*
 * Copyright (C) 2011 The Android Open Source Project
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
 */

package com.android.contacts.quickcontact;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.contacts.ContactPresenceIconUtil;
import com.android.contacts.R;

import java.util.ArrayList;
import java.util.List;


// The following lines are provided and maintained by Mediatek Inc.
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import com.android.contacts.ContactsUtils;
import com.android.contacts.util.Constants;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.quickcontact.QuickDataAction;

import android.provider.Telephony.SIMInfo; 
import android.provider.Telephony;
import android.app.Activity;
import android.net.Uri;
import android.widget.LinearLayout;
// The previous lines are provided and maintained by Mediatek Inc.
/** A fragment that shows the list of resolve items below a tab */
public class QuickContactListFragment extends Fragment {
    private static final String TAG = "QuickContactListFragment";
	
    private ListView mListView;
    private List<Action> mActions = new ArrayList<Action>();
    private RelativeLayout mFragmentContainer;
    private Listener mListener;

    /*
     * New Feature by Mediatek Begin.            
     * save context's object   
     * Original Android’s code:
             public DetailViewCache(View view,
                OnClickListener primaryActionClickListener,
                OnClickListener secondaryActionClickListener) {  
     */
    private Context mContext;
    /*
     * New Feature  by Mediatek End.
    */
    
    /*
     * New Feature by Mediatek Begin.            
     * save context's object   
     * public QuickContactListFragment() {             
     */
    public QuickContactListFragment(Context mContext) { 
    	this.mContext = mContext;
    /*
     * New Feature  by Mediatek End.
     */	
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        /** M: New Feature Theme Manager @{ */
        /*
         * Original Code: 
         * mFragmentContainer = (RelativeLayout)inflater.inflate(R.layout.quickcontact_list_fragment,container,
         * false);
         */
        mFragmentContainer = (RelativeLayout) inflater.inflate(R.layout.quickcontact_list_fragment_ext,
                container, false);

        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            int textColor = mContext.getResources().getThemeMainColor();
            if (textColor != 0) {
                View viewBottomLine = mFragmentContainer.findViewById(R.id.bottom_line_track);
                if (viewBottomLine != null) {
                    viewBottomLine.setBackgroundColor(textColor);
                }
            }
        }
        /** @} */
        mListView = (ListView) mFragmentContainer.findViewById(R.id.list);
        mListView.setItemsCanFocus(true);

        mFragmentContainer.setOnClickListener(mOutsideClickListener);
        configureAdapter();
        return mFragmentContainer;
    }

    public void setActions(List<Action> actions) {
        mActions.clear();
        for (Action act : actions) {
            mActions.add(act);
        }
        configureAdapter();
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    private void configureAdapter() {
        if (mActions == null || mListView == null) return;

        mListView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mActions.size();
            }

            @Override
            public Object getItem(int position) {
                return mActions.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Set action title based on summary value
                /*
                 * New Feature by Mediatek Begin. 
                 * Original Android’s code:
                 * final Action action = mActions.get(position);                           
                 */
                final Action action = (Action) mActions.get(position);
                /*
                 * New Feature  by Mediatek End.
                */
                String mimeType = action.getMimeType();

                final View resultView = convertView != null ? convertView
                        : getActivity().getLayoutInflater().inflate(
                                mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE) ?
                                        R.layout.quickcontact_list_item_address :
                                        R.layout.quickcontact_list_item,
                                        parent, false);

                // TODO: Put those findViewByIds in a container
                final TextView text1 = (TextView) resultView.findViewById(
                        android.R.id.text1);
                final TextView text2 = (TextView) resultView.findViewById(
                        android.R.id.text2);
                final View actionsContainer = resultView.findViewById(
                        R.id.actions_view_container);
                final ImageView alternateActionButton = (ImageView) resultView.findViewById(
                        R.id.secondary_action_button);
                final View alternateActionDivider = resultView.findViewById(R.id.vertical_divider);
                final ImageView presenceIconView =
                        (ImageView) resultView.findViewById(R.id.presence_icon);

                final LinearLayout associationSimLayout =
                        (LinearLayout)resultView.findViewById(R.id.association_sim_layout);

                actionsContainer.setOnClickListener(mPrimaryActionClickListener);
                actionsContainer.setTag(action);
                alternateActionButton.setOnClickListener(mSecondaryActionClickListener);
                alternateActionButton.setTag(action);

                final boolean hasAlternateAction = action.getAlternateIntent() != null;
                alternateActionDivider.setVisibility(hasAlternateAction ? View.VISIBLE : View.GONE);
                alternateActionButton.setImageDrawable(action.getAlternateIcon());
                alternateActionButton.setContentDescription(action.getAlternateIconDescription());
                alternateActionButton.setVisibility(hasAlternateAction ? View.VISIBLE : View.GONE);

                // The following lines are provided and maintained by Mediatek Inc.
                LinearLayout.LayoutParams pms = (LinearLayout.LayoutParams) text2.getLayoutParams();
                if (showNewAddWidget(action, resultView, hasAlternateAction)) {
                    associationSimLayout.setVisibility(View.VISIBLE);
                } else {
                    pms.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
                text2.setLayoutParams(pms);
                // The previous lines are provided and maintained by Mediatek Inc.
                /*
                 * New Feature by Mediatek Begin.
                 *   Original Android's code:
                 *     
                 *   CR ID: ALPS00308657
                 *   Descriptions: RCS
                 */

                ExtensionManager.getInstance().getContactDetailExtension()
                        .setViewVisibleWithCharSequence(resultView, getActivity(),
                                action.getMimeType(), null, action.getBody(),
                                ExtensionManager.COMMD_FOR_RCS, R.id.vertical_divider_vtcall,
                                R.id.vtcall_action_button, R.id.vertical_divider,
                                R.id.secondary_action_button, 0, 0);
                /*
                 * New Feature by Mediatek End.
                 */
                
                
                // Special case for phone numbers in accessibility mode
                if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    text1.setContentDescription(getActivity().getString(
                            R.string.description_dial_phone_number, action.getBody()));
                    if (hasAlternateAction) {
                        alternateActionButton.setContentDescription(getActivity()
                                .getString(R.string.description_send_message, action.getBody()));
                    }
                }

                text1.setText(action.getBody());
                if (text2 != null) {
                    CharSequence subtitle = action.getSubtitle();
                    text2.setText(subtitle);
                    if (TextUtils.isEmpty(subtitle)) {
                        text2.setVisibility(View.GONE);
                    } else {
                        text2.setVisibility(View.VISIBLE);
                    }
                }
                final Drawable presenceIcon = ContactPresenceIconUtil.getPresenceIcon(
                        getActivity(), action.getPresence());
                if (presenceIcon != null) {
                    presenceIconView.setImageDrawable(presenceIcon);
                    presenceIconView.setVisibility(View.VISIBLE);
                } else {
                    presenceIconView.setVisibility(View.GONE);
                }
                return resultView;
            }
        });
    }

    /** A data item (e.g. phone number) was clicked */
    protected final OnClickListener mPrimaryActionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Action action = (Action) v.getTag();
            if (mListener != null) mListener.onItemClicked(action, false);
        }
    };

    /** A secondary action (SMS) was clicked */
    protected final OnClickListener mSecondaryActionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Action action = (Action) v.getTag();
            if (mListener != null) mListener.onItemClicked(action, true);
        }
    };

    private final OnClickListener mOutsideClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) mListener.onOutsideClick();
        }
    };

    public interface Listener {
        void onOutsideClick();
        void onItemClicked(Action action, boolean alternate);
    }
    
    
    /*
     * New Feature by Mediatek Begin.              
     * show newly view for association and vtcall                          
     */
    public boolean showNewAddWidget(final Action action, final View resultView, final boolean hasAlternateAction) {
        if (null != action && Phone.CONTENT_ITEM_TYPE.equals(action.getMimeType())) {
            // is association
            final ImageView imgAssociationSimFlag = (ImageView) resultView
                    .findViewById(R.id.association_sim_icon);
            final TextView txtAssociationSimName = (TextView) resultView
                    .findViewById(R.id.association_sim_text);

            int simId = -1;
            final boolean isQuickDataAction = action instanceof QuickDataAction;
            if (isQuickDataAction) {
                simId = ((QuickDataAction) action).getSimId();
            }
            if (simId > -1) {
                if (imgAssociationSimFlag != null) {
                    imgAssociationSimFlag.setImageDrawable(mContext.getResources().getDrawable(
                            R.drawable.ic_association));
                    imgAssociationSimFlag.setVisibility(View.VISIBLE);
                }
                if (txtAssociationSimName != null) {

                    SIMInfo simInfo = SIMInfo.getSIMInfoById(mContext, simId);
                    if (simInfo != null) {
                        Log.i(TAG, "simInfo.mDisplayName is " + simInfo.mDisplayName);
                        Log.i(TAG, "simInfo.mColor is " + simInfo.mColor);
                        txtAssociationSimName.setText(simInfo.mDisplayName);
                        int slotId = SIMInfo.getSlotById(mContext, simId);
                        Log.d(TAG, "slotId = " + slotId);
                        if (slotId >= 0) {
                            Log.d(TAG, "slotId >= 0 ");
                            txtAssociationSimName
                                    .setBackgroundResource(Telephony.SIMBackgroundLightRes[simInfo.mColor]);
                        } else {
                            Log.d(TAG, "slotId < 0 ");
                            txtAssociationSimName
                                    .setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_locked);
                        }
                        // fix bug for Consistent UI by mediatek start
                        int paddingLeft = this.getResources().getDimensionPixelOffset(
                                R.dimen.dialpad_operator_horizontal_padding_left);
                        int paddingRight = this.getResources().getDimensionPixelOffset(
                                R.dimen.dialpad_operator_horizontal_padding_right);
                        txtAssociationSimName.setPadding(paddingLeft, 0, paddingRight, 0);
                     // fix bug for Consistent UI by mediatek end
                    } else {
                        Log.i(TAG, "not find siminfo");
                    }

                    txtAssociationSimName.setVisibility(View.VISIBLE);
                }
            } else {
                if (imgAssociationSimFlag != null) {
                    imgAssociationSimFlag.setVisibility(View.GONE);
                }
                if (txtAssociationSimName != null) {
                    txtAssociationSimName.setVisibility(View.GONE);
                }
            }

            final View vewVtCallDivider = resultView.findViewById(R.id.vertical_divider_vtcall);
            final ImageView btnVtCallAction = (ImageView) resultView
                    .findViewById(R.id.vtcall_action_button);
            final View viewActionDivider = resultView.findViewById(R.id.vertical_divider);
            if (isQuickDataAction && FeatureOption.MTK_VT3G324M_SUPPORT) {
                if (null != viewActionDivider) {
                    viewActionDivider.setVisibility(View.VISIBLE);
                }
                if (vewVtCallDivider != null && hasAlternateAction) {
                    vewVtCallDivider.setVisibility(View.VISIBLE);
                }
                if (btnVtCallAction != null) {
                    btnVtCallAction.setContentDescription(action.getAlternateIconDescription());
                    btnVtCallAction.setImageDrawable(mContext.getResources().getDrawable(
                            R.drawable.ic_video_call));
                    btnVtCallAction.setVisibility(View.VISIBLE);
                    btnVtCallAction.setTag(action);
                    btnVtCallAction.setOnClickListener(mVTCallActionClickListener);
                }
            } else {
                if (vewVtCallDivider != null) {
                    vewVtCallDivider.setVisibility(View.GONE);
                    /** M: Bug Fix for ALPS00363177 @{ */
                    vewVtCallDivider.setClickable(false);
                    /** @} */
                }
                if (null != viewActionDivider) {
                    viewActionDivider.setVisibility(View.GONE);
                    viewActionDivider.setClickable(false);
                }
                if (btnVtCallAction != null) {
                    btnVtCallAction.setVisibility(View.GONE);
                    /** M: Bug Fix for ALPS00363177 @{ */
                    btnVtCallAction.setClickable(false);
                    /** @} */
                }
            }
            return simId > -1;
        }
        return false;
    }    
    /*
     * New Feature  by Mediatek End.
    */
    
    
    /*
     * New Feature by Mediatek Begin.              
     * handle vtcall action           
     */
    protected final OnClickListener mVTCallActionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final QuickDataAction action = (QuickDataAction) v.getTag();
            String sNumber = action.getBody().toString();
            final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel",
                    sNumber, null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Constants.EXTRA_IS_VIDEO_CALL, true);
            intent.putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, (long) action.getSimId());
            intent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER);
            startActivity(intent);
            /*
             * Bug Fix by Mediatek Begin. CR ID: ALPS00119452
             */
            if (mContext instanceof Activity) {
                Activity activity = (Activity) mContext;
                activity.finish();
            }
            /*
             * Bug Fix by Mediatek End.
             */
        }
    };    
    /*
     * New Feature  by Mediatek End.
    */
    
    
}
