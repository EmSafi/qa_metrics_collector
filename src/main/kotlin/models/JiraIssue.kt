package models

import stores.JiraVersionStore

/**
 * Представляет задачу Jira.
 *
 * @property key уникальный идентификатор задачи.
 * @property type тип задачи.
 * @property summary название задачи.
 * @property subtasks список подзадач, связанных с данной задачей.
 */
data class JiraIssue(
    val key: String,
    val type: String,
    val summary: String,
    val estimations: List<JiraEstimation> = emptyList(),
    val worklogs: List<JiraWorklog> = emptyList(),
    val subtasks: List<JiraIssue> = emptyList()
)

fun List<JiraIssue>.getTotalEstimationByRole(role: String): Double {
    return this.sumOf { it.estimations.getByRole(role) }
}

fun JiraIssue.getWorklogByUsers(users: List<String>): Int {
    val version = JiraVersionStore.getByIssue(this)

    val timeSpent = this.worklogs
        .filter { worklog ->
            users.contains(worklog.author) && worklog.date >= version.startDate && worklog.date <= version.releaseDate
        }
        .sumOf { it.timeSpent }

    val subtasksTimeSpent = this.subtasks
        .sumOf { it.getWorklogByUsers(users) }

    return timeSpent + subtasksTimeSpent
}

fun List<JiraIssue>.getTotalWorklogByUsers(users: List<String>): Int {
    return this.sumOf { it.getWorklogByUsers(users) }
}

fun List<JiraIssue>.getSorted(): List<JiraIssue> {
    fun getTaskOrder(type: String): Int {
        return when (type) {
            "Epic" -> 1
            "История" -> 2
            "Задача" -> 3
            "Ошибка" -> 4
            else -> 5
        }
    }

    val sortedJiraIssuesList = this
        .sortedWith(compareBy({ getTaskOrder(it.type) }, { it.summary }))

    return sortedJiraIssuesList
}

fun List<JiraIssue>.removeSubtasks(): List<JiraIssue> {
    val allSubtasksKeys = this.flatMap { it.subtasks }.map { it.key }
    return this.filter { it.key !in allSubtasksKeys }
}