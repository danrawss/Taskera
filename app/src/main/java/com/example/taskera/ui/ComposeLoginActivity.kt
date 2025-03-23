package com.example.taskera.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.taskera.R
import com.example.taskera.ui.MainActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class ComposeLoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    // NEW: registerForActivityResult replaces onActivityResult
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // This block is called when the sign-in Intent returns
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        } else {
            Log.e("ComposeLoginActivity", "Sign-in canceled or failed, resultCode = ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Google Sign-In (requesting only the user's email)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if the user is already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // Already signed in, go straight to MainActivity
            goToMainActivity()
        } else {
            // Show the Compose login screen
            setContent {
                LoginScreen(onGoogleSignInClick = { signIn() })
            }
        }
    }

    private fun signIn() {
        // Use the new Activity Result API
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d("ComposeLoginActivity", "Sign-in success, email: ${account.email}")
                goToMainActivity()
            } else {
                Log.e("ComposeLoginActivity", "Sign-in returned null account")
            }
        } catch (e: ApiException) {
            Log.e("ComposeLoginActivity", "Sign-in failed: ${e.statusCode} ${e.message}", e)
        }
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
