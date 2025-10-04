package com.example.aidkriyachallenge.view.uicomponents

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.example.aidkriyachallenge.R
import com.example.aidkriyachallenge.common.WANDERER_PATH
import com.example.aidkriyachallenge.googleauthentication.GoogleAuthClient
import com.example.aidkriyachallenge.ui.theme.fredoka
import com.example.aidkriyachallenge.ui.theme.odin
import com.example.aidkriyachallenge.viewmodel.AuthEvent
import com.example.aidkriyachallenge.viewmodel.LoginUiState
import com.example.aidkriyachallenge.viewmodel.MyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("UseOfNonLambdaOffsetOverload")
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun SignUpContent(
    state: LoginUiState,
    onSignUp:() -> Unit,
    onLogin: () -> Unit,
    email: String,
    onEmailChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    heightSize: WindowHeightSizeClass,
    widthSize: WindowWidthSizeClass,
    viewModel: MyViewModel,
    googleAuthClient: GoogleAuthClient,
    isDark: Boolean,
    role: Boolean,
    onRoleChanged:(Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTapped by remember { mutableStateOf(false) }
    val haptic = LocalView.current

    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragged by remember { mutableStateOf(false) }
    var loginWidth = 0.dp

    var isError by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("Walker") }

    // Animated drag offset with bounce-back
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (!isDragging && !isDragged) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = "dragAnimation"
    )

    // Text fade animation based on drag position relative to text
    val textAlpha by animateFloatAsState(
        targetValue = run {
            val containerWidthPx = with(LocalDensity.current) { loginWidth.toPx() }
            val buttonSizePx = with(LocalDensity.current) { 50.dp.toPx() }
            val textCenterPx = containerWidthPx / 2f
            val iconCenterPx = animatedDragOffset + (buttonSizePx / 2f)

            // Calculate distance from icon center to text center
            val distanceToText = abs(iconCenterPx - textCenterPx)
            val fadeThreshold = buttonSizePx * 0.7f // Start fading when icon is 1.5x button size away from text

            when {
                // If icon hasn't reached the fade threshold, keep text fully visible
                distanceToText > fadeThreshold -> 0.3f
                // If icon is approaching or crossing text, fade based on proximity
                iconCenterPx < textCenterPx -> {
                    // Icon approaching from left - fade out as it gets closer
                    (distanceToText / fadeThreshold).coerceIn(0.3f, 1f)
                }
                else -> {
                    // Icon has crossed to the right - keep text invisible
                    1f
                }
            }
        },
        animationSpec = tween(durationMillis = 150),
        label = "textFadeAnimation"
    )

    val passwordLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.password_visible)
    )
    val passwordLottieProgress by animateLottieCompositionAsState(
        composition = passwordLottieComposition,
        isPlaying = isPasswordVisible,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )
    val loadingLottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading_icon)
    )
    val loadingProgress by animateLottieCompositionAsState(
        composition = loadingLottieComposition,
        isPlaying = isDragged,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )
    val infiniteTransition = rememberInfiniteTransition(label = "boxAnimation")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = if(!isTapped)0f else 5f,
        targetValue = if(!isTapped)10f else 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boxSlideAnimation"
    )

    LaunchedEffect(state.isLoading, state.error) {
        when {
            state.isLoading -> {
                isDragged = true

            }
            state.error != null -> {
                isError = true
                dragOffset = 0f
                delay(1500)
                isDragged = false
                isError = false
                state.error = null
            }
        }
    }

    state.error?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, "SingUp Failed", Toast.LENGTH_LONG).show()
        }
    }
    when{
        ( (widthSize== WindowWidthSizeClass.Expanded || widthSize == WindowWidthSizeClass.Medium) && (heightSize == WindowHeightSizeClass.Expanded || heightSize == WindowHeightSizeClass.Medium)) -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical =50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text("Email", color = MaterialTheme.colorScheme.primary.copy(0.7f), fontFamily = odin, fontWeight = FontWeight.Bold) },
                    placeholder = { Text(text="Enter your Email",color = MaterialTheme.colorScheme.primary.copy(0.4f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(50),
                    singleLine =  true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 10.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password", color = MaterialTheme.colorScheme.primary.copy(0.7f), fontFamily = odin, fontWeight = FontWeight.Bold) },
                    placeholder = { Text(text="Enter your Password",color = MaterialTheme.colorScheme.primary.copy(0.4f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if(isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(50),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 10.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            isPasswordVisible = !isPasswordVisible
                            haptic.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        },modifier = Modifier.padding(end = 20.dp)) {
                            if(isPasswordVisible){
                                LottieAnimation(
                                    composition = passwordLottieComposition,
                                    progress = passwordLottieProgress ,
                                    modifier = Modifier.size(50.dp),
                                    dynamicProperties = rememberLottieDynamicProperties(
                                        rememberLottieDynamicProperty(
                                            property = LottieProperty.COLOR,
                                            value = MaterialTheme.colorScheme.primary.toArgb(),
                                            keyPath = arrayOf("**")
                                        )
                                    )
                                )
                            }else{
                                Icon(
                                    painter = painterResource(R.drawable.hidden),
                                    contentDescription = "Password not visible",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                )

                OutlinedTextField(
                    value = if(role) "Wanderer" else "Walker",
                    onValueChange = {},
                    label = { Text("Role") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown, contentDescription = null,
                            Modifier.clickable { expanded = !expanded })
                    },
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                        .padding(horizontal = 10.dp),
                    shape = RoundedCornerShape(50)
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Walker") },
                        onClick = {
                            selectedRole = "Walker"
                            onRoleChanged(false)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Wanderer") },
                        onClick = {
                            selectedRole = "Wanderer"
                            onRoleChanged(true)
                            expanded = false
                        }
                    )
                }

                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)),
                        thickness = 5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(0.5f)
                    )
                    Text(
                        "or",
                        modifier = Modifier.padding(horizontal = 30.dp),
                        fontFamily = fredoka,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 28.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)),
                        thickness = 5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(0.3f)
                    )
                }
                OutlinedIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        scope.launch {
                            Log.d("GoogleAuthUI", "Sign-in button clicked")
                            val idToken = googleAuthClient.getGoogleIdToken()
                            if (idToken != null) {
                                Log.d("GoogleAuthUI", "Got ID Token: $idToken")
                                viewModel.onEvent(AuthEvent.Google(idToken,if(selectedRole == WANDERER_PATH) true else false))
                            } else {
                                Log.e("GoogleAuthUI", "Google Sign-in returned null token")
                                Toast.makeText(context, "Google Sign-in failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.google),
                        contentDescription = "GoogleSignIn",
                        modifier = Modifier.size(50.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Already have an account",
                        maxLines = 1,
                        autoSize =TextAutoSize.StepBased(
                            maxFontSize = 34.sp
                        ),
                        style = TextStyle(
                            fontFamily = odin,
                            color = MaterialTheme.colorScheme.primary.copy(0.7f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    BasicText(
                        "LOGIN HERE",
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(
                            maxFontSize = 28.sp
                        ),
                        style = TextStyle(
                            fontFamily = odin,
                            color = MaterialTheme.colorScheme.secondary.copy(0.7f)
                        ),
                        modifier = Modifier
                            .clickable {
                            haptic.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onLogin()
                        }
                            .weight(0.5f)
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val containerWidth = maxWidth
                    loginWidth = containerWidth
                    val buttonSize = animateDpAsState(
                        targetValue = if(isDragging || isTapped)100.dp else 70.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessVeryLow
                        )
                    )
                    val maxDragDistance = containerWidth - buttonSize.value - 10.dp // Account for padding

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .shadow(6.dp, shape = RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(
                                brush = if(isError){
                                    Brush.horizontalGradient(listOf(
                                        MaterialTheme.colorScheme.error.copy(0.7f),
                                        MaterialTheme.colorScheme.errorContainer
                                    ))
                                }else{
                                    Brush.horizontalGradient(listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    ))
                                }
                            )
                    ) {
                        // Draggable button
                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .offset(x = if (!isDragging && !isDragged) animatedOffset.dp else 0.dp)
                                .offset { IntOffset(animatedDragOffset.roundToInt(), 0) }
                                .shadow(if (!isDragged) 15.dp else 0.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (!isDragged) {
                                        if(!isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer                                    }
                                    else Color.Transparent
                                )
                                .size(buttonSize.value)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isTapped = true
                                            haptic.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            tryAwaitRelease()
                                            isTapped = false
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    val maxWidthPx = maxDragDistance.toPx()
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                            isTapped = false
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            // Check if dragged far enough to trigger the "done" state
                                            if (dragOffset > maxWidthPx * 0.8f) {
                                                onSignUp()
                                                if(isDragged) dragOffset = maxWidthPx // Snap to end
                                            } else {
                                                // Trigger bounce-back animation by setting dragOffset to 0
                                                dragOffset = 0f
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            dragOffset = 0f
                                        }
                                    ) { _, dragDelta ->
                                        val newOffset = dragOffset + dragDelta
                                        dragOffset = newOffset.coerceIn(0f, maxWidthPx)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                                if (isDragged) {
                                    LottieAnimation(
                                        composition = loadingLottieComposition,
                                        progress =  loadingProgress ,
                                        modifier = Modifier.size(50.dp),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.next),
                                        contentDescription = "Arrow Icon",
                                        modifier = Modifier.size(25.dp),
                                        tint = if(!isDark) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondary
                                    )
                                }

                        }
                        // Login text with fade animation
                        if(isDragged){
                            BasicText(
                                text = if(isError) "SIGNUP FAILED" else "ADDING YOU IN",
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    maxFontSize = 34.sp
                                ),
                                style = TextStyle(
                                    fontFamily = fredoka,
                                    color = if(isError)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 60.dp)
                            )
                        }else{
                            BasicText(
                                "CREATE ACCOUNT",
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    maxFontSize = 34.sp
                                ),
                                style = TextStyle(
                                    fontFamily = fredoka,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha),
                                ),
                                modifier = Modifier.align(Alignment.Center).padding(horizontal = 85.dp)
                            )
                        }
                    }
                }
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical =10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text("Email", color = MaterialTheme.colorScheme.primary.copy(0.7f), fontFamily = odin, fontWeight = FontWeight.Bold) },
                    placeholder = { Text(text="Enter your Email",color = MaterialTheme.colorScheme.primary.copy(0.4f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(50),
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    value = password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password", color = MaterialTheme.colorScheme.primary.copy(0.7f), fontFamily = odin, fontWeight = FontWeight.Bold) },
                    placeholder = { Text(text="Enter your Password",color = MaterialTheme.colorScheme.primary.copy(0.4f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if(isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(50),
                    trailingIcon = {
                        IconButton(onClick = {
                            isPasswordVisible = !isPasswordVisible
                            haptic.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }) {
                            if(isPasswordVisible){
                                LottieAnimation(
                                    composition = passwordLottieComposition,
                                    progress =  passwordLottieProgress ,
                                    modifier = Modifier.size(30.dp),
                                    dynamicProperties = rememberLottieDynamicProperties(
                                        rememberLottieDynamicProperty(
                                            property = LottieProperty.COLOR,
                                            value = MaterialTheme.colorScheme.primary.toArgb(),
                                            keyPath = arrayOf("**")
                                        )
                                    )
                                )
                            }else{
                                Icon(
                                    painter = painterResource(R.drawable.hidden),
                                    contentDescription = "Password not visible",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = if(role) "Wanderer" else "Walker",
                    onValueChange = {},
                    label = { Text("Role") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown, contentDescription = null,
                            Modifier.clickable { expanded = !expanded })
                    },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    shape = RoundedCornerShape(50)
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Walker") },
                        onClick = {
                            selectedRole = "Walker"
                            onRoleChanged(false)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Wanderer") },
                        onClick = {
                            selectedRole = "Wanderer"
                            onRoleChanged(true)
                            expanded = false
                        }
                    )
                }
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(0.5f)
                    )
                    Text(
                        "or",
                        modifier = Modifier.padding(horizontal = 30.dp),
                        fontFamily = fredoka,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(0.3f)
                    )
                }
                OutlinedIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        scope.launch {
                            Log.d("GoogleAuthUI", "Sign-in button clicked")
                            val idToken = googleAuthClient.getGoogleIdToken()
                            if (idToken != null) {
                                Log.d("GoogleAuthUI", "Got ID Token: $idToken")
                                viewModel.onEvent(AuthEvent.Google(idToken,if(selectedRole == WANDERER_PATH) true else false))
                            } else {
                                Log.e("GoogleAuthUI", "Google Sign-in returned null token")
                                Toast.makeText(context, "Google Sign-in failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Image(
                        painter = painterResource(R.drawable.google),
                        contentDescription = "GoogleSignIn",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Already have an account",
                        fontFamily = odin,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(0.7f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "LOGIN HERE",
                        fontFamily = odin,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(0.7f),
                        modifier = Modifier.clickable {
                            haptic.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onLogin()
                        }
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val containerWidth = maxWidth
                    loginWidth = containerWidth
                    val buttonSize = animateDpAsState(
                        targetValue = if(isDragging || isTapped)70.dp else 50.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessVeryLow
                        )
                    )
                    val maxDragDistance = containerWidth - buttonSize.value - 10.dp // Account for padding

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(6.dp, shape = RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(
                                brush = if(isError){
                                    Brush.horizontalGradient(listOf(
                                        MaterialTheme.colorScheme.error.copy(0.7f),
                                        MaterialTheme.colorScheme.errorContainer
                                    ))
                                }else{
                                    Brush.horizontalGradient(listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    ))
                                }
                            )
                    ) {
                        // Draggable button
                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .offset(x = if (!isDragging && !isDragged) animatedOffset.dp else 0.dp)
                                .offset { IntOffset(animatedDragOffset.roundToInt(), 0) }
                                .shadow(if (!isDragged) 15.dp else 0.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (!isDragged){
                                        if(!isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer
                                    }
                                    else Color.Transparent
                                )
                                .size(buttonSize.value)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isTapped = true
                                            haptic.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            tryAwaitRelease()
                                            isTapped = false
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    val maxWidthPx = maxDragDistance.toPx()
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                            isTapped = false
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            // Check if dragged far enough to trigger the "done" state
                                            if (dragOffset > maxWidthPx * 0.8f) {
                                                onSignUp()
                                                if(isDragged){dragOffset=maxWidthPx}// Snap to end
                                            } else {
                                                // Trigger bounce-back animation by setting dragOffset to 0
                                                dragOffset = 0f
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            dragOffset = 0f
                                        }
                                    ) { _, dragDelta ->
                                        val newOffset = dragOffset + dragDelta
                                        dragOffset = newOffset.coerceIn(0f, maxWidthPx)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if(isDragged) {
                                    LottieAnimation(
                                        composition = loadingLottieComposition,
                                        progress = loadingProgress ,
                                        modifier = Modifier.size(50.dp),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.next),
                                        contentDescription = "Arrow Icon",
                                        modifier = Modifier.size(25.dp),
                                        tint = if(!isDark) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondary
                                    )
                                }

                        }
                        // Login text with fade animation
                        if(isDragged){
                            BasicText(
                                text = if(isError) " SIGNUP FAILED" else "ADDING YOU IN",
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    maxFontSize = 24.sp
                                ),
                                style = TextStyle(
                                    fontFamily = fredoka,
                                    color = if(isError)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 60.dp)
                            )
                        }else{
                            BasicText(
                                "CREATE ACCOUNT",
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    maxFontSize = 24.sp
                                ),
                                style = TextStyle(
                                    fontFamily = fredoka,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha),
                                ),
                                modifier = Modifier.align(Alignment.Center).padding(horizontal = 85.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}