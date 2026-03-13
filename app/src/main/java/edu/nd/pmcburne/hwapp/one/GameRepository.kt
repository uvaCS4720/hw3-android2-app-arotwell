package edu.nd.pmcburne.hwapp.one

import androidx.room.withTransaction

class GameRepository(
    private val api: NcaaApiService,
    private val db: AppDatabase
) {
    private val dao = db.gameDao()

    suspend fun loadGames(date: String, gender: String): LoadGamesResult {
        return try {
            val (year, month, day) = date.split("-")
            val response = api.getGames(
                gender = gender,
                year = year,
                month = month,
                day = day
            )

            val entities = response.games.map { wrapper ->
                wrapper.game.toEntity(date, gender)
            }

            db.withTransaction {
                dao.insertGames(entities)
            }

            LoadGamesResult(
                games = dao.getGames(date, gender),
                isOffline = false,
                errorMessage = null
            )
        } catch (e: Exception) {
            val localGames = dao.getGames(date, gender)

            LoadGamesResult(
                games = localGames,
                isOffline = true,
                errorMessage = if (localGames.isEmpty()) {
                    "Could not load scores - No saved data is available for this date"
                } else {
                    "Showing saved scores because the network is unavailable"
                }
            )
        }
    }
}

data class LoadGamesResult(
    val games: List<GameEntity>,
    val isOffline: Boolean,
    val errorMessage: String?
)