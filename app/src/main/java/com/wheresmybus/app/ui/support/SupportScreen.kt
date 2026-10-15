package com.wheresmybus.app.ui.support

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheresmybus.app.ui.strings.AppLanguage
import com.wheresmybus.app.ui.strings.stringsFor
import com.wheresmybus.app.ui.theme.BgCream
import com.wheresmybus.app.ui.theme.ChipMint
import com.wheresmybus.app.ui.theme.HeaderGreen
import com.wheresmybus.app.ui.theme.HeaderGreenDark

@Composable
fun SupportScreen(language: AppLanguage, onBack: () -> Unit) {
    val strings = stringsFor(language)

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
            Text(strings.supportTitle, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ContactRow(Icons.Filled.Email, strings.supportContactEmail)
                        ContactRow(Icons.Filled.Call, strings.supportContactPhone)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    strings.supportFaqTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = HeaderGreen,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FaqCard(strings.supportFaqQ1, strings.supportFaqA1)
                Spacer(modifier = Modifier.height(10.dp))
                FaqCard(strings.supportFaqQ2, strings.supportFaqA2)
                Spacer(modifier = Modifier.height(10.dp))
                FaqCard(strings.supportFaqQ3, strings.supportFaqA3)
            }
        }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(ChipMint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = HeaderGreen, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
private fun FaqCard(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(question, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(answer, fontSize = 13.sp, color = Color(0xFF444444))
        }
    }
}

