package com.customview.customwave;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import androidx.annotation.Nullable;

import com.customview.customwave.tool.DpOrPxUtils;

/**
 * reference from https://www.jianshu.com/p/34bbcd80dc7a
 * 自定义波浪
 *
 * 2021/2/22
 */

public class WaveProgressView extends View {

    private final String TAG = "WaveProgressView";
    private final boolean DEBUG = true;

    private Paint wavePaint;
    private Path wavePath;
    private Paint secondWavePaint;

    private int waveNum;
    private int defaultSize;
    private int maxHeight;

    private float waveWidth;
    private float waveHeight;

    private int viewSize;//重新测量后View实际的宽高

    //添加动画效果
    private WaveProgressAnim waveProgressAnim;
    private float percent;//进度条占比
    private float progressNum;//可以更新的进度条数值
    private float maxNum;//进度条最大值

    private float waveMovingDistance;//波浪平移的距离

    private Paint circlePaint;//圆形进度框画笔
    private Bitmap bitmap;//缓存bitmap
    private Canvas bitmapCanvas;

    private int waveColor;//波浪颜色
    private int bgColor;//背景进度框颜色

    private int secondWaveColor;//第二层波浪颜色
    private boolean isDrawSecondWave;//是否绘制第二层波浪

    private OnAnimationListener onAnimationListener;
    public WaveProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    private void init(Context context,AttributeSet attrs){
        waveWidth = DpOrPxUtils.dip2px(context,20);
        waveHeight = DpOrPxUtils.dip2px(context,10);
        defaultSize = DpOrPxUtils.dip2px(context,200);
        maxHeight = DpOrPxUtils.dip2px(context,250);

        waveNum =(int) Math.ceil(Double.parseDouble(String.valueOf(defaultSize / waveWidth / 2)));//波浪的数量需要进一取整，所以使用Math.ceil函数
        if(DEBUG)
            Log.i(TAG,"init defaultSize:" + defaultSize + " waveWidth:" + waveWidth + " waveNum :" + waveNum);

        wavePath = new Path();

        wavePaint = new Paint();
        wavePaint.setColor(Color.GREEN);
        wavePaint.setAntiAlias(true);//设置抗锯齿

        wavePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));//根据绘制顺序的不同选择相应的模式即可

        //圆框
        circlePaint = new Paint();
        circlePaint.setColor(Color.GRAY);
        circlePaint.setAntiAlias(true);//设置抗锯齿


        percent = 0;
        progressNum = 0;
        maxNum = 100;
        waveProgressAnim = new WaveProgressAnim();

        waveMovingDistance = 0;

        //让波浪到达最高处后平移的速度改变 给动画设置监听即可
        waveProgressAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {
                if(percent == progressNum / maxNum){
                    waveProgressAnim.setDuration(8000);
                }
            }
        });


        TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.WaveProgressView);
        waveWidth = typedArray.getDimension(R.styleable.WaveProgressView_wave_width,DpOrPxUtils.dip2px(context,40));
        waveHeight = typedArray.getDimension(R.styleable.WaveProgressView_wave_height,DpOrPxUtils.dip2px(context,20));
        waveColor = typedArray.getColor(R.styleable.WaveProgressView_wave_color,getResources().getColor(R.color.dodgerblue));
        bgColor = typedArray.getColor(R.styleable.WaveProgressView_bg_color,getResources().getColor(R.color.lightslategray));
        secondWaveColor = typedArray.getColor(R.styleable.WaveProgressView_second_wave_color,getResources().getColor(R.color.mediumturquoise));
        typedArray.recycle();

        wavePaint.setColor(waveColor);

        circlePaint.setColor(bgColor);
        secondWavePaint = new Paint();
        secondWavePaint.setColor(secondWaveColor);
        secondWavePaint.setAntiAlias(true);//设置抗锯齿
        //因为要覆盖在第一层波浪上，且要让半透明生效，所以选SRC_ATOP模式
        secondWavePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        isDrawSecondWave = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //这里用到了缓存技术
        bitmap = Bitmap.createBitmap(viewSize, viewSize, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawCircle(viewSize/2, viewSize/2, viewSize/2, circlePaint);
        bitmapCanvas.drawPath(getWavePath(),wavePaint);
        if(isDrawSecondWave){
            bitmapCanvas.drawPath(getSecondWavePath(),secondWavePaint);
        }

        canvas.drawBitmap(bitmap, 0, 0, null);

    }
    public interface OnAnimationListener {
        /**
         * 如何处理波浪高度
         * @param percent 进度占比
         * @param waveHeight 波浪高度
         * @return
         */
        float howToChangeWaveHeight(float percent, float waveHeight);
    }

    public void setOnAnimationListener(OnAnimationListener onAnimationListener) {
        this.onAnimationListener = onAnimationListener;
    }

    private Path getSecondWavePath(){
        float changeWaveHeight = waveHeight;
        if(onAnimationListener!=null){
            changeWaveHeight =
                    onAnimationListener.howToChangeWaveHeight(percent,waveHeight) == 0 && percent < 1
                            ?waveHeight
                            :onAnimationListener.howToChangeWaveHeight(percent,waveHeight);
        }

        wavePath.reset();
        //移动到左上方，也就是p3点
        wavePath.moveTo(0, (1-percent)*viewSize);
        //移动到左下方，也就是p2点
        wavePath.lineTo(0, viewSize);
        //移动到右下方，也就是p1点
        wavePath.lineTo(viewSize, viewSize);
        //移动到右上方，也就是p0点
        wavePath.lineTo(viewSize + waveMovingDistance, (1-percent)*viewSize);

        //从p0开始向p3方向绘制波浪曲线（注意绘制二阶贝塞尔曲线控制点和终点x坐标的正负值）
        for (int i=0;i<waveNum*2;i++){
            wavePath.rQuadTo(-waveWidth/2, changeWaveHeight, -waveWidth, 0);
            wavePath.rQuadTo(-waveWidth/2, -changeWaveHeight, -waveWidth, 0);
        }

        //将path封闭起来
        wavePath.close();
        return wavePath;
    }

    /**
     * 是否绘制第二层波浪
     * @param isDrawSecondWave
     */
    public void setDrawSecondWave(boolean isDrawSecondWave) {
        this.isDrawSecondWave = isDrawSecondWave;
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = measureSize(defaultSize, heightMeasureSpec);
        int width = measureSize(defaultSize, widthMeasureSpec);

        int min = Math.min(width, height);// 获取View最短边的长度
        setMeasuredDimension(min, min);// 强制改View为以最短边为长度的正方形
        viewSize = min;
        waveNum =(int) Math.ceil(Double.parseDouble(String.valueOf(viewSize / waveWidth / 2)));
        if(DEBUG)
            Log.i(TAG,"onMeasure defaultSize:" + defaultSize + " height:" + height + " width :" + width
                    + " min:" + min + " waveNum:" + waveNum);

    }

    /**
     * 根据measureSpec的类型决定实际size
     * @param defaultSize
     * @param measureSpec
     * @return
     */
    private int measureSize(int defaultSize,int measureSpec) {
        int result = defaultSize;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);

        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }

    private Path getWavePath(){
        wavePath.reset();

        //移动到右上方，也就是p0点
        wavePath.moveTo(viewSize,(1 - percent)*viewSize);
        //移动到右下方，也就是p1点
        wavePath.lineTo(viewSize,viewSize);
        //移动到左下边，也就是p2点
        wavePath.lineTo(0,viewSize);
        //移动到左上方，也就是p3点
        wavePath.lineTo(-waveMovingDistance,(1 - percent)*viewSize);

        float changeWaveHeight = waveHeight;
        if(onAnimationListener!=null){
            changeWaveHeight =
                    onAnimationListener.howToChangeWaveHeight(percent,waveHeight) == 0 && percent < 1
                            ?waveHeight
                            :onAnimationListener.howToChangeWaveHeight(percent,waveHeight);
        }

        for (int i = 0; i < waveNum * 2; i++){
            wavePath.rQuadTo(waveWidth/2, changeWaveHeight, waveWidth, 0);
            wavePath.rQuadTo(waveWidth/2, -changeWaveHeight, waveWidth, 0);
        }
        //将path封闭起来
        wavePath.close();

        return wavePath;
    }

    public class WaveProgressAnim extends Animation {
        public WaveProgressAnim(){}
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if(percent < progressNum / maxNum){
                percent = interpolatedTime * progressNum / maxNum;
            }

            waveMovingDistance = interpolatedTime * waveNum * waveWidth * 2;

            if(DEBUG)
                Log.i(TAG," applyTransformation interpolatedTime:" + interpolatedTime + " progressNum:" + progressNum
                        + " maxNum:" + maxNum + " percent:" + percent);

            postInvalidate();
    }
    }

    /**
     * 设置进度条数值
     * @param progressNum 进度条数值
     * @param time 动画持续时间
     */
    public void setProgressNum(float progressNum, int time) {
        this.progressNum = progressNum;

        percent = 0;
        waveProgressAnim.setDuration(time);
        waveProgressAnim.setRepeatCount(Animation.INFINITE);//让动画无限循环
        waveProgressAnim.setInterpolator(new LinearInterpolator());//让动画匀速播放，不然会出现波浪平移停顿的现象
        this.startAnimation(waveProgressAnim);

    }

}