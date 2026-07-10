package com.example.coachapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedPostForThread == null) {
                FloatingActionButton(
                    onClick = { showCreatePost = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
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
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                Text("Vestiaire Anonyme", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Section anonymisée : doutes, conseils et réussites.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                Spacer(Modifier.height(16.dp))

                // --- CATEGORY FILTERS ---
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Tous") }
                    )
                    PostCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.icon} ${cat.label}") }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- FEED ---
                val filteredPosts = remember(selectedCategory, posts) {
                    if (selectedCategory == null) posts else posts.filter { it.category == selectedCategory }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredPosts) { post ->
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSOS) Color(0xFFFFF1F0) else if (isExpert) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSOS) androidx.compose.foundation.BorderStroke(2.dp, Color.Red) 
                 else if (isExpert) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                 else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Persona Avatar
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = if (isExpert) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpert) Icons.Default.Verified else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isExpert) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    val expertLabel = when (post.authorRole) {
                        "megadmin" -> "Expert Comité CD26-07 (Megadmin)"
                        "admin" -> "Modérateur Espace Coachs"
                        else -> "Expert Certifié"
                    }
                    
                    Text(
                        text = if (isExpert) expertLabel else post.persona ?: "Anonyme",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpert) MaterialTheme.colorScheme.primary else Color.Unspecified
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = SimpleDateFormat("HH:mm - dd MMM", Locale.FRENCH).format(Date(post.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        if (post.isAnonymizedByIA) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text("Anonymisé par IA", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                
                if (userRole == UserRole.ADMIN || userRole == UserRole.MEGADMIN) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Surface(
                    color = Color.Gray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = post.category.label,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(post.title ?: "Sujet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(post.content ?: "", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Je partage ce doute (${post.supportCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(Modifier.weight(1f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Comment, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("${post.comments.size} réponses", fontSize = 11.sp, color = Color.Gray)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Discussion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                PostCard(post, onClick = {}) // Current post as header
                Spacer(Modifier.height(24.dp))
                Text("RÉPONSES", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
            }

            if (post.comments.isEmpty()) {
                item {
                    Text("Aucun conseil pour le moment. Soyez le premier à aider ce coach !", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            } else {
                items(post.comments) { comment ->
                    CommentCard(comment)
                }
            }
        }

        // --- ADD COMMENT AREA ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                placeholder = { Text("Votre conseil anonyme...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
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
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
            }
        }
    }
}

@Composable
fun CommentCard(comment: AnonymousComment) {
    var score by remember { mutableIntStateOf(0) }
    var userVote by remember { mutableIntStateOf(0) } // 1 for up, -1 for down

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Vote side bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { 
                        if (userVote == 1) { score -= 1; userVote = 0 }
                        else { score += if (userVote == -1) 2 else 1; userVote = 1 }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, null, tint = if (userVote == 1) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                Text("${score}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                IconButton(
                    onClick = { 
                        if (userVote == -1) { score += 1; userVote = 0 }
                        else { score -= if (userVote == 1) 2 else 1; userVote = -1 }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, null, tint = if (userVote == -1) Color.Red else Color.Gray)
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.persona, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        SimpleDateFormat("HH:mm", Locale.FRENCH).format(Date(comment.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                
                // Moderation placeholder
                TextButton(
                    onClick = { /* Report */ },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Signaler", color = Color.Red.copy(alpha = 0.6f), fontSize = 10.sp)
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
            title = { Text("Nouveau sujet anonyme") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    
                    // --- SELECTEUR DE POSTURE (ADMIN ONLY) ---
                    if (userRole == UserRole.ADMIN || userRole == UserRole.MEGADMIN) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mode Officiel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Switch(checked = isOfficial, onCheckedChange = { isOfficial = it })
                        }
                    }

                    // --- IDENTITY SELECTION ---
                    if (isOfficial) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Vous postez en tant qu'Expert", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Text("Votre alias pour ce post :", style = MaterialTheme.typography.labelSmall)
                        var expanded by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            OutlinedCard(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedAlias, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                COACH_ALIASES.forEach { alias ->
                                    DropdownMenuItem(
                                        text = { Text(alias) },
                                        onClick = { selectedAlias = alias; expanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Thématique :", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), 
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PostCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Sujet / Titre") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content, 
                        onValueChange = { content = it }, 
                        label = { Text(if (isOfficial) "Message Officiel" else "Votre message (style libre)") }, 
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        placeholder = { Text(if (isOfficial) "Soyez clair et professionnel." else "L'IA réécrira ce message pour vous anonymiser.") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { isAnonymizing = true },
                    enabled = title.isNotBlank() && content.isNotBlank()
                ) {
                    Icon(if (isOfficial) Icons.AutoMirrored.Filled.Send else Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isOfficial) "Publier" else "Sécuriser & Poster")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
        )
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
