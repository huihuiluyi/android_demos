package com.nobody.demos.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Scroller
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.nobody.demos.R

class SlideMenuLayout(private var c:Context, attr:AttributeSet)
    : ConstraintLayout(c,attr) {

    private var mCollectView: View? = null
    private var mDeleteView: View? = null
    private var mContentView: View? = null

    /**
     * 记录上一次downX
     */
    private var mLastX = 0.0F

    /**
     * 内容区域的 bounds
     */
    private var mContentR : Rect = Rect(0,0,0,0)

    private var mScroller: Scroller = Scroller(c, AccelerateDecelerateInterpolator())

    /**
     * contentView 的偏移量
     */
    private var mOffsetX = 0.0F

    init {
        View.inflate(c, R.layout.slide_menu_layout,this)
        this.mCollectView = TextView(c).apply {
            id = R.id.sml_tv_collect
            text = "收藏"
            setBackgroundColor(Color.parseColor("#00FF00"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(-1, LayoutParams.MATCH_PARENT)
            setOnClickListener {
                Toast.makeText(c,"collect click",Toast.LENGTH_SHORT).show()
            }
            isClickable = false
        }
        this.mDeleteView = TextView(c).apply {
            id = R.id.sml_tv_delete
            text = "删除"
            setBackgroundColor(Color.parseColor("#FF0000"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(-1, LayoutParams.MATCH_PARENT)
            setOnClickListener {
                Toast.makeText(c,"delete click",Toast.LENGTH_SHORT).show()
            }
            isClickable = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getMode(heightMeasureSpec)
        val w = if(wMode == MeasureSpec.EXACTLY) wSize else measuredWidth
        val h = if(hMode == MeasureSpec.EXACTLY) hSize else measuredHeight
        setMeasuredDimension(w,h)
        mContentR.right = measuredWidth
        mContentR.bottom = measuredHeight
    }


    @SuppressLint("ResourceType")
    override fun onFinishInflate() {
        super.onFinishInflate()

        this.mContentView = getChildAt(0)

        removeAllViews()

        val deleteParams = (this.mDeleteView!!.layoutParams as LayoutParams).apply {
            topToTop = ConstraintSet.PARENT_ID
            bottomToBottom = ConstraintSet.PARENT_ID
            endToEnd = ConstraintSet.PARENT_ID
        }
        this.mDeleteView!!.layoutParams = deleteParams
        addView(this.mDeleteView,0)

        val collectParams = (this.mCollectView!!.layoutParams as LayoutParams).apply {
            topToTop = ConstraintSet.PARENT_ID
            bottomToBottom = ConstraintSet.PARENT_ID
            endToStart = R.id.sml_tv_delete
        }
        this.mCollectView!!.layoutParams = collectParams
        addView(this.mCollectView,1)

        addView(this.mContentView,2)
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val collectParams = this.mCollectView!!.layoutParams
        collectParams.width = measuredWidth/4
        collectParams.height = 0
        this.mCollectView!!.layoutParams = collectParams

        val deleteParams = this.mDeleteView!!.layoutParams
        deleteParams.width = measuredWidth/4
        deleteParams.height = 0
        this.mDeleteView!!.layoutParams = deleteParams
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                mLastX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                var dx = event.x - mLastX

                if (this.mOffsetX > 0) {
                    //右滑
                    dx = 0.toFloat()
                    this.mOffsetX = dx
                } else if (this.mOffsetX < -measuredWidth/2.0F) {
                    //左滑
                    dx = 0.0F
                    this.mOffsetX = -measuredWidth/2.0F
                } else {
                    this.mContentView?.let {content ->
                        content.scrollTo(-this.mOffsetX.toInt(),0)
                    }
                    this.mOffsetX += dx
                    mLastX = event.x
                }
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                if (mOffsetX > -measuredWidth/4.0F && mOffsetX < 0.0F) {
                    //关闭
                    this.mScroller.startScroll(-mOffsetX.toInt(),0, mOffsetX.toInt(),0,300)
                    this.mOffsetX = 0.0F
                    changeMenuState(false)
                } else if (this.mOffsetX <= -measuredWidth/4.0F && this.mOffsetX > -measuredWidth/2.0F) {
                    //打开
                    this.mScroller.startScroll(-mOffsetX.toInt(),0,(measuredWidth/2 + mOffsetX.toInt()),0,300)
                    this.mOffsetX = - measuredWidth/2.0F
                    changeMenuState(true)
                }
                invalidate()
            }
        }
        mContentR.left = mOffsetX.toInt()
        mContentR.right = measuredWidth + mOffsetX.toInt()
        return isInContentBounds(event.x.toInt(),event.y.toInt())
    }

    private fun changeMenuState(isMenuOpen:Boolean) {
        this.mCollectView!!.isClickable = isMenuOpen
        this.mDeleteView!!.isClickable = isMenuOpen
    }

    /**
     * 判断当前触碰是否在 content 区域
     */
    private fun isInContentBounds(x:Int,y:Int) :Boolean {
        return mContentR.contains(x, y)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mScroller.computeScrollOffset()) {
            this.mContentView!!.scrollTo(mScroller.currX,0)
            invalidate()
        }
    }

    public interface MenuListener {
        fun onMenuOpened()
        fun onMenuClosed()
        fun onCollectClicked()
        fun onDeleteClicked()
    }
}