package edu.nd.pmcburne.hwapp.one


data class GameResponse(
    val games: List<GameWrapper>
)

data class GameWrapper(
    val game: ApiGame
)

data class ApiGame(
    val gameID: String,
    val home: ApiTeam,
    val away: ApiTeam,
    val gameState: String?,
    val startDate: String,
    val startTime: String,
    val currentPeriod: String?,
    val contestClock: String?

)

data class ApiTeam(
    val score: String?,
    val winner: Boolean,
    val names: TeamNames
)

data class TeamNames(
    val short: String
)