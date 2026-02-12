package com.julian.dixmille.presentation.screen

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.julian.dixmille.domain.model.Player
import com.julian.dixmille.presentation.model.GameEndEvent
import com.julian.dixmille.presentation.navigation.GameEndNavigationEvent
import com.julian.dixmille.presentation.viewmodel.GameEndViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random

/**
 * Game end screen route - winner celebration and final rankings.
 *
 * @param winnerName The name of the winning player
 * @param winnerScore The winning score
 */
@Serializable
data class GameEndRoute(
    val winnerName: String,
    val winnerScore: Int
) : NavKey

/**
 * Game End screen EntryPoint - handles ViewModel injection and state collection.
 */
@Composable
fun GameEndEntryPoint(
    winnerName: String,
    winnerScore: Int,
    viewModel: GameEndViewModel = koinViewModel(),
    backStack: NavBackStack<NavKey>,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is GameEndNavigationEvent.NavigateToGameSetup -> {
                    // Clear back stack and go to setup
                    while (backStack.lastOrNull() != null) {
                        backStack.removeLastOrNull()
                    }
                    backStack += HomeRoute
                    backStack += GameSetupRoute
                }

                is GameEndNavigationEvent.NavigateToHome -> {
                    // Clear back stack and go to home
                    while (backStack.lastOrNull() != null) {
                        backStack.removeLastOrNull()
                    }
                    backStack += HomeRoute
                }
            }
        }
    }

    GameEndContent(
        winnerName = winnerName,
        winnerScore = winnerScore,
        playersByScore = state.playersByScore,
        onEvent = viewModel::onEvent,
    )
}

/**
 * Game End screen Content - pure UI composable.
 */
@Composable
fun GameEndContent(
    winnerName: String,
    winnerScore: Int,
    playersByScore: List<Player>,
    onEvent: (GameEndEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }

    // Start animations on composition
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Confetti layer (background)
        ConfettiAnimation()

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Trophy animation
            AnimatedTrophy(visible = showContent)

            Spacer(modifier = Modifier.height(24.dp))

            // Winner name animation
            AnimatedWinnerName(
                winnerName = winnerName,
                visible = showContent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Winner score
            AnimatedScore(
                score = winnerScore,
                visible = showContent,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Rankings
            FinalRankings(
                playersByScore = playersByScore,
                visible = showContent
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Close button (return to home)
                AnimatedOutlinedButton(
                    onClick = { onEvent(GameEndEvent.ReturnHome) },
                    visible = showContent,
                    modifier = Modifier.weight(1f),
                )

                // New game button
                AnimatedButton(
                    onClick = { onEvent(GameEndEvent.StartNewGame) },
                    visible = showContent,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Animated trophy/crown with scale and rotation effects.
 */
@Composable
private fun AnimatedTrophy(visible: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "trophy_scale"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\uD83D\uDC51",
            style = MaterialTheme.typography.displayLarge,
            fontSize = MaterialTheme.typography.displayLarge.fontSize * 2,
            modifier = Modifier.scale(1f + (scale * 0.1f))
        )
    }
}

/**
 * Animated winner name with scale-in effect.
 */
@Composable
private fun AnimatedWinnerName(
    winnerName: String,
    visible: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "name_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200),
        label = "name_alpha"
    )

    Text(
        text = "$winnerName WINS!",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
    )
}

/**
 * Animated score display.
 */
@Composable
private fun AnimatedScore(
    score: Int,
    visible: Boolean,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 300),
        label = "score_alpha"
    )

    Text(
        text = "$score points",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.alpha(alpha)
    )
}

/**
 * Final rankings display with staggered fade-in.
 */
@Composable
private fun FinalRankings(
    playersByScore: List<Player>,
    visible: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "FINAL STANDINGS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        playersByScore.forEachIndexed { index, player ->
            AnimatedRankingRow(
                rank = index + 1,
                player = player,
                visible = visible,
                delay = 400 + (index * 100)
            )
        }
    }
}

/**
 * Individual ranking row with animation.
 */
@Composable
private fun AnimatedRankingRow(
    rank: Int,
    player: Player,
    visible: Boolean,
    delay: Int
) {
    val offsetX by animateFloatAsState(
        targetValue = if (visible) 0f else -50f,
        animationSpec = tween(400, delayMillis = delay, easing = EaseOutCubic),
        label = "rank_offset_$rank"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = delay),
        label = "rank_alpha_$rank"
    )

    val medal = when (rank) {
        1 -> "\uD83E\uDD47"
        2 -> "\uD83E\uDD48"
        3 -> "\uD83E\uDD49"
        else -> "$rank."
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = medal,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (rank == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (rank == 1)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${player.totalScore}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * Animated new game button.
 */
@Composable
private fun AnimatedButton(
    onClick: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 850, easing = EaseOutBack),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "NEW GAME",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

/**
 * Animated outlined button (for secondary actions).
 */
@Composable
private fun AnimatedOutlinedButton(
    onClick: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 800, easing = EaseOutBack),
        label = "outlined_button_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = "HOME",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

/**
 * Confetti particle animation.
 */
@Composable
private fun ConfettiAnimation() {
    val confettiParticles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                startY = -0.1f - Random.nextFloat() * 0.2f,
                color = listOf(
                    Color(0xFF7F5AF0),  // Purple
                    Color(0xFF2CB67D),  // Green
                    Color(0xFFE8E8F0),  // White
                    Color(0xFF9B7AF0),  // Light purple
                    Color(0xFF4DD69C),  // Light green
                ).random(),
                size = (8 + Random.nextInt(8)).dp,
                speed = 0.3f + Random.nextFloat() * 0.4f,
                swing = Random.nextFloat() * 0.1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    confettiParticles.forEach { particle ->
        val offsetY by infiniteTransition.animateFloat(
            initialValue = particle.startY,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (3000 / particle.speed).toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "confetti_y_${particle.hashCode()}"
        )

        val offsetX by infiniteTransition.animateFloat(
            initialValue = -particle.swing,
            targetValue = particle.swing,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "confetti_x_${particle.hashCode()}"
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(
                        x = (maxWidth * (particle.x + offsetX)),
                        y = (maxHeight * offsetY)
                    )
                    .size(particle.size)
                    .background(particle.color, CircleShape)
            )
        }
    }
}

/**
 * Data class representing a confetti particle.
 */
private data class ConfettiParticle(
    val x: Float,
    val startY: Float,
    val color: Color,
    val size: androidx.compose.ui.unit.Dp,
    val speed: Float,
    val swing: Float
)
