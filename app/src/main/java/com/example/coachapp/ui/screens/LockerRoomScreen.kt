package com.example.coachapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LockerRoomScreen(
    modifier: Modifier = Modifier,
    seasonConfig: SeasonConfig,
    posts: List<AnonymousPost>,
    userRole: UserRole = UserRole.USER,
    onPost: (String, String, PostCategory, String?, Boolean) -> Unit,
    onDeletePost: (String) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<PostCategory?>(null) }
    var showCreatePost by remember { mutableStateOf(false) }
    var selectedPostForThread by remember { mutableStateOf<AnonymousPost?>(null) }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun CategoryFilters() {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Tous") },
                leadingIcon = { Icon(Icons.Default.Apps, null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00B4D8),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    labelColor = Color.White.copy(alpha = 0.6f),
                    iconColor = Color.White.copy(alpha = 0.4f)
                )
            )
            PostCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.label) },
                    leadingIcon = { 
                        Icon(getCategoryIcon(cat), null, modifier = Modifier.size(16.dp)) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00B4D8),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        labelColor = Color.White.copy(alpha = 0.6f),
                        iconColor = Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Let parent handle background
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (selectedPostForThread == null) {
                FloatingActionButton(
                    onClick = { showCreatePost = true },
                    containerColor = Color(0xFF00B4D8),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "Poster")
                }
            }
        }
    ) { innerPadding ->
        if (selectedPostForThread != null) {
            ThreadView(
                post = selectedPostForThread!!,
                persona = seasonConfig.coachProfile.coachPersona.ifEmpty { "Coach Anonyme" },
                onBack = { selectedPostForThread = null },
                onAddComment = { updatedPost ->
                    // For now, comments remain local or handled via a separate logic
                    selectedPostForThread = updatedPost
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
                    .fillMaxSize()
            ) {
                // Header (Fixed)
                Text(
                    "Vestiaire Anonyme", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    "Doutes, conseils et réussites partagés.", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = Color.White.copy(alpha = 0.6f)
                )
                
                Spacer(Modifier.height(16.dp))

                // --- CATEGORY FILTERS SECTION (1/3 of available space) ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    CategoryFilters()
                }

                Spacer(Modifier.height(16.dp))

                // --- FEED SECTION (2/3 of available space) ---
                val filteredPosts = remember(selectedCategory, posts) {
                    if (selectedCategory == null) posts else posts.filter { it.category == selectedCategory }
                }

                Box(modifier = Modifier.weight(2.0f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPosts, key = { it.id }) { post ->
                            PostCard(
                                post = post, 
                                userRole = userRole,
                                onClick = { selectedPostForThread = post },
                                onDelete = { onDeletePost(post.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (showCreatePost) {
        CreatePostDialog(
            userRole = userRole,
            onDismiss = { showCreatePost = false },
            onPost = { post, isOfficial ->
                onPost(post.title ?: "", post.content ?: "", post.category, post.persona, isOfficial)
                showCreatePost = false
            }
        )
    }
}

@Composable
fun PostCard(
    post: AnonymousPost, 
    userRole: UserRole = UserRole.USER,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val isSOS = post.category == PostCategory.SOS
    val isExpert = post.isFromExpert

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSOS) Color.Red.copy(alpha = 0.15f) 
                            else if (isExpert) Color(0xFF00B4D8).copy(alpha = 0.15f) 
                            else Color.White.copy(alpha = 0.06f)
        ),
        border = BorderStroke(
            0.5.dp, 
            if (isSOS) Color.Red.copy(alpha = 0.4f) 
            else if (isExpert) Color(0xFF00B4D8).copy(alpha = 0.4f) 
            else Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Persona Avatar
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = if (isExpert) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, if (isExpert) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpert) Icons.Default.Verified else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isExpert) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    val expertLabel = when (post.authorRole) {
                        "megadmin" -> "Expert Comité CD26-07"
                        "admin" -> "Modérateur Espace Coachs"
                        else -> "Expert Certifié"
                    }
                    
                    Text(
                        text = if (isExpert) expertLabel else post.persona ?: "Anonyme",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isExpert) Color(0xFF00B4D8) else Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = SimpleDateFormat("HH:mm · dd MMM", Locale.FRENCH).format(Date(post.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        if (post.isAnonymizedByIA) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(12.dp), tint = Color(0xFF00B4D8).copy(alpha = 0.6f))
                            Text("Sécurisé IA", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = Color(0xFF00B4D8).copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                
                if (userRole == UserRole.ADMIN || userRole == UserRole.MEGADMIN) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Surface(
                    color = if (isSOS) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, if (isSOS) Color.Red.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = post.category.label.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSOS) Color.Red else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                post.title ?: "Sujet", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                post.content ?: "", 
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.clickable { /* Logic support */ },
                    color = Color(0xFF00B4D8).copy(alpha = 0.1f),
                    shape = CircleShape,
                    border = BorderStroke(0.5.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(14.dp), tint = Color(0xFF00B4D8))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Je partage ce doute (${post.supportCount})", 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = Color(0xFF00B4D8)
                        )
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Comment, null, modifier = Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.3f))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${post.comments.size} réponses", 
                        fontSize = 12.sp, 
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ThreadView(
    post: AnonymousPost,
    persona: String,
    onBack: () -> Unit,
    onAddComment: (AnonymousPost) -> Unit,
    modifier: Modifier = Modifier
) {
    var newCommentText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) 
            }
            Text("Discussion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                PostCard(post, onClick = {}) // Current post as header
                Spacer(Modifier.height(32.dp))
                Text(
                    "RÉPONSES", 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(16.dp))
            }

            if (post.comments.isEmpty()) {
                item {
                    Text(
                        "Aucun conseil pour le moment. Soyez le premier à aider ce coach !", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.White.copy(alpha = 0.4f), 
                        modifier = Modifier.padding(32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(post.comments, key = { it.timestamp }) { comment ->
                    CommentCard(comment)
                }
            }
        }

        // --- ADD COMMENT AREA ---
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Votre conseil anonyme...", fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val newComment = AnonymousComment(persona = persona, content = newCommentText)
                        onAddComment(post.copy(comments = post.comments + newComment))
                        newCommentText = ""
                    },
                    enabled = newCommentText.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF00B4D8), 
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                }
            }
        }
    }
}

