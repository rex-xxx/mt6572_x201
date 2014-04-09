
package com.mediatek.contacts.list;

/**
 * Action callbacks that can be sent by a multiple choice contact picker.
 */
public interface ContactListMultiChoiceListener {

    /**
     * Response the Select All button.
     */
    void onSelectAll();

    /**
     * Response the clear selection button.
     */
    void onClearSelect();

    /**
     * Response the option action.
     */
    void onOptionAction();
}
