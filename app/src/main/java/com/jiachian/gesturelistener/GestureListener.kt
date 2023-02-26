package com.jiachian.gesturelistener

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlin.math.atan2
import kotlin.math.max

abstract class GestureListener : View.OnTouchListener {

    companion object {
        private const val WAIT_SECOND_POINTER_DURATION = 300L
        private const val WAIT_SECOND_POINTER_DISTANCE = 10L
        private const val TRIGGER_DOUBLE_TAP_DURATION = 200L
    }

    private enum class GestureState {
        IDLE,
        WAIT_SECOND_POINTER,
        ONE_POINT,
        TWO_POINT
    }

    private enum class ActionState {
        IDLE,
        CLICK,
        LONG_CLICK,
        DOUBLE_TAP,
        ONE_GESTURE,
        TWO_GESTURE
    }

    abstract fun onStart(event: MotionEvent): Boolean
    abstract fun onFinish(event: MotionEvent)
    open fun onClick(event: MotionEvent) {}
    open fun onLongClick(event: MotionEvent) {}
    open fun onDoubleTap(event: MotionEvent) {}
    open fun onDrag(event: MotionEvent, dx: Float, dy: Float) {}
    open fun onGesture(
        event: MotionEvent,
        ds: Float,
        dr: Float,
        dx: Float,
        dy: Float,
        centerPoint: PointF
    ) {
    }

    private var lastPoint = PointF(0f, 0f)
    private var upTime = MutableStateFlow(0L)
    private val clickPosition = PointF(0f, 0f)

    private val noFingerTouch = MutableStateFlow(true)
    private val firstFingerTouch = MutableStateFlow(false)
    private val secondFingerTouch = MutableStateFlow(false)

    private var doneTowGesture = false

    private var lastTwoPointerDistance = 0f
    private var lastTwoPointerDegree = 0f
    private var lastTwoPointerMidPointF = PointF()

    private val clickCount = MutableStateFlow(0)
    private val moveDistance = MutableStateFlow(0f)

    private var lastGestureState = GestureState.IDLE
    private val gestureState = combine(
        noFingerTouch,
        firstFingerTouch.debounce(WAIT_SECOND_POINTER_DURATION),
        secondFingerTouch,
        moveDistance
    ) { noFinger, firstTouch, secondTouch, moveDistance ->
        if (noFinger) {
            GestureState.IDLE
        } else if (secondTouch) {
            GestureState.TWO_POINT
        } else if (firstTouch || moveDistance > WAIT_SECOND_POINTER_DISTANCE) {
            GestureState.ONE_POINT
        } else {
            GestureState.WAIT_SECOND_POINTER
        }
    }.stateIn(CoroutineScope(Dispatchers.Unconfined), SharingStarted.Eagerly, GestureState.IDLE)

    private var lastActionState = ActionState.IDLE
    private val actionState = combine(
        gestureState,
        upTime.debounce(TRIGGER_DOUBLE_TAP_DURATION),
        moveDistance,
        clickCount
    ) { gesture, _, moveDistance, clickCount ->
        if (gesture == GestureState.IDLE && clickCount == 2) {
            ActionState.DOUBLE_TAP
        } else if (
            lastGestureState == GestureState.WAIT_SECOND_POINTER &&
            gesture == GestureState.IDLE
        ) {
            ActionState.CLICK
        } else if (
            lastGestureState == GestureState.ONE_POINT &&
            moveDistance < WAIT_SECOND_POINTER_DISTANCE &&
            gesture == GestureState.IDLE
        ) {
            ActionState.LONG_CLICK
        } else if (
            !doneTowGesture &&
            gesture == GestureState.ONE_POINT &&
            moveDistance >= WAIT_SECOND_POINTER_DISTANCE
        ) {
            ActionState.ONE_GESTURE
        } else if (gesture == GestureState.TWO_POINT) {
            ActionState.TWO_GESTURE
        } else {
            ActionState.IDLE
        }
    }.stateIn(CoroutineScope(Dispatchers.Unconfined), SharingStarted.Eagerly, ActionState.IDLE)

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v ?: return false
        event ?: return false

