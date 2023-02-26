# GestureListener

## About
A Custom TouchListener that detects gesture operations, currently supporting the following operations: 
1. single tap
2. double tap
3. long press
4. drag
4. two-finger operations

## How to use
```kotlin
view.setOnTouchListener(
            object : GestureListener() {
                override fun onStart(event: MotionEvent): Boolean = true

                override fun onFinish(event: MotionEvent) {}

                override fun onClick(event: MotionEvent) {
                    // onClick
                }

                override fun onLongClick(event: MotionEvent) {
                    // onLongClick
                }

                override fun onDoubleTap(event: MotionEvent) {
                    // onDoubleTap
                }

                override fun onDrag(event: MotionEvent, dx: Float, dy: Float) {
                    // onDrag
                }

                override fun onGesture(
                    event: MotionEvent,
                    ds: Float,
                    dr: Float,
                    dx: Float,
                    dy: Float,
                    centerPoint: PointF
                ) {
                    // onGesture
                }
            }
        )
```
## Video
https://user-images.githubusercontent.com/32809761/221399779-aba61c04-3094-4fe9-8ce3-97c87aaeae4b.mp4
