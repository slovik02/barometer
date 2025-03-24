package com.example.zpo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class main_page : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        val items = mutableListOf(
            Item("Trends", "Monitor pressure trends", R.drawable.trend),
            Item("Well-being", "How do you feel at this moment? " +
                    "Monitor your response to the specific atmospheric pressure", R.drawable.well),
            Item("Information", "Gain information how to deal" +
                    "with different pressure conditions", R.drawable.info)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.RV)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = ItemAdapter(items) { selectedItem ->
            if (selectedItem.title == "Trends") {
                val intent = Intent(this, trends::class.java)
                startActivity(intent)
            }
        }

        recyclerView.adapter = adapter
    }
}