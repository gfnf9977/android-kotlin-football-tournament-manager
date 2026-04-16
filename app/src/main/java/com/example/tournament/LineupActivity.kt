package com.example.tournament

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LineupActivity : AppCompatActivity() {

    private val rosterList = mutableListOf<Player>()
    private val rosterNames = mutableListOf<String>()

    private val positionSpinners = mutableListOf<Pair<String, Spinner>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lineup)

        val tvLineupTeamName = findViewById<TextView>(R.id.tvLineupTeamName)
        val spFormation = findViewById<Spinner>(R.id.spFormation)
        val layoutPositionsContainer = findViewById<LinearLayout>(R.id.layoutPositionsContainer)
        val btnSaveLineup = findViewById<Button>(R.id.btnSaveLineup)

        val teamId = intent.getStringExtra("TEAM_ID") ?: ""
        val teamName = intent.getStringExtra("TEAM_NAME") ?: "Команда"
        tvLineupTeamName.text = "Склад: $teamName"

        val database = FirebaseDatabase.getInstance("https://tournamentmanager-820f0-default-rtdb.europe-west1.firebasedatabase.app/").reference

        val formations = arrayOf("4-4-2", "4-3-3", "3-5-2", "4-2-3-1", "5-3-2")
        spFormation.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, formations).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        database.child("players").orderByChild("teamId").equalTo(teamId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val unsortedPlayers = mutableListOf<Player>()
                    for (child in snapshot.children) {
                        val p = child.getValue(Player::class.java)
                        if (p != null) unsortedPlayers.add(p)
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

                    rosterList.clear()
                    rosterNames.clear()
                    rosterNames.add("Оберіть гравця...")
                    for (p in sortedPlayers) {
                        rosterList.add(p)
                        rosterNames.add("${p.number}. ${p.name} [${p.position}]")
                    }
                    buildFormationForm("4-4-2", layoutPositionsContainer)
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        spFormation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (rosterNames.isNotEmpty()) buildFormationForm(formations[position], layoutPositionsContainer)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSaveLineup.setOnClickListener {
            val selectedIds = arrayListOf<String>()
            val selectedNames = arrayListOf<String>()

            for ((role, spinner) in positionSpinners) {
                val pos = spinner.selectedItemPosition
                if (pos > 0) {
                    val player = rosterList[pos - 1]
                    selectedIds.add(player.id)
                    selectedNames.add("[$role] ${player.name}")
                }
            }

            val uniqueIds = selectedIds.toSet()

            if (selectedIds.size < 11) {
                Toast.makeText(this, "Помилка! Ви обрали лише ${selectedIds.size} гравців з 11.", Toast.LENGTH_SHORT).show()
            } else if (uniqueIds.size < 11) {
                Toast.makeText(this, "Помилка! Гравець не може грати на 2 позиціях.", Toast.LENGTH_LONG).show()
            } else {
                selectedIds.reverse()
                selectedNames.reverse()

                val resultIntent = android.content.Intent()
                resultIntent.putStringArrayListExtra("LINEUP_IDS", selectedIds)
                resultIntent.putStringArrayListExtra("LINEUP_NAMES", selectedNames)
                setResult(android.app.Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun buildFormationForm(formation: String, container: LinearLayout) {
        container.removeAllViews()
        positionSpinners.clear()
        val parts = formation.split("-").map { it.toInt() }

        if (parts.size == 3) {
            createPitchLine(parts[2], "FWD", container)
            createPitchLine(parts[1], "MID", container)
        } else if (parts.size == 4) {
            createPitchLine(parts[3], "FWD", container)
            createPitchLine(parts[2], "CAM", container)
            createPitchLine(parts[1], "CDM", container)
        }
        createPitchLine(parts[0], "DEF", container)
        createPitchLine(1, "GK", container)
    }

    private fun createPitchLine(playerCount: Int, rolePrefix: String, container: LinearLayout) {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 16, 0, 16)
            }
            gravity = android.view.Gravity.CENTER
        }
        for (i in 1..playerCount) {
            rowLayout.addView(createPlayerSlot(rolePrefix))
        }
        container.addView(rowLayout)
    }

    private fun createPlayerSlot(rolePrefix: String): LinearLayout {
        val slotLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 12, 4, 12)
            }
            gravity = android.view.Gravity.CENTER
        }

        val jerseySize = (45 * resources.displayMetrics.density).toInt()
        val jerseyLayout = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(jerseySize, jerseySize).apply {
                bottomMargin = 8
            }
            setBackgroundResource(R.drawable.bg_jersey_empty)
        }

        val tvNumber = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            text = "+"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        jerseyLayout.addView(tvNumber)

        val tvRole = TextView(this).apply {
            text = rolePrefix
            textSize = 10f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundResource(R.drawable.bg_role_pill)
            setPadding(16, 4, 16, 4)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }

        val spPlayer = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            adapter = ArrayAdapter(this@LineupActivity, android.R.layout.simple_spinner_item, rosterNames).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setBackgroundResource(R.drawable.spinner_border)
            setPadding(0, 8, 0, 8)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        spPlayer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val selectedPlayer = rosterList[position - 1]
                    tvNumber.text = selectedPlayer.number.toString()
                    tvNumber.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // темно-зелений
                    jerseyLayout.setBackgroundResource(R.drawable.bg_jersey_filled)
                } else {
                    tvNumber.text = "+"
                    tvNumber.setTextColor(android.graphics.Color.WHITE)
                    jerseyLayout.setBackgroundResource(R.drawable.bg_jersey_empty)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        slotLayout.addView(jerseyLayout)
        slotLayout.addView(tvRole)
        slotLayout.addView(spPlayer)

        positionSpinners.add(Pair(rolePrefix, spPlayer))
        return slotLayout
    }
}