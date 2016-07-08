package com.chs.mycombinechartview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


import java.util.ArrayList;
import java.util.List;

/**
 * 作者：chs on 2016/7/6 10:08
 * 邮箱：657083984@qq.com
 * 组合图表
 */
public class CombineChart extends View {
    /* 用户点击到了无效位置 */
    public static final int INVALID_POSITION = -1;
    private int screenWidth, screenHeight;
    private List<BarChartBean> mBarData;
    private List<Float> winds;//风力的集合
    private List<Float> humidity;//湿度的集合
    private List<Float> temperature;//温度的集合
    private int colors[] = new int[]{Color.parseColor("#6FC5F4"), Color.parseColor("#78DA9F"), Color.parseColor("#FCAE84")};
    private static final String[] rightYLabels = new String[]{"0级", "5级", "10级", "%0rh", "50%rh", "100%rh", "-50。", "0。", "50。"};
    /**
     * item中的最大值
     */
    private float maxValueInItems;
    /**
     * bar的最高值
     */
    private float maxHeight;
    /**
     * 各种画笔 柱形图的 轴的 文本的 线形图的 画点的
     */
    private Paint barPaint, axisPaint, textPaint, linePaint, pointPaint;
    /**
     * 各种巨型 柱形图的 左边白色部分 右边白色部分
     */
    private Rect barRect, leftWhiteRect, rightWhiteRect;
    private Rect barRect1, barRect2;
    /**
     * 左边和上边的边距
     */
    private int leftMargin, topMargin, smallMargin;
    /**
     * 每一个bar的宽度
     */
    private int barWidth;
    /**
     * 每个bar之间的距离
     */
    private int barSpace;
    /**
     * x轴 y轴 起始坐标
     */
    private float xStartIndex, yStartIndex;
    /**
     * 背景的颜色
     */
    private static final int BG_COLOR = Color.parseColor("#EEEEEE");
    /**
     * 状态栏的高度是否已经获取过
     */
    private boolean statusHeightHasGet;
    /**
     * 向右边滑动的距离
     */
    private float leftMoving;
    /**
     * 左后一次的x坐标
     */
    private float lastPointX;
    /**
     * 当前移动的距离
     */
    private float movingThisTime = 0.0f;
    /**
     * 最大和最小分度值
     */
    private float maxDivisionValue, minDivisionValue;
    private int maxRight, minRight;
    /**
     * 线的路径
     */
    Path linePathW = new Path();//风
    Path linePathH = new Path();//湿度
    Path linePathT = new Path();//温度
    /**
     * 右边的Y轴分成3份  每一分的高度
     */
    private float lineMaxHeight;

    private OnItemBarClickListener mOnItemBarClickListener;
    private GestureDetector mGestureListener;
    public interface OnItemBarClickListener{
       void onClick(int position);
    }

    /**
     * 保存bar的左边和右边的x轴坐标点
     */
    private List<Integer> leftPoints = new ArrayList<>();
    private List<Integer> rightPoints = new ArrayList<>();
    public void setOnItemBarClickListener(OnItemBarClickListener onRangeBarClickListener) {
        this.mOnItemBarClickListener = onRangeBarClickListener;
    }

    public CombineChart(Context context) {
        super(context);
        init(context);
    }

    public CombineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CombineChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        mGestureListener = new GestureDetector(context,new RangeBarOnGestureListener());

        leftMargin = ScreenUtils.dp2px(context, 16);
        topMargin = ScreenUtils.dp2px(context, 20);
        smallMargin = ScreenUtils.dp2px(context, 6);

        barPaint = new Paint();
        barPaint.setColor(colors[0]);

        axisPaint = new Paint();
        axisPaint.setStrokeWidth(2);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStrokeWidth(4);
        linePaint.setColor(Color.WHITE);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setStyle(Paint.Style.FILL);

