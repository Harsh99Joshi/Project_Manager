package com.example.projectmanager.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmanager.R
import com.example.projectmanager.adapters.MemberListItemsAdapter
import com.example.projectmanager.models.User


abstract class MembersListDialog(
        context: Context,
        private var list: ArrayList<User>,
        private val title: String = ""
) : Dialog(context) {

    private var adapter: MemberListItemsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState )

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list, null)

        setContentView(view)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setUpRecyclerView(view)
    }

    private fun setUpRecyclerView(view: View) {
        view.findViewById<TextView>(R.id.tvTitle).text = title

        if (list.size > 0) {

            view.findViewById<RecyclerView>(R.id.rvList).layoutManager = LinearLayoutManager(context)
            adapter = MemberListItemsAdapter(context, list)
            view.findViewById<RecyclerView>(R.id.rvList).adapter = adapter

            adapter!!.setOnClickListener(object :
                MemberListItemsAdapter.OnClickListener {
                override fun onClick(position: Int, user: User, action:String) {
                    dismiss()
                    onItemSelected(user, action)
                }
            })
        }
    }

    protected abstract fun onItemSelected(user: User, action:String)
}
