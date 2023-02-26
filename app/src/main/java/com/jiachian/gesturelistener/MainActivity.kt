package com.jiachian.gesturelistener

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    private val root: ConstraintLayout by lazy { findViewById(R.id.root) }
    private val view: View by lazy { findViewById(R.id.view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupGestureListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureListener() {
        root.setOnTouchListener(
            object : GestureListener() {

                private var lastMatrix = Matrix()
                private val originTargetRect by lazy {
                    RectF(
                        view.left.toFloat(),
                        view.top.toFloat(),
                        view.right.toFloat(),
                        view.bottom.toFloat()
                    )
                }
                private val originTargetCenter by lazy {
                    PointF(originTargetRect.centerX(), originTargetRect.centerY())
                }

                private fun RectF.getCenter(): PointF {
                    return PointF(centerX(), centerY())
                }

                override fun onStart(event: MotionEvent): Boolean {
                    return true
                }

                override fun onFinish(event: MotionEvent) {

                }

                override fun onClick(event: MotionEvent) {
                    Toast.makeText(this@MainActivity, "onClick", Toast.LENGTH_SHORT).show()
                }

                override fun onLongClick(event: MotionEvent) {
                    Toast.makeText(this@MainActivity, "onLongClick", Toast.LENGTH_SHORT).show()
                }

                override fun onDoubleTap(event: MotionEvent) {
                    Toast.makeText(this@MainActivity, "onDoubleClick", Toast.LENGTH_SHORT).show()
                }

                override fun onDrag(event: MotionEvent, dx: Float, dy: Float) {
                    val newMatrix = Matrix(lastMatrix).apply {
                        postTranslate(dx, dy)
                    }
                    val newRect = RectF(originTargetRect)
                    newMatrix.mapRect(newRect, originTargetRect)
                    lastMatrix = newMatrix
                    val offset = newRect.getCenter().apply {
                        offset(-originTargetCenter.x, -originTargetCenter.y)
                    }
                    view.translationX = offset.x
                    view.translationY = offset.y
                }

                override fun onGesture(
                    event: MotionEvent,
                    ds: Float,
                    dr: Float,
                    dx: Float,
                    dy: Float,
                    centerPoint: PointF
                ) {
                    val newMatrix = Matrix(lastMatrix).apply {
                        postTranslate(dx, dy)
                        postScale(ds, ds, centerPoint.x, centerPoint.y)
                    }
                    val newRect = RectF(originTargetRect)
                    newMatrix.mapRect(newRect, originTargetRect)
                    lastMatrix = newMatrix
                    val offset = newRect.getCenter().apply {
                        offset(-originTargetCenter.x, -originTargetCenter.y)
                    }
                    view.scaleX *= ds
                    view.scaleY *= ds
                    view.translationX = offset.x
                    view.translationY = offset.y
                }
            }
        )
    }
}