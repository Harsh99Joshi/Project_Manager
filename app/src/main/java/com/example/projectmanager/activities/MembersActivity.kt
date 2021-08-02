package com.example.projectmanager.activities

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.adapters.MemberListItemsAdapter
import com.example.projectmanager.databinding.ActivityMembersBinding
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.Board
import com.example.projectmanager.models.User
import com.example.projectmanager.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL


class MembersActivity : BaseActivity() {

    private lateinit var binding: ActivityMembersBinding
    private lateinit var mBoardDetails: Board
    private lateinit var mAssignedMembersList: ArrayList<User>
    private var anyChangesMade: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMembersBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
        }

        setupActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMemberListDetails(this, mBoardDetails.assignedTo)
    }



    fun setupMembersList(list: ArrayList<User>){
        hideProgressDialog()
        mAssignedMembersList = list

        findViewById<RecyclerView>(R.id.rv_members_list).layoutManager =
                LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.rv_members_list).setHasFixedSize(true)

        val adapter = MemberListItemsAdapter(this, list)
        findViewById<RecyclerView>(R.id.rv_members_list).adapter = adapter
    }



    fun memberDetails(user: User){
        mBoardDetails.assignedTo.add(user.id)
        FirestoreClass().assignMemberToBoard(this, mBoardDetails, user)
    }



    fun memberAssignedSuccess(user: User){
        hideProgressDialog()
        mAssignedMembersList.add(user)

        anyChangesMade = true
        setupMembersList(mAssignedMembersList)
        SendNotificationToUserAsyncTask(mBoardDetails.name, user.fcmToken).execute()
    }



    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarMembersActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_back_24dp)
            actionBar.title = resources.getString(R.string.members)


        }

        binding.toolbarMembersActivity.setNavigationOnClickListener {
            onBackPressed()
        }

    }


    override fun onBackPressed() {
        if(anyChangesMade){
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_add_member ->{
                dialogAddMember()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }



    private fun dialogAddMember(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_member)
        dialog.findViewById<TextView>(R.id.tv_add).setOnClickListener {
            val email = dialog.findViewById<EditText>(R.id.et_email_search_member).text.toString()
            if(email.isNotEmpty()){
                dialog.dismiss()
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getMemberDetails(this, email)
            }else{
                Toast.makeText(this@MembersActivity, "Please enter an email", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    private inner class SendNotificationToUserAsyncTask(val boardName: String, val token: String):
        AsyncTask<Any, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog(resources.getString(R.string.please_wait))
        }

        override fun doInBackground(vararg params: Any?): String {
            var result: String
            var connection: HttpURLConnection?= null


            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.doInput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.setRequestProperty(Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}")
                connection.useCaches = false

                val wr= DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE, "You have been assigned to the board by ${mAssignedMembersList[0].name}")

                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult: Int = connection.responseCode
                if(httpResult == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream

                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    try {
                        while (reader.readLine().also { line=it }!=null){
                            sb.append(line+"\n")
                        }
                    }catch ( e: IOException){
                        e.printStackTrace()
                    }finally {
                        try {
                            inputStream.close()
                        }catch ( e: IOException){
                            e.printStackTrace()
                        }

                    }
                    result = sb.toString()
                }else{
                    result = connection.responseMessage
                }
            }


            catch (e: SocketTimeoutException){
                result = "Connection Timeout"
            }catch (e: Exception){
                result = "Error:" +e.message
            }finally {
                connection?.disconnect()
            }

            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            hideProgressDialog()
            if (result != null) {
                Log.e("JSON Response Result", result)
            }
        }
    }






  /*  private inner class SendNotificationToUserAsyncTask(val boardName: String, val token: String): ViewModel(){

        fun execute() = viewModelScope.launch {
            onPreExecute()
            val result = doInBackground()
            onPostExecute(result)
        }

        private fun onPreExecute() {
            showProgressDialog(resources.getString(R.string.please_wait))
        }


        private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
            var result: String=""
            var connection: HttpURLConnection?= null


            try {
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.doInput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")

                connection.setRequestProperty(Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}")
                connection.useCaches = false

                val wr= DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE, "You have been assigned to the board by ${mAssignedMembersList[0].name}")

                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult: Int = connection.responseCode
                if(httpResult == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream

                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    try {
                        while (reader.readLine().also { line=it }!=null){
                            sb.append(line+"\n")
                        }
                    }catch ( e: IOException){
                        e.printStackTrace()
                    }finally {
                        try {
                            inputStream.close()
                        }catch ( e: IOException){
                            e.printStackTrace()
                        }

                    }
                    result = sb.toString()
                }else{
                    result = connection.responseMessage
                }
            }


            catch (e: SocketTimeoutException){
                result = "Connection Timeout"
            }catch (e: Exception){
                result = "Error:" +e.message
            }finally {
                connection?.disconnect()
            }

            return@withContext result
        }

       private fun onPostExecute(result: String?) {
            hideProgressDialog()
        }
    }


   */

}