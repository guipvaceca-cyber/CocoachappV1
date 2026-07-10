package com.example.coachapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    isLoggingIn: Boolean = false,
    errorMessage: String? = null,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onDismissError: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CoCoach",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = (-2).sp
        )
        Text(
            text = if (isSignUpMode) "Créer un compte coach" else "Espace Entraîneur",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissError) {
                        Text("OK", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoggingIn
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoggingIn
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoggingIn) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { 
                    if (isSignUpMode) onSignUp(email, password)
                    else onLogin(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = true // On débloque pour permettre à Supabase de renvoyer l'erreur spécifique
            ) {
                Text(if (isSignUpMode) "S'inscrire" else "Se connecter")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                Text(
                    if (isSignUpMode) "Déjà un compte ? Se connecter" 
                    else "Nouveau ici ? Créer un compte"
                )
            }
        }
    }
}
