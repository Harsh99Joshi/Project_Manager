package com.example.projectmanager.activities

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.example.projectmanager.R
import com.example.projectmanager.databinding.ActivitySignUpBinding
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : BaseActivity() {
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
       binding = ActivitySignUpBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupActionBar()
    }

    private fun setupActionBar(){
        setSupportActionBar(binding.toolbarSignUpActivity)
        val actionBar = supportActionBar
        if(actionBar!=null){
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_black_back_24dp)
        }
        binding.toolbarSignUpActivity.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnSignUp.setOnClickListener {
            registerUser()
        }
    }



    private fun registerUser(){
        val name : String = binding.etName.text.toString()
        val email : String = binding.etEmail.text.toString()
        val password : String = binding.etPassword.text.toString()

        if(validateForm(name, email, password)){
            showProgressDialog("please wait...")
            FirebaseAuth.getInstance().
            createUserWithEmailAndPassword(email,password).addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    val firebaseUser: FirebaseUser = task.result!!.user!!
                    val registeredEmail = firebaseUser.email!!
                    val user = User(firebaseUser.uid,name,registeredEmail)
                    FirestoreClass().registerUser(this,user)
                } else {
                    hideProgressDialog()
                    Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun validateForm(name: String, email: String, password: String) : Boolean{
        return when{
            TextUtils.isEmpty(name) ->
            { showErrorSnackBar("Please enter a name")
            false
        }
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


    fun userRegisteredSuccess(){

        Toast.makeText(this, "User registered", Toast.LENGTH_SHORT).show()
        hideProgressDialog()
        FirebaseAuth.getInstance().signOut()
        finish()

    }
}