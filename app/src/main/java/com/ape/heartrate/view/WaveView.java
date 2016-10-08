package com.ape.heartrate.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.ape.heartrate.view.animator.LinearEvaluator;

import java.util.ArrayList;
import java.util.List;

/**
 * The description of use:
 * <br />
 * Created time:2014/6/13 16:01
 * Created by Dave
 */
public class WaveView extends View implements LinearEvaluator.EvaluatorListener,
        Animator.AnimatorListener {

    private static final String TAG = "WaveView";
    private static final boolean DEBUG = true;
    private static final long DURATION_DRAW_WAVE = 330;
    private static final int WAVE_PEAK_LEFT_HEIGHT = 1;
    /**
     * The max peek(contains the wave line) that will be shown
     */
    private static final int MAX_VALID_ENTRIES = 10;
    /**
     * the view width as 9 weights
     */
    private static final int WIDTH_TOTAL_WEIGHTS = 9;
    /**
     * the view height as 9 weights
     */
    private static final int HEIGHT_TOTAL_WEIGHTS = 9;
    private static final int[][] m_WavePeak_Weight = new int[][]{
            new int[]{4, 0, 6, 6},
            new int[]{5, 4, HEIGHT_TOTAL_WEIGHTS, 6},
            new int[]{7, 5, 0, HEIGHT_TOTAL_WEIGHTS},
            new int[]{WIDTH_TOTAL_WEIGHTS, 7, 6, 0}
    };

    private static final int[] WAVELINE_WEIGHT = new int[]{WIDTH_TOTAL_WEIGHTS, 0, 6, 6};

    //The start x and y of wave
    private float mStartX, mStartY;

    //draw the line when the heartrate is not gotten.
    private List<float[]> mPeakObjects;
    private float[] mLineObjects;
    private Paint mPeakPaint;
    private Paint mEmptyPaint = new Paint();
    private Path mWavePath = new Path();
    private float mCellWidth;
    private Bitmap mWaveBitmap;

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setValues();
    }

    public void debug(String msg) {
        if (DEBUG) android.util.Log.v(TAG, msg);
    }

    private void setValues() {

        mPeakPaint = new Paint();
        mPeakPaint.setColor(Color.WHITE);
        mPeakPaint.setAntiAlias(true);
        mPeakPaint.setStyle(Style.STROKE);
        mPeakPaint.setStrokeWidth(2.f);
    }

    private void initWave() {
        debug("initWave");
        mWavePath = new Path();
        mWavePath.moveTo(mStartX, mStartY);
        invalidate();
    }

    public void startWave(int value) {
        if (mWavePath == null)
            initWave();
        startWave(value, DURATION_DRAW_WAVE);
    }

    private void startWave(int value, long duration) {
        doAnimation(value, duration);
    }

    private void doAnimation(int value, long duration) {
        if (value == 0) {
            Animator animator = getLineAnimator(mLineObjects, duration);
            animator.addListener(this);
            animator.start();
        } else {
            AnimatorSet set = new AnimatorSet();
            List<Animator> peakAnim = getPeakAnimator(duration);
            set.playSequentially(peakAnim);
            set.start();
        }
    }

    private Animator getLineAnimator(float[] evaluatorObjects, long duration) {
        float startX = evaluatorObjects[0];
        float startY = evaluatorObjects[1];
        float endX = evaluatorObjects[2];
        float endY = evaluatorObjects[3];
        LinearEvaluator evaluator = new LinearEvaluator(startY, endY);
        evaluator.addListener(this);
        ValueAnimator animator = ValueAnimator.ofObject(evaluator, startX, endX);
        animator.setDuration((long) (duration * evaluatorObjects[4]));
        return animator;
    }

    private List<Animator> getPeakAnimator(long duration) {
        int size = mPeakObjects.size();
        List<Animator> peakAnimators = new ArrayList<Animator>();
        for (int i = 0; i < size; i++) {
            Animator animator = getLineAnimator(mPeakObjects.get(i), duration);
            peakAnimators.add(animator);
            if (i == size - 1) {
                animator.addListener(this);
            }
        }
        return peakAnimators;
    }


    public void stopPaint() {
        recycleWaveBitmap();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        doCalculate(getWidth(), getHeight());
    }

    private void doCalculate(int wWidth, int wHeight) {
        if (mPeakObjects == null) {//Just testing condition.
            final int validWidth = wWidth;
            final int validHeight = wHeight;
            float cellWidth = validWidth / MAX_VALID_ENTRIES;
            float cellHeight = validHeight;
            mCellWidth = cellWidth;
            caculateByWeights(cellWidth, cellHeight - WAVE_PEAK_LEFT_HEIGHT);
        }
    }


    private void caculateByWeights(float cellWidth, float cellHeight) {
        mPeakObjects = new ArrayList<>();
        int size = m_WavePeak_Weight.length;
        for (int i = size - 1; i >= 0; i--) {
            float[] wave = retreiveWaveLine(m_WavePeak_Weight[i], cellWidth, cellHeight);
            //debugWaveLine(wave);
            if (i == size - 1) {
                mStartX = wave[0];
                mStartY = wave[1];
            }

            mPeakObjects.add(wave);
        }
        mLineObjects = retreiveWaveLine(WAVELINE_WEIGHT, cellWidth, cellHeight);
        //debugWaveLine(mLineObjects);
    }

    private float[] retreiveWaveLine(int[] m_WavePeak_Weight, float cellWidth, float cellHeight) {
        int size = m_WavePeak_Weight.length;
        if (size != 4) {
            throw new RuntimeException("wrong size of array");
        }

        float startX, startY, endX = 0f, endY, fractionFromX, fractionToX, fractionFromY, fractionToY;
        int weightFromX, weightToX, weightFromY, weightToY;

        weightFromX = m_WavePeak_Weight[0];
        weightToX = m_WavePeak_Weight[1];
        weightFromY = m_WavePeak_Weight[2];
        weightToY = m_WavePeak_Weight[3];

        fractionFromX = getFractionOfWeight(weightFromX, WIDTH_TOTAL_WEIGHTS);
        fractionToX = getFractionOfWeight(weightToX, WIDTH_TOTAL_WEIGHTS);
        startX = cellWidth * fractionFromX;
        endX = cellWidth * fractionToX;

        fractionFromY = getFractionOfWeight(weightFromY, HEIGHT_TOTAL_WEIGHTS);
        fractionToY = getFractionOfWeight(weightToY, HEIGHT_TOTAL_WEIGHTS);
        startY = cellHeight * fractionFromY;
        endY = cellHeight * fractionToY;

        return new float[]{startX, startY, endX, endY, fractionFromX - fractionToX};
    }

    private float getFractionOfWeight(int weight, int totalWeights) {
        return (float) weight / totalWeights;
    }

    private void debugWaveLine(float[] waveLine) {
        debug("the line Object " + "\n" +
                "the startX is " + waveLine[0] + "\n" +
                "the startY is " + waveLine[1] + "\n" +
                "the endX is " + waveLine[2] + "\n" +
                "the endY is " + waveLine[3] + "\n" +
                "the fraction is " + waveLine[4] + "\n"
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mWaveBitmap != null) {
            canvas.drawBitmap(mWaveBitmap, 0, 0, mEmptyPaint);
        }
    }

    @Override
    public void onEvalutor(final float x, final float y) {
        debug("onEvalutor--- x is " + x + " & y is " + y);
        mWavePath.lineTo(x, y);
        recycleWaveBitmap();
        mWaveBitmap = getBitmap(mWavePath, mPeakPaint);
        invalidate();
    }

    private void recycleWaveBitmap() {
        if (mWaveBitmap != null) {
            mWaveBitmap.recycle();
            mWaveBitmap = null;
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mWavePath.offset(mCellWidth, 0);
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    private Bitmap getBitmap(Path path, Paint m_PeakPaint) {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPath(path, m_PeakPaint);
        return bitmap;
    }

}
