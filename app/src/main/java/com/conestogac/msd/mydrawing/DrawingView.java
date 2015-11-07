package com.conestogac.msd.mydrawing;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by CHANGHO on 2015-10-20.
 */

public class DrawingView extends View {
    public enum DrawMode {DRAW, ERASE, TEXT};    //drawing path
    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint;
    //initial color
    private int paintColor = 0xFF660000;
    //canvas
    private Canvas drawCanvas;

    //canvas bitmap
    private Bitmap canvasBitmap;

    private float brushSize, lastBrushSize;
    private boolean erase=false;
    private DrawMode drawMode = DrawMode.DRAW;;


    public DrawingView(Context context, AttributeSet attrs){
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing(){
//get drawing area setup for interaction
        drawPath = new Path();

        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);

        brushSize = getResources().getInteger(R.integer.medium_size);
        lastBrushSize = brushSize;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//view given size
        super.onSizeChanged(w, h, oldw, oldh);

        if (drawCanvas == null){
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(canvasBitmap);
        }

        Log.d("DrwaingView", "onSizeChanged: " + w + "," + h + "," + oldw + "," + oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
 //       if (drawMode == DrawMode.DRAW ||drawMode == DrawMode.ERASE ){
            //draw view

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
        Log.d("DrwaingView", "onDraw");



 /*       } else {
            drawPaint.setColor(Color.BLACK);
            drawPaint.setTextSize(16);
            canvas.drawText("My Text", 10, 10, drawPaint);
        }
  */
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//detect user touch
        float touchX = event.getX();
        float touchY = event.getY();
        if (drawMode != DrawMode.TEXT) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d("DrwaingView", "ACTION_DOWN");
                    drawPath.moveTo(touchX, touchY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("DrwaingView", "ACTION_MOVE");
                    drawPath.lineTo(touchX, touchY);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d("DrwaingView", "ACTION_UP");
                    drawCanvas.drawPath(drawPath, drawPaint);
                    drawPath.reset();
                    break;
                default:
                    return false;
            }
        } else {

        }
        invalidate();
        return true;
    }

    public void setCanvasBitmap(Bitmap bitmap) {
        Log.d("DrwaingView", "setCanvasBitmap");
        canvasBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        drawCanvas = new Canvas(canvasBitmap);
        invalidate();
    }

    public void setTextMode(){
        this.drawMode = DrawMode.TEXT;
    }

    public void setColor(String newColor){
//set color
        invalidate();
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor);
    }
    public void setBrushSize(float newSize){
//update size
        float pixelAmount = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                newSize, getResources().getDisplayMetrics());
        brushSize = pixelAmount;
        drawPaint.setStrokeWidth(brushSize);
    }
    public void setLastBrushSize(float lastSize){
        lastBrushSize=lastSize;
    }
    public float getLastBrushSize(){
        return lastBrushSize;
    }

    public void setErase(boolean isErase){
//set erase true or false
        erase=isErase;
        if(erase) {
            //Due to black color is shown when PorterDuff.Mode.CLEAR was used,
            //drawing with white color is used.
            drawPaint.setColor(0xffffffff);
            drawMode = DrawMode.ERASE;
        } else {
            drawMode = DrawMode.DRAW;
        }

  //      if(erase) drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
  //      else drawPaint.setXfermode(null);
    }
    public void startNew(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }
}