@Composable
fun CommentCard(comment: AnonymousComment) {
    var score by remember { mutableIntStateOf(0) }
    var userVote by remember { mutableIntStateOf(0) } // 1 for up, -1 for down

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Vote side bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { 
                        if (userVote == 1) { score -= 1; userVote = 0 }
                        else { score += if (userVote == -1) 2 else 1; userVote = 1 }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, null, tint = if (userVote == 1) Color(0xFF00B4D8) else Color.White.copy(alpha = 0.3f))
                }
                Text("${score}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                IconButton(
                    onClick = { 
                        if (userVote == -1) { score += 1; userVote = 0 }
                        else { score -= if (userVote == 1) 2 else 1; userVote = -1 }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, null, tint = if (userVote == -1) Color.Red else Color.White.copy(alpha = 0.3f))
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.persona, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date(comment.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                
                // Moderation placeholder
                TextButton(
                    onClick = { /* Report */ },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(32.dp).padding(top = 4.dp)
                ) {
                    Text("Signaler", color = Color.Red.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    userRole: UserRole,
    onDismiss: () -> Unit,
    onPost: (AnonymousPost, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PostCategory.TACTIC) }
    var isOfficial by remember { mutableStateOf(false) }
    var selectedAlias by remember { mutableStateOf(COACH_ALIASES.random()) }
    
    var isAnonymizing by remember { mutableStateOf(false) }
    var anonymizedContent by remember { mutableStateOf<String?>(null) }

    if (isAnonymizing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(16.dp))
                Text(if (isOfficial) "Vérification pro..." else "Anonymisation IA...")
            }},
            text = { Text(if (isOfficial) "L'IA vérifie le ton institutionnel du message." else "L'IA neutralise votre style de rédaction pour garantir votre anonymat total.") },
            confirmButton = {}
        )
        
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            anonymizedContent = processAnonymization(content)
            isAnonymizing = false
        }
    } else if (anonymizedContent != null) {
        AlertDialog(
            onDismissRequest = { anonymizedContent = null },
            title = { Text(if (isOfficial) "Validation du message officiel" else "Validation de la neutralité") },
            text = {
                Column {
                    Text("Version originale :", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(content, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Text(if (isOfficial) "Version finale (Expert) :" else "Version sécurisée par IA :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))) {
                        Text(anonymizedContent!!, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onPost(AnonymousPost(
                        persona = if (isOfficial) "Expert Comité CD26-07" else selectedAlias,
                        clubInitial = if (isOfficial) "CTD" else "CD",
                        category = category,
                        title = title,
                        content = anonymizedContent!!,
                        isAnonymizedByIA = !isOfficial
                    ), isOfficial)
                }) { Text("Confirmer l'envoi") }
            },
            dismissButton = {
                TextButton(onClick = { anonymizedContent = null }) { Text("Modifier") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF002147).copy(alpha = 0.98f),
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            title = { 
                Text(
                    "NOUVEAU SUJET ANONYME", 
                    fontWeight = FontWeight.Black, 
                    color = Color.White,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                ) 
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    
                    // --- SELECTEUR DE POSTURE (ADMIN ONLY) ---
                    if (userRole == UserRole.ADMIN || userRole == UserRole.MEGADMIN) {
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Mode Officiel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Publier en tant qu'Expert", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                                }
                                Switch(
                                    checked = isOfficial, 
                                    onCheckedChange = { isOfficial = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00B4D8))
                                )
                            }
                        }
                    }

                    // --- IDENTITY SELECTION ---
                    if (isOfficial) {
                        Surface(
                            color = Color(0xFF00B4D8).copy(alpha = 0.15f), 
                            shape = RoundedCornerShape(12.dp), 
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Vous postez en tant qu'Expert", fontWeight = FontWeight.Black, color = Color(0xFF00B4D8), fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text("VOTRE ALIAS POUR CE POST", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        var expanded by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Surface(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedAlias, modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                            DropdownMenu(
                                expanded = expanded, 
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color(0xFF001529)).border(0.5.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                COACH_ALIASES.forEach { alias ->
                                    DropdownMenuItem(
                                        text = { Text(alias, color = Color.White) },
                                        onClick = { selectedAlias = alias; expanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("THÉMATIQUE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PostCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label, fontSize = 11.sp) },
                                leadingIcon = { 
                                    Icon(getCategoryIcon(cat), null, modifier = Modifier.size(16.dp)) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00B4D8),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    labelColor = Color.White.copy(alpha = 0.6f),
                                    iconColor = Color.White.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = title, 
                        onValueChange = { title = it }, 
                        label = { Text("SUJET / TITRE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00B4D8),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = content, 
                        onValueChange = { content = it }, 
                        label = { Text(if (isOfficial) "MESSAGE OFFICIEL" else "VOTRE MESSAGE (STYLE LIBRE)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }, 
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(if (isOfficial) "Soyez clair et professionnel." else "L'IA réécrira ce message pour vous anonymiser.", color = Color.White.copy(alpha = 0.2f), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color(0xFF00B4D8),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { isAnonymizing = true },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(if (isOfficial) Icons.AutoMirrored.Filled.Send else Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isOfficial) "PUBLIER" else "SÉCURISER & POSTER", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { 
                TextButton(onClick = onDismiss) { 
                    Text("ANNULER", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) 
                } 
            }
        )
    }
}

fun getCategoryIcon(category: PostCategory): ImageVector {
    return when (category) {
        PostCategory.TACTIC -> Icons.Default.GridView
        PostCategory.MENTAL -> Icons.Default.Psychology
        PostCategory.MANAGEMENT -> Icons.Default.Groups
        PostCategory.EQUIPMENT -> Icons.Default.SportsVolleyball
        PostCategory.SUCCESS -> Icons.Default.Celebration
        PostCategory.SOS -> Icons.Default.Report
    }
}

/**
 * SIMULATED IA NEUTRALIZATION ENGINE
 * In a real app, this would call a Gemini / OpenAI API.
 */
fun processAnonymization(input: String): String {
    // Basic logic to demonstrate the transformation
    var text = input
    
    // 1. Remove excessive punctuation
    text = text.replace(Regex("[!]{2,}"), ".")
    text = text.replace(Regex("[?]{2,}"), "?")
    
    // 2. Formalize tone
    text = text.replace("Slt", "Bonjour")
    text = text.replace("j'en ai marre", "Je rencontre une difficulté")
    text = text.replace("font n'importe quoi", "manquent de discipline")
    text = text.replace("trop nul", "peu performant")
    
    // 3. Flatten person markers
    text = text.replace(Regex("mon équipe", RegexOption.IGNORE_CASE), "le collectif")
    text = text.replace(Regex("mes joueurs", RegexOption.IGNORE_CASE), "les pratiquants")
    
    // 4. Wrap with a professional intro if not already formal
    if (!text.startsWith("Bonjour") && !text.contains("Question")) {
        text = "Problématique soumise : " + text.replaceFirstChar { it.lowercase() }
    }
    
    return text
}
