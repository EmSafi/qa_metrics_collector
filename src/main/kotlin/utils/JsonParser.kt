package utils

import clients.JiraClient
import com.google.gson.JsonParseException
import io.restassured.path.json.JsonPath
import models.*
import org.apache.logging.log4j.LogManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Класс для парсинга JSON-ответов.
 */
class JsonParser {

    private val logger = LogManager.getLogger(this::class.java)

    /**
     * Извлекает общее количество найденных элементов из JSON-ответа.
     *
     * @param response JSON-ответ в виде объекта JsonPath.
     *
     * @return общее количество элементов, либо 0, если элемент не найден.
     */
    fun getTotal(response: JsonPath): Int {
        return try {
            val total = response.getInt("total")
            logger.info("Общее количество элементов: $total")
            total
        } catch (e: NoSuchElementException) {
            logger.warn("Элемент 'total' не найден в ответе. Возвращаем 0.")
            0
        }
    }

    /**
     * Извлекает список из даты начала и даты релиза для заданной версии из JSON-ответа.
     *
     * @param response JSON-ответ в виде объекта JsonPath.
     * @param version имя версии, для которой необходимо получить список дат.
     *
     * @return список из даты начала и даты релиза в формате строки,
     * либо текущая дата, если релиз еще не был выпущен или дата начала не указана.
     */
    fun getDates(response: JsonPath, version: String): List<LocalDate> {
        if (!versionExists(response, version)) {
            throw NoSuchElementException("Версия $version не найдена.")
        }

        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        fun fetchDate(key: String): LocalDate {
            val dateString = response.getString("find { it.name == '$version' }.$key") ?: currentDate
            if (dateString == currentDate) {
                logger.warn("Дата $key для версии $version не указана. Используется текущая дата: $currentDate")
            }
            return DateUtil().getFormatedDate(dateString)
        }

        return listOf(fetchDate("startDate"), fetchDate("releaseDate"))
    }

    /**
     * Получает список задач из ответа JSON.
     *
     * @param response Ответ в формате JSON, содержащий задачи.
     *
     * @return Список объектов [JiraIssue], представляющих задачи.
     *
     * @throws JsonParseException если не удалось разобрать ответ JSON.
     */
    fun getIssuesList(response: JsonPath, configs: Configurations): List<JiraIssue> {
        val total = response.getInt("total")
        if (total <= 0) return emptyList()

        return try {
            val issues: List<Map<String, Any>> = response.getList("issues")
            issues.map { parseIssue(it, issues, configs) }.removeSubtasks()
        } catch (e: Exception) {
            throw JsonParseException("Не удалось разобрать полученный список задач.", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseIssue(issueMap: Map<String, Any>, issues: List<Map<String, Any>>, configs: Configurations): JiraIssue {
        val key = issueMap["key"] as String
        val fieldsMap = issueMap["fields"] as Map<*, *>
        val type = (fieldsMap["issuetype"] as Map<*, *>)["name"] as String
        val summary = fieldsMap["summary"] as String
        val subtasksFields = fieldsMap["subtasks"] as List<Map<*, *>>
        val subtasks = getSubtasks(subtasksFields, issues, configs).toMutableList()

        if (type == "Epic") {
            subtasks += getEpicIssues(key, issues, configs)
        }

        val jiraClient = JiraClient(configs)
        val worklogs = jiraClient.getWorkLog(key)

        val devEstimation = (fieldsMap["customfield_14706"] as? Float)?.toDouble() ?: 0.0
        val qaEstimation = (fieldsMap["customfield_14705"] as? Float)?.toDouble() ?: 0.0
        val devOpsEstimation = (fieldsMap["customfield_14707"] as? Float)?.toDouble() ?: 0.0
        val analyticsEstimation = (fieldsMap["customfield_14708"] as? Float)?.toDouble() ?: 0.0

        return JiraIssue(key, type, summary,
            listOf(JiraEstimation("dev", devEstimation), JiraEstimation("qa", qaEstimation),
            JiraEstimation("devops", devOpsEstimation), JiraEstimation("analytics", analyticsEstimation)),
            worklogs, subtasks)
    }

    private fun getSubtasks(subtasksFields: List<Map<*, *>>, issues: List<Map<String, Any>>, configs: Configurations): List<JiraIssue> {
        return subtasksFields.mapNotNull { subtaskMap ->
            val subtaskKey = subtaskMap["key"] as String
            issues.find { it["key"] == subtaskKey }?.let { filteredIssueMap ->
                parseIssue(filteredIssueMap, issues, configs)
            }
        }
    }

    private fun getEpicIssues(epicKey: String, issues: List<Map<String, Any>>, configs: Configurations): List<JiraIssue> {
        return issues.mapNotNull { filteredIssueMap ->
            val fieldsMap = filteredIssueMap["fields"] as Map<*, *>
            if (fieldsMap["customfield_10421"] == epicKey) {
                parseIssue(filteredIssueMap, issues, configs)
            } else null
        }
    }

    /**
     * Получает общее время работы по пользователям из ответа JSON.
     *
     * @param response Ответ в формате JSON, содержащий трудозатраты.
     *
     * @return Общее время, затраченное пользователями в секундах.
     *
     * @throws JsonParseException если не удалось разобрать ответ JSON.
     */
    fun getWorklogs(response: JsonPath): List<JiraWorklog> {
        return try {
            val worklogs: List<Map<String, Any>> = response.getList("worklogs")

            buildList {
                worklogs.forEach { workLog ->
                    val author = workLog["author"] as Map<*, *>
                    val userName = author["displayName"] as String
                    val date = DateUtil().getFormatedDate(workLog["started"] as String)
                    val timeSpent = workLog["timeSpentSeconds"] as Int
                    add(JiraWorklog(userName, date, timeSpent))
                }
            }
        } catch (e: Exception) {
            throw JsonParseException("Не удалось разобрать полученный список задач.", e)
        }
    }

    /**
     * Получает список праздничных дней из ответа JSON.
     *
     * @param response Ответ в формате JSON, содержащий праздники.
     *
     * @return Список праздничных дней.
     *
     * @throws JsonParseException если не удалось разобрать ответ JSON.
     */
    fun getHolidaysList(response: JsonPath): List<LocalDate> {
        return try {
            val holidaysList: List<Map<String, Any>> = response.getList("holidays")

            holidaysList.map {
                DateUtil().getFormatedDate(it["date"] as String)
            }
        } catch (e: Exception) {
            throw JsonParseException("Не удалось разобрать полученный список праздничных дней.", e)
        }
    }

    /**
     * Проверяет, существует ли версия с указанным именем в JSON-ответе.
     *
     * @param response объект JsonPath
     * @param version версия
     *
     * @return true, если версия с указанным именем существует в JSON-ответе;
     *         false, если версия не найдена.
     */
    private fun versionExists(response: JsonPath, version: String): Boolean {
        return !response.getString(
            "find { it.name == '$version' }").isNullOrBlank()
    }
}