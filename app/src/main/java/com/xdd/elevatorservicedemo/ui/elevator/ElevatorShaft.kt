package com.xdd.elevatorservicedemo.ui.elevator

import android.animation.Animator
import android.animation.ValueAnimator
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import com.xdd.elevatorservicedemo.addDisposableOnGlobalLayoutListener
import com.xdd.elevatorservicedemo.databinding.ElevatorShaftBinding
import com.xdd.elevatorservicedemo.model.Elevator
import com.xdd.elevatorservicedemo.model.ElevatorService

class ElevatorShaft(private val shaftBinding: ElevatorShaftBinding) {
    companion object {
        private val animationInterpolator = DecelerateInterpolator()

        private fun ConstraintSet.connectVertical(
            view: View,
            topGuideline: Guideline,
            bottomGuideline: Guideline
        ) {
            connect(
                view.id,
                ConstraintSet.BOTTOM,
                bottomGuideline.id,
                ConstraintSet.TOP
            )
            connect(
                view.id,
                ConstraintSet.TOP,
                topGuideline.id,
                ConstraintSet.BOTTOM
            )
        }
    }

    private inner class MoveAnimation(movement: Elevator.Movement) : ChangeBounds() {

        init {
            val fromFloorId = movement.fromFloor.id
            val targetFloorId = movement.toFloor.id
            assert(targetFloorId != fromFloorId)

            val myDuration =
                kotlin.math.abs(targetFloorId - fromFloorId) * serviceConfig.animationDurationPerFloor

            // Setup Animator
            // Because we can't get progress update from Transition, create an Animator to simulate progress update
            val animator = ValueAnimator.ofInt(fromFloorId, targetFloorId).apply {
                interpolator = animationInterpolator
                duration = myDuration
                addUpdateListener {
                    elevator.setRealFloorId(it.animatedValue as Int)
                }
            }

            // Setup self (Transition)
            interpolator = animationInterpolator
            duration = myDuration
            addListener(LiveTransitionListener(animator))

            // Actually change location of ElevatorRoom
            val shaftLayout = shaftBinding.elevatorShaft
            val targetFloorIndex = serviceConfig.floorIdToIndex(targetFloorId)
            ConstraintSet().apply {
                clone(shaftLayout)
                connectVertical(
                    shaftBinding.elevatorRoomBinding.elevatorRoom,
                    floorBottomGuidelines[targetFloorIndex + 1],
                    floorBottomGuidelines[targetFloorIndex]
                )
                applyTo(shaftLayout)
            }

            TransitionManager.beginDelayedTransition(shaftLayout, this)
        }
    }

    private class LiveTransitionListener(private val boundAnimator: Animator) :
        Transition.TransitionListener {

        override fun onTransitionEnd(p0: Transition?) = boundAnimator.end()

        override fun onTransitionResume(p0: Transition?) = boundAnimator.resume()

        override fun onTransitionPause(p0: Transition?) = boundAnimator.pause()

        override fun onTransitionCancel(p0: Transition?) = boundAnimator.cancel()

        override fun onTransitionStart(p0: Transition?) = boundAnimator.start()
    }

    // key: floor index
    private lateinit var floorBottomGuidelines: List<Guideline>
    private lateinit var serviceConfig: ElevatorService.Config
    private lateinit var elevator: Elevator

    fun init(
        viewModel: ElevatorViewModel,
        elevatorIndex: Int,
        height: Int
    ) {
        serviceConfig = viewModel.elevatorService.config
        elevator = viewModel.elevatorService.elevators[elevatorIndex]

        shaftBinding.elevatorShaftBg.apply {
            // when height of elevatorShaft is updated, setup guidelines in elevatorShaft
            addDisposableOnGlobalLayoutListener {
                initElevatorShaftGuidelines()
            }

            val params = layoutParams
            params.height = height
            layoutParams = params
        }

        // When movement changed, cancel old transition and start a new one
        elevator.liveMovement.observeForever { nullableMovement ->
            nullableMovement?.takeIf {
                it.fromFloor != it.toFloor
            }?.let {
                TransitionManager.endTransitions(shaftBinding.elevatorShaft)
                MoveAnimation(it)
            }
        }
    }

    private fun initElevatorShaftGuidelines() {
        val parentLayout = shaftBinding.elevatorShaft
        val context = parentLayout.context

        val floorCount = serviceConfig.floorCount
        val eachPercentage = 1f / (floorCount)

        floorBottomGuidelines = List(floorCount + 1) {
            val guideline = Guideline(context).apply {
                id = View.generateViewId()
            }

            val layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                orientation = ConstraintLayout.LayoutParams.HORIZONTAL
                guidePercent = 1 - it * eachPercentage
            }

            parentLayout.addView(guideline, layoutParams)

            guideline
        }
    }
}