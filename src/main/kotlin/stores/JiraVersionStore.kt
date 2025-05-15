package stores

import models.JiraIssue
import models.JiraVersion

object JiraVersionStore {

    private val jiraVersionList = mutableListOf<JiraVersion>()

    fun add(jiraVersion: JiraVersion) {
        jiraVersionList.add(jiraVersion)
    }

    fun get(version: String): JiraVersion {
        return jiraVersionList.first {it.name == version}
    }

    fun getByIssue(jiraIssue: JiraIssue): JiraVersion {
        return jiraVersionList.first { version -> version.issues.contains(jiraIssue) ||
                version.issues.any { issue -> issue.subtasks.contains(jiraIssue) } }
    }

    fun getSorted(): List<JiraVersion> {
        return jiraVersionList.sortedBy { it.releaseDate }
    }
}