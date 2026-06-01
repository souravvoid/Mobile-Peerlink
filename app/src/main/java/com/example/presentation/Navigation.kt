package com.example.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.*
import com.example.network.DiscoveredPeer

@Composable
fun MainApp(viewModel: PeerLinkViewModel) {
    val navController = rememberNavController()
    
    val fingerprint by viewModel.fingerprintToApprove.collectAsState()
    val stats by viewModel.stats.collectAsState()
    
    if (fingerprint != null) {
        AlertDialog(
            onDismissRequest = { viewModel.answerApproval(false) },
            containerColor = CoreDarkVariant,
            title = { Text("Approve Connection", color = TextPrimary) },
            text = {
                Column {
                    val metadata = stats.metadata
                    if (metadata != null) {
                        Text("Incoming Transfer:", color = AuroraTeal, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                            items(metadata.files) { file ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = AuroraViolet, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(file.fileName, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Size: ${metadata.totalSize / 1024 / 1024} MB", color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text("Verify peer fingerprint:", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = fingerprint!!,
                        color = AuroraTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().background(DarkGlass).padding(16.dp).clip(RoundedCornerShape(8.dp))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.answerApproval(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal, contentColor = CoreDeepSpace)
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.answerApproval(false) }) {
                    Text("Reject", color = Color.Red)
                }
            }
        )
    }

    Scaffold(
        containerColor = CoreDeepSpace,
        contentColor = TextPrimary
    ) { padding ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(navController, viewModel) }
            composable("send") { SendScreen(navController, viewModel) }
            composable("receive") { ReceiveScreen(navController, viewModel) }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController, viewModel: PeerLinkViewModel) {
    val ip by viewModel.ipAddress.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PeerLink", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = AuroraTeal)
        Text("Secure Local Transfers", fontSize = 16.sp, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CoreDarkVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your Local IP", color = TextSecondary, fontSize = 14.sp)
                Text(ip, color = AuroraViolet, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { navController.navigate("send") },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = CoreDeepSpace)
                Spacer(Modifier.width(8.dp))
                Text("Send", color = CoreDeepSpace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = { navController.navigate("receive") },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Receive", tint = CoreDeepSpace)
                Spacer(Modifier.width(8.dp))
                Text("Receive", color = CoreDeepSpace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
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
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(context, uris)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = { 
            viewModel.cancelTransfer()
            navController.popBackStack() 
        }) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Send File", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AuroraTeal)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (inviteCode == null && stats.progress == 0f && stats.error == null && !stats.isComplete && !stats.isConnecting && !stats.isWaitingForApproval) {
            if (selectedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassLayer)
                        .border(2.dp, AuroraTeal, RoundedCornerShape(24.dp))
                        .clickable { launcher.launch(arrayOf("*/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AuroraTeal, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Tap to select files", color = TextPrimary)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(selectedFiles) { localFile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(GlassLayer, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Basic Icon
                            Icon(Icons.Default.Info, contentDescription = "File", tint = AuroraViolet, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(localFile.name, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${localFile.size / 1024} KB", color = TextSecondary, fontSize = 12.sp)
                            }
                            IconButton(onClick = { viewModel.removeFile(localFile.uri) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = { launcher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = CoreDarkVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add More", color = AuroraCyan)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { viewModel.startSending() },
                        colors = ButtonDefaults.buttonColors(containerColor = AuroraTeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send", color = CoreDeepSpace)
                    }
                }
            }
        } else if (inviteCode != null && !stats.isComplete && stats.error == null && stats.progress == 0f && !stats.isWaitingForApproval) {
            Text("Share this invite code with the receiver:", color = TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(CoreDarkVariant, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(inviteCode!!, color = AuroraCyan, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1)
                IconButton(onClick = { clipboard.setText(AnnotatedString(inviteCode!!)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AuroraViolet)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = AuroraTeal, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Waiting for receiver to connect...", color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            TransferProgressView(stats)
            val isChatConnected by viewModel.isChatConnected.collectAsState()
            if (isChatConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                ChatView(viewModel)
            }
        }
    }
}

@Composable
fun ReceiveScreen(navController: NavController, viewModel: PeerLinkViewModel) {
    var inputCode by remember { mutableStateOf("") }
    val stats by viewModel.stats.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    
    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose {
            viewModel.stopDiscovery()
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = { 
            viewModel.cancelTransfer()
            navController.popBackStack() 
        }) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Receive File", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AuroraViolet)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (stats.progress == 0f && stats.error == null && !stats.isComplete && !stats.isConnecting && !stats.isWaitingForApproval) {
            Text("Enter invite code manually:", color = TextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputCode,
                onValueChange = { inputCode = it },
                label = { Text("Invite Code", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuroraViolet,
                    unfocusedBorderColor = CoreDarkVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.startReceiving(inputCode) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = inputCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AuroraViolet)
            ) {
                Text("Connect Manually", color = CoreDeepSpace, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Nearby Discovery Divider ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = CoreDarkVariant)
                Text(
                    text = "OR CONNECT DIRECTLY",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = CoreDarkVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Nearby Senders Section ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nearby Senders", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AuroraTeal)
                CircularProgressIndicator(
                    color = AuroraTeal,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.5.dp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (discoveredPeers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassLayer)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Scanning Info Icon",
                            tint = AuroraTeal,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Scanning for active PeerLink senders on the same network...",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(discoveredPeers) { peer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassLayer)
                                .border(1.dp, AuroraTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
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
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Peer Device Icon",
                                    tint = AuroraTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peer.deviceName,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "IP: ${peer.ip}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
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
        } else {
            TransferProgressView(stats)
            val isChatConnected by viewModel.isChatConnected.collectAsState()
            if (isChatConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                ChatView(viewModel)
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
        Text("Real-Time Chat", color = AuroraTeal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().background(CoreDarkVariant, RoundedCornerShape(12.dp)).padding(8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                val align = if (msg.isMe) Alignment.End else Alignment.Start
                val color = if (msg.isMe) AuroraTeal else AuroraViolet
                val textColor = if (msg.isMe) CoreDeepSpace else TextPrimary
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = align) {
                    Box(modifier = Modifier.background(color, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(
                            text = if (msg.isFileCommand) "Sent File: ${msg.fileName}" else msg.text,
                            color = textColor
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
                modifier = Modifier.background(CoreDarkVariant, RoundedCornerShape(50)).size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Attach File", tint = AuroraViolet)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = textMessage,
                onValueChange = { textMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuroraCyan,
                    unfocusedBorderColor = CoreDarkVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { 
                    if (textMessage.isNotBlank()) {
                        viewModel.sendChatMessage(textMessage)
                        textMessage = ""
                    }
                },
                modifier = Modifier.background(AuroraTeal, RoundedCornerShape(50)).size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = CoreDeepSpace)
            }
        }
    }
}

@Composable
fun TransferProgressView(stats: com.example.domain.TransferStats) {
    Column(
        modifier = Modifier.fillMaxWidth().background(CoreDarkVariant, RoundedCornerShape(24.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (stats.error != null) {
            Icon(Icons.Default.Close, contentDescription = "Error", tint = Pink80, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Transfer Failed", color = Pink80, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stats.error, color = TextSecondary, textAlign = TextAlign.Center)
        } else if (stats.isComplete) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = AuroraTeal, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Transfer Complete", color = AuroraTeal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (stats.currentFileName != null) {
                Spacer(Modifier.height(8.dp))
                Text("Saved to Downloads/PeerLink/${stats.currentFileName}", color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else if (stats.isWaitingForApproval) {
            CircularProgressIndicator(color = AuroraViolet, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Approving Handshake...", color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text(stats.currentFileName ?: "Connecting...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { stats.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = AuroraCyan,
                trackColor = CoreDeepSpace,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(stats.progress * 100).toInt()}%", color = AuroraTeal)
                Text("%.1f MB/s".format(stats.speedMBps), color = AuroraViolet)
            }
        }
    }
}
