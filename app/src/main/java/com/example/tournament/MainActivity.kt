package com.example.tournament

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private val teamList = mutableListOf<Team>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etTeamName = findViewById<EditText>(R.id.etTeamName)
        val btnAddTeam = findViewById<Button>(R.id.btnAddTeam)
        val lvTeams = findViewById<ListView>(R.id.lvTeams)
        val btnGoToMatch = findViewById<Button>(R.id.btnGoToMatch)
        val btnGoToStatistics = findViewById<Button>(R.id.btnGoToStatistics)
        val btnGoToHistory = findViewById<Button>(R.id.btnGoToHistory)

        val database = FirebaseDatabase.getInstance("https://tournamentmanager-820f0-default-rtdb.europe-west1.firebasedatabase.app/").reference

        val tableAdapter = object : ArrayAdapter<Team>(this, R.layout.item_team, teamList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_team, parent, false)
                val team = teamList[position]

                view.findViewById<TextView>(R.id.tvTeamName).text = team.name
                view.findViewById<TextView>(R.id.tvMatches).text = "І: ${team.playedMatches}"
                view.findViewById<TextView>(R.id.tvGoals).text = "${team.goalsScored}:${team.goalsConceded}"
                view.findViewById<TextView>(R.id.tvPoints).text = "О: ${team.points}"

                return view
            }
        }
        lvTeams.adapter = tableAdapter

        database.child("teams").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                teamList.clear()
                for (teamSnapshot in snapshot.children) {
                    val team = teamSnapshot.getValue(Team::class.java)
                    if (team != null) {
                        team.id = teamSnapshot.key ?: ""
                        teamList.add(team)
                    }
                }
                teamList.sortByDescending { it.points }
                tableAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Помилка читання бази", Toast.LENGTH_SHORT).show()
            }
        })

        btnAddTeam.setOnClickListener {
            val teamName = etTeamName.text.toString().trim()
            if (teamName.isNotEmpty()) {
                val teamId = database.child("teams").push().key ?: return@setOnClickListener
                val newTeam = Team(id = teamId, name = teamName)
                database.child("teams").child(teamId).setValue(newTeam)
                etTeamName.text.clear()
            } else {
                Toast.makeText(this, "Введіть назву команди!", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoToMatch.setOnClickListener {
            val intent = Intent(this, MatchActivity::class.java)
            startActivity(intent)
        }

        btnGoToStatistics.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }

        btnGoToHistory.setOnClickListener {
            val intent = Intent(this, MatchHistoryActivity::class.java)
            startActivity(intent)
        }

        lvTeams.setOnItemClickListener { parent, view, position, id ->
            val selectedTeam = teamList[position]
            val intent = Intent(this, TeamDetailActivity::class.java)
            intent.putExtra("TEAM_ID", selectedTeam.id)
            intent.putExtra("TEAM_NAME", selectedTeam.name)
            startActivity(intent)
        }
    }
}