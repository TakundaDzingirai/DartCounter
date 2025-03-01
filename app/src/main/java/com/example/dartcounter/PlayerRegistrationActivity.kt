package com.example.dartcounter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class PlayerRegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_registration)

        val player1Name = findViewById<EditText>(R.id.player1Name)
        val player2Name = findViewById<EditText>(R.id.player2Name)
        val startButton = findViewById<Button>(R.id.startGameButton)

        startButton.setOnClickListener {
            val p1Name = player1Name.text.toString()
            val p2Name = player2Name.text.toString()
            if (p1Name.isNotEmpty() && p2Name.isNotEmpty()) {
                val intent = Intent(this, MatchActivity::class.java).apply {
                    putExtra("PLAYER1_NAME", p1Name)
                    putExtra("PLAYER2_NAME", p2Name)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter both player names", Toast.LENGTH_SHORT).show()
            }
        }
    }
}