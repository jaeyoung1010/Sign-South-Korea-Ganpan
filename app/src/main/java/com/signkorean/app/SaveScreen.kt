package com.signkorean.app

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.signkorean.designsystem.theme.SignKoreanPalette
import com.signkorean.designsystem.theme.SignKoreanSpacing
import com.signkorean.domain.model.ProfileAvatar
import com.signkorean.feature.learn.HomeHeader
import java.io.File
import java.util.UUID

@Composable
fun SaveScreen(
    profileAvatar: ProfileAvatar?,
    onOpenNotices: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val context = LocalContext.current
    val directory = remember { File(context.filesDir, "saved_sign_photos") }
    fun photos(): List<File> = directory.listFiles()?.filter { it.name.startsWith("photo_") && it.extension == "jpg" }?.sortedByDescending { it.lastModified() }.orEmpty()
    var photos by remember { mutableStateOf(photos()) }
    var pendingName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingName?.let { name ->
            val pending = File(directory, name)
            if (success && pending.length() > 0) {
                val saved = File(directory, "photo_${UUID.randomUUID()}.jpg")
                if (!pending.renameTo(saved)) error = true
            } else if (success) {
                error = true
            }
            if (pending.exists()) pending.delete()
        }
        pendingName = null
        photos = photos()
    }
    Column(Modifier.fillMaxSize()) {
        HomeHeader(onOpenNotices, onOpenSettings, onOpenProfile, profileAvatar)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(top = SignKoreanSpacing.medium, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(stringResource(R.string.save_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.save_description), modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyLarge)
                    Button(
                        onClick = {
                            try {
                                if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException()
                                val file = File(directory, "pending_${UUID.randomUUID()}.jpg")
                                pendingName = file.name
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.savedphotos", file)
                                camera.launch(uri)
                            } catch (_: ActivityNotFoundException) {
                                pendingName = null
                                error = true
                            } catch (_: Exception) {
                                pendingName = null
                                error = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(SignKoreanPalette.PRIMARY), contentColor = Color(SignKoreanPalette.INK)),
                    ) { Text(stringResource(R.string.save_take_photo), fontWeight = FontWeight.Bold) }
                    if (error) Text(stringResource(R.string.save_error), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    if (photos.isEmpty()) Text(stringResource(R.string.save_empty), modifier = Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(photos, key = { it.name }) { photo ->
                AsyncImage(model = photo, contentDescription = stringResource(R.string.save_photo), contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).clickable { selectedName = photo.name })
            }
        }
    }
    val selected = photos.firstOrNull { it.name == selectedName }
    if (selected != null) {
        Dialog(onDismissRequest = { selectedName = null }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(12.dp)) {
                    AsyncImage(model = selected, contentDescription = stringResource(R.string.save_photo), contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 550.dp))
                    TextButton(onClick = { selectedName = null }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_close)) }
                }
            }
        }
    }
}
