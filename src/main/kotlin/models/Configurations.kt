package models

import com.google.gson.annotations.SerializedName

/**
 * Класс, представляющий конфигурации для работы с JIRA.
 */
open class Configurations {

    @SerializedName("GetHolidays API URL")
    lateinit var holidaysApiURL: String

    @SerializedName("JIRA URL")
    lateinit var jiraURL: String

    @SerializedName("JIRA API URL")
    lateinit var jiraApiURL: String

    @SerializedName("Логин JIRA")
    lateinit var login: String

    @SerializedName("Пароль JIRA")
    lateinit var password: String

    @SerializedName("Метрики")
    lateinit var metrics: Map<String, String>

    @SerializedName("Список задач релиза")
    lateinit var jqlReleaseIssuesList: String

    @SerializedName("Разработчики")
    lateinit var devsList: List<String>

    @SerializedName("Тестировщики")
    lateinit var qaList: List<String>

    @SerializedName("Инженеры")
    lateinit var devOpsList: List<String>

    @SerializedName("Аналитики")
    lateinit var analyticsList: List<String>

    @SerializedName("Иконка")
    lateinit var icon: String
}