package com.example.projectmanager.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.databinding.ActivityMyProfileBinding
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.User
import com.example.projectmanager.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityMyProfileBinding
    private lateinit var galleryImageResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var mUserDetails: User

    companion object {
        private const val READ_STORAGE_REQUEST_CODE = 1
    }

    private var mSelectedImageUri: Uri? = null
    private var mProfileImageURL : String =""


    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupActionBar()

        FirestoreClass().loadUserData(this)


        binding.btnSelectProfileImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                showImageChooser()
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        READ_STORAGE_REQUEST_CODE)
            }
        }
        registerOnActivityForResult()


        binding.btnUpdate.setOnClickListener {
            if(mSelectedImageUri!=null){
                uploadUserImage()
            }else{
                showProgressDialog(resources.getString(R.string.please_wait))
                updateUserProfileData()
            }
        }

    }















    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarMyProfileActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_back_24dp)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        binding.toolbarMyProfileActivity.setNavigationOnClickListener {
            onBackPressed()
        }

    }











    fun setUserDataInUI(user: User) {

        mUserDetails = user
        Glide
                .with(this@MyProfileActivity)
                .load(user.image)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(binding.ivProfileImage)

        binding.etName.setText(user.name)
        binding.etEmail.setText(user.email)
        if (user.mobile != 0L) {
            binding.etMobile.setText(user.mobile.toString())
        }
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showImageChooser()
            }
        } else {
            Toast.makeText(this, "Allow settings", Toast.LENGTH_SHORT).show()
        }
    }





    private fun showImageChooser() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryImageResultLauncher.launch(galleryIntent)
    }






//replacement for onActivityResult
    private fun registerOnActivityForResult() {
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        galleryImageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                if (data != null) {
                    mSelectedImageUri = data.data
                    try {
                        Glide
                                .with(this@MyProfileActivity)
                                .load(mSelectedImageUri)
                                .centerCrop()
                                .placeholder(R.drawable.ic_user_place_holder)
                                .into(binding.ivProfileImage)
                         // val selectedImageBitmap: Bitmap =MediaStore.Images.Media.getBitmap(this.contentResolver,contentUri)
                        //  binding.ivProfileImage.setImageBitmap(selectedImageBitmap)
                        //OR
                       // binding.ivProfileImage.setImageURI(contentUri)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

    }



    private fun uploadUserImage(){
        showProgressDialog(resources.getString(R.string.please_wait))
        if(mSelectedImageUri!=null){
            //generating name for image storage
         //   findViewById<ImageView>(R.id.iv_user_image).setImageURI(mSelectedImageUri!!)
            val sRef : StorageReference =
                FirebaseStorage.getInstance().reference.child("USER_IMAGE" +
                        System.currentTimeMillis()+
                        "."+
                        getFileExtension(mSelectedImageUri))
            sRef.putFile(mSelectedImageUri!!).addOnSuccessListener {
                taskSnapshot ->
                Log.e("Firebase image URL",
                        taskSnapshot.metadata!!.reference!!.downloadUrl.toString())
                
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri -> Log.e("Downloadable image uri",uri.toString())
                    mProfileImageURL = uri.toString()
                    hideProgressDialog()
                    updateUserProfileData()
                }
            }.addOnFailureListener{
                exception ->
                Toast.makeText(this@MyProfileActivity, exception.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }
        }
    }


    //used to get the type of file type passed in.(eg image, audio, etc)(returns .png, .mp3, .mp4)
    private fun getFileExtension(uri: Uri?):String?{
        return MimeTypeMap.getSingleton().
        getExtensionFromMimeType(contentResolver.getType(uri!!))
    }



    fun profileUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }


    private fun updateUserProfileData() {
        val userHashMap = HashMap<String, Any>()
        var changesMade = false
        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
            changesMade = true
        }

        if (binding.etName.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = binding.etName.text.toString()

            changesMade = true

        }

        if (binding.etMobile.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = binding.etMobile.text.toString().toLong()
            changesMade = true

        }

        if (changesMade) {
            FirestoreClass().updateUserProfileData(this, userHashMap)
          //  findViewById<TextView>(R.id.tv_username).text=binding.etName.text.toString()

        }
    }


}












