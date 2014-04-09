package com.mediatek.gallery3d.pq;

import com.mediatek.gallery3d.pq.PictureQualityTool.DecodeImage;

import android.widget.SeekBar;

public class OnSeekBarChangelisenter implements SeekBar.OnSeekBarChangeListener{
   public SeekBarChangeInterface mLisenter;
   public Runnable mDecoder;
    
   public OnSeekBarChangelisenter(SeekBarChangeInterface mLisenter){
       this.mLisenter = mLisenter;
   }

   public void setDecodeImage(Runnable mDecoder) {
       this.mDecoder = mDecoder;
   }

   public void setLisenter(SeekBarChangeInterface mLisenter) {
       this.mLisenter = mLisenter;
   }
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // TODO Auto-generated method stub
        mLisenter.progressChanged(seekBar, progress, fromUser);
        
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        mLisenter.startTrackingTouch(seekBar);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        mLisenter.stopTrackingTouch(seekBar);
        (new Thread(mDecoder)).start();
    }

}
