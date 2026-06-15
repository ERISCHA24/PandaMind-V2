package com.example.animepopular.ui.screens.language

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.animepopular.ui.theme.*
import com.example.animepopular.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentLanguage == "id") "Pilih Bahasa" else "Select Language",
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (currentLanguage == "id") "Pilih Bahasa Tampilan" else "Choose Display Language",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (currentLanguage == "id") "Perubahan akan langsung diterapkan"
                else "Changes will be applied immediately",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LanguageCard(
                flag = "🇬🇧",
                name = "English",
                subLabel = "English Language",
                isSelected = currentLanguage == "en",
                onClick = {
                    viewModel.setLanguage("en")
                    onBack()
                }
            )
            Spacer(Modifier.height(14.dp))
            LanguageCard(
                flag = "🇮🇩",
                name = "Bahasa Indonesia",
                subLabel = "Indonesian Language",
                isSelected = currentLanguage == "id",
                onClick = {
                    viewModel.setLanguage("id")
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun LanguageCard(
    flag: String,
    name: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = if (isSelected) BorderStroke(2.dp, AccentColor) else null,
        elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(flag, fontSize = 36.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = TextPrimary, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold)
                Text(subLabel, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected",
                    tint = AccentColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}