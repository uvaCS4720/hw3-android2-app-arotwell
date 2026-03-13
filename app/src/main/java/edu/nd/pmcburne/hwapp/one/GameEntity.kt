package edu.nd.pmcburne.hwapp.one

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")


data class GameEntity (
    @PrimaryKey
    val id: String,


    val date: String,
    val gender: String,

    //home
    val homeTeam: String,
    val homeScore: String?,
    val homeWinner: Boolean,

    //away
    val awayTeam: String,
    val awayScore: String?,
    val awayWinner: Boolean,

    //game
    val gameStatus: String?, //upcoming, in progress, completed
    val gamePeriod: String?,
    val startTime: String,
    val contestClock: String?

)
fun ApiGame.toEntity(date: String, gender: String): GameEntity {
    return GameEntity(
        id = gameID,
        date = date,
        gender = gender,
        awayTeam = away.names.short,
        homeTeam = home.names.short,
        awayScore = away.score,
        homeScore = home.score,
        awayWinner = away.winner,
        homeWinner = home.winner,
        gameStatus = gameState,
        startTime = startTime,
        gamePeriod = currentPeriod,
        contestClock = contestClock
    )
}