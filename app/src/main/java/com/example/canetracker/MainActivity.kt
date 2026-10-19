package com.example.canetracker

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.canetracker.ui.AuthScreen
import com.example.canetracker.ui.theme.CaneTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: WaterDeliveryViewModel = viewModel()
            val darkTheme = viewModel.isDarkTheme(isSystemInDarkTheme())
            
            CaneTrackerTheme(darkTheme = darkTheme) {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    WaterDeliveryApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WaterDeliveryApp(viewModel: WaterDeliveryViewModel) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    if (viewModel.currentUser == null && !viewModel.isGuest) {
        AuthScreen(
            onSignInWithGoogle = { 
                val credentialManager = CredentialManager.create(context)
                
                // IMPORTANT: Replace the placeholder with your Web Client ID from Firebase Console
                val webClientId = "1026985176674-718vf3pp975bnee9597pgb0d7np3k4or.apps.googleusercontent.com"
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                scope.launch {
                    try {
                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential
                        if (credential is GoogleIdTokenCredential) {
                            viewModel.signInWithGoogle(credential.idToken)
                        }
                    } catch (e: GetCredentialException) {
                        Log.e("Auth", "Sign-in failed", e)
                        Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onContinueAsGuest = { viewModel.signInAsGuest() }
        )
    } else if (viewModel.showSplash) {
        SplashScreen()
    } else {
        HomeScreen(viewModel = viewModel)
    }
}
