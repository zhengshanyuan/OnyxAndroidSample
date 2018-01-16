package com.onyx.android.sample;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.pen.data.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class PenStylusTouchHelperDemoActivity extends AppCompatActivity {

    private static final String TAG = PenStylusTouchHelperDemoActivity.class.getSimpleName();
    /** skip point count*/
    private static final int INTERVAL = 10;

    @Bind(R.id.button_pen)
    Button buttonPen;
    @Bind(R.id.button_eraser)
    Button buttonEraser;
    @Bind(R.id.surfaceview)
    SurfaceView surfaceView;
    @Bind(R.id.cb_render)
    CheckBox cbRender;
    @Bind(R.id.rb_brush)
    RadioButton rbBrush;
    @Bind(R.id.rb_pencil)
    RadioButton rbPencil;

    private TouchHelper touchHelper;

    Paint paint = new Paint();
    private TouchPoint startPoint;
    private int countRec = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pen_stylus_touch_helper_demo);

        ButterKnife.bind(this);
        initPaint();
        initSurfaceView();
    }

    @Override
    protected void onResume() {
        touchHelper.setRawDrawingEnabled(true);
        super.onResume();
    }

    @Override
    protected void onPause() {
        touchHelper.setRawDrawingEnabled(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        touchHelper.closeRawDrawing();
        super.onDestroy();
    }

    private void initPaint(){
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
    }

    private void initSurfaceView() {
        touchHelper = TouchHelper.create(surfaceView, callback);

        surfaceView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int
                    oldRight, int oldBottom) {
                if (cleanSurfaceView()){
                    surfaceView.removeOnLayoutChangeListener(this);
                }
                List<Rect> exclude = new ArrayList<>();
                exclude.add(getRelativeRect(surfaceView, buttonEraser));
                exclude.add(getRelativeRect(surfaceView, buttonPen));
                exclude.add(getRelativeRect(surfaceView, cbRender));
                exclude.add(getRelativeRect(surfaceView, rbBrush));
                exclude.add(getRelativeRect(surfaceView, rbPencil));

                Rect limit = new Rect();
                surfaceView.getLocalVisibleRect(limit);
                touchHelper.setStrokeWidth(3.0f)
                           .setLimitRect(limit, exclude)
                           .openRawDrawing();
                touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_BRUSH);
            }
        });
    }

    @OnClick(R.id.button_pen)
    public void onPenClick() {
        touchHelper.setRawDrawingEnabled(true);
        onRenderEnableClick();
    }

    @OnClick(R.id.button_eraser)
    public void onEraserClick() {
        touchHelper.setRawDrawingEnabled(false);
        cleanSurfaceView();
    }

    @OnCheckedChanged(R.id.cb_render)
    public void onRenderEnableClick() {
        touchHelper.setRawDrawingRenderEnabled(cbRender.isChecked());
        Log.d(TAG,"onRenderEnableClick setRawDrawingRenderEnabled =  " + cbRender.isChecked());
    }

    @OnClick({R.id.rb_brush, R.id.rb_pencil})
    public void onRadioButtonClicked(RadioButton radioButton) {

        boolean checked = radioButton.isChecked();
        Log.d(TAG, radioButton.toString());
        switch (radioButton.getId()) {
            case R.id.rb_brush:
                if (checked) {
                    touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_BRUSH);
                    Log.d(TAG, "STROKE_STYLE_BRUSH");
                }
                break;
            case R.id.rb_pencil:
                if (checked) {
                    touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL);
                    Log.d(TAG, "STROKE_STYLE_PENCIL");
                }
                break;
        }
        // refresh ui
        onEraserClick();
        onPenClick();
    }

    public Rect getRelativeRect(final View parentView, final View childView) {
        int [] parent = new int[2];
        int [] child = new int[2];
        parentView.getLocationOnScreen(parent);
        childView.getLocationOnScreen(child);
        Rect rect = new Rect();
        childView.getLocalVisibleRect(rect);
        rect.offset(child[0] - parent[0], child[1] - parent[1]);
        return rect;
    }

    private boolean cleanSurfaceView() {
        if (surfaceView.getHolder() == null) {
            return false;
        }
        Canvas canvas = surfaceView.getHolder().lockCanvas();
        if (canvas == null) {
            return false;
        }
        canvas.drawColor(Color.WHITE);
        surfaceView.getHolder().unlockCanvasAndPost(canvas);
        return true;
    }

    private void drawRect(TouchPoint endPoint){
        Canvas canvas = surfaceView.getHolder().lockCanvas();
        if (canvas == null ) {
            return;
        }

        if (startPoint == null || endPoint == null) {
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
            return;
        }

        canvas.drawColor(Color.WHITE);
        canvas.drawRect(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY(), paint);
        Log.d(TAG,"drawRect ");
        surfaceView.getHolder().unlockCanvasAndPost(canvas);
    }

    private RawInputCallback callback = new RawInputCallback() {

        @Override
        public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onBeginRawDrawing");
            startPoint = touchPoint;
            Log.d(TAG,touchPoint.getX() +", " +touchPoint.getY());
            countRec = 0;
        }

        @Override
        public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawDrawing");
            if (!cbRender.isChecked()){
                drawRect(touchPoint);
            }
            Log.d(TAG,touchPoint.getX() +", " +touchPoint.getY());
        }

        @Override
        public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
            Log.d(TAG, "onRawDrawingTouchPointMoveReceived");
            Log.d(TAG,touchPoint.getX() +", " +touchPoint.getY());
            countRec++;
            countRec = countRec % INTERVAL;
            if (!cbRender.isChecked() && countRec == INTERVAL - 1 ){
                drawRect(touchPoint);
            }
            Log.d(TAG,"countRec = " + countRec);
        }

        @Override
        public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
            Log.d(TAG, "onRawDrawingTouchPointListReceived");
        }

        @Override
        public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onBeginRawErasing");
        }

        @Override
        public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
            Log.d(TAG, "onEndRawErasing");
        }

        @Override
        public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
            Log.d(TAG, "onRawErasingTouchPointMoveReceived");
        }

        @Override
        public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
            Log.d(TAG, "onRawErasingTouchPointListReceived");
        }
    };

}