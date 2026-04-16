package com.example.tournament

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MatchHistoryActivity : AppCompatActivity() {

    private val matchList = mutableListOf<Match>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_history)

        val lvMatchHistory = findViewById<ListView>(R.id.lvMatchHistory)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        lvMatchHistory.adapter = adapter

        val database = FirebaseDatabase.getInstance("https://tournamentmanager-820f0-default-rtdb.europe-west1.firebasedatabase.app/").reference

        database.child("matches").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                matchList.clear()
                (adapter as ArrayAdapter<String>).clear()

                for (child in snapshot.children) {
                    val m = child.getValue(Match::class.java)
                    if (m != null) {
                        matchList.add(m)
                        adapter.add("${m.team1Name} ${m.score1}:${m.score2} ${m.team2Name}")
                    }
                }
                matchList.reverse()
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        lvMatchHistory.setOnItemClickListener { _, _, position, _ ->
            val match = matchList[position]
            val detailMessage = """
                РЕЗУЛЬТАТ: ${match.score1}:${match.score2}
                
                ПОДІЇ:
                ${if(match.events.isEmpty()) "Без голів" else match.events}
                
                СКЛАД ${match.team1Name}:
                ${match.lineup1}
                
                СКЛАД ${match.team2Name}:
                ${match.lineup2}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("${match.team1Name} vs ${match.team2Name}")
                .setMessage(detailMessage)
                .setPositiveButton("ОК", null)
                .show()
        }
    }
}