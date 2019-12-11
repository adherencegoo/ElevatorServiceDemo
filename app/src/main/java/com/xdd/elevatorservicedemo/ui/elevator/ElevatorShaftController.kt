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
import androidx.lifecycle.Observer
import com.xdd.elevatorservicedemo.BR
import com.xdd.elevatorservicedemo.utils.addDisposableOnGlobalLayoutListener
import com.xdd.elevatorservicedemo.databinding.ElevatorShaftBinding
import com.xdd.elevatorservicedemo.model.Elevator
import com.xdd.elevatorservicedemo.model.ElevatorServiceConfig
import com.xdd.elevatorservicedemo.ui.BindingController
import com.xdd.elevatorservicedemo.utils.addOnPropertyChanged

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
                    binding.elevator!!.setRealFloorId(it.animatedValue as Int)
                }
            }

            // Setup self (Transition)
            interpolator = animationInterpolator
            duration = myDuration
            addListener(LiveTransitionListener(animator))

            // Actually change location of ElevatorRoom
            val targetFloorIndex = serviceConfig.floorIdToIndex(targetFloorId)
            ConstraintSet().apply {
                clone(typedRoot)
                connectVertical(
                    elevatorRoomController.typedRoot,
                    floorBottomGuidelines[targetFloorIndex + 1],
                    floorBottomGuidelines[targetFloorIndex]
                )
                applyTo(typedRoot)
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
        binding.addOnPropertyChanged { localBinding, propertyId ->
            if (propertyId == BR.elevator) {
                val localElevator = localBinding.elevator!!

                // When movement changed, cancel old transition and start a new one
                localElevator.liveMovement.observe(
                    localBinding.lifecycleOwner!!,
                    Observer { nullableMovement ->
                        nullableMovement?.takeIf {
                            it.fromFloor != it.toFloor
                        }?.let {
                            TransitionManager.endTransitions(typedRoot)
                            MoveAnimation(it)
                        }
                    })
            }
        }
    }

    fun setConfig(config: ElevatorServiceConfig) {
        serviceConfig = config
    }

    fun setTotalHeight(shaftHeight: Int, shaftTopMargin: Int, onUpdateFinished: (() -> Unit)?) {
        binding.elevatorShaftBg.apply {
            // when height of elevatorShaft is updated, setup guidelines in elevatorShaft
            addDisposableOnGlobalLayoutListener {
                binding.root.apply {
                    addDisposableOnGlobalLayoutListener {
                        initElevatorShaftGuidelines()
                        onUpdateFinished?.invoke()
                    }

                    /*
                     * *** Workaround ***
                     * ElevatorShaft (ConstraintLayout) itself is encapsulated in a scrollableElevatorShaft (NestedScrollView)
                     *
                     * if scrollableElevatorShaft's height is
                     *  (1) wrap_content: when ElevatorShaft is very tall, scrollableElevatorShaft will overlap views above it (top constraint is ignored)
                     *  (2) match_constraint: when ElevatorShaft is very short, scrollableElevatorShaft can occupy correct space, but its content (ElevatorShaft) will align top
                     *
                     * Workaround:
                     *  Adopt (2), and add topMargin to push ElevatorShaft to the bottom
                     * */
                    val params = layoutParams as ViewGroup.MarginLayoutParams
                    params.topMargin = shaftTopMargin
                    layoutParams = params
                }
            }

            val params = layoutParams
            params.height = shaftHeight
            layoutParams = params
        }
    }

    private fun initElevatorShaftGuidelines() {
        val context = typedRoot.context

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

            typedRoot.addView(guideline, layoutParams)

            guideline
        }
    }
}