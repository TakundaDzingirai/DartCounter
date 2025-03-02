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
        private const val TARGET_LEGS_KEY = "TARGET_NUMBER"
    }

    // Player game state with additional stats
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
        var name: String = "Player",
        // Additional stats
        var highestScore: Int = 0,
        var highestFinish: Int = 0,
        var checkoutAttempts: Int = 0,
        var successfulCheckouts: Int = 0,
        // Score frequency buckets (0-39, 40-59, ..., 160-179)
        var scores0to39: Int = 0,
        var scores40to59: Int = 0,
        var scores60to79: Int = 0,
        var scores80to99: Int = 0,
        var scores100to119: Int = 0,
        var scores120to139: Int = 0,
        var scores140to159: Int = 0,
        var scores160to179: Int = 0
    ) {
        val nineDartAverage: Float
            get() = if (nineDartLegs > 0) nineDartTotal.toFloat() / (nineDartLegs * TURNS_FOR_9_DART) else 0.0f
        val threeDartAverage: Float
            get() = if (totalDartsThrown > 0) totalScoreThrown.toFloat() / (totalDartsThrown / 3.0f) else 0.0f
        val checkoutPercentage: Float
            get() = if (checkoutAttempts > 0) (successfulCheckouts.toFloat() / checkoutAttempts) * 100 else 0.0f
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
        targetLegs = intent.getIntExtra(TARGET_LEGS_KEY, 1)

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
        modeText.setText(getString(R.string.mode_text, if (isLegsMode) "Legs" else "Sets", targetLegs))

        currentTurnText.setText(getString(R.string.current_turn_text, player1.name))

        // Button listeners
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (currentInput.length < MAX_INPUT_LENGTH) {
                    currentInput += index
                    totalScoreText.setText(currentInput)
                }
            }
        }

        backspaceIcon.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.dropLast(1)
                totalScoreText.setText(currentInput.ifEmpty { "" })
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
            totalScoreText.setText("")
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
                turnText.setText(getString(R.string.current_turn_text, otherPlayer.name))
            }
        } else {
            showToast(getString(R.string.invalid_score_message))
        }
    }

    private fun updatePlayerStats(player: Player, ui: PlayerUI, score: Int, diff: Int) { // Added diff as a parameter
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
            showStatsDialog(winner.name)
            disableGameInput()
            return
        }

        isPlayer1Start = !isPlayer1Start
        isPlayer1Turn = isPlayer1Start
        turnText.setText(getString(R.string.current_turn_text, if (isPlayer1Turn) player1.name else player2.name))
    }

    private fun updatePlayerUI(player: Player, ui: PlayerUI) {
        ui.apply {
            legsText.setText(getString(R.string.legs_text, player.name, player.legs))
            scoreText.setText(player.score.toString())
            dartsThrownText.setText(getString(R.string.darts_thrown_text, player.dartsThrownThisLeg * DARTS_PER_TURN))
            lastScoreText.setText(getString(R.string.last_score_text, player.lastScore))
            val threeDartAvg = player.threeDartAverage
            avgText.setText(getString(R.string.three_dart_avg_text, String.format("%.1f", threeDartAvg)))
            nineDartAvgText.setText(getString(R.string.nine_dart_avg_text, String.format("%.1f", player.nineDartAverage)))
        }
    }

    private fun playScoreAudio(score: Int) {
        if (score !in 1..MAX_SCORE) return

        val audioResName = "score${score}"
        val audioResId = resources.getIdentifier(audioResName, "raw", packageName)
        if (audioResId == 0) {
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
            showToast(getString(R.string.audio_error_toast, e.message))
            tts.speak(score.toString(), TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showStatsDialog(winnerName: String) {
        val statsBuilder = StringBuilder()
        statsBuilder.append(getString(R.string.statistics_header))
        statsBuilder.append("\n                                    ${player1.name}              ${player2.name}\n")
        statsBuilder.append(getString(R.string.three_dart_avg_line, String.format("%.2f", player1.threeDartAverage), String.format("%.2f", player2.threeDartAverage)))
        statsBuilder.append(getString(R.string.first_nine_avg_line, String.format("%.2f", player1.nineDartAverage), String.format("%.2f", player2.nineDartAverage)))
        statsBuilder.append(getString(R.string.highest_score_line, player1.highestScore, player2.highestScore))
        statsBuilder.append("\n${getString(R.string.checkouts_header)}\n")
        statsBuilder.append(getString(R.string.highest_finish_line, player1.highestFinish, player2.highestFinish))
        statsBuilder.append(getString(R.string.checkout_percentage_line, String.format("%.1f", player1.checkoutPercentage), String.format("%.1f", player2.checkoutPercentage)))
        statsBuilder.append("\n${getString(R.string.scores_header)}\n")
        statsBuilder.append(getString(R.string.score_180_line, player1.scores160to179, player2.scores160to179))
        statsBuilder.append(getString(R.string.score_160_plus_line, player1.scores160to179, player2.scores160to179))
        statsBuilder.append(getString(R.string.score_140_plus_line, player1.scores140to159, player2.scores140to159))
        statsBuilder.append(getString(R.string.score_120_plus_line, player1.scores120to139, player2.scores120to139))
        statsBuilder.append(getString(R.string.score_100_plus_line, player1.scores100to119, player2.scores100to119))
        statsBuilder.append(getString(R.string.score_80_plus_line, player1.scores80to99, player2.scores80to99))
        statsBuilder.append(getString(R.string.score_60_plus_line, player1.scores60to79, player2.scores60to79))
        statsBuilder.append(getString(R.string.score_40_plus_line, player1.scores40to59, player2.scores40to59))
        statsBuilder.append(getString(R.string.score_0_plus_line, player1.scores0to39, player2.scores0to39))

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.game_over_title))
            .setMessage(statsBuilder.toString())
            .setPositiveButton(getString(R.string.ok_button)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun disableGameInput() {
        findViewById<Button>(R.id.submitScoreButton).isEnabled = false
        (0..9).forEach { buttonId ->
            findViewById<Button>(resources.getIdentifier("btn$buttonId", "id", packageName))?.isEnabled = false
        }
        findViewById<ImageView>(R.id.backspaceIcon).isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        tts.shutdown()
    }
}