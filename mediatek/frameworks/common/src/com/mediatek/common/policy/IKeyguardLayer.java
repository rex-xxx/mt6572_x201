

package com.mediatek.common.policy;

import android.content.Context;
import android.view.View;

/**
 * Interface that used for data usage plug in feature
 * {@hide}
 */
public interface IKeyguardLayer {
    /**
     * If visible, host will call client.initialize(). a view to display the layer content. 
     * Visibility control can be done by calling setVisibility method of the returned view.
     */
    View create(); 
    
    /**
     * Destroy the layer. The associated view should be also removed from the view tree.
     */
    void destroy();

    /**
     * Get extra information of keyguard layer. Background layer should return a KeyguardLayerInfo object and foreground layer should return null.
     */
    KeyguardLayerInfo getKeyguardLayerInfo();
}