        fun updateLastTwoPointerData() {
            val p0 = PointF(event.getX(0), event.getY(0))
            val p1 = PointF(event.getX(1), event.getY(1))
            lastTwoPointerDistance = p0.distanceTo(p1)
            lastTwoPointerDegree = p0.degree(p1)
            lastTwoPointerMidPointF = p0.midPoint(p1)
        }

        val actionMasked = event.actionMasked
        val pointerCount = event.pointerCount
        val touchPoint = PointF(event.rawX, event.rawY)

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                moveDistance.value = 0f
                clickPosition.apply {
                    x = event.rawX
                    y = event.rawY
                }
                lastPoint = touchPoint
                doneTowGesture = false
                firstFingerTouch.value = true
                noFingerTouch.value = false
                if (!onStart(event)) {
                    return false
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                secondFingerTouch.value = true
                doneTowGesture = true
                updateLastTwoPointerData()
            }
            MotionEvent.ACTION_MOVE -> {
                moveDistance.value = max(touchPoint.distanceTo(clickPosition), moveDistance.value)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                secondFingerTouch.value = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (System.currentTimeMillis() - upTime.value > TRIGGER_DOUBLE_TAP_DURATION) {
                    clickCount.value = 1
                    upTime.value = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - upTime.value <= TRIGGER_DOUBLE_TAP_DURATION) {
                    clickCount.value = 2
                }
                noFingerTouch.value = true
                firstFingerTouch.value = false
            }
        }

        when (actionState.value) {
            ActionState.CLICK -> {
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        if (gestureState.value == GestureState.IDLE && clickCount.value != 2) {
                            onClick(event)
                            onFinish(event)
                        }
                    }, TRIGGER_DOUBLE_TAP_DURATION
                )
            }
            ActionState.LONG_CLICK -> {
                onLongClick(event)
                onFinish(event)
            }
            ActionState.DOUBLE_TAP -> {
                onDoubleTap(event)
                onFinish(event)
            }
            ActionState.ONE_GESTURE -> {
                val dx = touchPoint.x - lastPoint.x
                val dy = touchPoint.y - lastPoint.y
                onDrag(event, dx, dy)
                lastPoint = touchPoint
            }
            ActionState.TWO_GESTURE -> {
                if (pointerCount == 2) {
                    val p0 = PointF(event.getX(0), event.getY(0))
                    val p1 = PointF(event.getX(1), event.getY(1))
                    val currentDistance = p0.distanceTo(p1)
                    val currentSlope = p0.degree(p1)
                    val scale = currentDistance / lastTwoPointerDistance
                    val rotate = currentSlope - lastTwoPointerDegree
                    val translate = p0.midPoint(p1).apply {
                        offset(-lastTwoPointerMidPointF.x, -lastTwoPointerMidPointF.y)
                    }
                    lastTwoPointerDistance = currentDistance
                    lastTwoPointerDegree = currentSlope
                    lastTwoPointerMidPointF = p0.midPoint(p1)
                    onGesture(event, scale, rotate, translate.x, translate.y, p0.midPoint(p1))
                }
            }
            ActionState.IDLE -> {
                if (lastActionState == ActionState.ONE_GESTURE || lastActionState == ActionState.TWO_GESTURE) {
                    onFinish(event)
                }
            }
        }
        lastGestureState = gestureState.value
        lastActionState = actionState.value
        return true
    }

    private fun PointF.distanceTo(p: PointF): Float =
        PointF(x, y).apply { offset(-p.x, -p.y) }.length()

    private fun PointF.degree(p: PointF): Float =
        (atan2(
            p.y.toDouble() - y.toDouble(),
            p.x.toDouble() - x.toDouble()
        ) / Math.PI * 180).toFloat()

    private fun PointF.midPoint(p: PointF): PointF = PointF((x + p.x) / 2, (y + p.y) / 2)
}