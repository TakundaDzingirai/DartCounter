package com.example.dartcounter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast

class MatchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        // Get player names from PlayerRegistrationActivity
        val player1Name = intent.getStringExtra("PLAYER1_NAME") ?: "Player 1"
        val player2Name = intent.getStringExtra("PLAYER2_NAME") ?: "Player 2"

        val matchModeGroup = findViewById<RadioGroup>(R.id.matchModeGroup)
        val legsMode = findViewById<RadioButton>(R.id.legsMode)
        val setsMode = findViewById<RadioButton>(R.id.setsMode)
        val numberInput = findViewById<EditText>(R.id.numberInput)
        val startMatchButton = findViewById<Button>(R.id.startMatchButton)

        startMatchButton.setOnClickListener {
            val number = numberInput.text.toString().toIntOrNull()
            if (number == null || number <= 0) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isLegsMode = legsMode.isChecked
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra("PLAYER1_NAME", player1Name)
                putExtra("PLAYER2_NAME", player2Name)
                putExtra("IS_LEGS_MODE", isLegsMode)
                putExtra("TARGET_NUMBER", number)
            }
            startActivity(intent)
        }
    }
}