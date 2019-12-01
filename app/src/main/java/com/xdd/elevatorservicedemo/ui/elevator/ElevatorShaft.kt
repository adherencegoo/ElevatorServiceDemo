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

    private fun moveToFloor(targetFloorId: Int): Pair<Animator, Transition>? {
        val fromFloorId = elevator.realFloor.id
        if (targetFloorId == fromFloorId) {
            return null
        }

        val shaftLayout: ConstraintLayout = shaftBinding.elevatorShaft
        val myDuration =
            kotlin.math.abs(targetFloorId - fromFloorId) * serviceConfig.animationDurationPerFloor

        val transition: Transition = ChangeBounds().apply {
            interpolator = animationInterpolator
            duration = myDuration
        }

        // Because we can't get progress update from Transition, create an Animator to simulate progress update
        val animator = ValueAnimator.ofInt(fromFloorId, targetFloorId).apply {
            interpolator = animationInterpolator
            duration = myDuration
            addUpdateListener {
                elevator.setRealFloorId(it.animatedValue as Int)
            }
        }

        // Actually change location of ElevatorRoom
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

        // Start transition animation
        TransitionManager.beginDelayedTransition(shaftLayout, transition)
        animator.start()

        return Pair(animator, transition)
    }
}