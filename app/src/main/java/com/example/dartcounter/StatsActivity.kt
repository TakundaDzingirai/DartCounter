package com.example.dartcounter

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import java.util.IllegalFormatException
import java.util.Locale

class StatsActivity : AppCompatActivity() {

    private val TAG = "StatsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        Log.d(TAG, "Starting StatsActivity with intent: $intent")

        val winner = intent.getStringExtra("WINNER_NAME") ?: "Unknown"
        Log.d(TAG, "Winner name: $winner")

        // Handle Player1 retrieval based on API level
        val player1: Player? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PLAYER1", Player::class.java)
        } else {
            intent.getParcelableExtra("PLAYER1")
        }
        Log.d(TAG, "Player1: ${player1?.name ?: "null"}")

        // Handle Player2 retrieval based on API level
        val player2: Player? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("PLAYER2", Player::class.java)
        } else {
            intent.getParcelableExtra("PLAYER2")
        }
        Log.d(TAG, "Player2: ${player2?.name ?: "null"}")

        // Check if players are not null before proceeding
        if (player1 == null || player2 == null) {
            Log.e(TAG, "Player data missing: player1=$player1, player2=$player2")
            Toast.makeText(this, "Player data missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Find the TextView to display stats
        val statsTitle = findViewById<TextView>(R.id.statsTitle)
        val statsText = findViewById<TextView>(R.id.statsText)
        try {
            statsTitle.text = safeGetString(R.string.game_over_title, winner)
            statsText.text = buildStatsString(player1, player2)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting stats text: ${e.message}", e)
            Toast.makeText(this, "Error displaying stats: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up OK button click listener
        findViewById<Button>(R.id.okButton).setOnClickListener {
            onOkClick()
        }
    }

    private fun safeGetString(resId: Int, vararg args: Any): String {
        return try {
            getString(resId, *args)
        } catch (e: IllegalFormatException) {
            Log.e(TAG, "Invalid format for string resource $resId with args: ${args.contentToString()}", e)
            getString(resId) // Fallback to raw string without formatting if args fail
        }
    }

    private fun buildStatsString(p1: Player, p2: Player): String {
        val statsBuilder = StringBuilder()
        try {
            statsBuilder.append(getString(R.string.statistics_header))
            statsBuilder.append("\n                                    ${p1.name}              ${p2.name}\n")
            statsBuilder.append(safeGetString(R.string.three_dart_avg_line, String.format(Locale.getDefault(), "%.2f", p1.threeDartAverage), String.format(Locale.getDefault(), "%.2f", p2.threeDartAverage)))
            statsBuilder.append(safeGetString(R.string.first_nine_avg_line, String.format(Locale.getDefault(), "%.2f", p1.nineDartAverage), String.format(Locale.getDefault(), "%.2f", p2.nineDartAverage)))
            statsBuilder.append(safeGetString(R.string.highest_score_line, p1.highestScore, p2.highestScore))
            statsBuilder.append("\n${getString(R.string.checkouts_header)}\n")
            statsBuilder.append(safeGetString(R.string.highest_finish_line, p1.highestFinish, p2.highestFinish))
            statsBuilder.append(safeGetString(R.string.checkout_percentage_line, String.format(Locale.getDefault(), "%.1f", p1.checkoutPercentage), String.format(Locale.getDefault(), "%.1f", p2.checkoutPercentage)))
            statsBuilder.append("\n${getString(R.string.scores_header)}\n")
            statsBuilder.append(safeGetString(R.string.score_180_line, p1.scores160to179, p2.scores160to179))
            statsBuilder.append(safeGetString(R.string.score_160_plus_line, p1.scores160to179, p2.scores160to179))
            statsBuilder.append(safeGetString(R.string.score_140_plus_line, p1.scores140to159, p2.scores140to159))
            statsBuilder.append(safeGetString(R.string.score_120_plus_line, p1.scores120to139, p2.scores120to139))
            statsBuilder.append(safeGetString(R.string.score_100_plus_line, p1.scores100to119, p2.scores100to119))
            statsBuilder.append(safeGetString(R.string.score_80_plus_line, p1.scores80to99, p2.scores80to99))
            statsBuilder.append(safeGetString(R.string.score_60_plus_line, p1.scores60to79, p2.scores60to79))
            statsBuilder.append(safeGetString(R.string.score_40_plus_line, p1.scores40to59, p2.scores40to59))
            statsBuilder.append(safeGetString(R.string.score_0_plus_line, p1.scores0to39, p2.scores0to39))
        } catch (e: Exception) {
            Log.e(TAG, "Error building stats string: ${e.message}", e)
            throw e // Re-throw to ensure the error is caught in onCreate
        }

        return statsBuilder.toString()
    }

    private fun onOkClick() {
        finish() // Close StatsActivity and return to GameActivity or previous activity
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up resources")
        // No additional cleanup needed since Player objects are Parcelable and managed by the system
    }
}