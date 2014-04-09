package com.mediatek.gemini;

/**
 * Assisted class for transfer contact data
 * 
 * @author MTK80906
 */
public class GeminiSIMTetherItem {
    private String mName;
    private String mPhoneNumType;
    private String mSimName;
    private int mSimColor;
    private int mCheckedStatus;
    private String mPhoneNum;
    private String mSimId;

    private int mContactId;

    /**
     * Get the id of the contact
     * 
     * @return the contact id
     */
    public int getContactId() {
        return mContactId;
    }

    /**
     * set the contact related Id
     * 
     * @param contactId
     *            the id of contact
     */
    public void setContactId(int contactId) {
        this.mContactId = contactId;
    }

    /**
     * Construct class of GeminiSIMTetherItem
     */
    public GeminiSIMTetherItem() {
        mName = "";
        mPhoneNum = "";
        mSimColor = 0;
        mCheckedStatus = -1;
    }

    /**
     * Construct class of GeminiSIMTetherItem
     * 
     * @param name
     *            String
     * @param phoneNum
     *            String
     * @param simColor
     *            int
     * @param checkedStatus
     *            int
     */
    public GeminiSIMTetherItem(String name, String phoneNum, int simColor,
            int checkedStatus) {
        this.mName = name;
        this.mPhoneNum = phoneNum;
        this.mSimColor = simColor;
        this.mCheckedStatus = checkedStatus;
    }

    /**
     * get the sim id
     * 
     * @return mSimId the sim id of sim card
     */
    public String getSimId() {
        return mSimId;
    }

    /**
     * set the id of sim card
     * 
     * @param simId
     *            the id of sim card
     */
    public void setSimId(String simId) {
        this.mSimId = simId;
    }

    /**
     * get the type of phone number
     * 
     * @return mPhoneNumType
     */
    public String getPhoneNumType() {
        return mPhoneNumType;
    }

    /**
     * set type of phone number, mobile/office
     * 
     * @param phoneNumType
     *            the type of phone number
     */
    public void setPhoneNumType(String phoneNumType) {
        this.mPhoneNumType = phoneNumType;
    }

    /**
     * get sim name
     * 
     * @return mName
     */
    public String getName() {
        return mName;
    }

    /**
     * set the name of sim card
     * 
     * @param name
     *            the name of sim card
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * get mPhoneNum
     * 
     * @return mPhoneNum
     */
    public String getPhoneNum() {
        return mPhoneNum;
    }

    /**
     * set mPhoneNum
     * 
     * @param phoneNum
     *            the phone number of contact
     */
    public void setPhoneNum(String phoneNum) {
        this.mPhoneNum = phoneNum;
    }

    /**
     * get the sim color
     * 
     * @return mSimColor
     */
    public int getSimColor() {
        return mSimColor;
    }

    /**
     * set the color of sim
     * 
     * @param simColor
     *            the color of sim card
     */
    public void setSimColor(int simColor) {
        this.mSimColor = simColor;
    }

    /**
     * get the variable mCheckedStatus
     * 
     * @return mCheckedStatus
     */
    public int getCheckedStatus() {
        return mCheckedStatus;
    }

    /**
     * set whether it is checked
     * 
     * @param checkedStatus
     *            the state of check
     */
    public void setCheckedStatus(int checkedStatus) {
        this.mCheckedStatus = checkedStatus;
    }

    /**
     * get the sim name
     * 
     * @return the mSimName
     */
    public String getSimName() {
        return mSimName;
    }

    /**
     * set the sim name
     * 
     * @param simName
     *            name of sim
     */
    public void setSimName(String simName) {
        this.mSimName = simName;
    }

}
