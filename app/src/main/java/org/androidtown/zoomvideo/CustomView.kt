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
    private var maxScale = 5f
    private var saveScale = 1f
    private var mode = NONE



    private var mmatrix = Matrix()
    private var mScaleDetector: ScaleGestureDetector? = null
    private var m: FloatArray? = null

    private val last = PointF()
    private val start = PointF()
    private var right: Float = 0.toFloat()
    private var bottom: Float = 0.toFloat()

    private var m_zoomlayout:FrameLayout? =null
    private var m_ZoomRate:TextView? =null
    private var iszoom:Boolean = false

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

    fun setzoomlayout(layout:FrameLayout?, textView: TextView?){
        m_zoomlayout = layout
        m_ZoomRate = textView
    }

//    override fun onSaveInstanceState(): Parcelable? {
//        val bundle = Bundle()
//        bundle.putParcelable(SUPERSTATE_KEY, super.onSaveInstanceState())
//        bundle.putFloat(MIN_SCALE_KEY, minScale)
//        bundle.putFloat(MAX_SCALE_KEY, maxScale)
//        return bundle
//
//    }
//
//    public override fun onRestoreInstanceState(state: Parcelable?) {
//        var state = state
//        if (state is Bundle) {
//            val bundle = state as Bundle?
//            this.minScale = bundle!!.getInt(MIN_SCALE_KEY).toFloat()
//            this.minScale = bundle.getInt(MAX_SCALE_KEY).toFloat()
//            state = bundle.getParcelable(SUPERSTATE_KEY)
//        }
//        super.onRestoreInstanceState(state)
//    }

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
            val x = m!![Matrix.MTRANS_X]
            val y = m!![Matrix.MTRANS_Y]
            val curr = PointF(motionEvent.x, motionEvent.y)

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

                if(saveScale > 1.0f){
                    m_zoomlayout?.visibility = View.VISIBLE
                    m_ZoomRate?.text = String.format("%.1f", saveScale)
                }else if(saveScale <= 1.0f){
                    m_zoomlayout?.visibility = View.INVISIBLE
                }

                right = width * saveScale - width
                bottom = height * saveScale - height

                Log.d("확인","$right,$bottom , $width, $height")
                if (0 <= width || 0 <= height) {
                    mmatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
                    if (mScaleFactor < 1) {
                        mmatrix.getValues(m)
                        val x = m!![Matrix.MTRANS_X]
                        val y = m!![Matrix.MTRANS_Y]

                        if (mScaleFactor < 1) {
                            if (0 < width) {
                                if (y < -bottom)
                                    mmatrix.postTranslate(0f, -(y + bottom))
                                else if (y > 0)
                                    mmatrix.postTranslate(0f, -y)
                            } else {
                                if (x < -right)
                                    mmatrix.postTranslate(-(x + right), 0f)
                                else if (x > 0)
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
                        if (x < -right)
                            mmatrix.postTranslate(-(x + right), 0f)
                        else if (x > 0)
                            mmatrix.postTranslate(-x, 0f)
                        if (y < -bottom)
                            mmatrix.postTranslate(0f, -(y + bottom))
                        else if (y > 0)
                            mmatrix.postTranslate(0f, -y)
                    }
                }
                return true
            }
        }
    }



}