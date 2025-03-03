package com.example.dartcounter

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class GameActivity : AppCompatActivity() {

    private val TAG = "GameActivity"

    // Constants
    companion object {
        private const val INITIAL_SCORE = 501
        private const val MAX_INPUT_LENGTH = 3
        private const val MAX_SCORE = 180
        private const val DARTS_PER_TURN = 3
        private const val TURNS_FOR_9_DART = 3
        private const val TARGET_LEGS_KEY = "TARGET_NUMBER"
    }

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

    // UI holder for each player
    data class PlayerUI(
        val legsText: TextView,
        val scoreText: TextView,
        val avgText: TextView,
        val nineDartAvgText: TextView,
        val lastScoreText: TextView,
        val dartsThrownText: TextView
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        Log.d(TAG, "onCreate: Starting GameActivity")

        // Get target number of legs from intent
        targetLegs = intent.getIntExtra(TARGET_LEGS_KEY, 1)

        // Initialize MediaPlayer
        mediaPlayer = MediaPlayer().apply {
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: $what, $extra")
                Toast.makeText(this@GameActivity, "MediaPlayer error: $what, $extra", Toast.LENGTH_SHORT).show()
                false
            }
        }

        // Initialize TextToSpeech for fallback
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
                Toast.makeText(this@GameActivity, "TTS initialization failed", Toast.LENGTH_SHORT).show()
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
        modeText.text = getString(R.string.mode_text, if (isLegsMode) getString(R.string.first_to_legs) else getString(R.string.first_to_sets_3_legs_per_set), targetLegs)

        currentTurnText.text = getString(R.string.current_turn_text, player1.name)

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
                if (score in 1..MAX_SCORE) {
                    val currentPlayer = if (isPlayer1Turn) player1 else player2
                    val currentPlayerUI = if (isPlayer1Turn) player1UI else player2UI
                    val otherPlayer = if (isPlayer1Turn) player2 else player1
                    val otherPlayerUI = if (isPlayer1Turn) player2UI else player1UI
                    submitScore(currentPlayer, currentPlayerUI, otherPlayer, otherPlayerUI, score, currentTurnText)
                    playScoreAudio(score)
                } else {
                    showToast(getString(R.string.invalid_input_message))
                }
            } ?: showToast(getString(R.string.invalid_input_message))
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
                turnText.text = getString(R.string.current_turn_text, otherPlayer.name)
            }
        } else {
            showToast(getString(R.string.invalid_score_message))
        }
    }

    private fun updatePlayerStats(player: Player, ui: PlayerUI, score: Int, diff: Int) {
        player.apply {
            // Update highest score
            if (score > highestScore) highestScore = score

            // Update score frequency buckets
            when (score) {
                in 0..39 -> scores0to39++
                in 40..59 -> scores40to59++
                in 60..79 -> scores60to79++
                in 80..99 -> scores80to99++
                in 100..119 -> scores100to119++
                in 120..139 -> scores120to139++
                in 140..159 -> scores140to159++
                in 160..179 -> scores160to179++
            }

            // 9-dart average logic
            if (turnsThisLeg < TURNS_FOR_9_DART) {
                nineDartTemp += score
                turnsThisLeg++
                if (turnsThisLeg == TURNS_FOR_9_DART) {
                    nineDartTotal += nineDartTemp
                    nineDartLegs++
                    nineDartTemp = 0
                }
            }

            // Regular stats update
            this.score = diff
            totalScoreThrown += score
            dartsThrownThisLeg += 1
            totalDartsThrown += DARTS_PER_TURN
            lastScore = score

            // Track checkout attempts and highest finish (only on last turn of leg)
            if (diff == 0 && turnsThisLeg > 0) { // Check if this is the final turn (checkout)
                checkoutAttempts++
                successfulCheckouts++
                if (score > highestFinish) highestFinish = score
            } else if (turnsThisLeg > 0) { // Any other turn in the leg counts as a checkout attempt
                checkoutAttempts++
            }

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
        showToast(getString(R.string.leg_win_toast, winner.name))

        // Check for game over
        if (winner.legs == targetLegs) {
            Log.d(TAG, "Game over: ${winner.name} won, launching StatsActivity")
            try {
                showStatsActivity(winner.name)
                disableGameInput()
                // Do not finish immediately; wait for StatsActivity to take over
                // finish() will be handled in onActivityResult or by StatsActivity
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle game over: ${e.message}", e)
                showToast("Failed to show stats: ${e.message}")
                // Fallback: Return to MatchActivity if StatsActivity fails
                navigateToMatchActivity()
            }
            return
        }

        isPlayer1Start = !isPlayer1Start
        isPlayer1Turn = isPlayer1Start
        turnText.text = getString(R.string.current_turn_text, if (isPlayer1Turn) player1.name else player2.name)
    }

    private fun updatePlayerUI(player: Player, ui: PlayerUI) {
        ui.apply {
            legsText.text = getString(R.string.legs_text, player.name, player.legs)
            scoreText.text = player.score.toString()
            dartsThrownText.text = getString(R.string.darts_thrown_text, player.dartsThrownThisLeg * DARTS_PER_TURN)
            lastScoreText.text = getString(R.string.last_score_text, player.lastScore)
            val threeDartAvg = player.threeDartAverage
            avgText.text = getString(R.string.three_dart_avg_text, String.format(Locale.getDefault(), "%.1f", threeDartAvg))
            nineDartAvgText.text = getString(R.string.nine_dart_avg_text, String.format(Locale.getDefault(), "%.1f", player.nineDartAverage))
        }
    }

    private fun playScoreAudio(score: Int) {
        if (score !in 1..MAX_SCORE) return

        val audioResName = "score${score}"
        val audioResId = resources.getIdentifier(audioResName, "raw", packageName)
        if (audioResId == 0) {
            Log.w(TAG, "Audio file for score $score not found, using TTS fallback")
            showToast(getString(R.string.audio_not_found_toast, score))
            tts.speak(score.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }

        mediaPlayer.reset()
        try {
            mediaPlayer.setDataSource(resources.openRawResourceFd(audioResId))
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio for score $score: ${e.message}", e)
            showToast(getString(R.string.audio_error_toast, e.message))
            tts.speak(score.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showStatsActivity(winnerName: String) {
        val intent = Intent(this, StatsActivity::class.java)
        intent.putExtra("WINNER_NAME", winnerName)
        intent.putExtra("PLAYER1", player1)
        intent.putExtra("PLAYER2", player2)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP) // Ensure new task and clear back stack
        Log.d(TAG, "Launching StatsActivity with intent: $intent, Class: ${StatsActivity::class.java.name}")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch StatsActivity: ${e.message}", e)
            showToast("Failed to launch StatsActivity: ${e.message}")
        }
    }

    private fun disableGameInput() {
        // Disable all input buttons to prevent further gameplay
        findViewById<Button>(R.id.submitScoreButton).isEnabled = false
        (0..9).forEach { buttonId ->
            findViewById<Button>(resources.getIdentifier("btn$buttonId", "id", packageName))?.isEnabled = false
        }
        findViewById<ImageView>(R.id.backspaceIcon).isEnabled = false
    }

    private fun navigateToMatchActivity() {
        Log.w(TAG, "Falling back to MatchActivity due to StatsActivity launch failure")
        val intent = Intent(this, MatchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // Clear stack and bring to top
        startActivity(intent)
        finish() // Close GameActivity to prevent back navigation
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up resources")
        mediaPlayer.release()
        tts.shutdown()
    }
}