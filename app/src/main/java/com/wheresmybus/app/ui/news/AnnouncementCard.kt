package com.wheresmybus.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheresmybus.app.data.Announcement
import com.wheresmybus.app.data.AnnouncementSeverity
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.StatusLive
import com.wheresmybus.app.ui.theme.StatusRecent
import com.wheresmybus.app.util.formatRelativeTimeAgo

/** Shown on the Overview tab's "Latest updates" feed. */
@Composable
fun AnnouncementCard(announcement: Announcement, language: AppLanguage) {
    val strings = stringsFor(language)
    val (dotColor, label) = when (announcement.severity) {
        AnnouncementSeverity.WARNING -> StatusRecent to strings.newsSeverityWarning
        AnnouncementSeverity.INFO -> StatusLive to strings.newsSeverityInfo
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = dotColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatRelativeTimeAgo(announcement.createdAtEpochMillis),
                    color = Color.Gray,
                    fontSize = 12.sp,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                announcement.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                announcement.body,
                fontSize = 13.sp,
                color = Color(0xFF444444),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
