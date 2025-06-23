package com.example.zpo

import android.graphics.drawable.Icon
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(private val itemList: List<Item>, private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    /**
     * RecyclerView Adapter for displaying a list of [Item] objects.
     *
     * @param itemList The list of items to display.
     * @param onItemClick A lambda function that is called when an item is clicked,
     *                    passing the clicked [Item].
     */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        /**
         * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
         *
         * Inflates the item layout and creates a [ViewHolder].
         */
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        /**
         * Called by RecyclerView to display data at the specified position.
         *
         * Binds the item data to the ViewHolder and sets the click listener for the item.
         */
        val item = itemList[position]
        holder.bind(item)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        /**
         * Returns the total number of items in the data set held by the adapter.
         */
        return itemList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /**
         * ViewHolder class that holds references to the views for each item.
         *
         * @param itemView The root view of the item layout.
         */
        private val TitleTextView: TextView = itemView.findViewById(R.id.title)
        private val DescTextView: TextView = itemView.findViewById(R.id.desc)
        private val IconImageView: ImageView = itemView.findViewById(R.id.Iconimage)

        fun bind(item: Item) {
            TitleTextView.text = item.title
            DescTextView.text = item.description
            IconImageView.setImageResource(item.imageId)
        }
    }
}