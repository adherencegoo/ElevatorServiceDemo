package com.xdd.elevatorservicedemo.ui.elevator

import android.animation.Animator
import android.animation.ValueAnimator
import android.transition.ChangeBounds
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.transition.doOnEnd
import androidx.lifecycle.Observer
import com.xdd.elevatorservicedemo.databinding.ElevatorShaftBinding
import com.xdd.elevatorservicedemo.model.Elevator
import com.xdd.elevatorservicedemo.model.ElevatorServiceConfig
import com.xdd.elevatorservicedemo.ui.BindingController
import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged
import com.xdd.elevatorservicedemo.utils.applyConstraints
import com.xdd.elevatorservicedemo.utils.suspendGlobalLayout

class ElevatorShaftController(shaftBinding: ElevatorShaftBinding) :
    BindingController<ElevatorShaftBinding, ConstraintLayout>(shaftBinding) {

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

    private var activeMoveAnimation: MoveAnimation? = null

    private inner class MoveAnimation(val movement: Elevator.Movement) : ChangeBounds() {

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
                    binding.elevator!!.setRealFloorId(it.animatedValue as Int)
                }
            }

            // Setup self (Transition)
            interpolator = animationInterpolator
            duration = myDuration
            addListener(LiveTransitionListener(animator))
            doOnEnd {
                activeMoveAnimation = null
            }

            // Actually change location of ElevatorRoom
            val targetFloorIndex = serviceConfig.floorIdToIndex(targetFloorId)
            typedRoot.applyConstraints {
                it.connectVertical(
                    elevatorRoomController.typedRoot,
                    floorBottomGuidelines[targetFloorIndex + 1],
                    floorBottomGuidelines[targetFloorIndex]
                )
            }

            TransitionManager.beginDelayedTransition(typedRoot, this)
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

    private val elevatorRoomController = ElevatorRoomController(binding.elevatorRoomBinding)
    // key: floor index
    private lateinit var floorBottomGuidelines: List<Guideline>
    private lateinit var serviceConfig: ElevatorServiceConfig

    init {
        // When movement changed, cancel old transition and start a new one
        binding.elevator!!.liveMovement.observe(
            binding.lifecycleOwner!!,
            Observer { nullableMovement ->
                nullableMovement?.takeIf {
                    // Needs animation
                    it.fromFloor != it.toFloor
                            // Need this condition because a new movement may be created with same `toFloor` but different `futureDirection`
                            // --> Need not start a new animation
                            && activeMoveAnimation?.movement?.toFloor != it.toFloor
                }?.let {
                    activeMoveAnimation = MoveAnimation(it)
                }
            })
    }

    fun setConfig(config: ElevatorServiceConfig) {
        serviceConfig = config
        initElevatorShaftGuidelines(config.floorCount)
    }

    private fun initElevatorShaftGuidelines(floorCount: Int) {
        val context = typedRoot.context

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

            typedRoot.addView(guideline, layoutParams)

            guideline
        }
    }
}