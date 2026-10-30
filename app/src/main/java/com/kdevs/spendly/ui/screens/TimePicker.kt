package com.kdevs.spendly.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.kdevs.spendly.R
import com.kdevs.spendly.data.str
import com.kdevs.spendly.data.timeOf
import com.kdevs.spendly.data.todayMillis
import com.kdevs.spendly.data.withMinutes
import com.kdevs.spendly.ui.components.*
import com.kdevs.spendly.ui.state.setPicked
import com.kdevs.spendly.ui.theme.*

/** Full-screen time picker. Sends minutes since midnight back as "pickedTime". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerScreen(
    nav: NavController,
    minutes: Int,
) {
    val state =
        rememberTimePickerState(
            initialHour = (minutes / 60).coerceIn(0, 23),
            initialMinute = (minutes % 60).coerceIn(0, 59),
            is24Hour = false,
        )
    val picked = state.hour * 60 + state.minute
    ScreenScaffold(
        str(R.string.select_time),
        { nav.popBackStack() },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(str(R.string.use_2, timeOf(withMinutes(todayMillis(), picked))), {
                    nav.setPicked("pickedTime", picked)
                    nav.popBackStack()
                })
            }
        },
    ) {
        Card {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = state,
                    colors =
                        TimePickerDefaults.colors(
                            clockDialColor = Bg,
                            selectorColor = Primary,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = Ink,
                            periodSelectorBorderColor = Line,
                            periodSelectorSelectedContainerColor = PrimaryContainer,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = Primary,
                            periodSelectorUnselectedContentColor = Muted,
                            timeSelectorSelectedContainerColor = PrimaryContainer,
                            timeSelectorUnselectedContainerColor = Bg,
                            timeSelectorSelectedContentColor = Primary,
                            timeSelectorUnselectedContentColor = Ink,
                        ),
                )
            }
        }
    }
}
