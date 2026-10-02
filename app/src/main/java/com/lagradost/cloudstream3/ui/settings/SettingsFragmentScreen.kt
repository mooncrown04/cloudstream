package com.lagradost.cloudstream3.ui.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Crossfade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.getSettings
import com.lagradost.cloudstream3.ui.settings.components.SettingGroup
import com.lagradost.cloudstream3.ui.settings.components.SettingRow
import com.lagradost.cloudstream3.ui.settings.components.focusOutline
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment
import com.lagradost.cloudstream3.utils.UIUtils.isLayout
import com.lagradost.cloudstream3.utils.UIUtils.isTv
import com.lagradost.cloudstream3.utils.UIUtils.LayoutList.TV
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (Int) -> Unit,
    onBack: () -> Unit,
    searchState: TextFieldState
) {
    val isTv = isLayout(TV)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allSettings = remember { getSettings(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsSearch(textFieldState = searchState)

            val filterText = searchState.text.toString().trim()

            if (filterText.isEmpty()) {
                // Varsayılan Kategori Listesi
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allSettings.size) { index ->
                        val group = allSettings[index]
                        SettingGroup(title = stringResource(group.titleRes)) {
                            group.items.forEach { item ->
                                SettingRow(
                                    title = stringResource(item.titleRes),
                                    description = item.descriptionRes?.let { stringResource(it) },
                                    icon = item.iconRes,
                                    onClick = { onNavigate(item.destinationId) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Arama Sonuçları Listesi
                val filteredItems = remember(filterText) {
                    allSettings.flatMap { group ->
                        group.items.filter { item ->
                            val title = context.getString(item.titleRes)
                            val desc = item.descriptionRes?.let { context.getString(it) } ?: ""
                            title.contains(filterText, ignoreCase = true) || desc.contains(filterText, ignoreCase = true)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems.size) { index ->
                        val item = filteredItems[index]
                        SettingRow(
                            title = stringResource(item.titleRes),
                            description = item.descriptionRes?.let { stringResource(it) },
                            icon = item.iconRes,
                            onClick = { onNavigate(item.destinationId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSearch(textFieldState: TextFieldState) {
    var hasFocus by remember { mutableStateOf(false) }
    val focusProgress by animateFloatAsState(targetValue = if (hasFocus) 1.0f else 0.0f)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val isTv = isLayout(TV)

    val clearFocusAndHideKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    TextField(
        value = textFieldState.text.toString(),
        onValueChange = { newText ->
            textFieldState.edit { replace(0, length, newText) }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                clearFocusAndHideKeyboard()
                if (textFieldState.text.isNotBlank()) {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            }
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp - 12.dp * focusProgress)
            .focusOutline(enabled = isTv, CircleShape)
            .focusRequester(focusRequester)
            .onFocusChanged { newFocus ->
                hasFocus = newFocus.hasFocus
                // TV / Fire OS üzerinde otomatik klavye açılışını ve odak kilitlenmesini engelle
                if (!isTv) {
                    if (newFocus.hasFocus) {
                        keyboardController?.show()
                    } else {
                        keyboardController?.hide()
                    }
                }
            }
            .clickable(enabled = isTv) {
                // TV modunda klavye yalnızca kumandadan OK / Seçim tuşuna basılınca açılsın
                keyboardController?.show()
            },
        placeholder = {
            Text(text = stringResource(R.string.search_hint))
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.onBackground,
        ),
        leadingIcon = {
            Crossfade(
                targetState = hasFocus,
                label = "leftsearch",
            ) { value ->
                if (value) {
                    IconButton(onClick = {
                        textFieldState.edit { replace(0, length, "") }
                        clearFocusAndHideKeyboard()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.keyboard_arrow_left_24px),
                            contentDescription = null
                        )
                    }
                } else {
                    IconButton(onClick = {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.search_icon),
                            contentDescription = null
                        )
                    }
                }
            }
        },
        trailingIcon = {
            Crossfade(
                targetState = hasFocus && textFieldState.text.isNotEmpty(),
                label = "rightsearch",
            ) { value ->
                if (value) {
                    IconButton(onClick = {
                        textFieldState.edit { replace(0, length, "") }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            contentDescription = null
                        )
                    }
                }
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }
}
