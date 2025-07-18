package com.rifsxd.ksunext.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dergoogler.mmrl.ui.component.LabelItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.TemplateEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.ramcosta.composedestinations.result.getOr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.rifsxd.ksunext.R
import com.rifsxd.ksunext.ui.viewmodel.TemplateViewModel

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween

/**
 * @author weishu
 * @date 2023/10/20.
 */

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppProfileTemplateScreen(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<TemplateEditorScreenDestination, Boolean>
) {
    val viewModel = viewModel<TemplateViewModel>()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        if (viewModel.templateList.isEmpty()) {
            viewModel.fetchTemplates()
        }
    }

    // handle result from TemplateEditorScreen, refresh if needed
    resultRecipient.onNavResult { result ->
        if (result.getOr { false }) {
            scope.launch { viewModel.fetchTemplates() }
        }
    }

    val listState = rememberLazyListState()
    var showFab by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currIndex, currOffset) ->
                val isScrollingDown = currIndex > lastIndex ||
                        (currIndex == lastIndex && currOffset > lastOffset + 4)
                val isScrollingUp = currIndex < lastIndex ||
                        (currIndex == lastIndex && currOffset < lastOffset - 4)

                when {
                    isScrollingDown && showFab -> showFab = false
                    isScrollingUp && !showFab -> showFab = true
                }

                lastIndex = currIndex
                lastOffset = currOffset
            }
    }

    Scaffold(
        topBar = {
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current
            val showToast = fun(msg: String) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            TopBar(
                onBack = dropUnlessResumed { navigator.popBackStack() },
                onSync = {
                    scope.launch { viewModel.fetchTemplates(true) }
                },
                onImport = {
                    clipboardManager.getText()?.text?.let {
                        if (it.isEmpty()) {
                            showToast(context.getString(R.string.app_profile_template_import_empty))
                            return@let
                        }
                        scope.launch {
                            viewModel.importTemplates(
                                it, {
                                    showToast(context.getString(R.string.app_profile_template_import_success))
                                    viewModel.fetchTemplates(false)
                                },
                                showToast
                            )
                        }
                    }
                },
                onExport = {
                    scope.launch {
                        viewModel.exportTemplates(
                            {
                                showToast(context.getString(R.string.app_profile_template_export_empty))
                            }
                        ) {
                            clipboardManager.setText(AnnotatedString(it))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(
                    animationSpec = tween(200),
                    initialScale = 0.8f
                ) + fadeIn(animationSpec = tween(400)),
                exit = scaleOut(
                    animationSpec = tween(200),
                    targetScale = 0.8f
                ) + fadeOut(animationSpec = tween(400))
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navigator.navigate(
                            TemplateEditorScreenDestination(
                                TemplateViewModel.TemplateInfo(),
                                false
                            )
                        )
                    },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(id = R.string.app_profile_template_create)) },
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding),
            isRefreshing = viewModel.isRefreshing,
            onRefresh = {
                scope.launch { viewModel.fetchTemplates() }
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = remember {
                    PaddingValues(bottom = 16.dp /* Scaffold Fab Spacing + Fab container height */)
                }
            ) {
                items(viewModel.templateList, key = { it.id }) { app ->
                    TemplateItem(navigator, app)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateItem(
    navigator: DestinationsNavigator,
    template: TemplateViewModel.TemplateInfo
) {
    ListItem(
        modifier = Modifier
            .clickable {
                navigator.navigate(TemplateEditorScreenDestination(template, !template.local))
            },
        headlineContent = { Text(
            text = template.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        ) },
        supportingContent = {
            Column {
                Text(
                    text = "${template.id}${if (template.author.isEmpty()) "" else "@${template.author}"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                )
                Text(template.description)

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LabelItem(text = "UID: ${template.uid}")
                    LabelItem(text = "GID: ${template.gid}")
                    LabelItem(text = template.context)
                    if (template.local) {
                        LabelItem(text = "local")
                    } else {
                        LabelItem(text = "remote")
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit,
    onSync: () -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_profile_template),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
        actions = {
            IconButton(onClick = onSync) {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = stringResource(id = R.string.app_profile_template_sync)
                )
            }

            var showDropdown by remember { mutableStateOf(false) }
            IconButton(onClick = {
                showDropdown = true
            }) {
                Icon(
                    imageVector = Icons.Filled.ImportExport,
                    contentDescription = stringResource(id = R.string.app_profile_import_export)
                )

                DropdownMenu(expanded = showDropdown, onDismissRequest = {
                    showDropdown = false
                }) {
                    DropdownMenuItem(text = {
                        Text(stringResource(id = R.string.app_profile_import_from_clipboard))
                    }, onClick = {
                        onImport()
                        showDropdown = false
                    })
                    DropdownMenuItem(text = {
                        Text(stringResource(id = R.string.app_profile_export_to_clipboard))
                    }, onClick = {
                        onExport()
                        showDropdown = false
                    })
                }
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}