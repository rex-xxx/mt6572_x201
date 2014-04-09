package com.mediatek.settings.ext;


public interface IRcseOnlyApnExtension {
    
    /** is show the rcse type apn profile or not.
     * @return true show.
     * @return false hide.
     */
    public boolean isRcseOnlyApnEnabled();

    /** to call back when the rcse state changed.
     * @return true show.
     * @return false hide.
     */
    interface OnRcseOnlyApnStateChangedListener {
        void onRcseOnlyApnStateChanged(boolean isEnabled);
    }

    /** add a call back listener.
     * @param listener the call back to add
     */
    public void addRcseOnlyApnStateChanged(OnRcseOnlyApnStateChangedListener listener);

    /** remove a call back listener.
     * @param listener the call back to remove 
     */
    public void removeRcseOnlyApnStateChanged(OnRcseOnlyApnStateChangedListener listener);
}
