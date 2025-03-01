package com.example.dartcounter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
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
    private var player1DartsThrownThisLeg = 0  // Darts thrown in current leg
    private var player2DartsThrownThisLeg = 0  // Darts thrown in current leg
    private var player1TotalDartsThrown = 0    // Total darts thrown across all legs
    private var player2TotalDartsThrown = 0    // Total darts thrown across all legs
    private var player1TotalScoreThrown = 0    // Total score accumulated across all legs
    private var player2TotalScoreThrown = 0    // Total score accumulated across all legs
    private var player1LastScore = 0           // Last submitted score for Player 1
    private var player2LastScore = 0           // Last submitted score for Player 2
    private var currentInput = ""              // Tracks typed score

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Get data from intent
        val player1Name = intent.getStringExtra("PLAYER1_NAME") ?: "Player 1"
        val player2Name = intent.getStringExtra("PLAYER2_NAME") ?: "Player 2"
        val isLegsMode = intent.getBooleanExtra("IS_LEGS_MODE", true)
        val targetNumber = intent.getIntExtra("TARGET_NUMBER", 1)

        // Find views
        val player1LegsText = findViewById<TextView>(R.id.player1LegsText)
        val player1ScoreText = findViewById<TextView>(R.id.player1ScoreText)
        val player1AvgText = findViewById<TextView>(R.id.player1AvgText)
        val player1LastScoreText = findViewById<TextView>(R.id.player1LastScoreText)  // New
        val player1DartsThrownText = findViewById<TextView>(R.id.player1DartsThrownText)
        val player2LegsText = findViewById<TextView>(R.id.player2LegsText)
        val player2ScoreText = findViewById<TextView>(R.id.player2ScoreText)
        val player2AvgText = findViewById<TextView>(R.id.player2AvgText)
        val player2LastScoreText = findViewById<TextView>(R.id.player2LastScoreText)  // New
        val player2DartsThrownText = findViewById<TextView>(R.id.player2DartsThrownText)
        val modeText = findViewById<TextView>(R.id.modeText)
        val currentTurnText = findViewById<TextView>(R.id.currentTurnText)
        val totalScoreText = findViewById<TextView>(R.id.totalScoreText)
        val backspaceIcon = findViewById<ImageView>(R.id.backspaceIcon)
        val submitButton = findViewById<Button>(R.id.submitScoreButton)
        val numberButtons = listOf(
            findViewById<Button>(R.id.btn0),
            findViewById<Button>(R.id.btn1),
            findViewById<Button>(R.id.btn2),
            findViewById<Button>(R.id.btn3),
            findViewById<Button>(R.id.btn4),
            findViewById<Button>(R.id.btn5),
            findViewById<Button>(R.id.btn6),
            findViewById<Button>(R.id.btn7),
            findViewById<Button>(R.id.btn8),
            findViewById<Button>(R.id.btn9)
        )

        // Initial UI setup
        player1LegsText.text = "$player1Name: $player1Legs"
        player1ScoreText.text = player1Score.toString()
        player1AvgText.text = "3-Dart AVG: 0.0"
        player1LastScoreText.text = "Last Score: $player1LastScore"  // Initial setup
        player1DartsThrownText.text = "Darts Thrown: $player1DartsThrownThisLeg"
        player2LegsText.text = "$player2Name: $player2Legs"
        player2ScoreText.text = player2Score.toString()
        player2AvgText.text = "3-Dart AVG: 0.0"
        player2LastScoreText.text = "Last Score: $player2LastScore"  // Initial setup
        player2DartsThrownText.text = "Darts Thrown: $player2DartsThrownThisLeg"
        modeText.text = "Mode: ${if (isLegsMode) "First to $targetNumber Legs" else "First to $targetNumber Sets (3 Legs/Set)"}"
        currentTurnText.text = "$player1Name's turn to throw"
        totalScoreText.text = currentInput

        // Number button listeners
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (currentInput.length < 3) {  // Limit to 3 digits (max 180)
                    currentInput += index.toString()
                    totalScoreText.text = currentInput
                }
            }
        }

        // Backspace listener
        backspaceIcon.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.dropLast(1)
                totalScoreText.text = currentInput.ifEmpty { "" }
            }
        }

        // Submit button listener
        submitButton.setOnClickListener {
            val score = currentInput.toIntOrNull()
            if (score != null && score >= 0 && score <= 180) {
                if (isPlayer1Turn) {
                    val diff = player1Score - score
                    if (diff > 1 || diff == 0) {  // Valid: not below 0, not 1
                        player1Score = diff
                        player1TotalScoreThrown += score
                        player1DartsThrownThisLeg += 1
                        player1TotalDartsThrown += 3  // Accumulate across legs
                        player1LastScore = score  // Update last score
                        player1ScoreText.text = player1Score.toString()
                        player1DartsThrownText.text = "Darts Thrown: ${player1DartsThrownThisLeg * 3}"
                        player1LastScoreText.text = "Last Score: $player1LastScore"  // Update UI
                        val avg = if (player1TotalDartsThrown > 0) (player1TotalScoreThrown.toFloat() / (player1TotalDartsThrown / 3.0f)) else 0.0f
                        player1AvgText.text = "3-Dart AVG: %.1f".format(avg)

                        if (player1Score == 0) {
                            player1Legs += 1
                            player1LegsText.text = "$player1Name: $player1Legs"
                            player1Score = 501
                            player2Score = 501
                            player1DartsThrownThisLeg = 0  // Reset for new leg
                            player2DartsThrownThisLeg = 0  // Reset for new leg
                            player1ScoreText.text = player1Score.toString()
                            player2ScoreText.text = player2Score.toString()
                            player1DartsThrownText.text = "Darts Thrown: $player1DartsThrownThisLeg"
                            player2DartsThrownText.text = "Darts Thrown: $player2DartsThrownThisLeg"
                            Toast.makeText(this, "$player1Name wins a leg!", Toast.LENGTH_SHORT).show()
                        }

                        currentTurnText.text = "$player2Name's turn to throw"
                        isPlayer1Turn = false
                    } else {
                        Toast.makeText(this, "Invalid score: Cannot go below 0 or leave 1", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val diff = player2Score - score
                    if (diff > 1 || diff == 0) {  // Valid: not below 0, not 1
                        player2Score = diff
                        player2TotalScoreThrown += score
                        player2DartsThrownThisLeg += 1
                        player2TotalDartsThrown += 3  // Accumulate across legs
                        player2LastScore = score  // Update last score
                        player2ScoreText.text = player2Score.toString()
                        player2DartsThrownText.text = "Darts Thrown: ${player2DartsThrownThisLeg * 3}"
                        player2LastScoreText.text = "Last Score: $player2LastScore"  // Update UI
                        val avg = if (player2TotalDartsThrown > 0) (player2TotalScoreThrown.toFloat() / (player2TotalDartsThrown / 3.0f)) else 0.0f
                        player2AvgText.text = "3-Dart AVG: %.1f".format(avg)

                        if (player2Score == 0) {
                            player2Legs += 1
                            player2LegsText.text = "$player2Name: $player2Legs"
                            player1Score = 501
                            player2Score = 501
                            player1DartsThrownThisLeg = 0  // Reset for new leg
                            player2DartsThrownThisLeg = 0  // Reset for new leg
                            player1ScoreText.text = player1Score.toString()
                            player2ScoreText.text = player2Score.toString()
                            player1DartsThrownText.text = "Darts Thrown: $player1DartsThrownThisLeg"
                            player2DartsThrownText.text = "Darts Thrown: $player2DartsThrownThisLeg"
                            Toast.makeText(this, "$player2Name wins a leg!", Toast.LENGTH_SHORT).show()
                        }

                        currentTurnText.text = "$player1Name's turn to throw"
                        isPlayer1Turn = true
                    } else {
                        Toast.makeText(this, "Invalid score: Cannot go below 0 or leave 1", Toast.LENGTH_SHORT).show()
                    }
                }
                currentInput = ""
                totalScoreText.text = ""
            } else {
                Toast.makeText(this, "Please enter a valid score (0-180)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}