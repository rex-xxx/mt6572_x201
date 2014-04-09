
package com.mediatek.contacts.model;

import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;

import com.android.contacts.R;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountType.EditField;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.account.BaseAccountType.SimpleInflater;
import com.android.contacts.model.dataitem.DataKind;
import com.google.android.collect.Lists;
import com.mediatek.contacts.ExtensionManager;

public class SimAccountType extends BaseAccountType {

    public static final String TAG = "SimAccountType";
    public static final String ACCOUNT_TYPE = AccountType.ACCOUNT_TYPE_SIM;

    public SimAccountType(Context context, String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.resourcePackageName = null;
        this.syncAdapterPackageName = resPackageName;
        this.titleRes = R.string.account_sim_only;
        this.iconRes = R.drawable.ic_contact_account_sim;

        try {
            addDataKindStructuredNameForSim(context);
            addDataKindDisplayNameForSim(context);
            addDataKindPhone(context);
            addDataKindPhoto(context);
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }

    }

    protected DataKind addDataKindStructuredNameForSim(Context context) throws DefinitionException {
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

    protected DataKind addDataKindDisplayNameForSim(Context context) throws DefinitionException {
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
        Log.w("SimAccountType", "addDataKindPhone");
        kind.typeColumn = Phone.TYPE;
        kind.typeList = Lists.newArrayList();

        /** M:AAS&SNE @ { */
        if (!ExtensionManager.getInstance().getContactAccountExtension().hidePhoneLabel(
                ACCOUNT_TYPE, null, null, ExtensionManager.COMMD_FOR_AAS)) {
            kind.typeList.add(buildPhoneType(Phone.TYPE_MOBILE).setSpecificMax(2));
        }
        /** M: @ } */

        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));

        return kind;
    }

    @Override
    protected DataKind addDataKindPhoto(Context context)  throws DefinitionException {
        final DataKind kind = super.addDataKindPhoto(context);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Photo.PHOTO, -1, -1));
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
}
