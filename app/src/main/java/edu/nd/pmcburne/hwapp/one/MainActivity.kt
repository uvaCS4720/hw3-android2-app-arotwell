package edu.nd.pmcburne.hwapp.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import androidx.room.Room
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: GameRepository

    private lateinit var apiService: NcaaApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "games_database"
        ).build()

        repository = GameRepository(apiService, database)

        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(repository) as T
            }
        )[GameViewModel::class.java]

        setContent {
            MaterialTheme {
                NcaaScoresScreen(viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Root screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcaaScoresScreen(viewModel: GameViewModel) {

    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Initialise the date-picker at the currently selected date
    val initialMillis = remember(uiState.selectedDate) {
        runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(uiState.selectedDate)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    // ── Date-picker dialog ───────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("EST")
                        viewModel.onDateSelected(sdf.format(Date(millis)))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Scaffold ─────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NCAA Basketball Scores") },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshGames(uiState.selectedDate, uiState.selectedGender)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Offline banner ───────────────
            if (uiState.isOffline || uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error ?: "You are offline – showing saved scores.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ── Date picker button ───────────
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Date: ${uiState.selectedDate}")
            }

            // ── Men's / Women's toggle ───────
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SegmentedButton(
                    selected = uiState.selectedGender == "men",
                    onClick = { viewModel.onGenderSelected("men") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Men's") }

                SegmentedButton(
                    selected = uiState.selectedGender == "women",
                    onClick = { viewModel.onGenderSelected("women") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Women's") }
            }

            Spacer(Modifier.height(8.dp))

            // ── Loading spinner ──────────────
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // ── Empty state ──────────────────
            if (!uiState.isLoading && uiState.games.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No games found for this date.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── Games list ───────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.games, key = { it.id }) { game ->
                    GameCard(game = game, gender = uiState.selectedGender)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Game card
// ─────────────────────────────────────────────

@Composable
fun GameCard(game: GameEntity, gender: String) {

    // Normalise the status string coming from the NCAA API
    val status = game.gameStatus?.lowercase().orEmpty()
    val isLive = status.contains("live") || status.contains("progress") || status == "in_progress"
    val isFinal = status.contains("final") || status.contains("complete") ||
            status.contains("closed") || status == "f/ot"
    val isUpcoming = !isLive && !isFinal

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

            // ── Status / clock row ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                val statusLabel = when {
                    isFinal -> "FINAL"
                    isLive  -> "● LIVE"
                    else    -> game.startTime.toDisplayTime()
                }
                val statusColor = when {
                    isFinal -> Color.Gray
                    isLive  -> Color(0xFF2E7D32)   // green
                    else    -> MaterialTheme.colorScheme.primary
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Period + clock (live only); "Final" instead of clock when done
                when {
                    isLive -> {
                        val period = periodLabel(game.gamePeriod, gender)
                        val clock  = game.contestClock?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                        Text(
                            text = "$period$clock",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    isFinal -> {
                        // Period label still shows for F/OT clarity
                        if (!game.gamePeriod.isNullOrBlank()) {
                            Text(
                                text = periodLabel(game.gamePeriod, gender),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            Spacer(Modifier.height(8.dp))

            // ── Away team ────────────────────
            TeamRow(
                label        = "Away",
                teamName     = game.awayTeam,
                score        = if (!isUpcoming) game.awayScore else null,
                isWinner     = isFinal && game.awayWinner
            )

            Spacer(Modifier.height(6.dp))

            // ── Home team ────────────────────
            TeamRow(
                label        = "Home",
                teamName     = game.homeTeam,
                score        = if (!isUpcoming) game.homeScore else null,
                isWinner     = isFinal && game.homeWinner
            )

            // ── Tip-off time (upcoming only) ─
            if (isUpcoming) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = "Tip-off: ${game.startTime.toDisplayTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Team row within a card
// ─────────────────────────────────────────────

@Composable
fun TeamRow(
    label: String,
    teamName: String,
    score: String?,
    isWinner: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: label + name (+ trophy if winner)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.width(38.dp)
            )
            Text(
                text       = teamName,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
            )
            if (isWinner) {
                Spacer(Modifier.width(4.dp))
                Text("🏆", fontSize = 12.sp)
            }
        }

        // Right: score
        if (score != null) {
            Text(
                text       = score,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

/**
 * Converts a raw period string (e.g. "1", "2", "3") into a human-readable label.
 *
 * Men's NCAA basketball uses two 20-minute halves.
 * Women's NCAA basketball uses four 10-minute quarters.
 */
fun periodLabel(period: String?, gender: String): String {
    if (period.isNullOrBlank()) return ""
    val num = period.filter { it.isDigit() }.toIntOrNull()
    return when {
        // Overtime
        period.lowercase().contains("ot") -> period.uppercase()
        num == null -> period
        gender == "women" -> when (num) {
            1    -> "1st Qtr"
            2    -> "2nd Qtr"
            3    -> "3rd Qtr"
            4    -> "4th Qtr"
            else -> "${num}th Qtr"
        }
        else -> when (num) {              // men
            1    -> "1st Half"
            2    -> "2nd Half"
            else -> "OT"
        }
    }
}

/**
 * Converts a 24-hour time string (HH:mm or HH:mm:ss) to a user-friendly 12-hour format.
 */
fun String.toDisplayTime(): String {
    val patterns = listOf("HH:mm:ss", "HH:mm", "h:mm a", "h:mma")
    for (pattern in patterns) {
        runCatching {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            val out = SimpleDateFormat("h:mm a", Locale.US)
            return out.format(sdf.parse(this)!!)
        }
    }
    return this   // fall back to raw string if nothing parses
}