package com.example.notepad.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notepad.calendar.LunarCalendarFacade
import java.util.Date

@Composable
fun CalendarScreen(calendarFacade: LunarCalendarFacade) {
    val today = remember { Date() }
    val calendarInfo = remember(today) { calendarFacade.getCalendarInfo(today) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Lunar Calendar", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Today: ${calendarInfo.lunarDate.getFormattedString()}")
        if (calendarInfo.solarTerm != null) {
            Text(text = "Solar Term: ${calendarInfo.solarTerm}")
        }
        if (calendarInfo.festival != null) {
            Text(text = "Festival: ${calendarInfo.festival}")
        }
    }
}
