package com.ai.vis.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

object AppAnimations {
    private const val DURATION_SHORT = 200
    private const val DURATION_MEDIUM = 300
    private const val DURATION_LONG = 400
    
    fun fadeInAnimation(duration: Int = DURATION_MEDIUM): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = duration,
                easing = LinearOutSlowInEasing
            )
        )
    }
    
    fun fadeOutAnimation(duration: Int = DURATION_MEDIUM): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    fun scaleInAnimation(duration: Int = DURATION_MEDIUM): EnterTransition {
        return scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun scaleOutAnimation(duration: Int = DURATION_MEDIUM): ExitTransition {
        return scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideInFromBottomAnimation(duration: Int = DURATION_MEDIUM): EnterTransition {
        return slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideOutToBottomAnimation(duration: Int = DURATION_MEDIUM): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideInFromTopAnimation(duration: Int = DURATION_MEDIUM): EnterTransition {
        return slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideOutToTopAnimation(duration: Int = DURATION_MEDIUM): ExitTransition {
        return slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideInFromRightAnimation(duration: Int = DURATION_MEDIUM): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideOutToRightAnimation(duration: Int = DURATION_MEDIUM): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideInFromLeftAnimation(duration: Int = DURATION_MEDIUM): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun slideOutToLeftAnimation(duration: Int = DURATION_MEDIUM): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(
                durationMillis = duration,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(durationMillis = duration)
        )
    }
    
    fun springScaleInAnimation(): EnterTransition {
        return scaleIn(
            initialScale = 0.9f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        ) + fadeIn()
    }
}
