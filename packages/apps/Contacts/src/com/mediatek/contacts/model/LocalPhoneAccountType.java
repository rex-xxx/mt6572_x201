package com.mediatek.contacts.model;

import android.content.Context;
import android.net.sip.SipManager;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.util.Log;

import com.android.contacts.R;
import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.ext.ContactPluginDefault;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountType.EditField;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.google.android.collect.Lists;
import com.mediatek.contacts.ExtensionManager;

import java.util.Locale;

public class LocalPhoneAccountType extends BaseAccountType {
    public static final String TAG = "LocalPhoneAccountType";
    private ContactAccountExtension mCAccountEx;
    
    public static final String ACCOUNT_TYPE = AccountType.ACCOUNT_TYPE_LOCAL_PHONE;

    public LocalPhoneAccountType(Context context, String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.resourcePackageName = null;
        this.syncAdapterPackageName = resPackageName;
        this.titleRes = R.string.account_phone_only;
        this.iconRes = R.drawable.contact_account_phone;
        
        try {

                addDataKindStructuredName(context);
                addDataKindDisplayName(context);
                addDataKindIm(context);

            addDataKindPhoneticName(context);
            addDataKindNickname(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindStructuredPostal(context);
//            addDataKindIm(context);
            addDataKindOrganization(context);
            addDataKindPhoto(context);
            addDataKindNote(context);
//            addDataKindEvent(context);
            addDataKindWebsite(context);
            addDataKindGroupMembership(context);
            
            if (SipManager.isVoipSupported(context)) {
                addDataKindSipAddress(context);
            }
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    @Override
    protected DataKind addDataKindStructuredPostal(Context context) throws DefinitionException {
        final DataKind kindForLoacalPhone = super.addDataKindStructuredPostal(context);
        final boolean useJapaneseOrder = Locale.JAPANESE.getLanguage().equals(
                Locale.getDefault().getLanguage());
        kindForLoacalPhone.typeColumn = StructuredPostal.TYPE;
        kindForLoacalPhone.typeList = Lists.newArrayList();
        kindForLoacalPhone.typeList.add(buildPostalType(StructuredPostal.TYPE_WORK).setSpecificMax(1));
        kindForLoacalPhone.typeList.add(buildPostalType(StructuredPostal.TYPE_HOME).setSpecificMax(1));
        kindForLoacalPhone.typeList.add(buildPostalType(StructuredPostal.TYPE_OTHER).setSpecificMax(1));

        kindForLoacalPhone.fieldList = Lists.newArrayList();
        if (useJapaneseOrder) {
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.COUNTRY, R.string.postal_country,
                    FLAGS_POSTAL).setOptional(true));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POSTCODE, R.string.postal_postcode,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.REGION, R.string.postal_region,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.CITY, R.string.postal_city,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.NEIGHBORHOOD, R.string.postal_neighborhood,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POBOX, R.string.postal_pobox,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.STREET, R.string.postal_street,
                    FLAGS_POSTAL));
        } else {
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.STREET, R.string.postal_street,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POBOX, R.string.postal_pobox,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.NEIGHBORHOOD, R.string.postal_neighborhood,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.CITY, R.string.postal_city,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.REGION, R.string.postal_region,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.POSTCODE, R.string.postal_postcode,
                    FLAGS_POSTAL));
            kindForLoacalPhone.fieldList.add(new EditField(StructuredPostal.COUNTRY, R.string.postal_country,
                    FLAGS_POSTAL).setOptional(true));
        }

        return kindForLoacalPhone;
    }
    
    @Override
    protected DataKind addDataKindStructuredName(Context context) throws DefinitionException {
        DataKind kind = super.addDataKindStructuredName(context);
        boolean result = ExtensionManager.getInstance().getContactAccountExtension()
                .needNewDataKind(ContactPluginDefault.COMMD_FOR_OP01);
        if (result) {
            String str = null;
            int displayName = 0;
            int phoneticMiddelName = 0;
            int j = 0;
            boolean a = false;
            boolean b = false;
            int count = kind.fieldList.size();
            for (int i = 0; i < count; i++) {
                str = kind.fieldList.get(i).column.toString();
                Log.i(TAG, "str : " + str);
                if (str != null && str.equals("data1")) {
                    displayName = i;
                } else if (str != null && str.equals("data8")) {
                    phoneticMiddelName = i;
                } else if (str != null && str.equals("data4")) {
                    j = i;
                    a = kind.fieldList.get(i).longForm;
                    Log.i(TAG, " a : " + a);
                    kind.fieldList.get(i).setLongForm(false);
                    kind.fieldList.get(i).setOptional(true);
                    
                } else {
                    kind.fieldList.get(i).setLongForm(false);
                }
            }
            Log.i(TAG, " display_name : " + displayName
                    + " | phonetic_middel_name : " + phoneticMiddelName
                    + " |a : " + a);
            if (displayName != phoneticMiddelName) {
                kind.fieldList.remove(displayName);
                kind.fieldList.remove(phoneticMiddelName);
            } 
        }
        return kind;
    }

    @Override
    protected DataKind addDataKindDisplayName(Context context) throws DefinitionException {
        DataKind kind = super.addDataKindDisplayName(context);
        boolean result = ExtensionManager.getInstance().getContactAccountExtension()
                .needNewDataKind(ContactPluginDefault.COMMD_FOR_OP01);
        if (result) {
            String str = null;
            int displayName = 0;
            int count = kind.fieldList.size();
            for (int i = 0; i < count; i++) {
                str = kind.fieldList.get(i).column.toString();
                Log.i(TAG, "str : " + str);
                if (str != null && str.equals("data1")) {
                    displayName = i;
                } else if (str != null && str.equals("data5")) {
                    kind.fieldList.get(i).setOptional(true);
                    kind.fieldList.get(i).setLongForm(false);
                } else if (str != null && str.equals("data4")) {
                    kind.fieldList.get(i).setOptional(true);
                    kind.fieldList.get(i).setLongForm(false);
                } else if (str != null && str.equals("data6")) {
                    kind.fieldList.get(i).setOptional(true);
                    kind.fieldList.get(i).setLongForm(false);
                } else {
                    kind.fieldList.get(i).setLongForm(false);
                }
            }
            Log.i(TAG, " display_name : " + displayName);
            kind.fieldList.remove(displayName);
        }
        return kind;
    }

    @Override
    protected DataKind addDataKindIm(Context context) throws DefinitionException {
        DataKind kind = super.addDataKindIm(context);
        boolean result = ExtensionManager.getInstance().getContactAccountExtension()
                .needNewDataKind(ContactPluginDefault.COMMD_FOR_OP01);
        if (result) {
            kind.typeList.remove(buildImType(Im.PROTOCOL_GOOGLE_TALK));
        }
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
}
