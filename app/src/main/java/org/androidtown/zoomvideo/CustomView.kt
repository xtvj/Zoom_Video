package org.androidtown.zoomvideo

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.view.*

class ZoomableTextureView : TextureView {

    companion object {
        private val SUPERSTATE_KEY = "superState"
        private val MIN_SCALE_KEY = "minScale"
        private val MAX_SCALE_KEY = "maxScale"

        private val NONE = 0
        private val DRAG = 1
        private val ZOOM = 2
    }

    private var minScale = 1f
    private var maxScale = 3.24f
    private var saveScale = 1f
    private var mode = NONE

    private var mMinimapRate = 0.22F

    private var P_Left = 0
    private var P_Right = 0
    private var P_Top = 0
    private var P_Bottom = 0

    private var mmatrix = Matrix()
    private var mScaleDetector: ScaleGestureDetector? = null
    private var m: FloatArray? = null

    private val last = PointF()
    private val start = PointF()
    private var right: Float = 0.toFloat()
    private var bottom: Float = 0.toFloat()

    private var m_zoomlayout: FrameLayout? = null
    private var m_zoomRatelayout: RelativeLayout? = null
    private var m_ZoomRate: TextView? = null
    private var iszoom: Boolean = false

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

    fun setzoomlayout(layout: FrameLayout?, zoomlayout: RelativeLayout?, textView: TextView?) {
        m_zoomlayout = layout
        m_zoomRatelayout = zoomlayout
        m_ZoomRate = textView

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

    private inner class ZoomOnTouchListeners : View.OnTouchListener {
        init {
            m = FloatArray(9)
            mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        }

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
                    mode = DRAG
                }
                MotionEvent.ACTION_UP -> mode = NONE
                MotionEvent.ACTION_POINTER_DOWN -> {
                    last.set(motionEvent.x, motionEvent.y)
                    start.set(last)
                    mode = ZOOM
                }
                MotionEvent.ACTION_MOVE -> if (mode == ZOOM || mode == DRAG && saveScale > minScale) {
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
                MotionEvent.ACTION_POINTER_UP -> mode = NONE
            }

            mmatrix.getValues(m)
            x = m!![Matrix.MTRANS_X]
            y = m!![Matrix.MTRANS_Y]

            P_Left = -(x / saveScale * mMinimapRate).toInt()
            P_Top = -(y / saveScale * mMinimapRate).toInt()
            P_Right = ((right / saveScale) * mMinimapRate).toInt() - P_Left
            P_Bottom = ((bottom / saveScale) * mMinimapRate).toInt() - P_Top

            m_zoomlayout?.setPadding(P_Left, P_Top, P_Right, P_Bottom)

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

                if (saveScale > 1.0f) {
                    m_zoomlayout?.visibility = View.VISIBLE
                    m_ZoomRate?.text = String.format("x%.1f", saveScale)
                } else if (saveScale <= 1.0f) {
                    m_zoomlayout?.visibility = View.INVISIBLE
                }

                right = width * saveScale - width
                bottom = height * saveScale - height

                P_Left = 0
                P_Top = 0
                P_Right = 0
                P_Bottom = 0


                if (0 <= width || 0 <= height) {

                    mmatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)

                    mmatrix.getValues(m)
                    val x = m!![Matrix.MTRANS_X]
                    val y = m!![Matrix.MTRANS_Y]

                    P_Left = -(x / saveScale * mMinimapRate).toInt()
                    P_Top = -(y / saveScale * mMinimapRate).toInt()
                    P_Right = ((right / saveScale) * mMinimapRate).toInt()
                    P_Bottom = ((bottom / saveScale) * mMinimapRate).toInt()

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

                    if (P_Left < 0) {
                        P_Left = 0
                    } else if (P_Top < 0) {
                        P_Top = 0
                    }

                    P_Right -= P_Left
                    P_Bottom -= P_Top

                    m_zoomlayout?.setPadding(P_Left, P_Top, P_Right, P_Bottom)

                } else {
                    mmatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
                    mmatrix.getValues(m)
                    val x = m!![Matrix.MTRANS_X]
                    val y = m!![Matrix.MTRANS_Y]

                    P_Left = -(x / saveScale * mMinimapRate).toInt()
                    P_Top = -(y / saveScale * mMinimapRate).toInt()
                    P_Right = ((right / saveScale) * mMinimapRate).toInt()
                    P_Bottom = ((bottom / saveScale) * mMinimapRate).toInt()

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

                    if (P_Left < 0) {
                        P_Left = 0
                    } else if (P_Top < 0) {
                        P_Top = 0
                    }
                    P_Right -= P_Left
                    P_Bottom -= P_Top

                    m_zoomlayout?.setPadding(P_Left, P_Top, P_Right, P_Bottom)

                }
                return true
            }
        }
    }


}