package com.example.projectmanager.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.projectmanager.activities.*
import com.example.projectmanager.models.Board
import com.example.projectmanager.models.User
import com.example.projectmanager.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

open class FirestoreClass {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo : User){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserID()).set(userInfo, SetOptions.merge()).addOnSuccessListener {
                activity.userRegisteredSuccess()
            }.addOnFailureListener{
                e-> Log.e(activity.javaClass.simpleName," Error Registering")
            }
    }


    fun getCurrentUserID(): String{
        var currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if(currentUser!=null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }




    fun loadUserData(activity: Activity, readBoardsList: Boolean = true){
        mFireStore.collection(Constants.USERS)
                .document(getCurrentUserID()).get().addOnSuccessListener { document ->
                    val loggedInUser = document.toObject(User::class.java)!!
                    when(activity) {
                        is SignInActivity -> {
                            activity.signInSuccess(loggedInUser)
                        }
                        is MainActivity -> {
                            activity.updateNavigationUserDetails(loggedInUser, readBoardsList)
                        }
                        is MyProfileActivity ->{
                            activity.setUserDataInUI(loggedInUser)
                        }
                    }
                }.addOnFailureListener{
                when(activity) {
                    is SignInActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e("Sign in user","Error writing document")
                }
    }




    fun updateUserProfileData(activity: Activity,
                              userHashMap: HashMap<String, Any>){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserID())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName,"Profile Data updated")
                Toast.makeText(activity, "Profile updated", Toast.LENGTH_SHORT).show()
                when(activity) {
                    is MainActivity -> {
                        activity.tokenUpdateSuccess()
                    }
                    is MyProfileActivity -> {

                        activity.profileUpdateSuccess()
                    }
                }
            }.addOnFailureListener{
                e->
                    when(activity){
                        is MainActivity ->{
                            activity.hideProgressDialog()
                        }
                        is MyProfileActivity->{
                            activity.hideProgressDialog()
                        }
                    }


                Log.e(activity.javaClass.simpleName,
                    "Error while creating board",e)
                Toast.makeText(activity, "Error while updating profile", Toast.LENGTH_SHORT).show()
            }

    }



    fun createBoard(activity: CreateBoardActivity, board: Board) {
        mFireStore.collection(Constants.BOARDS)
            .document()
            .set(board, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Board created!")
                Toast.makeText(activity, "Board created!", Toast.LENGTH_SHORT).show()

        activity.boardCreatedSuccessfully()
    }.addOnFailureListener{
                    exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName," Error creating board", exception)
            }
    }



    fun getBoardsList(activity: MainActivity){
        mFireStore.collection(Constants.BOARDS)
                .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserID())
                .get()
                .addOnSuccessListener {
                    document ->
                    Log.i(activity.javaClass.simpleName, document.documents.toString())
                    val boardsList: ArrayList<Board> = ArrayList()
                    for(i in document.documents){
                        val board = i.toObject(Board::class.java)!!
                        board.documentId =i.id
                        boardsList.add(board)
                    }
                    activity.populateBoardsListToUI(boardsList)
                }.addOnFailureListener{
                    activity.hideProgressDialog()
                    Log.e(activity.javaClass.simpleName,"Error creating board")
                }

    }




    fun getBoardDetails(activity: TaskListActivity, documentId: String){
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener {
                    document ->
                Log.i(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetails(board)
                }.addOnFailureListener{
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error creating board")
            }
    }


    fun addUpdateTaskList(activity: Activity, board: Board) {
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
                .document(board.documentId)
                .update(taskListHashMap)
                .addOnSuccessListener {
                    Log.e(activity.javaClass.simpleName, "Task List updated")
                    if(activity is TaskListActivity) {
                        activity.addUpdateTaskListSuccess()
                    }else if (activity is CardDetailsActivity){
                        activity.addUpdateTaskListSuccess()
                    }
                }.addOnFailureListener {
                    exception ->
                if(activity is TaskListActivity) {
                    activity.hideProgressDialog()
                }else if (activity is CardDetailsActivity){
                    activity.hideProgressDialog()
                }
                    Log.e(activity.javaClass.simpleName, "Task List updating error",exception)
                }
    }


    fun getAssignedMemberListDetails(activity: Activity, assignedTo: ArrayList<String>){
        mFireStore.collection(Constants.USERS).whereIn(Constants.ID, assignedTo)
                .get()
                .addOnSuccessListener {
                    document ->
                    Log.e(activity.javaClass.simpleName, document.documents.toString())

                    val usersList: ArrayList<User> = ArrayList()

                    for(i in document.documents){
                        val user  = i.toObject(User::class.java)
                        if (user != null) {
                            usersList.add(user)
                        }
                    }
                    if(activity is MembersActivity)
                    activity.setupMembersList(usersList)
                    else if(activity is TaskListActivity)
                        activity.boardMembersDetailsList(usersList)
                }.addOnFailureListener { e->
                    if(activity is MembersActivity)
                        activity.hideProgressDialog()
                    else if(activity is TaskListActivity)
                         activity.hideProgressDialog()
                    Log.e(activity.javaClass.simpleName, "Error while getting members", e)
                }
    }


    fun getMemberDetails(activity: MembersActivity, email: String){
        mFireStore.collection(Constants.USERS)
                .whereEqualTo(Constants.EMAIL, email)
                .get()
                .addOnSuccessListener {
                    document ->
                    if(document.documents.size > 0){
                        val user = document.documents[0].toObject(User::class.java)
                        if (user != null) {
                            activity.memberDetails(user)
                        }
                    }else{
                        activity.hideProgressDialog()
                        activity.showErrorSnackBar("No such member found")
                    }
                }
    }


    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User){

        val assignedToHashMap = HashMap<String,Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
                .document(board.documentId)
                .update(assignedToHashMap)
                .addOnSuccessListener {
                    activity.memberAssignedSuccess(user)
                }.addOnFailureListener { f->
                    activity.hideProgressDialog()
                    Log.e(activity.javaClass.simpleName,
                            "Error while creating board in assignedMemberToBoard",f)
                }
    }
}