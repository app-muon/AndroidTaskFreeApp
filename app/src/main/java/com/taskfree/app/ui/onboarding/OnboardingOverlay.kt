package com.taskfree.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.taskfree.app.R

@Composable
fun OnboardingOverlay(tipManager: TipManager) {
    val tip by tipManager.currentTip.collectAsState()
    if (tip == null) return

    // Dismiss on back press
    BackHandler(onBack = tipManager::dismiss)

    val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrimColor)
            .clickable(onClick = tipManager::dismiss) // Dismiss on outside-tap
    ) {
        TipCard(tip = tip!!, onDismiss = tipManager::dismiss)
    }
}

@Composable
private fun TipCard(tip: OnboardingTip, onDismiss: () -> Unit) {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeight = 56.dp + navInset           // real bar height
    val gapAboveBar = 12.dp                          // visual breathing room

    val boxAlignment: Alignment
    val extraOffset: Modifier

    when (tip.anchor) {
        Anchor.AboveBottomBarInCentre -> {
            boxAlignment = Alignment.BottomCenter
            extraOffset = Modifier.padding(bottom = bottomBarHeight + gapAboveBar)
        }

        Anchor.AboveBottomBarOnRight -> {
            boxAlignment = Alignment.BottomEnd
            extraOffset = Modifier.padding(
                bottom = bottomBarHeight + gapAboveBar, end = 16.dp
            )
        }

        is Anchor.ScreenHeightPercent -> {
            boxAlignment = Alignment.TopCenter
            extraOffset = Modifier.offset(
                y = (LocalConfiguration.current.screenHeightDp.dp * tip.anchor.percent)
            )
        }

        Anchor.HandleOfFirstRow -> {
            boxAlignment = Alignment.TopCenter
            extraOffset = Modifier.offset(y = 140.dp)
        }

        is Anchor.Absolute -> {
            boxAlignment = Alignment.TopCenter
            extraOffset = Modifier.offset(y = tip.anchor.y)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(extraOffset), contentAlignment = boxAlignment
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            color = colorResource(R.color.dialog_background_colour)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorResource(R.color.dialog_primary_colour)
                )
                if (tip.body != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tip.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.surface_colour)
                    )
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.dialog_button_background_colour),
                        contentColor = colorResource(R.color.dialog_button_text_colour)
                    )
                ) {
                    Text(stringResource(R.string.got_it_confirmation))
                }
            }
        }
    }
}
