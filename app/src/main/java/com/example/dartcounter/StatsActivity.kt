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

        // Set up UI elements
        try {
            // Set title with winner's name
            val statsTitle = findViewById<TextView>(R.id.statsTitle)
            statsTitle.text = safeGetString(R.string.game_over_title, winner)

            // Set player names
            findViewById<TextView>(R.id.Player1).text = player1.name
            findViewById<TextView>(R.id.Player2).text = player2.name

            // Populate table with statistics
            populateStatsTable(player1, player2)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting stats: ${e.message}", e)
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

    private fun populateStatsTable(p1: Player, p2: Player) {
        try {
            // Legs
            findViewById<TextView>(R.id.player1Legs).text = p1.legs.toString()
            findViewById<TextView>(R.id.player2Legs).text = p2.legs.toString()

            // Three Dart Average
            findViewById<TextView>(R.id.player1ThreeDartAvg).text = String.format(Locale.getDefault(), "%.2f", p1.threeDartAverage)
            findViewById<TextView>(R.id.player2ThreeDartAvg).text = String.format(Locale.getDefault(), "%.2f", p2.threeDartAverage)

            // First Nine Dart Average
            findViewById<TextView>(R.id.player1NineDartAvg).text = String.format(Locale.getDefault(), "%.2f", p1.nineDartAverage)
            findViewById<TextView>(R.id.player2NineDartAvg).text = String.format(Locale.getDefault(), "%.2f", p2.nineDartAverage)

            // Highest Score
            findViewById<TextView>(R.id.player1HighestScore).text = if (p1.highestScore > 0) p1.highestScore.toString() else "-"
            findViewById<TextView>(R.id.player2HighestScore).text = if (p2.highestScore > 0) p2.highestScore.toString() else "-"

            // Highest Finish
            findViewById<TextView>(R.id.player1HighestFinish).text = if (p1.highestFinish > 0) p1.highestFinish.toString() else "-"
            findViewById<TextView>(R.id.player2HighestFinish).text = if (p2.highestFinish > 0) p2.highestFinish.toString() else "-"

            // Checkout Percentage
            findViewById<TextView>(R.id.player1CheckoutPercentage).text = String.format(Locale.getDefault(), "%.1f%%", p1.checkoutPercentage)
            findViewById<TextView>(R.id.player2CheckoutPercentage).text = String.format(Locale.getDefault(), "%.1f%%", p2.checkoutPercentage)

            // Score Ranges
            findViewById<TextView>(R.id.player1Score180).text = p1.scores180.toString()
            findViewById<TextView>(R.id.player2Score180).text = p2.scores180.toString()

            findViewById<TextView>(R.id.player1Score160Plus).text = p1.scores160to179.toString()
            findViewById<TextView>(R.id.player2Score160Plus).text = p2.scores160to179.toString()

            findViewById<TextView>(R.id.player1Score140Plus).text = p1.scores140to159.toString()
            findViewById<TextView>(R.id.player2Score140Plus).text = p2.scores140to159.toString()

            findViewById<TextView>(R.id.player1Score120Plus).text = p1.scores120to139.toString()
            findViewById<TextView>(R.id.player2Score120Plus).text = p2.scores120to139.toString()

            findViewById<TextView>(R.id.player1Score100Plus).text = p1.scores100to119.toString()
            findViewById<TextView>(R.id.player2Score100Plus).text = p2.scores100to119.toString()

            findViewById<TextView>(R.id.player1Score80Plus).text = p1.scores80to99.toString()
            findViewById<TextView>(R.id.player2Score80Plus).text = p2.scores80to99.toString()

            findViewById<TextView>(R.id.player1Score60Plus).text = p1.scores60to79.toString()
            findViewById<TextView>(R.id.player2Score60Plus).text = p2.scores60to79.toString()

            findViewById<TextView>(R.id.player1Score40Plus).text = p1.scores40to59.toString()
            findViewById<TextView>(R.id.player2Score40Plus).text = p2.scores40to59.toString()

            findViewById<TextView>(R.id.player1Score0Plus).text = p1.scores0to39.toString()
            findViewById<TextView>(R.id.player2Score0Plus).text = p2.scores0to39.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error populating stats table: ${e.message}", e)
            throw e // Re-throw to be caught in onCreate
        }
    }

    private fun onOkClick() {
        finish() // Close StatsActivity and return to previous activity
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up resources")
    }
}