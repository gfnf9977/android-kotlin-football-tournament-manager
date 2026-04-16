package com.example.tournament

data class Team(
    var id: String = "",
    val name: String = "",
    var points: Int = 0,
    var playedMatches: Int = 0,
    var goalsScored: Int = 0,
    var goalsConceded: Int = 0
)

data class Player(
    val id: String = "",
    val teamId: String = "",
    val name: String = "",
    val number: Int = 0,
    var position: String = "Не вказано",
    var goals: Int = 0,
    var assists: Int = 0,
    var matchesPlayed: Int = 0,
    var ratingSum: Double = 0.0
)

data class Match(
    var id: String = "",
    var team1Name: String = "",
    var team2Name: String = "",
    var score1: Int = 0,
    var score2: Int = 0,
    var events: String = "",
    var lineup1: String = "",
    var lineup2: String = ""
)