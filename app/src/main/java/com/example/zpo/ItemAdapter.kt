package com.example.zpo

import android.graphics.drawable.Icon
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(private val itemList: List<Item>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_item, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
    }


    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val TitleTextView: TextView = itemView.findViewById(R.id.title)
        private val DescTextView: TextView = itemView.findViewById(R.id.desc)
        private val IconImageView : ImageView = itemView.findViewById(R.id.Iconimage)

        fun bind(item: Item) {
            TitleTextView.text = item.title
            DescTextView.text = item.description
            IconImageView.setImageResource(item.imageId)
        }
    }
}