        barRect = new Rect(0, 0, 0, 0);
        barRect1 = new Rect(0, 0, 0, 0);
        barRect2 = new Rect(0, 0, 0, 0);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = getMeasuredWidth();
        screenHeight = getMeasuredHeight();
        //得到每个bar的宽度
        getItemsWidth(screenWidth, mBarData.size());
        //设置矩形的顶部 底部 右边Y轴的3部分每部分的高度
        getStatusHeight();
        leftWhiteRect = new Rect(0, 0, 0, screenHeight);
        rightWhiteRect = new Rect(screenWidth - leftMargin * 2 - 10, 0, screenWidth, screenHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        leftPoints.clear();
        rightPoints.clear();
        canvas.drawColor(BG_COLOR);
        //重置3条线
        linePathW.reset();
        linePathW.incReserve(winds.size());
        linePathH.reset();
        linePathH.incReserve(winds.size());
        linePathT.reset();
        linePathT.incReserve(winds.size());
        checkTheLeftMoving();
        textPaint.setTextSize(ScreenUtils.dp2px(getContext(), 10));
        for (int i = 0; i < mBarData.size(); i++) {
            //画bar的矩形
            barRect.left = (int) (xStartIndex + barWidth * i + barSpace * (i + 1) - leftMoving);
            barRect.top = (int) maxHeight + topMargin * 2 - (int) (maxHeight * (mBarData.get(i).getyNum() / maxValueInItems));
            barRect.right = barRect.left + barWidth;
            leftPoints.add(barRect.left);
            rightPoints.add(barRect.right);
            barPaint.setColor(colors[0]);
            canvas.drawRect(barRect, barPaint);

            barRect1.top = (int) maxHeight + topMargin * 2 - (int) (maxHeight * (mBarData.get(i).getyNum() / maxValueInItems))
                    - (int) (maxHeight * (mBarData.get(i).getyNum1() / maxValueInItems));
            barRect1.left = (int) (xStartIndex + barWidth * i + barSpace * (i + 1) - leftMoving);
            barRect1.right = barRect.left + barWidth;
            barRect1.bottom = barRect.top;
            barPaint.setColor(colors[1]);
            canvas.drawRect(barRect1, barPaint);

            barRect2.top = (int) maxHeight + topMargin * 2 - (int) (maxHeight * (mBarData.get(i).getyNum() / maxValueInItems))
                    - (int) (maxHeight * (mBarData.get(i).getyNum1() / maxValueInItems)) - (int) (maxHeight * (mBarData.get(i).getyNum2() / maxValueInItems));
            barRect2.left = (int) (xStartIndex + barWidth * i + barSpace * (i + 1) - leftMoving);
            barRect2.right = barRect.left + barWidth;
            barRect2.bottom = barRect1.top;
            barPaint.setColor(colors[2]);
            canvas.drawRect(barRect2, barPaint);
            //画x轴的text
            String text = mBarData.get(i).getxLabel();
            canvas.drawText(text, barRect.left - (textPaint.measureText(text) - barWidth) / 2, barRect.bottom + ScreenUtils.dp2px(getContext(), 10), textPaint);

            //确定线形图的路径 和 画圆点
            drawLines(canvas, i);
        }
        canvas.save();
        //画线型图
        canvas.drawPath(linePathW, linePaint);
        canvas.drawPath(linePathH, linePaint);
        canvas.drawPath(linePathT, linePaint);
        //画线上的点
        drawCircles(canvas);
//        linePath.rewind();

        //画X轴 下面的和上面的
        canvas.drawLine(xStartIndex, yStartIndex, screenWidth - leftMargin, yStartIndex, axisPaint);
        canvas.drawLine(xStartIndex, topMargin / 2, screenWidth - leftMargin, topMargin / 2, axisPaint);
        //画左边和右边的遮罩层
        int c = barPaint.getColor();
        barPaint.setColor(BG_COLOR);
        leftWhiteRect.right = (int) xStartIndex;

        barPaint.setColor(Color.WHITE);
        canvas.drawRect(leftWhiteRect, barPaint);
        canvas.drawRect(rightWhiteRect, barPaint);
        barPaint.setColor(c);

        //画左边的Y轴
        canvas.drawLine(xStartIndex, yStartIndex, xStartIndex, topMargin / 2, axisPaint);
        //画左边的Y轴text
        int maxYHeight = (int) (maxHeight / maxValueInItems * maxDivisionValue);
        for (int i = 1; i <= 10; i++) {
            float startY = barRect.bottom - maxYHeight * 0.1f * i;
            if (startY < topMargin / 2) {
                break;
            }
            canvas.drawLine(xStartIndex, startY, xStartIndex + 10, startY, axisPaint);
            String text = String.valueOf(maxDivisionValue * 0.1f * i);
            canvas.drawText(text, xStartIndex - textPaint.measureText(text) - 5, startY + textPaint.measureText("0") / 2, textPaint);
        }

        //画右边的Y轴和右边Y轴text
        canvas.drawLine(screenWidth - leftMargin * 2 - 10, yStartIndex, screenWidth - leftMargin * 2 - 10, topMargin / 2, axisPaint);
        float eachHeight = ((barRect.bottom - topMargin / 2) / 6);
        for (int j = 0; j < 7; j++) {
            float startY = barRect.bottom - eachHeight * j;
//            if (startY < topMargin / 2) {
//                break;
//            }
            canvas.drawLine(screenWidth - leftMargin * 2 - 10, startY, screenWidth - leftMargin * 2 - 20, startY, axisPaint);
            String text = rightYLabels[j];
            if (j < 2) {
                textPaint.setColor(Color.parseColor("#EE6867"));
                canvas.drawText(text, screenWidth - leftMargin * 2 - 5, startY, textPaint);
            } else {
                switch (j) {
                    case 2:
                        canvas.drawText(text, screenWidth - leftMargin * 2 - 5, startY + textPaint.measureText("级"), textPaint);
                        String text2 = rightYLabels[j + 1];
                        textPaint.setColor(Color.parseColor("#549EF3"));
                        canvas.drawText(text2, screenWidth - leftMargin * 2 - 5, startY, textPaint);
                        break;
                    case 3:
                        String text3 = rightYLabels[j + 1];
                        canvas.drawText(text3, screenWidth - leftMargin * 2 - 5, startY, textPaint);
                        break;
                    case 4:
                        String text4 = rightYLabels[j + 1];
                        canvas.drawText(text4, screenWidth - leftMargin * 2 - 5, startY + textPaint.measureText("级"), textPaint);
                        String text41 = rightYLabels[j + 2];
                        textPaint.setColor(Color.parseColor("#FFD401"));
                        canvas.drawText(text41, screenWidth - leftMargin * 2 - 5, startY, textPaint);
                        break;
                    case 5:
                        String text5 = rightYLabels[j + 2];
                        canvas.drawText(text5, screenWidth - leftMargin * 2 - 5, startY, textPaint);
                        break;
                    case 6:
                        String text6 = rightYLabels[j + 2];
                        canvas.drawText(text6, screenWidth - leftMargin * 2 - 5, startY + textPaint.measureText("级"), textPaint);
                        textPaint.setColor(Color.BLACK);
                        break;
                }
            }
        }
    }

