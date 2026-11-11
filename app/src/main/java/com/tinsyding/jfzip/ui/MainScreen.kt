package com.tinsyding.jfzip.ui

import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tinsyding.jfzip.BuildState
import com.tinsyding.jfzip.R
import com.tinsyding.jfzip.ReadyArchive
import com.tinsyding.jfzip.UiState
import com.tinsyding.jfzip.util.shareArchive
import java.io.File

@Composable
fun MainScreen(
    state: UiState,
    onAddUris: (List<Uri>) -> Unit,
    onRemoveUri: (Uri) -> Unit,
    onClearUris: () -> Unit,
    onNameChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onOpenSettings: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(100),
    ) { uris ->
        onAddUris(uris)
    }

    val openPicker: () -> Unit = {
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MainTopBar(onOpenSettings = onOpenSettings)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp),
            ) {
                ArchiveCard(
                    prefix = state.prefix,
                    name = state.name,
                    onNameChange = onNameChange,
                )

                SectionHeader(
                    count = state.uris.size,
                    onClear = onClearUris,
                )

                if (state.uris.isEmpty()) {
                    EmptyPick(
                        onPick = openPicker,
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 24.dp),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(96.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = state.uris,
                            key = { _, uri -> uri.toString() },
                        ) { index, uri ->
                            ImageCell(
                                uri = uri,
                                index = index,
                                onRemove = { onRemoveUri(uri) },
                            )
                        }
                        item {
                            AddCell(onPick = openPicker)
                        }
                    }
                }
            }

            BottomAction(
                state = state,
                onGenerate = onGenerate,
                onShare = { file ->
                    runCatching { shareArchive(context, file) }
                        .onFailure {
                            Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
                        }
                },
            )
        }
    }
}

@Composable
private fun MainTopBar(onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "JFZip",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "设置",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ArchiveCard(
    prefix: String,
    name: String,
    onNameChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = "压缩包",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DateStamp(prefix)
                Text(
                    text = "-",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                NameField(
                    name = name,
                    onNameChange = onNameChange,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = ".zip",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun DateStamp(prefix: String) {
    val accent = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = Modifier
            .rotate(-1.2f)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = prefix,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = accent,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun NameField(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val underline by animateColorAsState(
        targetValue = if (focused) colors.tertiary.copy(alpha = 0.7f) else colors.outline,
        animationSpec = tween(120),
        label = "nameUnderline",
    )

    Box(
        modifier = modifier
            .drawBehind {
                val y = size.height - 1.dp.toPx()
                drawLine(
                    color = underline,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(bottom = 4.dp)
            .semantics { contentDescription = "姓名" },
    ) {
        if (name.isEmpty() && !focused) {
            Text(
                text = "姓名",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        BasicTextField(
            value = name,
            onValueChange = { onNameChange(it.take(24)) },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = colors.onBackground,
            ),
            cursorBrush = SolidColor(colors.tertiary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
    }
}

@Composable
private fun SectionHeader(count: Int, onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "图片",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.weight(1f))
        if (count > 0) {
            Text(
                text = "$count 张",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "清空",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ImageCell(uri: Uri, index: Int, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = colors.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .background(colors.surface.copy(alpha = 0.92f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(colors.surface.copy(alpha = 0.92f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "移除",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun AddCell(onPick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = "添加图片",
            tint = colors.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyPick(
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onPick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "选择图片",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun BottomAction(
    state: UiState,
    onGenerate: () -> Unit,
    onShare: (File) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            when (val build = state.build) {
                is BuildState.Ready -> {
                    ReadySummary(build.archive)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onGenerate,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.onBackground,
                            ),
                        ) {
                            Text("重新生成", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = { onShare(build.archive.file) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("分享", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                is BuildState.Building -> {
                    Text(
                        text = "${build.done.toString().padStart(2, '0')} / ${build.total}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (build.total == 0) 0f else build.done.toFloat() / build.total
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = colors.tertiary,
                        trackColor = colors.outline,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("生成中", style = MaterialTheme.typography.labelLarge)
                    }
                }

                else -> {
                    Button(
                        onClick = onGenerate,
                        enabled = state.uris.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                        ),
                    ) {
                        Text("生成压缩包", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadySummary(archive: ReadyArchive) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = archive.name,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${archive.count} 张 · ${Formatter.formatShortFileSize(context, archive.size)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
