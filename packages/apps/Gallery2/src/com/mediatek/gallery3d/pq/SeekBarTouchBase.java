package com.mediatek.gallery3d.pq;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AbsoluteLayout.LayoutParams;

public class SeekBarTouchBase implements OnTouchListener{
    String TAG = "Gallery2/SeekBarTouchBase";
        int  lastY;
        private int WindowsWidth;
        private int WindowsHeight;
        private TextView left;
        private TextView right;
        private TextView center;
        private SetViewVisible mLisenter;
        private Object obj;
        public SeekBarTouchBase(int WindowsWidth, int WindowsHeight, TextView left, TextView right, TextView center){
            this.WindowsWidth = WindowsWidth;
            this.WindowsHeight = WindowsHeight;
            this.left = left;
            this.right = right;
            this.center = center;
            Log.d(TAG,"w=="+WindowsWidth+"  h=="+WindowsHeight);
        }
        public void setLisenter(SetViewVisible  lisenter ,SeekBar obj) {
            mLisenter = lisenter;
            this.obj = obj;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
             int ea=event.getAction();
              Log.i(TAG, "V.l:"+v.getLeft()+" V.r=="+v.getRight()+" v.t"+v.getTop()+" v.b=="+v.getBottom());
              Log.i(TAG, "V.padingbottom=="+v.getPaddingBottom()+" v.getScrollX=="+v.getScrollX()+" getScrollY="+v.getScaleY());
                Log.i("TAG", "Touch:"+ea);
                switch(ea){
                case MotionEvent.ACTION_DOWN:  
                 lastY = (int) event.getRawY();
                 if (mLisenter != null ) {
                     mLisenter.setVisible(obj);
                 }
                 if (left.getVisibility() == View.GONE) {
                     left.setVisibility(View.VISIBLE);
                     right.setVisibility(View.VISIBLE);
                     center.setVisibility(View.VISIBLE);
                  }
                      Log.d(TAG,"lastY=="+lastY);
                           break;
                 /**  
                 * layout(l,t,r,b)  
                 * l  Left position, relative to parent   
                   t  Top position, relative to parent   
                   r  Right position, relative to parent   
                   b  Bottom position, relative to parent    
                  * */  
                case MotionEvent.ACTION_MOVE: 
                     int dy =(int)event.getRawY() - lastY;
                 int top = v.getTop()+ dy;   
                 int bottom = v.getBottom()+ dy;
                 if(top < 0){   
                     top = 0;   
                     bottom = top + v.getHeight();
                 }
                 if(bottom > WindowsHeight){
                     bottom = WindowsHeight;
                     top = bottom - v.getHeight();
                 }
                 v.layout(v.getLeft(), top, v.getRight(), bottom);
                 lastY = (int) event.getRawY();
                 LayoutParams paramsLeft = new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT, v.getLeft(), bottom);
                 left.setLayoutParams(paramsLeft);
                 LayoutParams paramsRight = new LayoutParams(LayoutParams.WRAP_CONTENT,
                           LayoutParams.WRAP_CONTENT, v.getRight()-20, bottom);
                 right.setLayoutParams(paramsRight);
                 LayoutParams paramsCenter = new LayoutParams(LayoutParams.WRAP_CONTENT,
                           LayoutParams.WRAP_CONTENT, v.getLeft() + v.getWidth()/2, bottom);
                 center.setLayoutParams(paramsCenter);
                 break;
                case MotionEvent.ACTION_UP:
                 break;
                }
            // TODO Auto-generated method stub
            return false;
        }
        
    }

