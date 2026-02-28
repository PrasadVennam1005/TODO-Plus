package com.todoplus.sample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

// TODO(@design_team priority:HIGH category:ui issue:DESIGN-99): Standardize these padding values in the Jetpack Compose theme
@Composable
fun UserProfileScreen(userId: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        
        Text(text = "User Profile", style = MaterialTheme.typography.headlineMedium)
        
        // TODO(priority:CRITICAL category:performance due:2024-01-01): This recomposes way too often. Add remember block. (OVERDUE!)
        val userFlow = fetchUserFromDatabase(userId)
        
        // FIXME(priority:MEDIUM issue:BUG-112): The placeholder image is broken on Android 12+
        ProfileImage(url = userFlow.avatarUrl)
    }
}
