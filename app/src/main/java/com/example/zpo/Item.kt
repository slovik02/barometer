package com.example.zpo

data class Item(
    /**
     * Data class representing an item to be displayed in a RecyclerView.
     *
     * @param title The title text of the item.
     * @param description A short description or subtitle for the item.
     * @param imageId The resource ID of the item's icon/image.
     */
    val title: String,
    val description: String,
    val imageId: Int
)


