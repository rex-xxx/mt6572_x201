package com.mediatek.gallery3d.pq;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.widget.ImageView;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
public class ImageViewTouchBase  implements OnTouchListener{
    String TAG = "Gallery2/ImageViewTouchBase";
    private PointF startPoint = null;
    private Matrix matrix = null;
    private Matrix currMatrix ;
    private int drug = 5;
    private int zoom = 10;
    private int type = 0;
    private float startDistance;
    private PointF midPointF; 
    private ImageView mImageView;
    private Bitmap mBitmap;
    private float moveX = 0;
    private float moveY = 0;
    private RectF mImageRectInCanvasCoordinateSystem = new RectF();
    private PointF middlePointerInCanvasCoordinate = new PointF();
    private float currentScale = 1.0f;
    private BitmapRegionDecoder mdecoder;
    private int insampleSize;
    private  GestureDetector mGestureDetector;
    private  SetViewVisible mSetVisibleLisenter;
    private SetXYAxisIndex mSetXYAxisLisenter;
    public ImageViewTouchBase(Context context,Matrix mMetric, ImageView mImageView 
                    , Bitmap mBitmap , BitmapRegionDecoder mdecoder,int insampleSize ) {
        // TODO Auto-generated constructor stub
        startPoint = new PointF();   
        matrix = mMetric; 
        currMatrix = new Matrix(); 
        this.mImageView =  mImageView;
        this.mBitmap = mBitmap;
        if (mMetric != null) {
            float[] values = new float[9];
            mMetric.getValues(values);
            currentScale = values[0];
            mImageRectInCanvasCoordinateSystem.set(0, 0, mBitmap.getWidth()*currentScale, mBitmap.getHeight()*currentScale);
        } else {
            mImageRectInCanvasCoordinateSystem.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        }

        middlePointerInCanvasCoordinate.set(mBitmap.getWidth()/2, mBitmap.getHeight()/2);
        this.mdecoder = mdecoder;
        this.insampleSize = insampleSize;
        mGestureDetector = new GestureDetector(context,new MyGestureListener());
    }
    public void setInsampleSize(int size ) {
        this.insampleSize = size;
     }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //mGestureDetector.onTouchEvent(event);
        float[] values0 = new float[9];
        matrix.getValues(values0);
       float ImageX0 = ((event.getX()  - values0[2]) );
    float ImageY0 =  ((event.getY()  - values0[5]) );
    Log.d(TAG, "Event.x="+ImageX0+ " Event.y="+ImageY0);
    PointF mPointInCanVasCoordinate0 = getPointerFromCanvasCoordinatedSystem(ImageX0, ImageY0);
    if(mPointInCanVasCoordinate0.x > mImageRectInCanvasCoordinateSystem.right || mPointInCanVasCoordinate0.y > mImageRectInCanvasCoordinateSystem.bottom
            || mPointInCanVasCoordinate0.x < 0 || mPointInCanVasCoordinate0.y < 0)
        return false;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN://
            Log.d(TAG,"ACTION_DOWN!!!");
            currMatrix.set(matrix);//
            startPoint.set(event.getX(),event.getY());//
            type = drug;//
            break;
        case MotionEvent.ACTION_MOVE://
            Log.d(TAG,"ACTION_MOVE!!!");
            if(type == drug){
                float mX = event.getX() - startPoint.x;//
                float mY = event.getY() - startPoint.y;//
                moveX += mX;
                moveY += mY;
                matrix.set(currMatrix); //
                matrix.postTranslate(mX, mY);//
            }else if(type == zoom){
                float distance = distance(event);
                if(distance > 5f){
                    matrix.set(currMatrix); //
                    float cale =  distance / startDistance; //
                    float[] values = new float[9];
                    matrix.getValues(values);
                    Log.d(TAG,"midPointF.x=="+(midPointF.x-values[2])+" midPointF.y=="+(midPointF.y-values[5]));
                    Log.d(TAG,"midPointF.x - TranslateX=="+(midPointF.x ) +" midPointF.y - TranslateY=="+(midPointF.y ));
                    Bitmap result2 = Bitmap.createBitmap(
                            mBitmap.getWidth(), mBitmap.getHeight(), Config.ARGB_8888);
                    Canvas canvas2 = new Canvas(result2);
                    canvas2.drawBitmap(mBitmap, 0, 0, null);
                    Paint mPaint = new Paint();
                    mPaint.setARGB(255, 255, 0, 255);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x),  (middlePointerInCanvasCoordinate.y), mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x)-1, (middlePointerInCanvasCoordinate.y), mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x)-1, (middlePointerInCanvasCoordinate.y)-1, mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x),  (middlePointerInCanvasCoordinate.y)-1, mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x)+1, (middlePointerInCanvasCoordinate.y)-1, mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x)+1, (middlePointerInCanvasCoordinate.y), mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x)+1, (middlePointerInCanvasCoordinate.y)+1, mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x), (middlePointerInCanvasCoordinate.y)+1, mPaint);
                    canvas2.drawPoint((middlePointerInCanvasCoordinate.x)-1, (middlePointerInCanvasCoordinate.y)+1, mPaint);
                    mImageView.setImageBitmap(result2);
                    matrix.preScale(cale, cale, middlePointerInCanvasCoordinate.x, middlePointerInCanvasCoordinate.y); //
                }
            }
            break;
        case MotionEvent.ACTION_POINTER_DOWN://
            Log.d(TAG,"ACTION_POINTER_DOWN!!!");
            startDistance = distance(event); //
            if(startDistance > 5f){ //
                type = zoom;
                currMatrix.set(matrix); //
                midPointF = getMidPoinF(event);
                float[] values = new float[9];
                matrix.getValues(values);
                Log.d(TAG,"moveX="+values[2]+"     moveY="+values[5]);
                middlePointerInCanvasCoordinate = getPointerFromCanvasCoordinatedSystem(midPointF.x -values[2], midPointF.y - values[5]);
            }
            break;
        case MotionEvent.ACTION_UP://
            float[] values = new float[9];
            matrix.getValues(values);
            Log.d(TAG,"moveX="+values[2]+"     moveY="+values[5]);
            float ImageX;
            float ImageY;
            ImageX = ((event.getX()  - values[2]) );
            ImageY =  ((event.getY()  - values[5]) );
            PointF mPointInCanVasCoordinate = getPointerFromCanvasCoordinatedSystem(ImageX, ImageY);
            Bitmap result = Bitmap.createBitmap(
                    16, 16, Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            Rect rect = new Rect((int) (mPointInCanVasCoordinate.x - 8),(int) (mPointInCanVasCoordinate.y -8), (int) (mPointInCanVasCoordinate.x+8), (int )(mPointInCanVasCoordinate.y + 8));
            Rect dest = new Rect(0, 0, 16, 16);
            Log.d(TAG,"ImageX="+ImageX+"     ImageY="+ImageY);
            Bitmap result2 = Bitmap.createBitmap(
                    mBitmap.getWidth(), mBitmap.getHeight(), Config.ARGB_8888);
            Canvas canvas2 = new Canvas(result2);
            canvas2.drawBitmap(mBitmap, 0, 0, null);
            canvas.drawBitmap(result2, rect, dest, null);
            canvas2.drawBitmap(result, 0, 0, null);
 ////********************get Image from bitmap region decoder for test!!!!

            if (mdecoder != null) {
                Log.d(TAG,"[mBitmap  ]"+mBitmap.getWidth() +" "+ mBitmap.getHeight());
                
                Rect mReginRect= new Rect();
                int left = (int) (mPointInCanVasCoordinate.x*this.insampleSize - 8);
                int right = (int) (mPointInCanVasCoordinate.y*this.insampleSize - 8);
                mReginRect.set(left, right, left + 16, right+ 16);
                Rect mReginOfRegiondecoder = new Rect(0, 0, mdecoder.getHeight(), mdecoder.getWidth());

              try {
                  if (mReginOfRegiondecoder.contains(mReginRect)) {
                      Bitmap bitmapDecodeFromRegionDecoder = mdecoder.decodeRegion(mReginRect, null);
                      canvas2.drawBitmap(bitmapDecodeFromRegionDecoder, 0, 32, null);
                  }
              }catch (IllegalArgumentException e) {
                  Log.d(TAG,"[mReginOfRegiondecoder.contains(mReginRect)]"+ mReginRect.left+ " "+mReginRect.right+" "+mReginRect.top+ " "+mReginRect.bottom );
                  Log.d(TAG,"[mReginOfRegiondecoder.contains(mReginRect)]"+ mReginOfRegiondecoder.left+ " "+mReginOfRegiondecoder.right+" "+mReginOfRegiondecoder.top+ " "+mReginOfRegiondecoder.bottom );
                  Log.d(TAG,"Bitmap region decoder has a issue: "+e);
              }

            } else {
                Log.d(TAG," mdecoder ==== null");
            }

////********************get Image from bitmap region decoder for test!!!!
            Paint mPaint = new Paint();
            mPaint.setARGB(255, 255, 255, 255);
            canvas2.drawPoint(mPointInCanVasCoordinate.x, mPointInCanVasCoordinate.y, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x-1, mPointInCanVasCoordinate.y, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x-1, mPointInCanVasCoordinate.y-1, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x, mPointInCanVasCoordinate.y-1, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x+1, mPointInCanVasCoordinate.y-1, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x+1, mPointInCanVasCoordinate.y, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x+1, mPointInCanVasCoordinate.y+1, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x, mPointInCanVasCoordinate.y+1, mPaint);
            canvas2.drawPoint(mPointInCanVasCoordinate.x-1, mPointInCanVasCoordinate.y+1, mPaint);
            mImageView.setImageBitmap(result2);
            Log.d(TAG, "mPointInCanVasCoordinate.x="+mPointInCanVasCoordinate.x+ "mPointInCanVasCoordinate.y="+mPointInCanVasCoordinate.y);
            if (mSetXYAxisLisenter != null && mPointInCanVasCoordinate.x >0 && mPointInCanVasCoordinate.y > 0) {
                mSetXYAxisLisenter.setAxisIndex((int)mPointInCanVasCoordinate.x*this.insampleSize, (int)mPointInCanVasCoordinate.y*this.insampleSize);
            }
            if (mSetVisibleLisenter != null) {
                mSetVisibleLisenter.setVisible(null);
            }
            break;
        case MotionEvent.ACTION_POINTER_UP://
            type = 0; //
            break;

        default:
            break;
        }
        Log.d(TAG,"matrix=="+matrix.toString());
        setImageRectInCanvas();
        mImageView.setImageMatrix(matrix); //
        mImageView.invalidate();
        return true;
    }   


