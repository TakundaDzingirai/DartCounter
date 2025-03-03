package com.example.dartcounter

import android.os.Parcel
import android.os.Parcelable

data class Player(
    var score: Int = 501,
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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "Player",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(score)
        parcel.writeInt(legs)
        parcel.writeInt(sets)
        parcel.writeInt(dartsThrownThisLeg)
        parcel.writeInt(totalDartsThrown)
        parcel.writeInt(totalScoreThrown)
        parcel.writeInt(lastScore)
        parcel.writeInt(nineDartTotal)
        parcel.writeInt(nineDartLegs)
        parcel.writeInt(turnsThisLeg)
        parcel.writeInt(nineDartTemp)
        parcel.writeString(name)
        parcel.writeInt(highestScore)
        parcel.writeInt(highestFinish)
        parcel.writeInt(checkoutAttempts)
        parcel.writeInt(successfulCheckouts)
        parcel.writeInt(scores0to39)
        parcel.writeInt(scores40to59)
        parcel.writeInt(scores60to79)
        parcel.writeInt(scores80to99)
        parcel.writeInt(scores100to119)
        parcel.writeInt(scores120to139)
        parcel.writeInt(scores140to159)
        parcel.writeInt(scores160to179)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Player> {
        override fun createFromParcel(parcel: Parcel): Player = Player(parcel)
        override fun newArray(size: Int): Array<Player?> = arrayOfNulls(size)
    }

    val nineDartAverage: Float
        get() = if (nineDartLegs > 0) nineDartTotal.toFloat() / (nineDartLegs * 3) else 0.0f
    val threeDartAverage: Float
        get() = if (totalDartsThrown > 0) totalScoreThrown.toFloat() / (totalDartsThrown / 3.0f) else 0.0f
    val checkoutPercentage: Float
        get() = if (checkoutAttempts > 0) (successfulCheckouts.toFloat() / checkoutAttempts) * 100 else 0.0f
}