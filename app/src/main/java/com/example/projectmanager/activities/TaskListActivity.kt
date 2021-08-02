package com.example.projectmanager.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.adapters.TaskListItemsAdapter
import com.example.projectmanager.databinding.ActivityTaskListBinding
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.Board
import com.example.projectmanager.models.Card
import com.example.projectmanager.models.Task
import com.example.projectmanager.models.User
import com.example.projectmanager.utils.Constants

class TaskListActivity : BaseActivity() {

    private lateinit var binding: ActivityTaskListBinding
    private lateinit var taskListMembersLauncher: ActivityResultLauncher<Intent>
    private lateinit var CardDetailLauncher: ActivityResultLauncher<Intent>
    private lateinit var mBoardDetails: Board
    private lateinit var mBoardDocumentID: String
    lateinit var mAssignedMembersList: ArrayList<User>


    override fun onCreate(savedInstanceState: Bundle?) {

        binding = ActivityTaskListBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        var boardDocumentId = ""
        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            mBoardDocumentID = intent.getStringExtra(Constants.DOCUMENT_ID).toString()
        }

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this, mBoardDocumentID)

        registerOnActivityForResult()
    }


    fun boardDetails(board: Board) {

        mBoardDetails = board
        hideProgressDialog()
        setupActionBar()




        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMemberListDetails(this, mBoardDetails.assignedTo)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_members -> {
                val intent = Intent(this, MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
                taskListMembersLauncher.launch(intent)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarTaskListActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_back_24dp)
            actionBar.title = mBoardDetails.name
        }

        binding.toolbarTaskListActivity.setNavigationOnClickListener {
            onBackPressed()
        }

    }

    fun addUpdateTaskListSuccess() {
        hideProgressDialog()

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this, mBoardDetails.documentId)
    }


    fun createTaskList(taskListName: String) {
        val task = Task(taskListName, FirestoreClass().getCurrentUserID())
        mBoardDetails.taskList.add(0, task)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this, mBoardDetails)
    }


    fun updateTaskList(position: Int, listName: String, model: Task) {
        val task = Task(listName, model.createdBy)
        mBoardDetails.taskList[position] = task
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this, mBoardDetails)
    }


    fun deleteTaskList(position: Int) {
        mBoardDetails.taskList.removeAt(position)
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this, mBoardDetails)
    }


    fun addCardToTaskList(position: Int, cardName: String) {
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size - 1)

        val cardAssignedUsersList: ArrayList<String> = ArrayList()
        cardAssignedUsersList.add(FirestoreClass().getCurrentUserID())

        val card = Card(cardName, FirestoreClass().getCurrentUserID(), cardAssignedUsersList)
        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)

        val task = Task(mBoardDetails.taskList[position].title,
                mBoardDetails.taskList[position].createdBy,
                cardsList)

        mBoardDetails.taskList[position] = task


        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this, mBoardDetails)
    }


    private fun registerOnActivityForResult() {
        //returns: the launcher that can be used to start the activity or dispose of the prepared call.
        taskListMembersLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getBoardDetails(this, mBoardDocumentID)

            } else {
                Toast.makeText(this, "No member added", Toast.LENGTH_SHORT).show()
            }


        }

        CardDetailLauncher =  registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result ->
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getBoardDetails(this, mBoardDocumentID)

            } else {
                Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show()
            }


        }
    }



    fun cardDetails(taskListPosition: Int, cardPosition: Int){
        val intent = Intent(this,CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION, taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION, cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST, mAssignedMembersList)
        CardDetailLauncher.launch(intent)
    }



    fun boardMembersDetailsList(list:ArrayList<User>){
        mAssignedMembersList = list
        hideProgressDialog()

        val addTaskList = Task(resources.getString(R.string.add_list))
        mBoardDetails.taskList.add(addTaskList)

        findViewById<RecyclerView>(R.id.rv_task_list).layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        findViewById<RecyclerView>(R.id.rv_task_list).setHasFixedSize(true)

        val adapter = TaskListItemsAdapter(this, mBoardDetails.taskList)
        findViewById<RecyclerView>(R.id.rv_task_list).adapter = adapter

    }


    fun updateCardsInTaskList(taskListPosition: Int, cards: ArrayList<Card>){
        mBoardDetails.taskList.removeAt(mBoardDetails.taskList.size-1)
        mBoardDetails.taskList[taskListPosition].cards = cards
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this, mBoardDetails)
    }
}
