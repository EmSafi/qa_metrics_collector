package builders

import models.Configurations
import stores.JiraVersionStore
import utils.Configs
import java.time.LocalDate

/**
 * Класс для построения JQL-запросов.
 *
 * @param configs конфигурационные данные
 */
class JqlBuilder(private val configs: Configurations): AbstractBuilder() {

    /**
     * Создает JQL-запрос на основе заданного релиза, конфигураций и ключа JQL.
     *
     * @param release название релиза, которое будет подставлено в запрос.
     * @param jqlKey ключ JQL, который используется для получения шаблона JQL.
     *
     * @return сформированный JQL-запрос.
     */
    fun create(release: String, jqlKey: String, releaseDate: LocalDate): String {
        logger.info("Создание JQL-запроса для релиза: $release")

        // Заменяем <ReleaseVersion>
        var jqlQuery = jqlKey.replace("<ReleaseVersion>", release)

        // Заменяем <ProjectName>, если оно присутствует
        if (jqlQuery.contains("<ProjectName>")) {
            val projectName = getProjectName(release)
            jqlQuery = jqlQuery.replace("<ProjectName>", projectName)
        }

        // Заменяем <ReleaseDate>, если оно присутствует
        if (jqlQuery.contains("<ReleaseDate>")) {
            val formattedDate = releaseDate.toString() // Здесь можно изменить формат даты, если нужно
            jqlQuery = jqlQuery.replace("<ReleaseDate>", formattedDate)
        }

        logger.info("Сформированный JQL-запрос: $jqlQuery")
        return jqlQuery
    }

    /**
     * Определяет имя проекта на основе названия релиза.
     *
     * @param release название релиза.
     *
     * @return имя проекта, соответствующее релизу.
     */
    private fun getProjectName(release: String): String {
        return when {
            release.contains("VTBL") -> "ВТБ Лизинг"
            release.contains("GPN") -> "ГПН"
            else -> "Продукт"
        }
    }
}