import builders.HtmlBuilder
import builders.JqlBuilder
import builders.MetricValuesMapBuilder
import clients.JiraClient
import com.google.gson.JsonParseException
import models.JiraVersion
import org.apache.logging.log4j.LogManager
import stores.JiraVersionStore
import utils.Configs
import utils.DateUtil
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.naming.ConfigurationException

private val logger = LogManager.getLogger()

/**
 * Главный метод для запуска приложения, отвечает за инициализацию конфигураций и запуск процесса
 * сбора метрик и трудозатрат.
 *
 * @throws JsonParseException если возникла ошибка при чтении конфигураций
 */
fun main() {
    logger.info("Старт сбора метрик.")
    val configs = Configs().readConfig()
    logger.info("Конфиги получены.")

    logger.info("Получение параметров из строки запуска.")
    val currentVersion = System.getProperty("currentVersion")
    if (currentVersion.isNullOrBlank()) {
        throw ConfigurationException("CURRENT_VERSION не был передан.")
    }
    val previousVersions = System.getProperty("previousVersionsList")
    val versionsList = buildVersionsList(currentVersion, previousVersions).also {
        logger.info("Список версий: ${it.joinToString(", ")}")
    }

    val jiraClient = JiraClient(configs)
    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    val futures: MutableList<Future<*>> = mutableListOf()

    versionsList.forEach { version ->
        futures.add(executor.submit {
            val versionDates = jiraClient.getVersionDates(version)
            val startDate = versionDates[0]
            val releaseDate = versionDates[1]
            val metrics = MetricValuesMapBuilder(configs).create(version, releaseDate)
            val jql = configs.jqlReleaseIssuesList.replace("<ReleaseVersion>", version)
            val issues = jiraClient.getIssuesByJql(jql)
            JiraVersionStore.add(JiraVersion(version, startDate, releaseDate, metrics, issues))
        })
    }

    futures.forEach { it.get() }
    executor.shutdown()

    logger.info("Начинаем составление отчета в формате html.")
    HtmlBuilder(configs).create(currentVersion)
}

/**
 * Формирует список версий, включая текущую и предыдущие версии.
 *
 * @param currentVersion Текущая версия приложения.
 * @param previousVersions Предыдущие версии приложения.
 *
 * @return Список версий, включая текущую и предыдущие версии.
 */
private fun buildVersionsList(currentVersion: String, previousVersions: String?): List<String> {
    val versionsSet = mutableSetOf(currentVersion)

    if (!previousVersions.isNullOrBlank()) {
        versionsSet.addAll(previousVersions.split(", "))
        if (versionsSet.size < ((1 + previousVersions.split(", ").size))) {
            logger.info("В списке версий есть повторяющиеся значения. Они будут удалены.")
        }
    }

    return versionsSet.toList()
}