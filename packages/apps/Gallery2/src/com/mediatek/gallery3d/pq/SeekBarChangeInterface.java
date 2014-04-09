package com.mediatek.gallery3d.pq;

import android.widget.SeekBar;

public interface SeekBarChangeInterface {
    public void stopTrackingTouch(SeekBar seekBar);
    public void startTrackingTouch(SeekBar seekBar);
    public void progressChanged(SeekBar seekBar, int progress,
            boolean fromUser);
}
