package com.example.projectmanager.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.example.projectmanager.R
import com.example.projectmanager.databinding.ActivityCreateBoardBinding
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.Board
import com.example.projectmanager.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CreateBoardActivity : BaseActivity() {


    private lateinit var binding: ActivityCreateBoardBinding
    private lateinit var galleryImageResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var mUserName: String
    private var mSelectedImageUri: Uri? = null
    private var mBoardImageURL : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCreateBoardBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupActionBar()

        if(intent.hasExtra(Constants.NAME)){
            mUserName = intent.getStringExtra(Constants.NAME).toString()
        }

        binding.btnSetBoardImage.setOnClickListener {
            showImageChooser()
        }

        binding.btnCreate.setOnClickListener {
            if(mSelectedImageUri!=null){
                uploadBoardImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                createBoard()
            }
        }
        registerOnActivityForResult()
    }



    private fun setupActionBar() {
        setSupportActionBar(findViewById(R.id.toolbar_create_board_activity))

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_back_24dp)
            actionBar.title = resources.getString(R.string.create_board_title)
        }
        findViewById<Toolbar>(R.id.toolbar_create_board_activity).setNavigationOnClickListener {
            onBackPressed()
        }
    }




    private fun showImageChooser() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryImageResultLauncher.launch(galleryIntent)
    }




    private fun registerOnActivityForResult() {
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        galleryImageResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == RESULT_OK) {
                    val data: Intent? = result.data
                    if (data != null) {
                        mSelectedImageUri = data.data
                        binding.ivBoardImage.setImageURI(mSelectedImageUri)
                    }
                }
            }
    }


    fun boardCreatedSuccessfully(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()

    }


    private fun createBoard(){
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID())

        var board = Board(
            binding.etBoardName.text.toString(),
            mBoardImageURL,
            mUserName,
            assignedUsersArrayList
        )

        FirestoreClass().createBoard(this, board)
    }


    private fun uploadBoardImage(){
        showProgressDialog(resources.getString(R.string.please_wait))
        if(mSelectedImageUri!=null){
            //generating name for image storage
            //   findViewById<ImageView>(R.id.iv_user_image).setImageURI(mSelectedImageUri!!)
            val sRef : StorageReference =
                FirebaseStorage.getInstance().reference.child("BOARD_IMAGE" +
                        System.currentTimeMillis()+
                        "."+
                        getFileExtension(mSelectedImageUri))
            sRef.putFile(mSelectedImageUri!!).addOnSuccessListener {
                    taskSnapshot ->
                Log.e("board image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString())

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                        uri -> Log.e("Downloadable board image uri",uri.toString())
                    mBoardImageURL = uri.toString()
                    hideProgressDialog()
                    createBoard()
                }
            }.addOnFailureListener{
                    exception ->
                Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
        }
    }



    private fun getFileExtension(uri: Uri?):String?{
        return MimeTypeMap.getSingleton().
        getExtensionFromMimeType(contentResolver.getType(uri!!))
    }
}