    /**
     * 画线上的点
     */
    private void drawCircles(Canvas canvas) {
        for(int i = 0;i<mBarData.size();i++){
            float lineHeight = winds.get(i) * lineMaxHeight / 10;
            pointPaint.setColor(Color.parseColor("#549EF3"));
            canvas.drawCircle(leftPoints.get(i) + barWidth / 2, barRect.bottom - lineHeight, 10, pointPaint);
            float lineHeight2 = humidity.get(i) * lineMaxHeight / 100;
            pointPaint.setColor(Color.parseColor("#FFD401"));
            canvas.drawCircle(leftPoints.get(i) + barWidth / 2, barRect.bottom - lineHeight2 - lineMaxHeight, 10, pointPaint);
            float lineHeight3 = (temperature.get(i)+50) * lineMaxHeight / 100;
            pointPaint.setColor(Color.parseColor("#549EF3"));
            canvas.drawCircle(leftPoints.get(i) + barWidth / 2, barRect.bottom - lineHeight3 - lineMaxHeight*2, 10, pointPaint);
        }
    }

    /**
     * 画线形图
     *
     * @param canvas
     * @param i
     */
    private void drawLines(Canvas canvas, int i) {
        float lineHeight = winds.get(i) * lineMaxHeight / 10;
        if (i == 0) {
            linePathW.moveTo(barRect.left + barWidth / 2, barRect.bottom - lineHeight);
        } else {
            linePathW.lineTo(barRect.left + barWidth / 2, barRect.bottom - lineHeight);
        }

        float lineHeight2 = humidity.get(i) * lineMaxHeight / 100;
        if (i == 0) {
            linePathH.moveTo(barRect.left + barWidth / 2, barRect.bottom - lineHeight2 - lineMaxHeight);
        } else {
            linePathH.lineTo(barRect.left + barWidth / 2, barRect.bottom - lineHeight2 - lineMaxHeight);
        }

        float lineHeight3 = (temperature.get(i)+50) * lineMaxHeight / 100;
        if (i == 0) {
            linePathT.moveTo(barRect.left + barWidth / 2, barRect.bottom - lineHeight3 - lineMaxHeight*2);
        } else {
            linePathT.lineTo(barRect.left + barWidth / 2, barRect.bottom - lineHeight3 - lineMaxHeight*2);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastPointX = event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                float movex = event.getRawX();
                movingThisTime = lastPointX - movex;
                leftMoving = leftMoving + movingThisTime;
                lastPointX = movex;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                new Thread(new SmoothScrollThread(movingThisTime)).start();
                break;
            default:
                return super.onTouchEvent(event);
        }
        if(mGestureListener!=null){
            mGestureListener.onTouchEvent(event);
        }
        return true;
    }

