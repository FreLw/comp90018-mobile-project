package com.comp90018.app.features.map.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Background
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.MemberHuntQuizViewModel
import com.comp90018.app.features.map.components.formatDistance
import com.comp90018.app.sensors.location.LocationOutput

/** Member-only team-hunt checkpoint with history questions for each destination. */
@Composable
internal fun MemberHuntQuiz(
    relic: MapRelic,
    locationOutput: LocationOutput,
    quizSessionKey: String,
    onCompleted: ((String?) -> Unit) -> Unit,
    onFinished: () -> Unit,
    onBack: () -> Unit,
) {
    val latestCompletion by rememberUpdatedState(onCompleted)
    val quizViewModel: MemberHuntQuizViewModel = viewModel(
        key = "member-quiz:$quizSessionKey",
        factory = MemberHuntQuizViewModel.factory { complete -> latestCompletion(complete) },
    )
    val state by quizViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(quizViewModel, relic.memberQuizQuestions) {
        quizViewModel.updateQuestions(relic.memberQuizQuestions)
    }
    LaunchedEffect(quizViewModel, locationOutput.distanceToTargetMeters, relic.insideRadiusMeters) {
        quizViewModel.updateLocation(locationOutput.distanceToTargetMeters, relic.insideRadiusMeters)
    }
    LaunchedEffect(state.completed) {
        if (state.completed) onFinished()
    }
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Team hunt checkpoint", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
        Text(relic.name, color = Brand, fontWeight = FontWeight.Medium)
        if (!state.closeEnough) {
            Text("Reach the treasure location to unlock your questions.", color = Muted)
            locationOutput.distanceToTargetMeters?.let { Text("${it.formatDistance()} away", color = Ink) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to map") }
        } else if (state.questions.isEmpty()) {
            Text("No questions are available for this destination yet.", color = Muted)
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to map") }
        } else {
            Text("Question ${state.questionIndex + 1} of ${state.questions.size}", color = Muted)
            Text(requireNotNull(state.currentQuestion).prompt, style = MaterialTheme.typography.titleLarge, color = Ink)
            requireNotNull(state.currentQuestion).options.forEachIndexed { index, option ->
                OutlinedButton(onClick = { quizViewModel.answer(index) }, enabled = state.canAnswer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${'A' + index}. $option",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.incorrect) Text("Not quite — try again.", color = RelicRed)
            if (state.submitting) Text("Saving your task progress...", color = Muted)
            state.completionError?.let { Text(it, color = RelicRed) }
            OutlinedButton(onClick = onBack, enabled = !state.submitting, modifier = Modifier.fillMaxWidth()) { Text("Back to map") }
        }
    }
}
