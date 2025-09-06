package com.example.aidkriyachallenge.view.screens

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.aidkriyachallenge.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("ContextCastToActivity")
@OptIn( ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SplashScreen(
    onFinished: () -> Unit

) {

    var startAnimation by remember { mutableStateOf(false) }

    val activity = LocalContext.current as Activity
    val windowSize = calculateWindowSizeClass(activity)
    val windowWidth = windowSize.widthSizeClass
    val windowHeight = windowSize.heightSizeClass
    val isDark = isSystemInDarkTheme()

    // Animations
    val offsetY = remember { Animatable(-6000f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val cornerRadius = remember { Animatable(20f) } // start rounded square
    val circleOffsetX = remember { Animatable(0f) }
    val circleScale = remember { Animatable(1f) }
    val imageOffsetX = remember { Animatable(-8000f) }
    val imageAlpha = remember { Animatable(1f) }// offscreen

    LaunchedEffect(Unit) {



        startAnimation= true
        // 1. Drop from top
        offsetY.animateTo(0f, tween(500))

        // 2. Rotate + scale
        var rotationJob = launch {
            rotation.animateTo(45f, tween(500))
        }
        var scaleJob = launch {
            scale.animateTo(2f, tween(500))
        }
        rotationJob.join()
        scaleJob.join()

        // 3. Shrink + morph into circle
        scaleJob = launch {scale.animateTo(1f, tween(400))}
        var cornerRadiusJob = launch {cornerRadius.animateTo(100f, tween(400))}
        rotationJob = launch { rotation.animateTo(90f, tween(400)) }
        scaleJob.join()
        cornerRadiusJob.join()
        rotationJob.join()

        // 4. Text Slide In
        val imageJob = launch {
            imageOffsetX.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
        }
        var circlePositionJob = launch {
            circleOffsetX
                .animateTo(when{
                    (windowWidth == WindowWidthSizeClass.Compact || windowWidth == WindowWidthSizeClass.Medium || windowHeight == WindowHeightSizeClass.Compact ) -> 370f
                    else -> 450f
                }, tween(500, easing = FastOutSlowInEasing))
        }
        val circleSizeJob = launch {
            scale.animateTo(0.2f, tween(500, easing = FastOutSlowInEasing))
        }

        imageJob.join()
        circlePositionJob.join()
        circleSizeJob.join()

        delay(200)
        // 5. Circle expands to fill screen
        val imageVisibleJob = launch {
            imageAlpha.animateTo(0f, tween(400))
        }
        var circleExpandJob = launch {
            circleScale.animateTo(200f, tween(500))
        }
        imageVisibleJob.join()
        circleExpandJob.join()

        cornerRadiusJob = launch { cornerRadius.animateTo(20f, tween(500)) }
        circleExpandJob = launch { circleScale
            .animateTo(when{
                (windowWidth == WindowWidthSizeClass.Compact || windowWidth == WindowWidthSizeClass.Medium || windowHeight == WindowHeightSizeClass.Compact) -> 25f
                else -> 40f
            }, tween(500)) }
        rotationJob = launch { rotation.animateTo(45f, tween(500)) }
        var positionYjob = launch { offsetY.animateTo(-1000f, tween(500)) }
        circlePositionJob = launch { circleOffsetX.animateTo(-1000f, tween(500)) }
        circleExpandJob.join()
        cornerRadiusJob.join()
        rotationJob.join()
        positionYjob.join()
        circlePositionJob.join()

        delay(200)

        rotationJob = launch { rotation.animateTo(135f, tween(500)) }
        positionYjob = launch { offsetY.animateTo(1000f, tween(500)) }
        circlePositionJob = launch { circleOffsetX.animateTo(1000f, tween(500)) }
        rotationJob.join()
        positionYjob.join()
        circlePositionJob.join()

        launch { circleScale.animateTo(300f, tween(300)) }
        delay(300)

        startAnimation = false
        onFinished()
        // 7. Navigate )
    }


    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = offsetY.value
                    translationX = circleOffsetX.value
                    scaleX = scale.value * circleScale.value
                    scaleY = scale.value * circleScale.value
                    rotationZ = rotation.value
                }
                .size(120.dp)
                .background(
                    brush = if(!isDark){
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    }else{
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    },
                    shape = RoundedCornerShape(cornerRadius.value.dp)
                )
        )

        Image(
            painter = painterResource(R.drawable.aidkriya_logo),
            contentDescription = "App LOGO",
            modifier = Modifier
                .alpha(imageAlpha.value)
                .graphicsLayer {
                    translationX = imageOffsetX.value
                }
                .size(
                    when{
                        (windowWidth == WindowWidthSizeClass.Compact || windowWidth == WindowWidthSizeClass.Medium || windowHeight == WindowHeightSizeClass.Compact) -> 200.dp
                        else -> 300.dp
                    }
                )
        )
    }
}
