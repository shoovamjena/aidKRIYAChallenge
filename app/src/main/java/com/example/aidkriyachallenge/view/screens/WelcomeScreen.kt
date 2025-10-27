package com.example.aidkriyachallenge.view.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.googleauthentication.GoogleAuthClient
import com.example.aidkriyachallenge.ui.theme.fredoka
import com.example.aidkriyachallenge.view.uicomponents.LoginContent
import com.example.aidkriyachallenge.view.uicomponents.SignUpContent
import com.example.aidkriyachallenge.viewModel.LoginUiState
import com.example.aidkriyachallenge.viewModel.MyViewModel

enum class SelectedScreen{
    Login,
    SignUp
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@SuppressLint("ConfigurationScreenWidthHeight", "ContextCastToActivity")
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun WelcomeScreen(
    viewModel: MyViewModel,
    googleAuthClient: GoogleAuthClient,
    state: LoginUiState,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, Boolean) -> Unit,
    onForgotPassword: (String) -> Unit,
){
    val isDark = isSystemInDarkTheme()
    val activity = LocalContext.current as Activity
    val windowSize = calculateWindowSizeClass(activity)
    val widthSize = windowSize.widthSizeClass
    val heightSize = windowSize.heightSizeClass

    val interactionSource = remember { MutableInteractionSource()}
    val haptics = LocalView.current
    var visible by rememberSaveable { mutableStateOf(false) }

    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var isWanderer by rememberSaveable { mutableStateOf(false) }
    var signUpEmail by rememberSaveable { mutableStateOf("") }
    var signUpPassword by rememberSaveable { mutableStateOf("") }

    var selectedScreen by rememberSaveable { mutableStateOf(SelectedScreen.Login) }



    val sizeAnimation by animateDpAsState(
        targetValue = when {
            ( widthSize == WindowWidthSizeClass.Expanded || heightSize == WindowHeightSizeClass.Expanded) -> if (visible) 100.dp else 250.dp
            else -> if (visible) 200.dp else 350.dp
        },
        animationSpec = tween(durationMillis = 500),
        label = "sizeAnimation"
    )

    val paddingTopAnimation by animateDpAsState(
        targetValue = if (visible) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "paddingAnimation"
    )

    val alignmentBias by animateFloatAsState(
        targetValue = when{
            ((widthSize == WindowWidthSizeClass.Compact || widthSize == WindowWidthSizeClass.Medium) && (heightSize == WindowHeightSizeClass.Expanded || heightSize == WindowHeightSizeClass.Medium)) -> {
                if (visible) -0.9f else 0f
            } else -> {
                if (visible)  -0.6f else 0f
            }
        }, // -1f for Top, 0f for Center
        animationSpec = tween(durationMillis = 500),
        label = "alignmentAnimation"
    )
    // Create the alignment object from the animated bias
    val animatedAlignment = remember(alignmentBias) {
        when{
            ((widthSize == WindowWidthSizeClass.Compact || widthSize == WindowWidthSizeClass.Medium) && (heightSize == WindowHeightSizeClass.Expanded || heightSize == WindowHeightSizeClass.Medium)) -> {
                BiasAlignment(horizontalBias = 0f, verticalBias = alignmentBias)
            }
            else -> {
                BiasAlignment(horizontalBias = alignmentBias, verticalBias = 0f)
            }
        }
    }

    val openIcon by rememberLottieComposition(
        spec = if(isSystemInDarkTheme()) LottieCompositionSpec.RawRes(R.raw.open_icon_dark)
        else LottieCompositionSpec.RawRes(R.raw.open_icon_light)
    )
    val openIconAnimation by animateLottieCompositionAsState(
        composition = openIcon,
        isPlaying = visible,
        iterations = 1,
        speed = 0.85f
    )

    //Handling back gesture for the login box visibility
    BackHandler(enabled = visible) {
        visible = false
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = if (!isDark) {
                        arrayOf(
                            0.15f to MaterialTheme.colorScheme.primary.copy(0.7f),
                            0.3f to MaterialTheme.colorScheme.primary.copy(0.5f),
                            0.5f to MaterialTheme.colorScheme.surfaceBright.copy(0.5f)
                        )
                    } else {
                        arrayOf(
                            0.15f to MaterialTheme.colorScheme.primaryContainer.copy(0.7f),
                            0.3f to MaterialTheme.colorScheme.primaryContainer.copy(0.5f),
                            0.5f to MaterialTheme.colorScheme.surfaceDim.copy(0.5f)
                        )
                    }
                )
            )
    ){

        when{
            (widthSize == WindowWidthSizeClass.Compact || heightSize == WindowHeightSizeClass.Expanded) -> {
                Button(
                    onClick = {
                        visible=true
                        haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .padding(horizontal = 60.dp)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 50.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(50))
                        .align(Alignment.BottomCenter),
                    colors = ButtonDefaults.buttonColors(
                        if(isDark) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.secondary),
                ) {
                    Text(
                        "BEGIN YOUR WALK",
                        fontFamily = fredoka,
                        color = if(isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                    )
                }
            }
            else -> {
                AnimatedVisibility(
                    visible = !visible,
                    enter = slideInHorizontally(
                        animationSpec = tween(500, easing = LinearOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -fullWidth }
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = tween(500, easing = LinearOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -fullWidth }
                    ),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Button(
                        onClick = {
                            visible=true
                            haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .padding(horizontal = 120.dp)
                            .fillMaxWidth()
                            .shadow(4.dp, shape = RoundedCornerShape(50)),
                        colors = ButtonDefaults.buttonColors(
                            if(isDark) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.secondary
                        ),
                    ) {
                        Text(
                            "BEGIN YOUR WALK",
                            fontFamily = fredoka,
                            color = if(isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                        )
                    }
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.aidkriya_logo),
            contentDescription = "aidkriya logo",
            modifier = Modifier
                .size(sizeAnimation) // Use animated size
                .align(animatedAlignment) // Use animated alignment
                .padding(top = paddingTopAnimation) // Use animated padding
        )
        when{
            (widthSize == WindowWidthSizeClass.Compact || heightSize == WindowHeightSizeClass.Expanded) -> {
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    visible = visible,
                    enter = slideInVertically(
                        animationSpec = tween(500, easing = LinearOutSlowInEasing),
                        initialOffsetY = {fullHeight -> fullHeight  }
                    ),
                    exit = slideOutVertically(
                        animationSpec = tween(500, easing = LinearOutSlowInEasing),
                        targetOffsetY = { fullHeight -> fullHeight }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((0.8f * LocalConfiguration.current.screenHeightDp).dp)
                            .shadow(
                                10.dp,
                                shape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp)
                            )
                            .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ){
                        // Encapsulate toggle and content in a Column
                        LazyColumn (
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                LottieAnimation(
                                    composition = openIcon,
                                    progress = openIconAnimation,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .alpha(0.7f)
                                        .clickable(
                                            interactionSource, null,
                                            onClick = {
                                                visible = false
                                                haptics.performHapticFeedback(
                                                    HapticFeedbackConstants.CONTEXT_CLICK
                                                )
                                            }
                                        )
                                        .rotate(180f),
                                )
                            }
                            item {
                                LoginSignUpToggle(
                                    selectedScreen = selectedScreen,
                                    onScreenSelected = { newScreen -> selectedScreen = newScreen
                                        haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)}
                                )
                            }
                            item {
                                when (selectedScreen) {
                                    SelectedScreen.Login -> LoginContent(
                                        state = state,
                                        onLogin = { onLogin(loginEmail.trim(),loginPassword.trim()) },
                                        email = loginEmail,
                                        onEmailChanged = { loginEmail = it},
                                        password = loginPassword,
                                        onPasswordChanged = { loginPassword = it},
                                        heightSize = heightSize,
                                        widthSize = widthSize,
                                        viewModel = viewModel,
                                        googleAuthClient = googleAuthClient,
                                        isDark = isDark,
                                        onForgotPassword = onForgotPassword
                                    )
                                    SelectedScreen.SignUp -> SignUpContent(
                                        onLogin = {selectedScreen = SelectedScreen.Login},
                                        email = signUpEmail,
                                        onEmailChanged = { signUpEmail = it},
                                        password = signUpPassword,
                                        onPasswordChanged = { signUpPassword = it},
                                        heightSize = heightSize,
                                        widthSize = widthSize,
                                        onSignUp = { onSignUp(signUpEmail.trim(),signUpPassword.trim(),isWanderer) },
                                        state = state,
                                        viewModel = viewModel,
                                        googleAuthClient = googleAuthClient,
                                        isDark = isDark,
                                        role = isWanderer,
                                        onRoleChanged = {role->
                                            isWanderer = role}
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    visible = visible,
                    enter = slideInHorizontally(
                        animationSpec = tween(500, easing = LinearOutSlowInEasing),
                        initialOffsetX = {fullHeight -> fullHeight  }
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = tween(500, easing = LinearOutSlowInEasing),
                        targetOffsetX = { fullHeight -> fullHeight }
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .padding(start = (.5f * LocalConfiguration.current.screenWidthDp).dp)
                            .shadow(
                                10.dp,
                                shape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp)
                            )
                            .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ){
                        // Encapsulate toggle and content in a Column
                        Row(
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            LottieAnimation(
                                composition = openIcon,
                                progress = openIconAnimation,
                                modifier = Modifier
                                    .size(36.dp)
                                    .alpha(0.7f)
                                    .clickable(
                                        interactionSource, null,
                                        onClick = {
                                            visible = false
                                            haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                        }
                                    )
                                    .align(Alignment.CenterVertically)
                                    .rotate(90f),
                            )
                            LazyColumn  {
                                item {
                                    LoginSignUpToggle(
                                        selectedScreen = selectedScreen,
                                        onScreenSelected = { newScreen -> selectedScreen = newScreen
                                            haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)}
                                    )
                                }
                                item {
                                    when (selectedScreen) {
                                        SelectedScreen.Login -> LoginContent(
                                            state = state,
                                            onLogin = { onLogin(loginEmail.trim(),loginPassword.trim())},
                                            email = loginEmail,
                                            onEmailChanged = { loginEmail = it},
                                            password = loginPassword,
                                            onPasswordChanged = { loginPassword = it},
                                            heightSize = heightSize,
                                            widthSize = widthSize,
                                            viewModel = viewModel,
                                            googleAuthClient = googleAuthClient,
                                            isDark = isDark,
                                            onForgotPassword = onForgotPassword
                                        )
                                        SelectedScreen.SignUp -> SignUpContent(
                                            onLogin = {selectedScreen = SelectedScreen.Login},
                                            email = signUpEmail,
                                            onEmailChanged = { signUpEmail = it},
                                            password = signUpPassword,
                                            onPasswordChanged = { signUpPassword = it},
                                            heightSize = heightSize,
                                            widthSize = widthSize,
                                            onSignUp = { onSignUp(signUpEmail.trim(),signUpPassword.trim(),isWanderer) },
                                            state = state,
                                            viewModel = viewModel,
                                            googleAuthClient = googleAuthClient,
                                            isDark = isDark,
                                            role = isWanderer,
                                            onRoleChanged = {role->
                                                isWanderer = role}
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@SuppressLint("UseOfNonLambdaOffsetOverload", "ContextCastToActivity")
@Composable
fun LoginSignUpToggle(
    selectedScreen: SelectedScreen,
    onScreenSelected: (SelectedScreen) -> Unit
) {
    val activity = LocalContext.current as Activity
    val windowSize = calculateWindowSizeClass(activity)
    val widthSize = windowSize.widthSizeClass
    val heightSize = windowSize.heightSizeClass
    val haptics = LocalView.current

    // SDK / Theme checks
    val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val largeScreen = (widthSize == WindowWidthSizeClass.Expanded || widthSize == WindowWidthSizeClass.Medium) && (heightSize == WindowHeightSizeClass.Expanded || heightSize == WindowHeightSizeClass.Medium)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(top = 40.dp)
            .height(if (largeScreen) 80.dp else 50.dp)
            .shadow(5.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(
                if (!isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(
                    0.5f
                )
            )
    ) {

        val maxWidth = this.maxWidth
        val xOffset by animateDpAsState(
            targetValue = if (selectedScreen == SelectedScreen.Login) 0.dp else maxWidth / 2,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            label = "indicatorOffset"
        )

        // Animated text sizes
        val textSizeLogin by animateIntAsState(
            targetValue = if (selectedScreen == SelectedScreen.Login) {
                if (largeScreen) 30 else 20
            } else {
                if (largeScreen) 24 else 14
            },
            animationSpec = tween(300, easing = EaseInOut),
            label = "loginTextSize"
        )
        val textSizeSign by animateIntAsState(
            targetValue = if (selectedScreen == SelectedScreen.SignUp) {
                if (largeScreen) 30 else 20
            } else {
                if (largeScreen) 24 else 14
            },
            animationSpec = tween(300, easing = EaseInOut),
            label = "signupTextSize"
        )

        // The draggable overlay
        Box(
            modifier = Modifier
                .width(maxWidth / 2)
                .fillMaxHeight()
                .padding(5.dp)
                .offset(x = xOffset)
                .clip(RoundedCornerShape(50))
                .then(
                    if (isAndroid12OrAbove) {
                        Modifier
                            .blur(5.dp)
                            .border(
                                width = 5.dp,
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, Color.White)
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .shadow(15.dp, shape = RoundedCornerShape(50))
                            .background(
                                if (!isDark) Color.White.copy(0.7f)
                                else Color.White.copy(0.3f)
                            )
                    }
                )

        )

        // Clickable texts
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource, null) {
                        onScreenSelected(SelectedScreen.Login)
                        haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOGIN",
                    fontSize = textSizeLogin.sp,
                    color = if(!isDark){
                        if (selectedScreen == SelectedScreen.Login)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(0.5f)
                    }else{
                        if (selectedScreen == SelectedScreen.Login)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = fredoka
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource, null) {
                        onScreenSelected(SelectedScreen.SignUp)
                        haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SIGNUP",
                    fontSize = textSizeSign.sp,
                    color = if(!isDark){
                        if (selectedScreen == SelectedScreen.Login)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(0.5f)
                    }else{
                        if (selectedScreen == SelectedScreen.Login)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = fredoka
                )
            }
        }
    }
}