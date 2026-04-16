package com.example.tournament

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TeamDetailActivity : AppCompatActivity() {

    private val playerList = mutableListOf<Player>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_detail)

        val tvDetailTeamName = findViewById<TextView>(R.id.tvDetailTeamName)
        val etPlayerName = findViewById<EditText>(R.id.etPlayerName)
        val etPlayerNumber = findViewById<EditText>(R.id.etPlayerNumber)
        val spinnerPosition = findViewById<Spinner>(R.id.spinnerPosition)
        val btnAddPlayer = findViewById<Button>(R.id.btnAddPlayer)
        val lvPlayers = findViewById<ListView>(R.id.lvPlayers)

        val teamId = intent.getStringExtra("TEAM_ID") ?: ""
        val teamName = intent.getStringExtra("TEAM_NAME") ?: "Невідома команда"
        tvDetailTeamName.text = "Склад: $teamName"

        val database = FirebaseDatabase.getInstance("https://tournamentmanager-820f0-default-rtdb.europe-west1.firebasedatabase.app/").reference

        val positionsArray = arrayOf("Воротар", "Захисник", "Півзахисник", "Нападник", "Універсал")
        val posAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positionsArray)
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPosition.adapter = posAdapter

        val playerNamesList = mutableListOf<String>()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, playerNamesList)
        lvPlayers.adapter = adapter

        database.child("players").orderByChild("teamId").equalTo(teamId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val unsortedPlayers = mutableListOf<Player>()
                    for (playerSnapshot in snapshot.children) {
                        val player = playerSnapshot.getValue(Player::class.java)
                        if (player != null) unsortedPlayers.add(player)
                    }

                    val sortedPlayers = unsortedPlayers.sortedBy { player ->
                        when (player.position) {
                            "Воротар" -> 1
                            "Захисник" -> 2
                            "Півзахисник" -> 3
                            "Нападник" -> 4
                            else -> 5
                        }
                    }

                    playerList.clear()
                    playerNamesList.clear()
                    for (player in sortedPlayers) {
                        playerList.add(player)
                        playerNamesList.add("#${player.number} ${player.name} [${player.position}]")
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@TeamDetailActivity, "Помилка бази", Toast.LENGTH_SHORT).show()
                }
            })

        btnAddPlayer.setOnClickListener {
            val name = etPlayerName.text.toString().trim()
            val numberStr = etPlayerNumber.text.toString()
            val selectedPosition = spinnerPosition.selectedItem.toString()

            if (teamId.isEmpty()) return@setOnClickListener

            if (name.isNotEmpty() && numberStr.isNotEmpty()) {
                val playerId = database.child("players").push().key ?: return@setOnClickListener

                val newPlayer = Player(
                    id = playerId,
                    teamId = teamId,
                    name = name,
                    number = numberStr.toInt(),
                    position = selectedPosition
                )

                database.child("players").child(playerId).setValue(newPlayer)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Гравця додано!", Toast.LENGTH_SHORT).show()
                        etPlayerName.text.clear()
                        etPlayerNumber.text.clear()
                    }
            } else {
                Toast.makeText(this, "Введіть ім'я та номер!", Toast.LENGTH_SHORT).show()
            }
        }

        lvPlayers.setOnItemClickListener { parent, view, position, id ->
            val clickedPlayer = playerList[position]

            val avgRating = if (clickedPlayer.matchesPlayed > 0) {
                String.format("%.2f", clickedPlayer.ratingSum.toDouble() / clickedPlayer.matchesPlayed)
            } else {
                "Немає оцінок"
            }

            val statsMessage = """
                Позиція: ${clickedPlayer.position}
                
                ⚽ Голи: ${clickedPlayer.goals}
                👟 Асисти: ${clickedPlayer.assists}
                🏟 Зіграно матчів: ${clickedPlayer.matchesPlayed}
                ⭐ Середня оцінка: $avgRating
            """.trimIndent()

            val builder = AlertDialog.Builder(this)
            builder.setTitle(clickedPlayer.name)
            builder.setMessage(statsMessage)

            builder.setPositiveButton("Змінити позицію") { _, _ ->
                val posBuilder = AlertDialog.Builder(this)
                posBuilder.setTitle("Нова позиція для: ${clickedPlayer.name}")
                posBuilder.setItems(positionsArray) { _, which ->
                    clickedPlayer.position = positionsArray[which]
                    database.child("players").child(clickedPlayer.id).setValue(clickedPlayer)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Позицію оновлено!", Toast.LENGTH_SHORT).show()
                        }
                }
                posBuilder.setNegativeButton("Скасувати", null)
                posBuilder.show()
            }

            builder.setNegativeButton("Закрити", null)
            builder.show()
        }
    }
}