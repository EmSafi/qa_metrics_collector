package models

import java.time.LocalDate

data class JiraVersion(
    val name: String,
    val startDate: LocalDate,
    val releaseDate: LocalDate,
    val metrics: List<JiraMetric>,
    val issues: List<JiraIssue>
)
