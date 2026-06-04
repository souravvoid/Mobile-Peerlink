package com.example.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.network.DiscoveredPeer
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: PeerLinkViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val fingerprint by viewModel.fingerprintToApprove.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val activeException by viewModel.activeException.collectAsState()

    // 🔒 Establish Secure pairing dialog for connection handshake approval
    if (fingerprint != null) {
        AlertDialog(
            onDismissRequest = { viewModel.answerApproval(false) },
            containerColor = CoreDarkVariant,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Trust Identification Handshake", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val metadata = stats.metadata
                    if (metadata != null) {
                        Text("Incoming File Bundle:", color = AuroraTeal, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .background(CoreDeepSpace.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            items(metadata.files) { file ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = AuroraViolet, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = file.fileName,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Total payload: ${"%.2f".format(metadata.totalSize / (1024.0 * 1024.0))} MB",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text("Verify peer security certificate signature:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = fingerprint!!,
                        color = AuroraTeal,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkGlass, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.answerApproval(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal, contentColor = CoreDeepSpace),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Authorize & Trust", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.answerApproval(false) }) {
                    Text("Reject Connection", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main structural layout incorporating bottom tabs on home, history, and settings screens
        Scaffold(
            containerColor = CoreDeepSpace,
            contentColor = TextPrimary,
            bottomBar = {
                if (currentRoute in listOf("home", "history", "settings")) {
                    NavigationBar(
                        containerColor = CoreDarkVariant,
                        tonalElevation = 8.dp,
                        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Place, contentDescription = "Discover peers") },
                            label = { Text("Discovery", fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoreDeepSpace,
                                selectedTextColor = AuroraTeal,
                                indicatorColor = AuroraTeal,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "history",
                            onClick = { navController.navigate("history") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.History, contentDescription = "Transfers Log") },
                            label = { Text("History", fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoreDeepSpace,
                                selectedTextColor = AuroraViolet,
                                indicatorColor = AuroraViolet,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = { navController.navigate("settings") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Configuration settings") },
                            label = { Text("Settings", fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoreDeepSpace,
                                selectedTextColor = AuroraCyan,
                                indicatorColor = AuroraCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") { HomeScreen(navController, viewModel) }
                composable("send") { SendScreen(navController, viewModel) }
                composable("receive") { ReceiveScreen(navController, viewModel) }
                composable("history") { HistoryScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) }
                composable("file_browser") { FileBrowserScreen(navController, viewModel) }
            }
        }

        // Custom High-Fidelity Error Notification Banner
        AnimatedVisibility(
            visible = activeException != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .padding(bottom = if (currentRoute in listOf("home", "history", "settings")) 80.dp else 0.dp)
        ) {
            if (activeException != null) {
                ErrorSnackbarCard(
                    exception = activeException!!,
                    onDismiss = { viewModel.clearActiveException() }
                )
            }
        }
    }
}

@Composable
fun ErrorSnackbarCard(
    exception: com.example.util.PeerLinkException,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    Color(0xFFEF4444),
                    Color(0xFFEE5959),
                    Color(0xFFF97316)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Determine appropriate icon dynamically
                val icon = when (exception) {
                    is com.example.util.PeerLinkException.DiscoveryException -> Icons.Default.Search
                    is com.example.util.PeerLinkException.SocketConnectionException -> Icons.Default.SignalWifi4Bar
                    is com.example.util.PeerLinkException.ConnectionInterruptedException -> Icons.Default.Warning
                    is com.example.util.PeerLinkException.FileAccessPermissionException -> Icons.Default.Folder
                    is com.example.util.PeerLinkException.PeerRejectedException -> Icons.Default.Lock
                    is com.example.util.PeerLinkException.EncryptionHandshakeException -> Icons.Default.Lock
                    else -> Icons.Default.Warning
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Error notification icon",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exception.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exception.description,
                        color = TextPrimary.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close notifications banner",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actionable Recovery Guidance container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEF4444).copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Troubleshooting instruction",
                    tint = Color(0xFFF97316).copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = exception.recoveryHint,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, viewModel: PeerLinkViewModel) {
    val ip by viewModel.ipAddress.collectAsState()
    val localName by viewModel.deviceName.collectAsState()
    val isVisible by viewModel.visibilityEnabled.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(localName) }

    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose {
            viewModel.stopDiscovery()
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = CoreDarkVariant,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Rename local station", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Display Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraTeal,
                        unfocusedBorderColor = CoreDeepSpace,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.updateDeviceName(tempName)
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal, contentColor = CoreDeepSpace)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App top identity banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("PeerLink", fontSize = 32.sp, fontWeight = FontWeight.Black, color = AuroraTeal)
                Text("P2P Decentralized LAN Transfer OS", fontSize = 12.sp, color = TextSecondary)
            }
            // Live scanning capsule badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassLayer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AuroraTeal.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("SCANNING", color = AuroraTeal, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Your Local Device identification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuroraTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(localName, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit name",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            tempName = localName
                                            showEditDialog = true
                                        }
                                )
                            }
                            Text("Local Coordinate Node", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    // Visibility Capsule Switch Indicator
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isVisible) AuroraTeal.copy(alpha = 0.15f) else CoreDeepSpace)
                            .clickable { viewModel.updateVisibility(!isVisible) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isVisible) AuroraTeal else TextSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isVisible) "Visible" else "Hidden",
                            color = if (isVisible) AuroraTeal else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CoreDeepSpace, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("IPv4 NETWORK LOCATION", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(ip, color = AuroraCyan, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassLayer),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.SignalWifi4Bar, contentDescription = null, tint = AuroraCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active LAN", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Direct Quick navigation triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.navigate("send") },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Send file bundle", tint = CoreDeepSpace)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Bundle", color = CoreDeepSpace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { navController.navigate("receive") },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Receive pending files", tint = CoreDeepSpace)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Receive Files", color = CoreDeepSpace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Discovery Live Feeds Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Active Discovered Senders", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = { viewModel.startDiscovery() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Rescan multicast network", tint = AuroraTeal)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (discoveredPeers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ScanningRadarBg()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Searching for nearby PeerLink broadcasts...",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(discoveredPeers) { peer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CoreDarkVariant)
                            .border(1.dp, AuroraTeal.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { viewModel.connectToDiscoveredPeer(peer) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AuroraTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(peer.deviceName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("IP: ${peer.ip}", color = TextSecondary, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.connectToDiscoveredPeer(peer) },
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Connect", fontSize = 12.sp, color = CoreDeepSpace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningRadarBg() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .drawBehind {
                drawCircle(
                    color = AuroraTeal.copy(alpha = alpha),
                    radius = (size.minDimension / 2) * scale
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(AuroraTeal.copy(alpha = 0.15f))
                .border(2.dp, AuroraTeal, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = AuroraTeal,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun SendScreen(navController: NavController, viewModel: PeerLinkViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val inviteCode by viewModel.inviteCode.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    var showPairQr by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(context, uris)
        }
    }

    if (showPairQr && inviteCode != null) {
        ShowPairingQrDialog(inviteCode = inviteCode!!, onDismiss = { showPairQr = false })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                viewModel.cancelTransfer()
                navController.popBackStack()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
            }
            Text("Send Files", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = { viewModel.clearFiles() }, enabled = selectedFiles.isNotEmpty()) {
                Icon(Icons.Default.Delete, contentDescription = "Clear selected files list", tint = if (selectedFiles.isNotEmpty()) Color.Red else TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (inviteCode == null && stats.progress == 0f && stats.error == null && !stats.isComplete && !stats.isConnecting && !stats.isWaitingForApproval) {
            if (selectedFiles.isEmpty()) {
                Column(modifier = Modifier.weight(1f)) {
                    // SAF Permission-Safe Explanation Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = AuroraTeal.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AuroraTeal.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AuroraTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Security Status", tint = AuroraTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Storage Access Framework (SAF)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("No storage permission required. Files are accessed via secure system-brokered virtual selections on demand.", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    // Large Comprehensive File Scanner Button Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CoreDarkVariant)
                            .border(2.dp, AuroraTeal, RoundedCornerShape(24.dp))
                            .clickable { navController.navigate("file_browser") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(AuroraTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "Add Files", tint = AuroraTeal, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("Launch In-App File Explorer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("Explore local document repositories, folders, and visual media grids", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("SELECT FILES VIA STORAGE FILTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Media Categories Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { launcher.launch(arrayOf("image/*")) },
                            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoreDeepSpace)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AuroraViolet.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Images Filter", tint = AuroraViolet, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Images", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("image/*", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { launcher.launch(arrayOf("video/*")) },
                            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoreDeepSpace)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AuroraCyan.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Videos Filter", tint = AuroraCyan, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Videos", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("video/*", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audio & Documents Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { launcher.launch(arrayOf("audio/*")) },
                            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoreDeepSpace)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AuroraTeal.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = "Audio Filter", tint = AuroraTeal, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Audio Track", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("audio/*", color = TextSecondary, fontSize = 10.sp)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { launcher.launch(arrayOf("application/pdf", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoreDeepSpace)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AuroraViolet.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = "Documents Filter", tint = AuroraViolet, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Documents", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("PDF, TXT, DOCX", color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            } else {
                Text("SELECTED FILE BUNDLE (${selectedFiles.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedFiles) { localFile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CoreDarkVariant)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when {
                                localFile.name.endsWith(".jpg", true) || localFile.name.endsWith(".png", true) || localFile.name.endsWith(".jpeg", true) -> Icons.Default.Image
                                localFile.name.endsWith(".mp4", true) || localFile.name.endsWith(".mkv", true) || localFile.name.endsWith(".avi", true) -> Icons.Default.PlayArrow
                                localFile.name.endsWith(".pdf", true) || localFile.name.endsWith(".docx", true) || localFile.name.endsWith(".txt", true) -> Icons.Default.Description
                                else -> Icons.Default.InsertDriveFile
                            }
                            
                            val tint = when (icon) {
                                Icons.Default.Image -> AuroraViolet
                                Icons.Default.PlayArrow -> AuroraCyan
                                Icons.Default.Description -> AuroraTeal
                                else -> TextSecondary
                            }

                            Icon(icon, contentDescription = "Selected File", tint = tint, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(localFile.name, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${"%.2f".format(localFile.size / (1024.0 * 1024.0))} MB", color = TextSecondary, fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.removeFile(localFile.uri) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove file", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { navController.navigate("file_browser") },
                        colors = ButtonDefaults.buttonColors(containerColor = CoreDarkVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add More", color = AuroraCyan)
                    }
                    Button(
                        onClick = { viewModel.startSending() },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Init Transfer", color = CoreDeepSpace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (inviteCode != null && !stats.isComplete && stats.error == null && stats.progress == 0f && !stats.isWaitingForApproval) {
            // Coordinate Invitation Details Card for host
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(AuroraTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Router, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Secure Connection Handshake", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("Share the invite coordinates below or show pairing QR", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CoreDeepSpace, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(inviteCode!!, color = AuroraCyan, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
                        IconButton(onClick = { clipboard.setText(AnnotatedString(inviteCode!!)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Coordinates", tint = AuroraViolet)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPairQr = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = CoreDeepSpace)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pair via QR Code", color = CoreDeepSpace, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = AuroraTeal, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for receiver client connection...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            // Live transfer updates panel
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TransferProgressView(stats)
                    val isChatConnected by viewModel.isChatConnected.collectAsState()
                    if (isChatConnected) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ChatView(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReceiveScreen(navController: NavController, viewModel: PeerLinkViewModel) {
    var inputCode by remember { mutableStateOf("") }
    val stats by viewModel.stats.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()

    var showScannerDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose {
            viewModel.stopDiscovery()
        }
    }

    if (showScannerDialog) {
        SimulatedQrScanner(
            onScanned = { code ->
                inputCode = code
                showScannerDialog = false
                viewModel.startReceiving(code)
            },
            onCancel = { showScannerDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                viewModel.cancelTransfer()
                navController.popBackStack()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close Screen", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Receive Files", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (stats.progress == 0f && stats.error == null && !stats.isComplete && !stats.isConnecting && !stats.isWaitingForApproval) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Enter pairing coordinates from sender", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        label = { Text("6-Digit Coordinate Code", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuroraViolet,
                            unfocusedBorderColor = CoreDeepSpace,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = { showScannerDialog = true },
                            modifier = Modifier
                                .background(GlassLayer, RoundedCornerShape(12.dp))
                                .size(50.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan pairing QR Code", tint = AuroraViolet)
                        }
                        Button(
                            onClick = { viewModel.startReceiving(inputCode) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            enabled = inputCode.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Connect Sender", color = CoreDeepSpace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Auto-Discovery listing
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = CoreDarkVariant)
                Text(
                    text = "OR CHOOSE NEARBY",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = CoreDarkVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Discovered Senders", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                CircularProgressIndicator(color = AuroraTeal, modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp)
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (discoveredPeers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CoreDarkVariant)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Awaiting PeerLink broadcasts in local router subnet...",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(discoveredPeers) { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CoreDarkVariant)
                                .border(1.dp, AuroraTeal.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .clickable { viewModel.connectToDiscoveredPeer(peer) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuroraTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(peer.deviceName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("IP: ${peer.ip}", color = TextSecondary, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.connectToDiscoveredPeer(peer) },
                                colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Connect", fontSize = 11.sp, color = CoreDeepSpace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Live transfer progress updates screen
            Box(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TransferProgressView(stats)
                    val isChatConnected by viewModel.isChatConnected.collectAsState()
                    if (isChatConnected) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ChatView(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedQrScanner(onScanned: (String) -> Unit, onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    var manualCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = CoreDarkVariant,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Align Scanner Target", color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Place peer QR target in viewfinder frame", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .border(2.dp, AuroraTeal, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        // Drawing corner framing bounds
                        Box(modifier = Modifier.size(20.dp).background(Color.White).align(Alignment.TopStart))
                        Box(modifier = Modifier.size(20.dp).background(Color.White).align(Alignment.TopEnd))
                        Box(modifier = Modifier.size(20.dp).background(Color.White).align(Alignment.BottomStart))
                    }

                    // Sliding scanner laser scan-bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 180.dp * laserOffset)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, AuroraTeal, Color.Transparent)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("OR USE MANUAL OVERWRITE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("6-Digit Coordinate Code", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraViolet,
                        unfocusedBorderColor = CoreDeepSpace,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (manualCode.isNotBlank()) {
                        onScanned(manualCode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal, contentColor = CoreDeepSpace),
                enabled = manualCode.isNotBlank()
            ) {
                Text("Pair Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ShowPairingQrDialog(inviteCode: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CoreDarkVariant,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Scan to Pair Station", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Display this unique session QR to scanning device", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(150.dp)) {
                        val sqSize = 10f
                        val w = size.width
                        val h = size.height

                        // Position square corner coordinates
                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(35f, 35f))
                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(8f, 8f), size = androidx.compose.ui.geometry.Size(19f, 19f))

                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(w - 35f, 0f), size = androidx.compose.ui.geometry.Size(35f, 35f))
                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(w - 27f, 8f), size = androidx.compose.ui.geometry.Size(19f, 19f))

                        drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, h - 35f), size = androidx.compose.ui.geometry.Size(35f, 35f))
                        drawRect(Color.White, topLeft = androidx.compose.ui.geometry.Offset(8f, h - 27f), size = androidx.compose.ui.geometry.Size(19f, 19f))

                        // Seed matrix layout from the parsed coordinate key hash
                        val step = 10f
                        val r = java.util.Random(inviteCode.hashCode().toLong())
                        for (x in 0..14) {
                            for (y in 0..14) {
                                if ((x < 4 && y < 4) || (x > 10 && y < 4) || (x < 4 && y > 10)) continue
                                if (r.nextBoolean()) {
                                    drawRect(
                                        Color.Black,
                                        topLeft = androidx.compose.ui.geometry.Offset(x * step + 5f, y * step + 5f),
                                        size = androidx.compose.ui.geometry.Size(step, step)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pairing Coordinates Token:", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))

                val clipboard = LocalClipboardManager.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CoreDeepSpace, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(inviteCode, color = AuroraCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(inviteCode)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Token", tint = AuroraViolet, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal, contentColor = CoreDeepSpace)
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun HistoryScreen(viewModel: PeerLinkViewModel) {
    val historyList by viewModel.transferHistory.collectAsState()
    var filterMode by remember { mutableStateOf("ALL") } // "ALL", "SENT", "RECEIVED"

    val filteredList = remember(historyList, filterMode) {
        when (filterMode) {
            "SENT" -> historyList.filter { it.direction == "SEND" }
            "RECEIVED" -> historyList.filter { it.direction == "RECEIVE" }
            else -> historyList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Transfers Log", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AuroraViolet)
                Text("Historic connection audit record", fontSize = 12.sp, color = TextSecondary)
            }
            IconButton(onClick = { viewModel.clearHistory() }, enabled = historyList.isNotEmpty()) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Clear absolute audit logs history", tint = if (historyList.isNotEmpty()) Color.Red else TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips bar list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "SENT", "RECEIVED").forEach { mode ->
                val selected = filterMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) AuroraViolet else CoreDarkVariant)
                        .clickable { filterMode = mode }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = mode,
                        color = if (selected) CoreDeepSpace else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No historic transactions matched",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { item ->
                    val isSend = item.direction == "SEND"
                    val itemDate = remember(item.timestamp) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                        sdf.format(Date(item.timestamp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CoreDarkVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSend) AuroraTeal.copy(alpha = 0.15f) else AuroraViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSend) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                                contentDescription = if (isSend) "Sent File" else "Received File",
                                tint = if (isSend) AuroraTeal else AuroraViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "To: ${item.peerName} • ${itemDate}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${"%.2f".format(item.fileSize / (1024.0 * 1024.0))} MB",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (item.isSuccess) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (item.isSuccess) "Success" else "Failed",
                                    color = if (item.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: PeerLinkViewModel) {
    val name by viewModel.deviceName.collectAsState()
    val isVisible by viewModel.visibilityEnabled.collectAsState()
    val isAutoAccept by viewModel.autoAcceptEnabled.collectAsState()
    val localDir by viewModel.saveLocation.collectAsState()
    val localTheme by viewModel.themeMode.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf(name) }

    var showEditDirDialog by remember { mutableStateOf(false) }
    var inputDir by remember { mutableStateOf(localDir) }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = CoreDarkVariant,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Rename local identification station name", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Device Peer Name", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraCyan,
                        unfocusedBorderColor = CoreDeepSpace,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            viewModel.updateDeviceName(inputName)
                        }
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan, contentColor = CoreDeepSpace)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showEditDirDialog) {
        AlertDialog(
            onDismissRequest = { showEditDirDialog = false },
            containerColor = CoreDarkVariant,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Edit target save directory", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = inputDir,
                    onValueChange = { inputDir = it },
                    label = { Text("Local Folder Path", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuroraCyan,
                        unfocusedBorderColor = CoreDeepSpace,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputDir.isNotBlank()) {
                            viewModel.updateSaveLocation(inputDir)
                        }
                        showEditDirDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraCyan, contentColor = CoreDeepSpace)
                ) {
                    Text("Update Path")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDirDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Preferences", fontSize = 28.sp, fontWeight = FontWeight.Black, color = AuroraCyan)
        Text("LocalSend operational defaults configuration", fontSize = 12.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(20.dp))

        // Device preferences section
        Text("STATION IDENTITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuroraCyan)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            inputName = name
                            showEditNameDialog = true
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Device Name", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(name, color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
                HorizontalDivider(color = CoreDeepSpace, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Discovery", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Advertise discoverability on LAN", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { viewModel.updateVisibility(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CoreDeepSpace,
                            checkedTrackColor = AuroraCyan
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Transfer preferences section
        Text("OPERATIONAL LAUNCH PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuroraCyan)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Accept Handshakes", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Authorize incoming file bundle queries directly", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isAutoAccept,
                        onCheckedChange = { viewModel.updateAutoAccept(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CoreDeepSpace,
                            checkedTrackColor = AuroraCyan
                        )
                    )
                }
                HorizontalDivider(color = CoreDeepSpace, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            inputDir = localDir
                            showEditDirDialog = true
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Save Directory", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(localDir, color = TextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual layout preferences section
        Text("VISUAL RENDERING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuroraCyan)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Theme Palette Style", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(localTheme, color = TextSecondary, fontSize = 12.sp)
                }
                Text("Midnight Space", color = AuroraTeal, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ChatView(viewModel: PeerLinkViewModel) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    var textMessage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        Text("Real-Time Connection Chat", color = AuroraTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CoreDeepSpace, RoundedCornerShape(12.dp))
                .padding(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                val align = if (msg.isMe) Alignment.End else Alignment.Start
                val color = if (msg.isMe) AuroraTeal else AuroraViolet
                val textColor = if (msg.isMe) CoreDeepSpace else TextPrimary

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = align) {
                    Box(modifier = Modifier.background(color, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(
                            text = if (msg.isFileCommand) "Sent File: ${msg.fileName}" else msg.text,
                            color = textColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    viewModel.sendFileViaChat(context, listOf(uri))
                }
            }
            IconButton(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier
                    .background(CoreDarkVariant, RoundedCornerShape(50))
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Attach File Bundle", tint = AuroraViolet)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = textMessage,
                onValueChange = { textMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type coordinates or message...", color = TextSecondary, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuroraCyan,
                    unfocusedBorderColor = CoreDarkVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textMessage.isNotBlank()) {
                        viewModel.sendChatMessage(textMessage)
                        textMessage = ""
                    }
                },
                modifier = Modifier
                    .background(AuroraTeal, RoundedCornerShape(50))
                    .size(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send text", tint = CoreDeepSpace, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TransferProgressView(stats: com.example.domain.TransferStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CoreDarkVariant, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (stats.error != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Error icon indicator", tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Transfer Failed", color = Color(0xFFEF4444), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(stats.error, color = TextSecondary, textAlign = TextAlign.Center, fontSize = 12.sp)
        } else if (stats.isComplete) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AuroraTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Success tick icon", tint = AuroraTeal, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Transfer Complete", color = AuroraTeal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (stats.currentFileName != null) {
                Spacer(Modifier.height(6.dp))
                Text("Saved to Downloads/PeerLink/${stats.currentFileName}", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        } else if (stats.isWaitingForApproval) {
            CircularProgressIndicator(color = AuroraViolet, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Approving Handshake Connection...", color = TextSecondary, fontSize = 13.sp)
        } else {
            Text(stats.currentFileName ?: "Establishing LAN link...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { stats.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AuroraCyan,
                trackColor = CoreDeepSpace,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(stats.progress * 100).toInt()}%", color = AuroraTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("%.1f MB/s".format(stats.speedMBps), color = AuroraViolet, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
