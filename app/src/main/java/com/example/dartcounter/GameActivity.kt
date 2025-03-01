package com.example.dartcounter

import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.Locale

class GameActivity : AppCompatActivity() {

    // Constants
    companion object {
        private const val INITIAL_SCORE = 501
        private const val MAX_INPUT_LENGTH = 3
        private const val MAX_SCORE = 180
        private const val DARTS_PER_TURN = 3
        private const val TURNS_FOR_9_DART = 3
        private const val INVALID_SCORE_MESSAGE = "Invalid score: Cannot go below 0 or leave 1"
        private const val INVALID_INPUT_MESSAGE = "Please enter a valid score (0-180)"
        private const val TARGET_LEGS_KEY = "TARGET_NUMBER" // Key for intent extra
    }

    // Player game state
    data class Player(
        var score: Int = INITIAL_SCORE,
        var legs: Int = 0,
        var sets: Int = 0,
        var dartsThrownThisLeg: Int = 0,
        var totalDartsThrown: Int = 0,
        var totalScoreThrown: Int = 0,
        var lastScore: Int = 0,
        var nineDartTotal: Int = 0,
        var nineDartLegs: Int = 0,
        var turnsThisLeg: Int = 0,
        var nineDartTemp: Int = 0,
        var name: String = "Player"
    ) {
        val nineDartAverage: Float
            get() = if (nineDartLegs > 0) nineDartTotal.toFloat() / (nineDartLegs * TURNS_FOR_9_DART) else 0.0f
    }

    // UI holder for each player
    data class PlayerUI(
        val legsText: TextView,
        val scoreText: TextView,
        val avgText: TextView,
        val nineDartAvgText: TextView,
        val lastScoreText: TextView,
        val dartsThrownText: TextView
    )

    // Game state
    private val player1 = Player()
    private val player2 = Player()
    private lateinit var player1UI: PlayerUI
    private lateinit var player2UI: PlayerUI
    private var isPlayer1Turn = true
    private var isPlayer1Start = true
    private var currentInput = ""
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var tts: TextToSpeech
    private var targetLegs: Int = 1 // Default value, overridden by intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Get target number of legs from intent
        targetLegs = intent.getIntExtra(TARGET_LEGS_KEY, 1) // Default to 1 if not set

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer()

        // Initialize TextToSpeech for fallback
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // Initialize players with intent data
        player1.name = intent.getStringExtra("PLAYER1_NAME") ?: "Player 1"
        player2.name = intent.getStringExtra("PLAYER2_NAME") ?: "Player 2"

        // Initialize UI holders
        player1UI = PlayerUI(
            findViewById(R.id.player1LegsText),
            findViewById(R.id.player1ScoreText),
            findViewById(R.id.player1AvgText),
            findViewById(R.id.player1NineDartAvgText),
            findViewById(R.id.player1LastScoreText),
            findViewById(R.id.player1DartsThrownText)
        )
        player2UI = PlayerUI(
            findViewById(R.id.player2LegsText),
            findViewById(R.id.player2ScoreText),
            findViewById(R.id.player2AvgText),
            findViewById(R.id.player2NineDartAvgText),
            findViewById(R.id.player2LastScoreText),
            findViewById(R.id.player2DartsThrownText)
        )

        // Common UI elements
        val modeText = findViewById<TextView>(R.id.modeText)
        val currentTurnText = findViewById<TextView>(R.id.currentTurnText)
        val totalScoreText = findViewById<TextView>(R.id.totalScoreText)
        val backspaceIcon = findViewById<ImageView>(R.id.backspaceIcon)
        val submitButton = findViewById<Button>(R.id.submitScoreButton)
        val numberButtons = (0..9).map { findViewById<Button>(resources.getIdentifier("btn$it", "id", packageName)) }

        // Initial UI setup
        updatePlayerUI(player1, player1UI)
        updatePlayerUI(player2, player2UI)
        val isLegsMode = intent.getBooleanExtra("IS_LEGS_MODE", true)
        modeText.text = "Mode: ${if (isLegsMode) "First to $targetLegs Legs" else "First to $targetLegs Sets (3 Legs/Set)"}"
        currentTurnText.text = "${player1.name}'s turn to throw"

