package org.androidtown.zoomvideo

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View

class ZoomableTextureView : TextureView {

    companion object {

        private val NONE = 0
        private val DRAG = 1
        private val ZOOM = 2
    }

    private var minScale = 1f
    private var maxScale = 6f
    private var saveScale = 1f
    private var mode = NONE

    private var mmatrix = Matrix()
    private var mScaleDetector: ScaleGestureDetector? = null
    private var m: FloatArray? = null

    private val last = PointF()
    private val start = PointF()
    private var right: Float = 0.toFloat()
    private var bottom: Float = 0.toFloat()

    fun setMinScale(scale: Float) {
        if (scale < 1.0f || scale > maxScale)
            throw RuntimeException("minScale can't be lower than 1 or larger than maxScale($maxScale)")
        else
            minScale = scale
    }

    fun setMaxScale(scale: Float) {
        if (scale < 1.0f || scale < minScale)
            throw RuntimeException("maxScale can't be lower than 1 or minScale($minScale)")
        else
            minScale = scale
    }


    constructor(context: Context) : super(context) {
        initView(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(attrs)
    }

    private fun initView(attrs: AttributeSet?) {
        val a = context!!.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ZoomableTextureView,
            0, 0
        )
        try {
            minScale = a.getFloat(R.styleable.ZoomableTextureView_minScale, minScale)
            maxScale = a.getFloat(R.styleable.ZoomableTextureView_maxScale, maxScale)
        } finally {
            a.recycle()
        }

        setOnTouchListener(ZoomOnTouchListeners())
    }

    private inner class ZoomOnTouchListeners : OnTouchListener {
        init {
            m = FloatArray(9)
            mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        }

        private var startTime = System.currentTimeMillis()
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {

            mScaleDetector!!.onTouchEvent(motionEvent)

            mmatrix.getValues(m)
            var x = m!![Matrix.MTRANS_X]
            var y = m!![Matrix.MTRANS_Y]
            val curr = PointF(motionEvent.x, motionEvent.y)

            right = width * saveScale - width
            bottom = height * saveScale - height

            when (motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    last.set(motionEvent.x, motionEvent.y)
                    start.set(last)
                    mode = NONE
                    startTime = System.currentTimeMillis()
                }

                MotionEvent.ACTION_UP -> {
                    if (mode == NONE) {
                        performClick()
                    } else {
                        mode = NONE
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    last.set(motionEvent.x, motionEvent.y)
                    start.set(last)
                    mode = ZOOM
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mode == ZOOM || mode == DRAG && saveScale > minScale && motionEvent.pointerCount >= 2) {
                        var deltaX = curr.x - last.x// x difference
                        var deltaY = curr.y - last.y// y difference
                        if (y + deltaY > 0)
                            deltaY = -y
                        else if (y + deltaY < -bottom)
                            deltaY = -(y + bottom)

                        if (x + deltaX > 0)
                            deltaX = -x
                        else if (x + deltaX < -right)
                            deltaX = -(x + right)
                        mmatrix.postTranslate(deltaX, deltaY)
                        last.set(curr.x, curr.y)
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    if (motionEvent.pointerCount < 2) {
                        mode = NONE
                    }
                }
            }

            mmatrix.getValues(m)
            x = m!![Matrix.MTRANS_X]
            y = m!![Matrix.MTRANS_Y]

            this@ZoomableTextureView.setTransform(mmatrix)
            this@ZoomableTextureView.invalidate()
            return true
        }

        private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                mode = ZOOM
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                var mScaleFactor = detector.scaleFactor
                val origScale = saveScale

                saveScale *= mScaleFactor

                if (saveScale > maxScale) {
                    saveScale = maxScale
                    mScaleFactor = maxScale / origScale
                } else if (saveScale < minScale) {
                    saveScale = minScale
                    mScaleFactor = minScale / origScale
                }

                right = width * saveScale - width
                bottom = height * saveScale - height

                if (0 <= width || 0 <= height) {

                    mmatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)

                    mmatrix.getValues(m)
                    val x = m!![Matrix.MTRANS_X]
                    val y = m!![Matrix.MTRANS_Y]

                    if (mScaleFactor < 1) {

                        if (0 < width) {
                            if (y < -bottom) {
                                mmatrix.postTranslate(0f, -(y + bottom))
                            } else if (y > 0) {
                                mmatrix.postTranslate(0f, -y)
                            }
                        } else {
                            if (x < -right) {
                                mmatrix.postTranslate(-(x + right), 0f)
                            } else if (x > 0) {
                                mmatrix.postTranslate(-x, 0f)
                            }
                        }
                    }

                } else {
                    mmatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
                    mmatrix.getValues(m)
                    val x = m!![Matrix.MTRANS_X]
                    val y = m!![Matrix.MTRANS_Y]

                    if (mScaleFactor < 1) {

                        if (x < -right) {
                            mmatrix.postTranslate(-(x + right), 0f)
                        } else if (x > 0) {
                            mmatrix.postTranslate(-x, 0f)
                        }
                        if (y < -bottom) {
                            mmatrix.postTranslate(0f, -(y + bottom))
                        } else if (y > 0) {
                            mmatrix.postTranslate(0f, -y)
                        }

                    }

                }
                return true
            }
        }
    }


}