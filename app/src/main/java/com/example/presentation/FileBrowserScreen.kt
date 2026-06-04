package com.example.presentation

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class MediaStoreFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String?,
    val dateModified: Long,
    val path: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileBrowserScreen(navController: NavController, viewModel: PeerLinkViewModel) {
    val context = LocalContext.current
    var allFiles by remember { mutableStateOf<List<MediaStoreFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Images, 2: Videos, 3: Audio, 4: Docs, 5: Folders

    // Inside folder tracking
    var selectedFolderName by remember { mutableStateOf<String?>(null) }

    // Fetch files Reactively on opening
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allFiles = queryMediaStoreFiles(context)
            isLoading = false
        }
    }

    val selectedFilesState by viewModel.selectedFiles.collectAsState()
    val selectedUris = remember(selectedFilesState) {
        selectedFilesState.map { it.uri }.toSet()
    }

    // Categorize files
    val images = remember(allFiles) {
        allFiles.filter {
            it.mimeType?.startsWith("image/") == true || it.name.lowercase().let { n ->
                n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".heic")
            }
        }
    }

    val videos = remember(allFiles) {
        allFiles.filter {
            it.mimeType?.startsWith("video/") == true || it.name.lowercase().let { n ->
                n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi") || n.endsWith(".3gp") || n.endsWith(".mov")
            }
        }
    }

    val audio = remember(allFiles) {
        allFiles.filter {
            it.mimeType?.startsWith("audio/") == true || it.name.lowercase().let { n ->
                n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".aac") || n.endsWith(".flac") || n.endsWith(".m4a") || n.endsWith(".ogg")
            }
        }
    }

    val documents = remember(allFiles) {
        allFiles.filter {
            val mime = it.mimeType?.lowercase() ?: ""
            val name = it.name.lowercase()
            mime.startsWith("text/") ||
            mime.contains("pdf") ||
            mime.contains("word") ||
            mime.contains("excel") ||
            mime.contains("powerpoint") ||
            mime.contains("zip") ||
            mime.contains("rar") ||
            mime.contains("tar") ||
            mime.contains("octet-stream") ||
            name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx") ||
            name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") || name.endsWith(".pptx") ||
            name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".apk") || name.endsWith(".json")
        }
    }

    // Folders parsing
    val foldersMap = remember(allFiles) {
        allFiles.groupBy { getParentFolderName(it.path) }
    }

    // Calculate currently active files to display
    val currentFileList = remember(selectedTab, allFiles, images, videos, audio, documents, selectedFolderName, foldersMap) {
        if (selectedTab == 5) {
            if (selectedFolderName != null) {
                foldersMap[selectedFolderName] ?: emptyList()
            } else {
                emptyList()
            }
        } else {
            when (selectedTab) {
                0 -> allFiles
                1 -> images
                2 -> videos
                3 -> audio
                4 -> documents
                else -> allFiles
            }
        }
    }

    // Filter by search query
    val filteredFiles = remember(currentFileList, searchQuery) {
        if (searchQuery.isBlank()) {
            currentFileList
        } else {
            currentFileList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val tabs = listOf("All", "Images", "Videos", "Music", "Docs", "Folders")

    Scaffold(
        containerColor = CoreDeepSpace,
        contentColor = TextPrimary,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CoreDarkVariant)
                    .statusBarsPadding()
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (selectedTab == 5 && selectedFolderName != null) {
                                selectedFolderName = null
                            } else {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(CoreDeepSpace, CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == 5 && selectedFolderName != null) selectedFolderName!! else "File Explorer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (selectedTab == 5 && selectedFolderName != null) "Browsing local subdirectory" else "Select local documents or media to share",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (selectedFilesState.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuroraTeal.copy(alpha = 0.15f))
                                .border(1.dp, AuroraTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${selectedFilesState.size} SELECTED",
                                color = AuroraTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom High-Fidelity Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search files by name...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextPrimary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CoreDeepSpace,
                        unfocusedContainerColor = CoreDeepSpace,
                        focusedBorderColor = AuroraCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable category tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    edgePadding = 0.dp,
                    divider = {},
                    indicator = {}
                ) {
                    tabs.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                selectedTab = index
                                // Clean folder state unless viewing Folders
                                if (index != 5) {
                                    selectedFolderName = null
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            val tint = when (index) {
                                1 -> AuroraViolet
                                2 -> AuroraCyan
                                3 -> AuroraTeal
                                4 -> AuroraViolet
                                5 -> AuroraCyan
                                else -> AuroraTeal
                            }
                            
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) tint.copy(alpha = 0.15f) else CoreDeepSpace)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) tint else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) tint else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AuroraTeal, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Indexing local storage files...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else if (selectedTab == 5 && selectedFolderName == null) {
                // Show directories grouping view
                val foldersList = foldersMap.keys.sortedBy { it.lowercase() }
                
                if (foldersList.isEmpty()) {
                    EmptyExplorerState(
                        icon = Icons.Default.FolderOpen,
                        title = "No Folders Detected",
                        subtitle = "We could not locate any local database or file directories."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                    ) {
                        items(foldersList) { folderName ->
                            val filesInFolder = foldersMap[folderName] ?: emptyList()
                            FolderItemCard(
                                folderName = folderName,
                                fileCount = filesInFolder.size,
                                onClick = { selectedFolderName = folderName }
                            )
                        }
                    }
                }
            } else if (filteredFiles.isEmpty()) {
                EmptyExplorerState(
                    icon = Icons.Default.InsertDriveFile,
                    title = "No matching files",
                    subtitle = "We indexed ${allFiles.size} total items, but none matched your query/filters."
                )
            } else {
                // Renders elegant Grid for images, list for other files
                if (selectedTab == 1) {
                    // Images Grid (Beautiful grid selector)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                    ) {
                        items(filteredFiles) { file ->
                            val isChecked = selectedUris.contains(file.uri)
                            ImageThumbnailCard(
                                file = file,
                                isChecked = isChecked,
                                onClick = {
                                    if (isChecked) {
                                        viewModel.removeFile(file.uri)
                                    } else {
                                        viewModel.addFiles(context, listOf(file.uri))
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Standard visual list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                    ) {
                        // Header info if browsing inside folder
                        if (selectedTab == 5 && selectedFolderName != null) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                        .clickable { selectedFolderName = null }
                                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AuroraCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Back to Folders Catalog", color = AuroraCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        items(filteredFiles) { file ->
                            val isChecked = selectedUris.contains(file.uri)
                            FileListItemCard(
                                file = file,
                                isChecked = isChecked,
                                onClick = {
                                    if (isChecked) {
                                        viewModel.removeFile(file.uri)
                                    } else {
                                        viewModel.addFiles(context, listOf(file.uri))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Floater action confirmation container
            AnimatedVisibility(
                visible = selectedFilesState.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(AuroraTeal, AuroraCyan, AuroraViolet))
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.padding(start = 6.dp)) {
                            val totalSize = selectedFilesState.sumOf { it.size }
                            Text(
                                text = "${selectedFilesState.size} file(s) selected",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${"%.2f".format(totalSize / (1024.0 * 1024.0))} MB total payload",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text("Share Now", color = CoreDeepSpace, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Check, contentDescription = "Confirm Selection", tint = CoreDeepSpace, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItemCard(
    folderName: String,
    fileCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuroraCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (folderName.lowercase()) {
                        "download", "downloads" -> Icons.Default.ArrowDownward
                        "documents", "document" -> Icons.Default.Description
                        "pictures", "pictures", "dcim" -> Icons.Default.Image
                        "music", "audio" -> Icons.Default.MusicNote
                        "movies", "video", "videos" -> Icons.Default.Movie
                        else -> Icons.Default.Folder
                    },
                    contentDescription = null,
                    tint = AuroraCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "$fileCount indexed item(s)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate in folder",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FileListItemCard(
    file: MediaStoreFile,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    val dateString = remember(file.dateModified) {
        try {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(file.dateModified * 1000L))
        } catch (e: Exception) {
            "Unknown date"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) AuroraTeal.copy(alpha = 0.05f) else CoreDarkVariant
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isChecked) AuroraTeal.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fileIconAndTint = remember(file.mimeType, file.name) {
                val name = file.name.lowercase()
                when {
                    file.mimeType?.startsWith("image/") == true || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") -> {
                        Icons.Default.Image to AuroraViolet
                    }
                    file.mimeType?.startsWith("video/") == true || name.endsWith(".mp4") || name.endsWith(".mkv") -> {
                        Icons.Default.PlayCircle to AuroraCyan
                    }
                    file.mimeType?.startsWith("audio/") == true || name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") -> {
                        Icons.Default.AudioFile to AuroraTeal
                    }
                    name.endsWith(".pdf") -> {
                        Icons.Default.Description to AuroraViolet
                    }
                    name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".tar") || name.endsWith(".gz") -> {
                        Icons.Default.FolderZip to AuroraCyan
                    }
                    name.endsWith(".apk") -> {
                        Icons.Default.Android to AuroraTeal
                    }
                    else -> {
                        Icons.Default.InsertDriveFile to TextSecondary
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(fileIconAndTint.second.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileIconAndTint.first,
                    contentDescription = null,
                    tint = fileIconAndTint.second,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "${"%.2f".format(file.size / (1024.0 * 1024.0))} MB",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = " • ",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = dateString,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = isChecked,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AuroraTeal,
                    checkmarkColor = CoreDeepSpace,
                    uncheckedColor = TextSecondary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ImageThumbnailCard(
    file: MediaStoreFile,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(CoreDarkVariant)
            .border(
                2.dp,
                if (isChecked) AuroraTeal else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        // Load real thumbnails asynchronously using Coil
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file.uri)
                .crossfade(true)
                .build(),
            contentDescription = file.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for visual aesthetics and checkbox visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Checkbox container
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = AuroraTeal,
                    checkmarkColor = CoreDeepSpace,
                    uncheckedColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.size(24.dp)
            )
        }

        // File size at bottom left
        Text(
            text = "${"%.1f".format(file.size / (1024.0 * 1024.0))} M",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
    }
}

@Composable
fun EmptyExplorerState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AuroraViolet.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AuroraViolet,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

// MediaStore helper query function
private fun queryMediaStoreFiles(context: Context): List<MediaStoreFile> {
    val list = mutableListOf<MediaStoreFile>()
    val externalUri = MediaStore.Files.getContentUri("external")
    
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.DATA
    )

    // Index all items matching documents, pictures, music, video
    val selection = """
        ${MediaStore.Files.FileColumns.SIZE} > 0 AND (
            ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'image/%' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'video/%' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'audio/%' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} = 'application/pdf' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} = 'text/plain' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE '%word%' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE '%excel%' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE '%powerpoint%' OR 
            ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE '%zip%' OR
            ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf' OR 
            ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.txt' OR 
            ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.docx' OR 
            ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.xlsx' || 
            ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.zip' || 
            ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.apk'
        )
    """.trimIndent()

    val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

    try {
        context.contentResolver.query(externalUri, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = if (idCol != -1) cursor.getLong(idCol) else continue
                val name = if (nameCol != -1) cursor.getString(nameCol) ?: "unknown_file" else "unknown_file"
                val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                val mimeType = if (mimeCol != -1) cursor.getString(mimeCol) else null
                val dateModified = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                if (size > 0) {
                    val contentUri = ContentUris.withAppendedId(externalUri, id)
                    list.add(MediaStoreFile(contentUri, name, size, mimeType, dateModified, path))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun getParentFolderName(path: String): String {
    if (path.isEmpty()) return "Other"
    val parts = path.split("/")
    if (parts.size >= 2) {
        val parent = parts[parts.size - 2]
        if (parent.isNotEmpty() && parent != "0" && parent != "emulated") {
            return parent
        }
    }
    return "Download"
}