        // Button listeners
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (currentInput.length < MAX_INPUT_LENGTH) {
                    currentInput += index
                    totalScoreText.text = currentInput
                }
            }
        }

        backspaceIcon.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.dropLast(1)
                totalScoreText.text = currentInput.ifEmpty { "" }
            }
        }

        submitButton.setOnClickListener {
            currentInput.toIntOrNull()?.let { score ->
                if (score in 1..MAX_SCORE) { // Updated to 1..MAX_SCORE since you have score1.mp3 to score180.mp3
                    val currentPlayer = if (isPlayer1Turn) player1 else player2
                    val currentPlayerUI = if (isPlayer1Turn) player1UI else player2UI
                    val otherPlayer = if (isPlayer1Turn) player2 else player1
                    val otherPlayerUI = if (isPlayer1Turn) player2UI else player1UI
                    submitScore(currentPlayer, currentPlayerUI, otherPlayer, otherPlayerUI, score, currentTurnText)
                    playScoreAudio(score)
                } else {
                    showToast(INVALID_INPUT_MESSAGE)
                }
            } ?: showToast(INVALID_INPUT_MESSAGE)
            currentInput = ""
            totalScoreText.text = ""
        }
    }

    private fun submitScore(
        currentPlayer: Player, currentUI: PlayerUI,
        otherPlayer: Player, otherUI: PlayerUI,
        score: Int, turnText: TextView
    ) {
        val diff = currentPlayer.score - score
        if (diff > 1 || diff == 0) {
            updatePlayerStats(currentPlayer, currentUI, score, diff)
            if (currentPlayer.score == 0) {
                handleLegWin(currentPlayer, currentUI, otherPlayer, otherUI, turnText)
            } else {
                isPlayer1Turn = !isPlayer1Turn
                turnText.text = "${otherPlayer.name}'s turn to throw"
            }
        } else {
            showToast(INVALID_SCORE_MESSAGE)
        }
    }

    private fun updatePlayerStats(player: Player, ui: PlayerUI, score: Int, newScore: Int) {
        player.apply {
            if (turnsThisLeg < TURNS_FOR_9_DART) {
                nineDartTemp += score
                turnsThisLeg++
                if (turnsThisLeg == TURNS_FOR_9_DART) {
                    nineDartTotal += nineDartTemp
                    nineDartLegs++
                    nineDartTemp = 0
                }
            }
            this.score = newScore
            totalScoreThrown += score
            dartsThrownThisLeg += 1
            totalDartsThrown += DARTS_PER_TURN
            lastScore = score
            updatePlayerUI(this, ui)
        }
    }

    private fun handleLegWin(
        winner: Player, winnerUI: PlayerUI,
        loser: Player, loserUI: PlayerUI,
        turnText: TextView
    ) {
        winner.legs += 1
        winner.score = INITIAL_SCORE
        loser.score = INITIAL_SCORE
        winner.dartsThrownThisLeg = 0
        loser.dartsThrownThisLeg = 0
        winner.turnsThisLeg = 0
        loser.turnsThisLeg = 0
        updatePlayerUI(winner, winnerUI)
        updatePlayerUI(loser, loserUI)
        showToast("${winner.name} wins a leg!")

        // Check for game over
        if (winner.legs == targetLegs) {
            showGameOverDialog(winner.name)
            disableGameInput() // Optional: Disable further input
            return // Exit early to prevent continuing the game
        }

        isPlayer1Start = !isPlayer1Start
        isPlayer1Turn = isPlayer1Start
        turnText.text = "${if (isPlayer1Turn) player1.name else player2.name}'s turn to throw"
    }

    private fun updatePlayerUI(player: Player, ui: PlayerUI) {
        ui.apply {
            legsText.text = "${player.name}: ${player.legs}"
            scoreText.text = player.score.toString()
            dartsThrownText.text = "Darts Thrown: ${player.dartsThrownThisLeg * DARTS_PER_TURN}"
            lastScoreText.text = "Last Score: ${player.lastScore}"
            val threeDartAvg = if (player.totalDartsThrown > 0) player.totalScoreThrown.toFloat() / (player.totalDartsThrown / 3.0f) else 0.0f
            avgText.text = "3-Dart AVG: %.1f".format(threeDartAvg)
            nineDartAvgText.text = "9-Dart AVG: %.1f".format(player.nineDartAverage)
        }
    }

    private fun playScoreAudio(score: Int) {
        // Map scores 1-180 to raw resource IDs (e.g., score1.mp3, score2.mp3, ..., score180.mp3)
        if (score !in 1..MAX_SCORE) return // Ensure score is valid (1-180, no 0)

        val audioResName = "score${score}"
        val audioResId = resources.getIdentifier(audioResName, "raw", packageName)
        if (audioResId == 0) {
            showToast("Audio file for score $score not found, using TTS fallback")
            tts.speak(score.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            return // Resource not found, use TTS fallback
        }

        // Stop any ongoing playback and play new audio
        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(resources.openRawResourceFd(audioResId))
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            showToast("Error playing audio: ${e.message}")
            tts.speak(score.toString(), TextToSpeech.QUEUE_FLUSH, null, null) // Fallback to TTS
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showGameOverDialog(winnerName: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("$winnerName has won the match with $targetLegs legs!")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish() // Close the activity and return to MatchActivity
            }
            .setCancelable(false) // Prevent dismissing by tapping outside
            .show()
    }

    private fun disableGameInput() {
        // Disable all input buttons to prevent further gameplay
        findViewById<Button>(R.id.submitScoreButton).isEnabled = false
        (0..9).forEach { buttonId ->
            findViewById<Button>(resources.getIdentifier("btn$buttonId", "id", packageName))?.isEnabled = false
        }
        findViewById<ImageView>(R.id.backspaceIcon).isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release() // Clean up MediaPlayer
        tts.shutdown() // Clean up TextToSpeech
    }
}