//
    private float distance(MotionEvent event){
        float eX = event.getX(1) - event.getX(0); //
        float eY = event.getY(1) - event.getY(0);
        return  FloatMath.sqrt(eX * eX  + eY * eY);
    }

    private PointF getMidPoinF(MotionEvent event){
        float x = (event.getX(1) + event.getX(0)) / 2;
        float y = (event.getY(1) + event.getY(0)) / 2;
        return new PointF(x, y);
    } 
    private void setImageRectInCanvas() {
        float[] values = new float[9];
        matrix.getValues(values);
        currentScale = values[0];
        float x = (middlePointerInCanvasCoordinate.x*(1 - values[0]));
        float y = (middlePointerInCanvasCoordinate.y*(1 - values[0]));
        mImageRectInCanvasCoordinateSystem.set(0, 0, (x + mBitmap.getWidth()*values[0]) - x, (y + mBitmap.getHeight()*values[0]) - y);
    }
    private PointF getPointerFromCanvasCoordinatedSystem (float x, float y) {
            float left = mImageRectInCanvasCoordinateSystem.left;
            float top = mImageRectInCanvasCoordinateSystem.top;
            float scale = mImageRectInCanvasCoordinateSystem.width()/mBitmap.getWidth();
            Log.d(TAG," x=="+x+" y=="+y+" left=="+left+"  top=="+top+" right=="+mImageRectInCanvasCoordinateSystem.width()
                    +"bottom="+mImageRectInCanvasCoordinateSystem.height()+" scale=="+scale);
            float X = (x - left)/scale;
            float Y = (y - top)/scale;
            Log.d(TAG," XX=="+X+" YY=="+Y);
            return new PointF(X, Y);
    }

    private class MyGestureListener
    extends GestureDetector.SimpleOnGestureListener {
        // M: modified for MTK UX issues:
        // use onSingleTapConfirmed to avoid action bar from poping up
        // during double tap gesture.
        //@Override
        //public boolean onSingleTapUp(MotionEvent e) {
        //    return mListener.onSingleTapUp(e.getX(), e.getY());
        //}

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            
            return false;
        }

        @Override
        public boolean onScroll(
                MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }
    }
    
    public void setVisibleLisenter( SetViewVisible  lisenter ) {
        mSetVisibleLisenter = lisenter;
    }

    public void setXYAxisLisenter( SetXYAxisIndex  lisenter ) {
        mSetXYAxisLisenter = lisenter;
    }
}
