package com.example.project5.data.cloud

import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthManager {
    private val auth = FirebaseAuth.getInstance()

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                onResult(it.isSuccessful, it.exception?.message)
            }
    }

    fun getUserId(): String? = auth.currentUser?.uid

    fun logout() {
        auth.signOut()
    }

}