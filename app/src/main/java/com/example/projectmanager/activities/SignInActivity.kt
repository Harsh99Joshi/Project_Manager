package com.example.projectmanager.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.example.projectmanager.R
import com.example.projectmanager.databinding.ActivitySignInBinding
import com.example.projectmanager.models.User
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        binding = ActivitySignInBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupActionBar()

        auth = FirebaseAuth.getInstance()

        binding.btnSignIn.setOnClickListener {
            signInRegisteredUser()
        }
    }

    private fun setupActionBar(){
        setSupportActionBar(binding.toolbarSignInActivity)
        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_back_24dp)
        }
        binding.toolbarSignInActivity.setNavigationOnClickListener {
            onBackPressed()
        }
    }


    private fun signInRegisteredUser(){
        val email: String = binding.etEmailSignin.text.toString().trim { it<= ' '}
        val password: String = binding.etPasswordSignin.text.toString()

        if(validateForm(email, password)){
            showProgressDialog(resources.getString(R.string.please_wait))
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        hideProgressDialog()
                        if(task.isSuccessful){
                            Log.d("Sign In","signInWithEmail:Success")
                            val user = auth.currentUser
                            startActivity(Intent(this, MainActivity::class.java))
                        }else{
                            Log.w("Sign in","Failed to sign in", task.exception)
                            Toast.makeText(baseContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                        }
                    }
        }
    }


    private fun validateForm(email: String, password: String) : Boolean{
        return when{
            TextUtils.isEmpty(email) ->
            { showErrorSnackBar("Please enter a email address")
                false
            }

            TextUtils.isEmpty(password) ->
            { showErrorSnackBar("Please enter a password")
                false
            }
            else -> {
                true

            }
        }
    }


    fun signInSuccess(user: User){
        hideProgressDialog()
        startActivity(Intent(this, MainActivity::class.java))
        finish()

    }
}