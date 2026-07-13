package com.devicemonitor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devicemonitor.ui.components.StatusCard

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Device Monitor", style = MaterialTheme.typography.headlineLarge)
        
        StatusCard(title = "Battery Level", value = "75%")
        StatusCard(title = "Network", value = "WIFI")
        StatusCard(title = "Status", value = "Online")
        
        Button(onClick = { /* TODO: Trigger sync */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Sync Now")
        }
    }
}
