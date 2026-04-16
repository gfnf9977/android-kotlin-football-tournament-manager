package com.example.tournament

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

class StatisticsActivity : AppCompatActivity() {

    private val allPlayers = mutableListOf<Player>()
    private val teamNamesMap = mutableMapOf<String, String>()
    private val displayList = mutableListOf<StatItem>()

    private val database = FirebaseDatabase.getInstance("https://tournamentmanager-820f0-default-rtdb.europe-west1.firebasedatabase.app/").reference

    private lateinit var adapter: StatisticAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val spStatCategory = findViewById<Spinner>(R.id.spStatCategory)
        val lvStatistics = findViewById<ListView>(R.id.lvStatistics)

        val categories = arrayOf(
            "⚽ Найкращі бомбардири (Голи)",
            "👟 Найкращі асистенти (Паси)",
            "⭐ Найцінніші гравці MVP (Рейтинг)"
        )
        spStatCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        adapter = StatisticAdapter(this, displayList)
        lvStatistics.adapter = adapter

        database.child("teams").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val teamId = child.key ?: continue
                    val teamName = child.child("name").getValue(String::class.java) ?: "Невідома"
                    teamNamesMap[teamId] = teamName
                }
                loadPlayers()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        spStatCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateList(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadPlayers() {
        database.child("players").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allPlayers.clear()
                for (child in snapshot.children) {
                    val player = child.getValue(Player::class.java)
                    if (player != null) {
                        allPlayers.add(player)
                    }
                }
                val currentCategory = findViewById<Spinner>(R.id.spStatCategory).selectedItemPosition
                updateList(currentCategory)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateList(categoryIndex: Int) {
        displayList.clear()

        when (categoryIndex) {
            0 -> {
                val sortedScorers = allPlayers.filter { it.goals > 0 }.sortedByDescending { it.goals }

                for ((index, p) in sortedScorers.withIndex()) {
                    val teamName = teamNamesMap[p.teamId] ?: "Вільний агент"
                    displayList.add(
                        StatItem(
                            rank = index + 1,
                            playerName = p.name,
                            teamName = teamName,
                            statValue = p.goals.toString(),
                            statSuffix = "голів"
                        )
                    )
                }
            }
            1 -> {
                val sortedAssists = allPlayers.filter { it.assists > 0 }.sortedByDescending { it.assists }

                for ((index, p) in sortedAssists.withIndex()) {
                    val teamName = teamNamesMap[p.teamId] ?: "Вільний агент"
                    displayList.add(
                        StatItem(
                            rank = index + 1,
                            playerName = p.name,
                            teamName = teamName,
                            statValue = p.assists.toString(),
                            statSuffix = "асистів"
                        )
                    )
                }
            }
            2 -> {
                val playedPlayers = allPlayers.filter { it.matchesPlayed > 0 }

                val sortedMVP = playedPlayers.sortedByDescending { it.ratingSum.toDouble() / it.matchesPlayed }

                for ((index, p) in sortedMVP.withIndex()) {
                    val teamName = teamNamesMap[p.teamId] ?: "Вільний агент"
                    val avgRating = p.ratingSum.toDouble() / p.matchesPlayed
                    val formattedRating = String.format("%.2f", avgRating)

                    displayList.add(
                        StatItem(
                            rank = index + 1,
                            playerName = p.name,
                            teamName = teamName,
                            statValue = formattedRating,
                            statSuffix = "рейтинг",
                            matchesPlayed = p.matchesPlayed
                        )
                    )
                }
            }
        }

        if (displayList.isEmpty()) {
            displayList.add(
                StatItem(
                    rank = 0,
                    playerName = "Немає даних",
                    teamName = "",
                    statValue = "",
                    statSuffix = "",
                    isEmpty = true
                )
            )
        }

        adapter.notifyDataSetChanged()
    }

    data class StatItem(
        val rank: Int,
        val playerName: String,
        val teamName: String,
        val statValue: String,
        val statSuffix: String,
        val matchesPlayed: Int = 0,
        val isEmpty: Boolean = false
    )

    // Кастомний адаптер для відображення статистики
    inner class StatisticAdapter(
        private val context: AppCompatActivity,
        private val items: List<StatItem>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): StatItem = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_statistic, parent, false)
            val item = getItem(position)

            val tvRank = view.findViewById<TextView>(R.id.tvRank)
            val tvPlayerName = view.findViewById<TextView>(R.id.tvPlayerName)
            val tvTeamName = view.findViewById<TextView>(R.id.tvTeamName)
            val tvStatValue = view.findViewById<TextView>(R.id.tvStatValue)
            val tvStatSuffix = view.findViewById<TextView>(R.id.tvStatSuffix)

            if (item.isEmpty) {
                tvRank.visibility = View.GONE
                tvPlayerName.text = item.playerName
                tvTeamName.visibility = View.GONE
                tvStatValue.visibility = View.GONE
                tvStatSuffix.visibility = View.GONE
            } else {
                tvRank.visibility = View.VISIBLE
                tvRank.text = "${item.rank}."

                tvPlayerName.text = item.playerName

                if (item.teamName.isNotEmpty()) {
                    tvTeamName.visibility = View.VISIBLE
                    tvTeamName.text = item.teamName
                } else {
                    tvTeamName.visibility = View.GONE
                }

                tvStatValue.visibility = View.VISIBLE
                tvStatValue.text = item.statValue

                tvStatSuffix.visibility = View.VISIBLE
                tvStatSuffix.text = item.statSuffix

                if (item.matchesPlayed > 0) {
                    val tvMatchesPlayed = view.findViewById<TextView>(R.id.tvMatchesPlayed)
                    tvMatchesPlayed.visibility = View.VISIBLE
                    tvMatchesPlayed.text = "(${item.matchesPlayed} матчів)"
                }
            }

            return view
        }
    }
}