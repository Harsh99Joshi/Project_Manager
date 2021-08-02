package com.example.projectmanager.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.adapters.BoardItemsAdapter
import com.example.projectmanager.databinding.ActivityMainBinding
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.Board
import com.example.projectmanager.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.installations.FirebaseInstallations

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var profileActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var reloadBoard:ActivityResultLauncher<Intent>
    private lateinit var mUserName: String
    private lateinit var mSharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
       binding = ActivityMainBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupActionBar()
        binding.navView.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(Constants.PROJECT_MANAGER_PREFERENCES, Context.MODE_PRIVATE)
        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)


        if(tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this, true)
        }else{
            FirebaseInstallations.getInstance().getToken(true).addOnCompleteListener{ task ->
                if (task.isSuccessful)
                    updateFCMToken(task.result!!.token)
            }
        }


        FirestoreClass().loadUserData(this)
        registerOnActivityForResult()


        findViewById<FloatingActionButton>(R.id.fab_create_board).setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)

            reloadBoard.launch(intent)
        }

    }

    private fun setupActionBar(){
        setSupportActionBar(findViewById(R.id.toolbar_main_activity))
        findViewById<Toolbar>(R.id.toolbar_main_activity).setNavigationOnClickListener {

            toggleDrawer()
        }

    }

    private fun toggleDrawer(){
        if(findViewById<DrawerLayout>(R.id.drawer_layout).isDrawerOpen(GravityCompat.START)){
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        }else{
            findViewById<DrawerLayout>(R.id.drawer_layout).openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if(findViewById<DrawerLayout>(R.id.drawer_layout).isDrawerOpen(GravityCompat.START)){
            findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        }else{
            doubleBackToExit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile -> {
                val profileIntent = Intent(this, MyProfileActivity::class.java)
                profileActivityResultLauncher.launch(profileIntent)
            }
            R.id.nav_sign_out -> {
                Toast.makeText(this, "You have successfully signed out!",
                        Toast.LENGTH_SHORT).show()
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        return true
    }


    fun updateNavigationUserDetails(user: com.example.projectmanager.models.User, readBoardsList: Boolean) {

        hideProgressDialog()
        mUserName = user.name
        Glide
            .with(this)
            .load(user.image)
            .fitCenter()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(findViewById(R.id.iv_user_image))

        if(readBoardsList){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this)
        }

        findViewById<TextView>(R.id.tv_username).text = user.name
    }




    //replacement for onActivityResult
    private fun registerOnActivityForResult() {
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        profileActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                                 FirestoreClass().loadUserData(this)

                }else{
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                }
                  

            }

        reloadBoard = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                FirestoreClass().getBoardsList(this)

            }else{
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }

        }
        }



    fun populateBoardsListToUI(boardsList: ArrayList<Board>){
        hideProgressDialog()
        if(boardsList.size>0){
            findViewById<RecyclerView>(R.id.rv_boards_list).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_no_boards_available).visibility = View.GONE

            findViewById<RecyclerView>(R.id.rv_boards_list).layoutManager = LinearLayoutManager(this)
           // findViewById<RecyclerView>(R.id.rv_boards_list).setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this, boardsList)
            findViewById<RecyclerView>(R.id.rv_boards_list).adapter = adapter

            adapter.setOnClickListener(object : BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity,TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)

                }

            })
        }else{
            val rv = findViewById<RecyclerView>(R.id.rv_boards_list)
            rv.visibility = View.GONE
            findViewById<TextView>(R.id.tv_no_boards_available).visibility = View.VISIBLE
        }
    }


    fun tokenUpdateSuccess(){
        hideProgressDialog()
        val editor : SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this, true)
    }


    private fun updateFCMToken(token :String){
        val userHashMap = HashMap<String,Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this, userHashMap)
    }
}
