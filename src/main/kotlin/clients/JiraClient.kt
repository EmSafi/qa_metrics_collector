package clients

import io.restassured.RestAssured
import io.restassured.response.Response
import models.Configurations
import models.JiraIssue
import models.JiraWorklog
import org.apache.logging.log4j.LogManager
import utils.JsonParser
import java.time.LocalDate
import java.util.Base64

/**
 * Класс для взаимодействия с JIRA API.
 *
 * @param configs конфигурационные данные
 */
class JiraClient(private val configs: Configurations) {

    private val logger = LogManager.getLogger(this::class.java)

    private val jiraApiURL = configs.jiraApiURL

    private var jiraToken: String? = null

    /**
     * Получает метрики из JIRA по переданному JQL-запросу.
     *
     * @param jql строка, представляющая собой JQL-запрос для поиска в JIRA.
     *
     * @return totalCount, полученный по запросу.
     *
     * @throws IllegalArgumentException если переданная строка jql является пустой или null.
     */
    fun getMetricsByJql(jql: String): Int {
        if (jql.isBlank()) {
            throw IllegalArgumentException("JQL-запрос не может быть пустым или null.")
        }
        logger.info("Отправляем запрос в JIRA для получения метрики c JQL: $jql")
        val response = makeSearchRequest(getAuthToken(), jql)
        return JsonParser().getTotal(response.jsonPath())
    }

    /**
     * Получает список задач релиза из JIRA по заданному JQL-запросу.
     *
     * @param jql строка, представляющая собой JQL-запрос для поиска в JIRA.
     *
     * @return Список объектов JiraIssue, соответствующих результатам JQL-запроса.
     *
     * @throws IllegalArgumentException Если jql является пустым или null.
     */
    fun getIssuesByJql(jql: String): List<JiraIssue> {
        if (jql.isBlank()) {
            throw IllegalArgumentException("JQL-запрос не может быть пустым или null.")
        }
        logger.info("Отправляем запрос в JIRA для получения задач c JQL: $jql")
        val response = makeSearchRequest(getAuthToken(), jql)
        return JsonParser().getIssuesList(response.jsonPath(), configs)
    }

    /**
     * Получает список из даты начала и даты релиза для версии из JIRA.
     *
     * @param version версия в JIRA.
     *
     * @return список из даты начала и даты релиза в JIRA.
     *
     * @throws IllegalArgumentException если переданная строка version является пустой или null.
     */
    fun getVersionDates(version: String): List<LocalDate> {
        if (version.isBlank()) {
            throw IllegalArgumentException("Версия не может быть пустой или null.")
        }
        logger.info("Отправляем запрос в JIRA для получения списка версий.")
        val response = getVersions(getAuthToken())
        return JsonParser().getDates(response.jsonPath(), version)
    }

    /**
     * Получает общее количество часов, затраченных на задачу в определенной версии для группы пользователей.
     *
     * @param issueKey Ключ задачи в JIRA. Не должен быть пустым или null.
     * @param currentVersion Текущая версия, для которой необходимо получить данные по затраченным часам.
     * @param userList Список пользователей, для которых необходимо получить данные по затраченным часам.
     *
     * @return Общее количество часов, затраченных на задачу указанными пользователями.
     *
     * @throws IllegalArgumentException Если issueKey является пустым или null.
     */
    fun getWorkLog(issueKey: String?): List<JiraWorklog> {
        if (issueKey.isNullOrEmpty()) {
            throw IllegalArgumentException("Ключ задачи не может быть пустым или null.")
        }
        logger.info("Отправляем запрос в JIRA для получения списаний задачи $issueKey.")
        val response = getIssueWorkLogs(getAuthToken(), issueKey)
        return JsonParser().getWorklogs(response.jsonPath())
    }

    /**
     * Выполняет GET-запрос к JIRA API с использованием заданного JQL.
     *
     * @param authToken токен авторизации для доступа к API.
     * @param jql строка JQL для поиска.
     *
     * @return объект Response с результатами запроса.
     *
     * @throws RuntimeException если произошла ошибка при выполнении запроса.
     */
    private fun makeSearchRequest(authToken: String, jql: String): Response {
        return try {
            RestAssured.given()
                .header("Authorization", "Basic $authToken")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .queryParam("jql", jql)
                .queryParam("maxResults", 1000)
                .get("${jiraApiURL}search")
                .also { response ->
                    if (response.statusCode() != 200) {
                        throw RuntimeException("Ошибка при выполнении запроса: ${response.statusCode()}")
                    }
                    logger.info("Запрос поиска задач по фильтру выполнен успешно.")
                }
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при выполнении запроса: ${e.message}", e)
        }
    }

    /**
     * Выполняет GET-запрос к JIRA API для получения списка версий проекта.
     *
     * @param authToken токен авторизации для доступа к API.
     *
     * @return объект Response с результатами запроса.
     *
     * @throws RuntimeException если произошла ошибка при выполнении запроса.
     */
    private fun getVersions(authToken: String): Response {
        return try { RestAssured.given()
            .header("Authorization", "Basic $authToken")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .get("${jiraApiURL}project/UZEDO/versions")
            .also { response ->
                if (response.statusCode() != 200) {
                    throw RuntimeException("Ошибка при выполнении запроса: ${response.statusCode()}")
                }
                logger.info("Запрос получения списка версий выполнен успешно.")
            }
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при выполнении запроса: ${e.message}", e)
        }
    }

    /**
     * Выполняет запрос к JIRA для получения всех трудозатрат по указанному ключу задачи.
     *
     * @param authToken Токен аутентификации для доступа к JIRA API.
     * @param issueKey Ключ задачи в JIRA, для которой необходимо получить трудозатраты.
     *
     * @return Ответ от JIRA API, содержащий трудозатраты для указанной задачи.
     *
     * @throws RuntimeException Если запрос завершился с ошибкой или произошла другая ошибка.
     */
    private fun getIssueWorkLogs(authToken: String, issueKey: String): Response {
        return try {
            RestAssured.given()
                .header("Authorization", "Basic $authToken")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .queryParam("maxResults", 1000)
                .get("${jiraApiURL}issue/$issueKey/worklog")
                .also { response ->
                    if (response.statusCode() != 200) {
                        throw RuntimeException("Ошибка при выполнении запроса: ${response.statusCode()}")
                    }
                    logger.info("Запрос выполнен успешно.")
                }
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при выполнении запроса: ${e.message}", e)
        }
    }

    /**
     * Получает токен авторизации для JIRA.
     *
     * @return строка с токеном авторизации.
     *
     * @throws IllegalStateException если не удается получить имя пользователя или пароль.
     */
    private fun getAuthToken(): String {
        if (jiraToken.isNullOrEmpty()) {
            logger.info("Формируем токен авторизации в JIRA.")
            val login = configs.login
            val password = configs.password
            jiraToken = Base64.getEncoder()
                .encodeToString("$login:$password".toByteArray(Charsets.UTF_8))
        }
        else {
            logger.info("Токен авторизации в JIRA уже сформирован.")
        }
        return jiraToken!!
    }
}