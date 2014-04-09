package com.android.camera.manager;

import android.view.View;

import com.android.camera.Camera;
import com.android.camera.R;

public class LearningView extends ViewManager implements View.OnClickListener {
    public interface LearningListener {
        void onProtectiveCurtainClick();
        void onCancelBgTraining();
    }
    
    private View mCancelButton;
    private View mContainer;
    private LearningListener mListener;
    
    public LearningView(Camera context) {
        super(context, VIEW_LAYER_SETTING);
    }

    @Override
    protected View getView() {
        View view = inflate(R.layout.bg_replacement_training_message);
        mCancelButton = view.findViewById(R.id.bg_replace_cancel_button);
        mContainer = view.findViewById(R.id.bg_replace_message_frame);
        mCancelButton.setOnClickListener(this);
        mContainer.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View view) {
        if (mListener == null) {
            return;
        }
        if (view == mCancelButton) {
            mListener.onCancelBgTraining();
        } else if (view == mContainer) {
            mListener.onProtectiveCurtainClick();
        }
    }

    public void setListener(LearningListener listener) {
        mListener = listener;
    }
}
