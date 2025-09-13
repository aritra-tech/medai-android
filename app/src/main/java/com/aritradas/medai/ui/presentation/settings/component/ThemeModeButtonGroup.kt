package com.aritradas.medai.ui.presentation.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ThemePreference.SYSTEM,
        ThemePreference.LIGHT,
        ThemePreference.DARK
    )
    val labels = listOf("System", "Light", "Dark")
    val unCheckedIcons = listOf(
        Icons.Outlined.Brightness6,
        Icons.Outlined.LightMode,
        Icons.Outlined.DarkMode
    )
    val checkedIcons = listOf(
        Icons.Filled.Brightness6,
        Icons.Filled.LightMode,
        Icons.Filled.DarkMode
    )

    Row(
        modifier.then(Modifier.padding(horizontal = 8.dp)),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        val modifiers = listOf(Modifier.weight(1f), Modifier.weight(1.2f), Modifier.weight(1f))
        options.forEachIndexed { index, themePref ->
            ToggleButton(
                checked = currentTheme == themePref,
                onCheckedChange = { onThemeSelected(themePref) },
                modifier = modifiers[index].semantics { role = Role.RadioButton },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
            ) {
                Icon(
                    if (currentTheme == themePref) checkedIcons[index] else unCheckedIcons[index],
                    contentDescription = labels[index]
                )
            }
        }
    }
}