package builders

import clients.JiraClient
import models.Configurations
import models.JiraMetric
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Класс для создания мапы метрик по версиям.
 *
 * @param configs конфигурационные данные
 */
class MetricValuesMapBuilder(private val configs: Configurations): AbstractBuilder() {

    /**
     * Создает мапу, связывающую версии с метриками и их значениями.
     *
     * Этот метод выполняет параллельные запросы к JIRA для получения метрик по каждой версии
     * из списка версий и возвращает мапу, где ключами являются версии, а значениями — списки
     * метрик с их значений.
     *
     * @param versionsList Список версий, для которых будут собраны метрики.
     * @param metricsList Список метрик, для которых будут собраны данные.
     *
     * @return metricValuesMap Мапа, где ключами являются версии, а значениями - списки метрик с их значениями.
     */
    fun create(version: String, releaseDate: LocalDate): List<JiraMetric> {
        logger.info("Создаем мапу метрик для версий: $version")
        val jiraClient = JiraClient(configs)
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val futures = mutableListOf<Future<JiraMetric>>()

        try {
            configs.metrics.forEach { metric ->
                val name = metric.key
                val jqlKey = metric.value
                val jqlQuery = JqlBuilder(configs).create(version, jqlKey, releaseDate)

                val future = executor.submit<JiraMetric> {
                    val value = jiraClient.getMetricsByJql(jqlQuery)
                    JiraMetric(name, value)
                }
                futures.add(future)
            }

            return futures.map { it.get() }
        } finally {
            executor.shutdown()
            logger.info("Создание мапы завершено.")
        }
    }
}