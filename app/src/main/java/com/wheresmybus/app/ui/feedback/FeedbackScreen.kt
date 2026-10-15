package com.wheresmybus.app.ui.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.wheresmybus.app.ui.BusViewModel
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.HeaderGreen
import com.wheresmybus.app.ui.theme.HeaderGreenDark
import com.wheresmybus.app.ui.theme.StatusLive

@Composable
fun FeedbackScreen(
    viewModel: BusViewModel,
    language: AppLanguage,
    onBack: () -> Unit,
) {
    val strings = stringsFor(language)
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSent by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BgCream)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderGreen)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HeaderGreenDark)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(strings.feedbackTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (isSent) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusLive, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(strings.feedbackSentTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(strings.feedbackSentSubtitle, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBack) { Text(strings.done) }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(strings.feedbackHint, color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text(strings.feedbackPlaceholder) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.submitFeedback(message.trim()) { success ->
                            isSubmitting = false
                            if (success) {
                                isSent = true
                            } else {
                                Toast.makeText(context, strings.feedbackFailedToast, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = message.trim().isNotEmpty() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text(strings.submit)
                    }
                }
            }
        }
    }
}
