package com.wheresmybus.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.data.JourneyHistoryEntry
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.ChipMint
import com.wheresmybus.app.ui.theme.HeaderGreen
import com.wheresmybus.app.util.formatRelativeTimeAgo

@Composable
fun HistoryScreen(
    viewModel: BusViewModel,
    language: AppLanguage,
    onEntryClick: (fromName: String, toName: String) -> Unit,
) {
    val strings = stringsFor(language)
    val history by viewModel.journeyHistory.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BgCream)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGreen)
                .padding(16.dp),
        ) {
            Text(strings.historyTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.historyEmpty, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(history, key = { it.searchedAtEpochMillis }) { entry ->
                    HistoryRow(entry) { onEntryClick(entry.fromStopName, entry.toStopName) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: JourneyHistoryEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(ChipMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = HeaderGreen, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.fromStopName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp).padding(horizontal = 4.dp),
                    )
                    Text(entry.toStopName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(formatRelativeTimeAgo(entry.searchedAtEpochMillis), color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