    /**
     * 检查向左滑动的距离 确保没有画出屏幕
     */
    private void checkTheLeftMoving() {
        if (leftMoving < 0) {
            leftMoving = 0;
        }

        if (leftMoving > (maxRight - minRight)) {
            leftMoving = maxRight - minRight;
        }
    }

    /**
     * 设置矩形的顶部 底部 右边Y轴的3部分每部分的高度
     */
    private void getStatusHeight() {
        barRect.top = topMargin * 2;
        barRect.bottom = screenHeight - topMargin/2;
        maxHeight = barRect.bottom - barRect.top;
        lineMaxHeight = (barRect.bottom - topMargin / 2) / 3;

        yStartIndex = barRect.bottom;
    }

    /**
     * 赋值
     *
     * @param items       柱形图的值
     * @param winds       风力线形图的值
     * @param humidity    湿度线形图的值
     * @param temperature 温度线形图的值
     */
    public void setItems(List<BarChartBean> items, List<Float> winds, List<Float> humidity, List<Float> temperature) {
        if (items == null || winds == null) {
            throw new RuntimeException("BarChartView.setItems(): the param items cannot be null.");
        }
        if (items.size() == 0) {
            return;
        }
        this.mBarData = items;
        this.winds = winds;
        this.humidity = humidity;
        this.temperature = temperature;
        //计算最大值
        maxValueInItems = items.get(0).getyNum() + items.get(0).getyNum1() + items.get(0).getyNum2();
        for (BarChartBean barChartBean : items) {
            float totalNum = barChartBean.getyNum() + barChartBean.getyNum1() + barChartBean.getyNum2();
            if (totalNum > maxValueInItems) {
                maxValueInItems = totalNum;
            }
        }
        //获取分度值
        getRange(maxValueInItems, 0);

        invalidate();
    }

    /**
     * 设定每个bar的宽度 和向右边滑动的时候右边的最大距离
     * @param screenWidth
     * @param size
     */
    private void getItemsWidth(int screenWidth, int size) {
        int barMinWidth = ScreenUtils.dp2px(getContext(), 40);
        int barMinSpace = ScreenUtils.dp2px(getContext(), 10);

        barWidth = (screenWidth - leftMargin * 2) / (size + 3);
        barSpace = (screenWidth - leftMargin * 2 - barWidth * size) / (size + 1);
        if (barWidth < barMinWidth || barSpace < barMinSpace) {
            barWidth = barMinWidth;
            barSpace = barMinSpace;
        }
        maxRight = (int) (xStartIndex + (barSpace + barWidth) * mBarData.size())+barSpace*2;
        minRight = screenWidth - barSpace - leftMargin;
    }

