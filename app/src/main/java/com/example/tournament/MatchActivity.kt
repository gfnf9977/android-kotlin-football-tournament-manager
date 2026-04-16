package com.example.tournament

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MatchActivity : AppCompatActivity() {

    private val teamsList = mutableListOf<Team>()
    private val playersTeam1 = mutableListOf<Player>()
    private val playersTeam2 = mutableListOf<Player>()

    private var startingLineupIds1 = arrayListOf<String>()
    private var startingLineupNames1 = arrayListOf<String>()

    private var startingLineupIds2 = arrayListOf<String>()
    private var startingLineupNames2 = arrayListOf<String>()

    private var score1 = 0
    private var score2 = 0

    private var selectedTeam1: Team? = null
    private var selectedTeam2: Team? = null

    private var matchEventsLog = ""

    private val database = FirebaseDatabase.getInstance("https://tournamentmanager-820f0-default-rtdb.europe-west1.firebasedatabase.app/").reference

    private val lineupLauncher1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startingLineupIds1 = result.data?.getStringArrayListExtra("LINEUP_IDS") ?: arrayListOf()
            startingLineupNames1 = result.data?.getStringArrayListExtra("LINEUP_NAMES") ?: arrayListOf()
            findViewById<TextView>(R.id.tvLineup1Display).text = startingLineupNames1.joinToString("\n")
            findViewById<Button>(R.id.btnSetLineup1).text = "Склад 1 ✅"
            checkIfReadyToStart()
        }
    }

    private val lineupLauncher2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startingLineupIds2 = result.data?.getStringArrayListExtra("LINEUP_IDS") ?: arrayListOf()
            startingLineupNames2 = result.data?.getStringArrayListExtra("LINEUP_NAMES") ?: arrayListOf()
            findViewById<TextView>(R.id.tvLineup2Display).text = startingLineupNames2.joinToString("\n")
            findViewById<Button>(R.id.btnSetLineup2).text = "Склад 2 ✅"
            checkIfReadyToStart()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        val spTeam1 = findViewById<Spinner>(R.id.spTeam1)
        val spTeam2 = findViewById<Spinner>(R.id.spTeam2)
        val btnSetLineup1 = findViewById<Button>(R.id.btnSetLineup1)
        val btnSetLineup2 = findViewById<Button>(R.id.btnSetLineup2)
        val btnStartMatch = findViewById<Button>(R.id.btnStartMatch)

        val layoutScoreboard = findViewById<LinearLayout>(R.id.layoutScoreboard)
        val tvLiveScore = findViewById<TextView>(R.id.tvLiveScore)
        val btnAddGoal1 = findViewById<Button>(R.id.btnAddGoal1)
        val btnAddGoal2 = findViewById<Button>(R.id.btnAddGoal2)
        val btnFinishMatch = findViewById<Button>(R.id.btnFinishMatch)

        val teamNamesList = mutableListOf<String>()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teamNamesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTeam1.adapter = adapter
        spTeam2.adapter = adapter

        database.child("teams").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val team = child.getValue(Team::class.java)
                    if (team != null) {
                        team.id = child.key ?: ""
                        teamsList.add(team)
                        teamNamesList.add(team.name)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnSetLineup1.setOnClickListener {
            if (spTeam1.selectedItemPosition < 0) return@setOnClickListener
            selectedTeam1 = teamsList[spTeam1.selectedItemPosition]
            val intent = Intent(this, LineupActivity::class.java)
            intent.putExtra("TEAM_ID", selectedTeam1!!.id)
            intent.putExtra("TEAM_NAME", selectedTeam1!!.name)
            lineupLauncher1.launch(intent)
        }

        btnSetLineup2.setOnClickListener {
            if (spTeam2.selectedItemPosition < 0) return@setOnClickListener
            selectedTeam2 = teamsList[spTeam2.selectedItemPosition]
            val intent = Intent(this, LineupActivity::class.java)
            intent.putExtra("TEAM_ID", selectedTeam2!!.id)
            intent.putExtra("TEAM_NAME", selectedTeam2!!.name)
            lineupLauncher2.launch(intent)
        }

        btnStartMatch.setOnClickListener {
            spTeam1.isEnabled = false
            spTeam2.isEnabled = false
            findViewById<LinearLayout>(R.id.layoutLineupButtons).visibility = View.GONE
            btnStartMatch.visibility = View.GONE

            layoutScoreboard.visibility = View.VISIBLE
            loadRosters()
        }

        btnAddGoal1.setOnClickListener {
            val startingPlayers = startingLineupIds1.mapNotNull { id -> playersTeam1.find { it.id == id } }
            showGoalDetailsDialog(selectedTeam1!!, startingPlayers) { scorer, assistant, minute ->
                score1++
                tvLiveScore.text = "$score1 : $score2"
                scorer.goals++
                database.child("players").child(scorer.id).setValue(scorer)

                if (assistant != null) {
                    assistant.assists++
                    database.child("players").child(assistant.id).setValue(assistant)
                    val eventMessage = "$minute' ⚽ ${scorer.name} (пас: ${assistant.name})"
                    addEventToUI(eventMessage, isTeam1 = true)
                } else {
                    val eventMessage = "$minute' ⚽ ${scorer.name} (соло)"
                    addEventToUI(eventMessage, isTeam1 = true)
                }
            }
        }

        btnAddGoal2.setOnClickListener {
            val startingPlayers = startingLineupIds2.mapNotNull { id -> playersTeam2.find { it.id == id } }
            showGoalDetailsDialog(selectedTeam2!!, startingPlayers) { scorer, assistant, minute ->
                score2++
                tvLiveScore.text = "$score1 : $score2"
                scorer.goals++
                database.child("players").child(scorer.id).setValue(scorer)

                if (assistant != null) {
                    assistant.assists++
                    database.child("players").child(assistant.id).setValue(assistant)
                    val eventMessage = "$minute' ⚽ ${scorer.name} (пас: ${assistant.name})"
                    addEventToUI(eventMessage, isTeam1 = false)
                } else {
                    val eventMessage = "$minute' ⚽ ${scorer.name} (соло)"
                    addEventToUI(eventMessage, isTeam1 = false)
                }
            }
        }

        btnFinishMatch.setOnClickListener {
            if (selectedTeam1 != null && selectedTeam2 != null) {
                val t1 = selectedTeam1!!
                val t2 = selectedTeam2!!

                t1.playedMatches++
                t1.goalsScored += score1
                t1.goalsConceded += score2
                if (score1 > score2) t1.points += 3 else if (score1 == score2) t1.points += 1

                t2.playedMatches++
                t2.goalsScored += score2
                t2.goalsConceded += score1
                if (score2 > score1) t2.points += 3 else if (score1 == score2) t2.points += 1

                database.child("teams").child(t1.id).setValue(t1)
                database.child("teams").child(t2.id).setValue(t2)

                val matchId = database.child("matches").push().key ?: return@setOnClickListener

                val newMatch = Match(
                    id = matchId,
                    team1Name = t1.name,
                    team2Name = t2.name,
                    score1 = score1,
                    score2 = score2,
                    events = matchEventsLog,
                    lineup1 = startingLineupNames1.joinToString("\n"),
                    lineup2 = startingLineupNames2.joinToString("\n")
                )

                database.child("matches").child(matchId).setValue(newMatch)

                val played1 = startingLineupIds1.mapNotNull { id -> playersTeam1.find { it.id == id } }
                val played2 = startingLineupIds2.mapNotNull { id -> playersTeam2.find { it.id == id } }

                if (played1.isNotEmpty() && played2.isNotEmpty()) {
                    showRatingsDialog(t1, played1, startingLineupNames1, t2, played2, startingLineupNames2)
                } else {
                    finish()
                }
            }
        }
    }

    private fun checkIfReadyToStart() {
        val btnStartMatch = findViewById<Button>(R.id.btnStartMatch)
        if (startingLineupIds1.isNotEmpty() && startingLineupIds2.isNotEmpty()) {
            btnStartMatch.isEnabled = true
        }
    }

    private fun loadRosters() {
        playersTeam1.clear()
        playersTeam2.clear()

        database.child("players").orderByChild("teamId").equalTo(selectedTeam1!!.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val p = child.getValue(Player::class.java)
                        if (p != null) playersTeam1.add(p)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        database.child("players").orderByChild("teamId").equalTo(selectedTeam2!!.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val p = child.getValue(Player::class.java)
                        if (p != null) playersTeam2.add(p)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showGoalDetailsDialog(team: Team, roster: List<Player>, onGoalRegistered: (Player, Player?, String) -> Unit) {
        if (roster.isEmpty()) return
        val playerNames = roster.map { "${it.number}. ${it.name}" }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Автор голу (${team.name})")
        builder.setItems(playerNames) { _, scorerIdx ->
            val scorer = roster[scorerIdx]

            fun askForMinute(selectedScorer: Player, selectedAssistant: Player?) {
                val input = EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    hint = "Наприклад: 45 або 90"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }

                AlertDialog.Builder(this)
                    .setTitle("На якій хвилині забито гол?")
                    .setView(input)
                    .setCancelable(false)
                    .setPositiveButton("Зберегти гол") { _, _ ->
                        val minute = input.text.toString().trim()
                        val minuteStr = if (minute.isNotEmpty()) minute else "??"
                        onGoalRegistered(selectedScorer, selectedAssistant, minuteStr)
                    }
                    .show()
            }

            val assistBuilder = AlertDialog.Builder(this)
            assistBuilder.setTitle("Хто віддав асист?")
            val assistants = roster.filter { it.id != scorer.id }
            val assistantNames = assistants.map { "${it.number}. ${it.name}" }.toTypedArray()

            assistBuilder.setItems(assistantNames) { _, assistIdx ->
                askForMinute(scorer, assistants[assistIdx])
            }
            assistBuilder.setNegativeButton("Без асисту (соло)") { _, _ ->
                askForMinute(scorer, null)
            }
            assistBuilder.show()
        }
        builder.show()
    }

    private fun addEventToUI(message: String, isTeam1: Boolean) {
        val layoutEventsContainer = findViewById<LinearLayout>(R.id.layoutEventsContainer)
        val scrollViewEvents = findViewById<ScrollView>(R.id.scrollViewEvents)

        val tvEvent = TextView(this).apply {
            text = message
            textSize = 14f
            setPadding(16, 8, 16, 8)
            setTextColor(android.graphics.Color.BLACK)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = if (isTeam1) android.view.Gravity.START else android.view.Gravity.END
            params.setMargins(0, 4, 0, 4)
            layoutParams = params

            setBackgroundColor(if (isTeam1) android.graphics.Color.parseColor("#E3F2FD") else android.graphics.Color.parseColor("#FFEBEE"))
        }

        layoutEventsContainer.addView(tvEvent)

        matchEventsLog += "$message\n"

        scrollViewEvents.post {
            scrollViewEvents.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showRatingsDialog(
        team1: Team, players1: List<Player>, names1: List<String>,
        team2: Team, players2: List<Player>, names2: List<String>
    ) {
        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val playerSeekBars = mutableMapOf<Player, SeekBar>()

        fun buildTeamRatingsBlock(team: Team, players: List<Player>, names: List<String>) {
            val tvTitle = TextView(this@MatchActivity).apply {
                text = "Команда: ${team.name}"
                textSize = 20f
                setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 32, 0, 16)
            }
            container.addView(tvTitle)

            for (i in players.indices) {
                val player = players[i]
                val displayName = if (i < names.size) names[i] else player.name

                val tvName = TextView(this@MatchActivity).apply {
                    text = displayName
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 16, 0, 8)
                }

                val tvScore = TextView(this@MatchActivity).apply {
                    text = "Оцінка: 6"
                    textSize = 14f
                    setTextColor(android.graphics.Color.DKGRAY)
                }

                val seekBar = SeekBar(this@MatchActivity).apply {
                    max = 9
                    progress = 5
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            tvScore.text = "Оцінка: ${progress + 1}"
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                }

                container.addView(tvName)
                container.addView(seekBar)
                container.addView(tvScore)
                playerSeekBars[player] = seekBar
            }
        }

        buildTeamRatingsBlock(team1, players1, names1)
        buildTeamRatingsBlock(team2, players2, names2)

        scrollView.addView(container)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Оцінки за матч (1-10)")
        builder.setView(scrollView)
        builder.setCancelable(false)

        builder.setPositiveButton("Зберегти та вийти") { _, _ ->
            for ((player, bar) in playerSeekBars) {
                val rating = bar.progress + 1
                player.ratingSum += rating
                player.matchesPlayed += 1
                database.child("players").child(player.id).setValue(player)
            }
            Toast.makeText(this, "Матч успішно збережено!", Toast.LENGTH_LONG).show()
            finish()
        }

        builder.show()
    }
}