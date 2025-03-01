package com.example.dartcounter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class GameActivity : AppCompatActivity() {
    private var player1Score = 501
    private var player2Score = 501
    private var isPlayer1Turn = true
    private var player1Legs = 0
    private var player2Legs = 0
    private var player1Sets = 0
    private var player2Sets = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val player1Name = intent.getStringExtra("PLAYER1_NAME") ?: "Player 1"
        val player2Name = intent.getStringExtra("PLAYER2_NAME") ?: "Player 2"
        val isLegsMode = intent.getBooleanExtra("IS_LEGS_MODE", true)
        val targetNumber = intent.getIntExtra("TARGET_NUMBER", 1)

        val player1NameText = findViewById<TextView>(R.id.player1NameText)
        val player1ScoreText = findViewById<TextView>(R.id.player1ScoreText)
        val player2NameText = findViewById<TextView>(R.id.player2NameText)
        val player2ScoreText = findViewById<TextView>(R.id.player2ScoreText)
        val currentTurnText = findViewById<TextView>(R.id.currentTurnText)
        val scoreInput = findViewById<EditText>(R.id.scoreInput)
        val submitButton = findViewById<Button>(R.id.submitScoreButton)

        player1NameText.text = "$player1Name (Legs: $player1Legs, Sets: $player1Sets)"
        player2NameText.text = "$player2Name (Legs: $player2Legs, Sets: $player2Sets)"
        player1ScoreText.text = player1Score.toString()
        player2ScoreText.text = player2Score.toString()
        currentTurnText.text = "Current Turn: $player1Name\nMode: ${if (isLegsMode) "First to $targetNumber Legs" else "First to $targetNumber Sets (3 Legs/Set)"}"

        submitButton.setOnClickListener {
            val score = scoreInput.text.toString().toIntOrNull()
            if (score != null && score >= 0) {
                if (isPlayer1Turn) {
                    val diff = player1Score - score
                    if (diff >= 0 && diff != 1) {  // Valid score: not negative and not leaving 1
                        player1Score = diff
                        player1ScoreText.text = player1Score.toString()
                        currentTurnText.text = "Current Turn: $player2Name\nMode: ${if (isLegsMode) "First to $targetNumber Legs" else "First to $targetNumber Sets (3 Legs/Set)"}"
                        isPlayer1Turn = false
                    } else {
                        Toast.makeText(this, "Invalid score: Cannot go below 0 or leave 1", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val diff = player2Score - score
                    if (diff >= 0 && diff != 1) {  // Valid score: not negative and not leaving 1
                        player2Score = diff
                        player2ScoreText.text = player2Score.toString()
                        currentTurnText.text = "Current Turn: $player1Name\nMode: ${if (isLegsMode) "First to $targetNumber Legs" else "First to $targetNumber Sets (3 Legs/Set)"}"
                        isPlayer1Turn = true
                    } else {
                        Toast.makeText(this, "Invalid score: Cannot go below 0 or leave 1", Toast.LENGTH_SHORT).show()
                    }
                }
                scoreInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter a valid score", Toast.LENGTH_SHORT).show()
            }
        }
    }
}