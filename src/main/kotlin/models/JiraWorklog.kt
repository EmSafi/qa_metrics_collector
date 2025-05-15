package models

import java.time.LocalDate

data class JiraWorklog(
    val author: String,
    val date: LocalDate,
    val timeSpent: Int
)
