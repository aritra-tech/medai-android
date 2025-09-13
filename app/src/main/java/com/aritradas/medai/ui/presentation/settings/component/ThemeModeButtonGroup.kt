package com.aritradas.medai.ui.presentation.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aritradas.medai.domain.model.ThemePreference

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeModeButtonGroup(
    currentTheme: ThemePreference,
    onThemeSelected: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
) {
    val options = listOf(
        ThemePreference.SYSTEM,
        ThemePreference.LIGHT,
        ThemePreference.DARK
    )
    val labels = listOf("System", "Light", "Dark")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(
                if (isFirstItem && isLastItem) RoundedCornerShape(20.dp)
                else if (isFirstItem) RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                )
                else if (isLastItem) RoundedCornerShape(
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp,
                    topStart = 4.dp,
                    topEnd = 4.dp
                )
                else RoundedCornerShape(4.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose your preferred theme",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, themePref ->
                    ToggleButton(
                        checked = currentTheme == themePref,
                        onCheckedChange = { onThemeSelected(themePref) },
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}