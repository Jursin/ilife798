package com.github.ilife798.ui.page.license

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.ilife798.data.viewmodel.AppViewModel
import com.github.ilife798.shared.resources.Res
import com.github.ilife798.ui.component.BlurredTopAppBar
import com.github.ilife798.ui.theme.captureForBlur
import com.github.ilife798.ui.theme.rememberAppBlurBackdrop
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.author
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun LicensePage(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val blurBackdrop = rememberAppBlurBackdrop(viewModel.state.appBlur)
    val uriHandler = LocalUriHandler.current
    val libs by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    val libraries =
        remember(libs) {
            libs?.libraries?.sortedBy { it.name.lowercase() }.orEmpty()
        }
    var selectedLibrary by remember { mutableStateOf<Library?>(null) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BlurredTopAppBar("开放源代码许可", blurBackdrop, scrollBehavior, onBack = onBack) },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .captureForBlur(blurBackdrop)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            overscrollEffect = null,
        ) {
            items(libraries, key = { it.uniqueId }) { library ->
                LibraryCard(library = library) {
                    selectedLibrary = library
                    showLicenseDialog = true
                }
            }
        }
    }

    selectedLibrary?.let { library ->
        WindowDialog(
            show = showLicenseDialog,
            title = library.name,
            summary = library.artifactVersion,
            summaryColor = MiuixTheme.colorScheme.primary,
            onDismissRequest = { showLicenseDialog = false },
            onDismissFinished = { selectedLibrary = null },
            content = {
                val dismissState = LocalDismissState.current
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                ) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .scrollEndHaptic()
                                .overScrollVertical(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = null,
                    ) {
                        items(library.licenses.toList()) { license ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    CardDefaults.defaultColors(
                                        color = MiuixTheme.colorScheme.surfaceContainerHighest,
                                    ),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = license.name,
                                        style = MiuixTheme.textStyles.title3,
                                        color = MiuixTheme.colorScheme.primary,
                                        modifier =
                                            Modifier.clickable {
                                                license.url?.let { uriHandler.openUri(it) }
                                            },
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text =
                                            license.licenseContent
                                                ?: "该许可证无公开文本",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!library.website.isNullOrBlank()) {
                            TextButton(
                                modifier = Modifier.weight(1f),
                                onClick = { uriHandler.openUri(library.website!!) },
                                text = "访问主页",
                            )
                        }
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = { dismissState?.invoke() },
                            text = "关闭",
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun LibraryCard(
    library: Library,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = library.name,
                    style = MiuixTheme.textStyles.title2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                )
                library.artifactVersion?.let {
                    Text(
                        text = it,
                        style = MiuixTheme.textStyles.title3,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = library.author,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            library.licenses.forEach { license ->
                Card(
                    colors =
                        CardDefaults.defaultColors(
                            color = MiuixTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            contentColor = MiuixTheme.colorScheme.onSurface,
                        ),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = license.name,
                            color = MiuixTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
