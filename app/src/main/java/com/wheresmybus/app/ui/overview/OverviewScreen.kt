package com.wheresmybus.app.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.news.AnnouncementCard
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.HeaderGreen

@Composable
fun OverviewScreen(viewModel: BusViewModel, language: AppLanguage) {
    val strings = stringsFor(language)
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val districtCount = remember(stops) { stops.map { it.district }.filter { it.isNotBlank() }.distinct().size }

    Column(modifier = Modifier.fillMaxSize().background(BgCream)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGreen)
                .padding(16.dp),
        ) {
            Text(strings.overviewTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(routes.size.toString(), strings.overviewRoutesLabel, Modifier.weight(1f))
                    StatCard(stops.size.toString(), strings.overviewStopsLabel, Modifier.weight(1f))
                    StatCard(districtCount.toString(), strings.overviewDistrictsLabel, Modifier.weight(1f))
                }
            }

            item {
                Text(
                    strings.overviewUpdatesTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = HeaderGreen,
                )
            }

            if (announcements.isEmpty()) {
                item {
                    Text(strings.newsEmpty, color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                items(announcements, key = { it.id }) { announcement ->
                    AnnouncementCard(announcement, language)
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = HeaderGreen)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}
