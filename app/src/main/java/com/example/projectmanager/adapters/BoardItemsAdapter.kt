package com.example.projectmanager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projectmanager.R
import com.example.projectmanager.models.Board

open class BoardItemsAdapter(private val context: Context,
                             private var list: ArrayList<Board>):


    RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var onClickListener: OnClickListener?=null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return myViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.item_board,parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if(holder is myViewHolder){
            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.ic_board_place_holder)
                .into(holder.itemView.findViewById(R.id.iv_board_image_rv))

            holder.itemView.findViewById<TextView>(R.id.tv_name_rv).text = model.name
            holder.itemView.findViewById<TextView>(R.id.tv_created_by_rv).text = "Created by ${model.createdBy}"

            holder.itemView.setOnClickListener {
                if(onClickListener!=null){
                    onClickListener!!.onClick(position, model)
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return list.size
    }





    interface OnClickListener{
        fun onClick(position: Int, model: Board)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
    private class myViewHolder(view: View): RecyclerView.ViewHolder(view){

    }

}