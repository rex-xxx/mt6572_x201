
package com.mediatek.contacts.model;

import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;

import com.android.contacts.R;
import com.android.contacts.ext.Anr;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountType.EditField;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.account.BaseAccountType.SimpleInflater;
import com.android.contacts.model.dataitem.DataKind;
import com.google.android.collect.Lists;
import com.mediatek.common.telephony.AlphaTag;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.extension.aassne.SimUtils;

import java.util.List;

public class UsimAccountType extends BaseAccountType {
    public static final String TAG = "UsimAccountType";
    public static final String ACCOUNT_TYPE = AccountType.ACCOUNT_TYPE_USIM;

    public UsimAccountType(Context context, String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.titleRes = R.string.account_usim_only;
        this.iconRes = R.drawable.ic_contact_account_usim;
        this.resourcePackageName = null;
        this.syncAdapterPackageName = resPackageName;

        try {
            addDataKindStructuredNameForUsim(context);
            addDataKindDisplayNameForUsim(context);
            /** M:SNE @ { */
            if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureEnabled(
                    ExtensionManager.COMMD_FOR_SNE)) {
                addDataKindNickname(context);
            }
            /** M: @ } */

            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindPhoto(context);
            addDataKindGroupMembership(context);
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    protected DataKind addDataKindStructuredNameForUsim(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(StructuredName.CONTENT_ITEM_TYPE,
                R.string.nameLabelsGroup, -1, true, R.layout.structured_name_editor_view));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);

        kind.typeOverallMax = 1;

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME, R.string.full_name,
                FLAGS_PERSON_NAME));

        return kind;
    }

    protected DataKind addDataKindDisplayNameForUsim(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME,
                R.string.nameLabelsGroup, -1, true, R.layout.text_fields_editor_view));
        kind.typeOverallMax = 1;

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME, R.string.full_name,
                FLAGS_PERSON_NAME));

        return kind;
    }

    @Override
    protected DataKind addDataKindPhone(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindPhone(context);
        Log.d("UsimAccountType", "call addDataKindPhone()");
        kind.typeColumn = Phone.TYPE;
        /** M:AAS @ { */
        if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureEnabled(
                ExtensionManager.COMMD_FOR_AAS)) {
            kind.typeList = Lists.newArrayList();
            kind.typeList.add(buildPhoneType(Anr.TYPE_AAS).setSpecificMax(-1).setCustomColumn(String.valueOf(-1)));
            kind.typeList.add(buildPhoneType(Phone.TYPE_CUSTOM).setSpecificMax(-1));
            /** M: @ } */
        } else {
            kind.typeList = Lists.newArrayList();
            kind.typeList.add(buildPhoneType(Phone.TYPE_MOBILE).setSpecificMax(-1));
            kind.typeList.add(buildPhoneType(Phone.TYPE_OTHER).setSpecificMax(-1));
            /**
             * M: Bug Fix for ALPS00557517
             * origin code: kind.typeOverallMax = 2;
             * @ { */
        }
        kind.typeOverallMax = -1;
        /** @ } */
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));

        return kind;
    }

    /** M:AAS @ { */
    public static void updatePhoneType(int slotId, DataKind kind) {
        if (kind.typeList == null) {
            kind.typeList = Lists.newArrayList();
        } else {
            kind.typeList.clear();
        }
        List<AlphaTag> atList = SimUtils.getAAS(slotId);
        final int specificMax = -1;
        Log.d("UsimAccountType", "[updatePhoneType] slot = " + slotId + " specificMax=" + specificMax);

        kind.typeList.add(buildPhoneType(Anr.TYPE_AAS).setSpecificMax(specificMax)
                .setCustomColumn(String.valueOf(-1)));
        for (AlphaTag tag : atList) {
            final int recordIndex = tag.getRecordIndex();
            Log.d("BaseAccountType", "updatePhoneType() label=" + tag.getAlphaTag());
            kind.typeList.add(buildPhoneType(Anr.TYPE_AAS).setSpecificMax(specificMax).setCustomColumn(
                    String.valueOf(recordIndex)));
        }
        kind.typeList.add(buildPhoneType(Phone.TYPE_CUSTOM).setSpecificMax(specificMax));

        kind.typeOverallMax = specificMax;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));
    }
    /** M: @ } */

    /** M:SNE @ { */
    protected DataKind addDataKindNickname(Context context) throws DefinitionException {
        Log.d(TAG, "addDataKindNickname() for USIM");
        DataKind kind = addKind(new DataKind(Nickname.CONTENT_ITEM_TYPE, R.string.nicknameLabelsGroup, 115, true,
                R.layout.text_fields_editor_view));
        kind.typeOverallMax = 1;
        kind.actionHeader = new SimpleInflater(R.string.nicknameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.defaultValues = new ContentValues();
        kind.defaultValues.put(Nickname.TYPE, Nickname.TYPE_DEFAULT);

        return kind;
    }

    public static void updateNickname(DataKind kind, boolean hasSne) {
        if (hasSne) {
            if (kind.fieldList == null) {
                kind.fieldList = Lists.newArrayList();
            } else {
                kind.fieldList.clear();
            }
            kind.fieldList.add(new EditField(Nickname.NAME, R.string.nicknameLabelsGroup,
                    FLAGS_PERSON_NAME));
        } else if (!hasSne) {
            kind.fieldList = null;
        }
    }
    /** M: @ } */

    protected DataKind addDataKindEmail(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindEmail(context);

        kind.typeOverallMax = 1;
        kind.typeColumn = Email.TYPE;
        kind.typeList = Lists.newArrayList();
        /** M:AAS @ { original code:
        kind.typeList.add(buildEmailType(Email.TYPE_MOBILE));*/
        if (!ExtensionManager.getInstance().getContactAccountExtension().isFeatureEnabled(
                ExtensionManager.COMMD_FOR_AAS)) {
            kind.typeList.add(buildEmailType(Email.TYPE_MOBILE));
        }
        /** M: @ } */

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Email.DATA, R.string.emailLabelsGroup, FLAGS_EMAIL));

        return kind;
    }

    @Override
    protected DataKind addDataKindPhoto(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindPhoto(context);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Photo.PHOTO, -1, -1));

        return kind;
    }

    @Override
    protected DataKind addDataKindNote(Context context) throws DefinitionException {
        final DataKind kind = super.addDataKindNote(context);
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Note.NOTE, R.string.label_notes, FLAGS_NOTE));

        return kind;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return true;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

    @Override
    public boolean isIccCardAccount() {
        return true;
    }

    @Override
    public boolean isUSIMAccountType() {
        return true;
    }
}
