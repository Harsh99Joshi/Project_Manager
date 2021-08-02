package com.example.projectmanager.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.example.projectmanager.R
import com.example.projectmanager.adapters.CardMembersAdapter
import com.example.projectmanager.databinding.ActivityCardDetailsBinding
import com.example.projectmanager.dialogs.LabelColorListDialog
import com.example.projectmanager.dialogs.MembersListDialog
import com.example.projectmanager.firebase.FirestoreClass
import com.example.projectmanager.models.*
import com.example.projectmanager.utils.Constants
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CardDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityCardDetailsBinding
    private lateinit var mBoardDetails: Board
    private var mTaskListPosition = -1
    private var mCardPosition = -1
    private var mSelectedColor = ""
    private lateinit var mMembersDetailList: ArrayList<User>
    private var mSelectedDueDateMS: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCardDetailsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        getIntentData()
        setupActionBar()

        findViewById<EditText>(R.id.et_name_card_details)
                .setText(mBoardDetails.taskList[mTaskListPosition]
                        .cards[mCardPosition].name)

        findViewById<EditText>(R.id.et_name_card_details).setSelection(
                findViewById<EditText>(R.id.et_name_card_details).text.toString().length
        )

        mSelectedColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].labelColor
        if (mSelectedColor.isNotEmpty()) {
            setColor()
        }


        binding.btnUpdateCardDetails.setOnClickListener {
            if (findViewById<EditText>(R.id.et_name_card_details).text.toString().isNotEmpty()) {
                updateCardDetails()
            } else {
                Toast.makeText(this@CardDetailsActivity, "Please enter a card name", Toast.LENGTH_SHORT).show()
            }
        }


        binding.tvSelectLabelColor.setOnClickListener {
            labelColorsListDialog()
        }



        binding.tvSelectMembers.setOnClickListener {
            membersListDialog()
        }

        setupSelectedMemberList()
        mSelectedDueDateMS = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate


        if(mSelectedDueDateMS > 0){
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = sdf.format(Date(mSelectedDueDateMS))
            binding.tvSelectDueDate.text = selectedDate
        }


        binding.tvSelectDueDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarCardDetailsActivity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_back_24dp)
            actionBar.title = mBoardDetails
                    .taskList[mTaskListPosition]
                    .cards[mCardPosition].name
        }

        binding.toolbarCardDetailsActivity.setNavigationOnClickListener {
            onBackPressed()
        }
    }


    private fun getIntentData() {
        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }
        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)) {
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)!!
        }
        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)) {
            mCardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)!!
        }
        if (intent.hasExtra(Constants.BOARD_MEMBERS_LIST)) {
            mMembersDetailList = intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_delete_card -> {
                alertDialogForDeleteCard(
                        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun addUpdateTaskListSuccess() {
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }


    private fun updateCardDetails() {
        val card = Card(
                findViewById<EditText>(R.id.et_name_card_details).text.toString(),
                mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createBy,
                mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
                mSelectedColor,
                mSelectedDueDateMS
        )

        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)

        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }


    private fun deleteCard() {
        val cardsList: ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards
        cardsList.removeAt(mCardPosition)

        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size - 1)

        taskList[mTaskListPosition].cards = cardsList
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)

    }


    private fun alertDialogForDeleteCard(cardName: String) {
        val builder = AlertDialog.Builder(this)

        builder.setTitle(resources.getString(R.string.alert))

        builder.setMessage(
                resources.getString(
                        R.string.confirmation_message_to_delete_card,
                        cardName
                )
        )
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton(resources.getString(R.string.yes)) { dialogInterface, which ->
            dialogInterface.dismiss()
            deleteCard()

        }
        builder.setNegativeButton(resources.getString(R.string.no)) { dialogInterface, which ->
            dialogInterface.dismiss()
        }

        val alertDialog: AlertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()
    }


    private fun colorsList(): ArrayList<String> {
        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")
        return colorsList
    }


    private fun setColor() {
        binding.tvSelectLabelColor.text = ""
        binding.tvSelectLabelColor.setBackgroundColor(Color.parseColor(mSelectedColor))
    }


    private fun labelColorsListDialog() {
        val colorsList: ArrayList<String> = colorsList()
        val listDialog = object : LabelColorListDialog(
                this,
                colorsList,
                resources.getString(R.string.str_select_label_color),
                mSelectedColor) {
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }


    private fun membersListDialog() {
        var cardAssignedMemberList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo
        if (cardAssignedMemberList.size > 0) {
            for (i in mMembersDetailList.indices) {
                for (j in cardAssignedMemberList) {
                    if (mMembersDetailList[i].id == j) {
                        mMembersDetailList[i].selected = true
                    }
                }
            }
        } else {
            for (i in mMembersDetailList.indices) {
                mMembersDetailList[i].selected = false

            }
        }

        val listDialog = object : MembersListDialog(this, mMembersDetailList, resources.getString(R.string.select_members)){
            override fun onItemSelected(user: User, action: String) {
                if(action == Constants.SELECT){
                    if(!mBoardDetails.taskList[mTaskListPosition]
                                    .cards[mCardPosition]
                                    .assignedTo.contains(user.id)){
                        mBoardDetails.taskList[mTaskListPosition]
                                .cards[mCardPosition]
                                .assignedTo.add(user.id)
                    }
                }
                else{
                    mBoardDetails.taskList[mTaskListPosition]
                            .cards[mCardPosition]
                            .assignedTo.remove(user.id)

                    for(i in mMembersDetailList.indices){
                        if(mMembersDetailList[i].id == user.id){
                            mMembersDetailList[i].selected = false
                        }
                    }
                }

                setupSelectedMemberList()
            }

        }.show()

    }


    private fun setupSelectedMemberList(){
        val cardAssignedMembersList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo

        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()
        for (i in mMembersDetailList.indices) {
            for (j in cardAssignedMembersList) {
                if (mMembersDetailList[i].id == j) {
                    val selectedMember = SelectedMembers(
                            mMembersDetailList[i].id,
                            mMembersDetailList[i].image
                    )
                    selectedMembersList.add(selectedMember)
                }
            }
        }

        if(selectedMembersList.size>0){
            selectedMembersList.add(SelectedMembers("",""))
            binding.tvSelectMembers.visibility=View.GONE
            binding.rvSelectedMembersList.visibility=View.VISIBLE

            binding.rvSelectedMembersList.layoutManager = GridLayoutManager(
                    this,
                    6,
            )
            val adapter = CardMembersAdapter(this, selectedMembersList, true)
            binding.rvSelectedMembersList.adapter = adapter
            adapter.setOnClickListener(
                    object : CardMembersAdapter.OnClickListener{
                        override fun onClick() {
                            membersListDialog()
                        }
                    }
            )
        }else{
            binding.tvSelectMembers.visibility=View.VISIBLE
            binding.rvSelectedMembersList.visibility=View.GONE
        }
    }


    private fun showDatePicker() {

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)


        val dpd = DatePickerDialog(
                this,
                { view, year, monthOfYear, dayOfMonth ->

                    val sDayOfMonth = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                    val sMonthOfYear =
                            if ((monthOfYear + 1) < 10) "0${monthOfYear + 1}" else "${monthOfYear + 1}"

                    val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"
                    binding.tvSelectDueDate.text = selectedDate


                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)


                    val theDate = sdf.parse(selectedDate)


                    mSelectedDueDateMS = theDate!!.time
                },
                year,
                month,
                day
        )
        dpd.show()
    }

}