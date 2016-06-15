package com.zlx.gooview.ui;

import com.zlx.gooview.util.GeometryUtil;
import com.zlx.gooview.util.Utils;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * 粘性控件
 * @author poplar
 *
 */
public class GooView extends View {

	private Paint paint;

	public GooView(Context context) {
		this(context, null);
	}

	public GooView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GooView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		paint = new Paint(Paint.ANTI_ALIAS_FLAG); // 抗锯齿
		paint.setColor(Color.RED); // 设置画笔为红色
		
	}
	
	PointF mDragCenter = new PointF(80f, 80f); // 固定圆圆心
	float mDragRadius = 16f; // 固定圆半径
	PointF mStickCenter = new PointF(150f, 150f); // 固定圆圆心
	float mStickRadius = 12f; // 固定圆半径
	PointF[] mStickPoints = new PointF[]{ // 固定圆附着点
			new PointF(250f,250f),
			new PointF(250f,350f),
	};
	PointF[] mDragPoints = new PointF[]{ // 拖拽圆的附着点
			new PointF(50f,250f),
			new PointF(50f,350f),
	};
	
	PointF mControlPoint = new PointF(150f, 300f); // 控制点
	private int statusBarHeight;
	
	@Override
	protected void onDraw(Canvas canvas) {
		// 计算真实变量
		

			// 根据距离计算固定圆半径
			float tempStickRadius = computeStickRadius();
			
			float yOffset = mStickCenter.y - mDragCenter.y;
			float xOffset = mStickCenter.x - mDragCenter.x;
			
			Double lineK = null;
			if(xOffset != 0){
				lineK = (double) (yOffset / xOffset);
			}
			// 四个附着点坐标
			// 拖拽圆的附着点
			mDragPoints = GeometryUtil.getIntersectionPoints(mDragCenter, mDragRadius, lineK);
			mStickPoints = GeometryUtil.getIntersectionPoints(mStickCenter, tempStickRadius, lineK);
			
			// 控制点坐标
			mControlPoint = GeometryUtil.getMiddlePoint(mDragCenter, mStickCenter);
			
			
			
		// 将画布向上平移状态栏的高度
		canvas.save();
		canvas.translate(0, -statusBarHeight);
			
			// 绘制最大范围  (参考用)
			paint.setStyle(Style.STROKE);
			canvas.drawCircle(mStickCenter.x, mStickCenter.y, farestDistance, paint);
			paint.setStyle(Style.FILL);
			
			// 绘制附着点 (参考用)
//			paint.setColor(Color.BLUE);
//			canvas.drawCircle(mDragPoints[0].x, mDragPoints[0].y, 3f, paint);
//			canvas.drawCircle(mDragPoints[1].x, mDragPoints[1].y, 3f, paint);
//			canvas.drawCircle(mStickPoints[0].x, mStickPoints[0].y, 3f, paint);
//			canvas.drawCircle(mStickPoints[1].x, mStickPoints[1].y, 3f, paint);
//			paint.setColor(Color.RED);

		if(!isDisappear){
			// 没有消失才绘制
			if(!isOutOfRange){
				// 没超出范围, 才绘制
				// 绘制中间连接部分
				Path path = new Path(); // 路径
				// 跳到点1
				path.moveTo(mStickPoints[0].x, mStickPoints[0].y);
				// 点1 -> 点2 画曲线
				path.quadTo(mControlPoint.x, mControlPoint.y, mDragPoints[0].x, mDragPoints[0].y);
				// 点2 -> 点3 画直线
				path.lineTo(mDragPoints[1].x, mDragPoints[1].y);
				// 点3 -> 点4 画曲线
				path.quadTo(mControlPoint.x, mControlPoint.y, mStickPoints[1].x, mStickPoints[1].y);
				path.close(); //封闭曲线
				canvas.drawPath(path, paint);
				
				// 绘制固定圆
				canvas.drawCircle(mStickCenter.x, mStickCenter.y, tempStickRadius, paint);
			}
			
			// 绘制拖拽圆
			canvas.drawCircle(mDragCenter.x, mDragCenter.y, mDragRadius, paint);
		}
		
		canvas.restore(); // 恢复至上一次save时, 画布的状态
	}
	
	float farestDistance = 80f;
	private boolean isOutOfRange = false; // 是否超出范围
	private boolean isDisappear = false; // 是否消失
	/**
	 * 根据两圆圆心距离计算固定圆半径
	 * @return
	 */
	private float computeStickRadius() {
		
		float distance = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
		
		// 0 -> 80f
		distance = Math.min(distance, farestDistance);
		
		float percent = distance / farestDistance; // 0.0 -> 1.0
		System.out.println("percent: " + percent);
		
		// 12f -> 3f
		return GeometryUtil.evaluateValue(percent, mStickRadius, mStickRadius * 0.25f);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x;
		float y;
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			isOutOfRange = false;
			isDisappear = false;
			x = event.getRawX();
			y = event.getRawY();
			// 更新拖拽圆圆心坐标
			updateDragCenter(x, y);
			
			break;
		case MotionEvent.ACTION_MOVE:
			x = event.getRawX();
			y = event.getRawY();
			// 更新拖拽圆圆心坐标
			updateDragCenter(x, y);
			
			float distance = GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter);
			if(distance > farestDistance){ // 超出最大范围, 断开
				isOutOfRange  = true;
				invalidate();
			}
			
			break;
		case MotionEvent.ACTION_UP:
			
			if(isOutOfRange){
				if(GeometryUtil.getDistanceBetween2Points(mDragCenter, mStickCenter) > farestDistance){
					// 1. 超出最大范围, 断开, 没放回去, 松手, 消失
					isDisappear = true;
					invalidate();
				}else {
					// 2. 超出最大范围, 断开, 又放回去, 松手, 恢复
					updateDragCenter(mStickCenter.x, mStickCenter.y);
				}
			}else {
				// 3. 没超出最大范围, 松手, 弹回去
				final PointF startP = new PointF(mDragCenter.x, mDragCenter.y);
				
				ValueAnimator animator = ValueAnimator.ofFloat(100.0f);
				
				animator.addUpdateListener(new AnimatorUpdateListener() {
					
					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						float fraction = animation.getAnimatedFraction();
						// 0.0 -> 1.0
						PointF p = GeometryUtil.getPointByPercent(startP, mStickCenter, fraction);
						
						updateDragCenter(p.x, p.y);
					}
				});
				animator.setInterpolator(new OvershootInterpolator(4));
				
				animator.setDuration(500);
				animator.start();
				
			}
			
			
			break;

		default:
			break;
		}
		
		return true;
	}

	/**
	 * 更新拖拽圆圆心
	 * @param x
	 * @param y
	 */
	private void updateDragCenter(float x, float y) {
		mDragCenter.set(x, y);
		invalidate();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		statusBarHeight = Utils.getStatusBarHeight(this);
	}
}