    /**
     * 得到最大和最小的分度值
     *
     * @param maxValueInItems
     * @param min
     */
    private void getRange(float maxValueInItems, float min) {
        int scale = getScale(maxValueInItems);
        float unScaleValue = (float) (maxValueInItems / Math.pow(10, scale));

        maxDivisionValue = (float) (getRangeTop(unScaleValue) * Math.pow(10, scale));

        xStartIndex = getDivisionTextMaxWidth(maxDivisionValue) + 10;
    }

    /**
     * 得到最大宽度值得文本
     *
     * @param maxDivisionValue
     * @return
     */
    private float getDivisionTextMaxWidth(float maxDivisionValue) {
        Paint textPaint = new Paint();
        textPaint.setTextSize(ScreenUtils.dp2px(getContext(), 10));
        float max = textPaint.measureText(String.valueOf(maxDivisionValue * 1.0f));
        for (int i = 2; i <= 10; i++) {
            float w = textPaint.measureText(String.valueOf(maxDivisionValue * 0.1f * i));
            if (w > max) {
                max = w;
            }
        }
        return max;
    }

    private float getRangeTop(float value) {
        //value: [1,10)
        if (value < 1.2) {
            return 1.2f;
        }

        if (value < 1.5) {
            return 1.5f;
        }

        if (value < 2.0) {
            return 2.0f;
        }

        if (value < 3.0) {
            return 3.0f;
        }

        if (value < 4.0) {
            return 4.0f;
        }

        if (value < 5.0) {
            return 5.0f;
        }

        if (value < 6.0) {
            return 6.0f;
        }

        if (value < 8.0) {
            return 8.0f;
        }

        return 10.0f;
    }
    public static int getScale(float value) {
        if (value >= 1 && value < 10) {
            return 0;
        }

        if (value >= 10) {
            return 1 + getScale(value / 10);
        } else {
            return getScale(value * 10) - 1;
        }
    }
    /**
     *	根据点击的手势位置识别是第几个柱图被点击
     * @param x
     * @param y
     * @return -1时表示点击的是无效位置
     */
    private int identifyWhichItemClick( float x, float y ){
        float leftx = 0;
        float rightx = 0;
        for( int i = 0; i < mBarData.size(); i++ ){
            leftx = leftPoints.get(i);
            rightx = rightPoints.get(i);
            if( x < leftx ){
                break;
            }
            if( leftx <= x && x <= rightx ){
                return i;
            }
        }
        return INVALID_POSITION;
    }

    /**
     *	手势监听器
     * @author A Shuai
     *
     */
    private class RangeBarOnGestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {  }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int position = identifyWhichItemClick(e.getX(), e.getY());
            if( position != INVALID_POSITION && mOnItemBarClickListener != null ){
                mOnItemBarClickListener.onClick(position);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {  }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

    }

    /**
     * 左右滑动的时候 当手指抬起的时候  使滑动慢慢停止 不会立刻停止
     */
    private class SmoothScrollThread implements Runnable {
        float lastMoving;
        boolean scrolling = true;

        private SmoothScrollThread(float lastMoving) {
            this.lastMoving = lastMoving;
            scrolling = true;
        }

        @Override
        public void run() {
            while (scrolling) {
                long start = System.currentTimeMillis();
                lastMoving = (int) (0.9f * lastMoving);
                leftMoving += lastMoving;

                checkTheLeftMoving();
                postInvalidate();

                if (Math.abs(lastMoving) < 5) {
                    scrolling = false;
                }

                long end = System.currentTimeMillis();
                if (end - start < 20) {
                    try {
                        Thread.sleep(20 - (end - start));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
