package builders

import kotlinx.html.*
import kotlinx.html.stream.createHTML
import models.*
import stores.JiraVersionStore
import utils.DateUtil
import utils.MathUtil
import java.io.File
import java.nio.charset.Charset
import java.util.*

/**
 * Класс для построения HTML-отчетов по метрикам.
 */
class HtmlBuilder(private val configs: Configurations): AbstractBuilder() {

    private val jiraURL = configs.jiraURL

    private val metricNames = configs.metrics.keys

    /**
     * Создает HTML-отчет на основе заданных списка метрик, списка версий, мапы значений метрик и мапы трудозатрат.
     *
     * @param currentVersion текущая версия.
     */
    fun create(currentVersion: String) {

        logger.info("Создаем HTML-отчет по метрикам.")

        val versionsList = JiraVersionStore.getSorted()

        val htmlContent = createHTML().html {
            head {
                meta(charset = "UTF-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
                title("Отчет по релизу $currentVersion")

                val iconBytes = configs.icon.toByteArray(Charsets.UTF_8)
                val iconBase64 = Base64.getEncoder().encodeToString(iconBytes)
                link(rel = "icon", href = "data:image/svg+xml;base64,$iconBase64", type = "image/svg+xml")

                loadScript("src/main/resources/chart.umd.js")
                loadScript("src/main/resources/xlsx.full.min.js")

                style {
                    unsafe { raw(parseStyles()) }
                }
            }
            body {
                div("container") {
                    div("box") { +"ОТЧЕТ ПО РЕЛИЗУ $currentVersion" }
                    div("box right-box") {
                        button {
                            onClick = "exportToExcel()"
                            +"Экспорт в Excel"
                        }
                    }
                }
                div("table-container") {
                    div("caption") {
                        span("caption-title") { +"Статистика по метрикам" }
                    }
                    this@body.createMetricsTable(versionsList)
                }
                if (versionsList.size > 1) {
                    div("chart-container") {
                        this@body.createChartContainer()
                    }
                }
                this.createStatBoxes(currentVersion)
                div("table-container") {
                    div("caption")
                    {
                        span("caption-title") { +"Детализация трудозатрат" }
                        div("text-container") {
                            span("text-item text-item-epic") { +"Эпик" }
                            span("text-item text-item-story") { +"История" }
                            span("text-item text-item-task") { +"Задача" }
                            span("text-item text-item-bug") { +"Ошибка" }
                            span("text-item text-item-other") { +"Прочее" }
                        }
                    }
                    this@body.createWorkLogTable(currentVersion)
                    div("caption") {
                        span("caption-title") { +"ВСЕГО: ${JiraVersionStore.get(currentVersion).issues.size}" }
                    }
                }
                script {
                    unsafe {
                        raw(
                            ScriptBuilder().create(versionsList, metricNames)
                        )
                    }
                }
            }
        }

        File("report.html").writeText(htmlContent, Charset.forName("UTF-8"))
        logger.info("HTML-отчет успешно создан и сохранен в файл report.html.")
    }

    /**
     * Читает стили из файла styles.css.
     *
     * @return строка с содержимым файла стилей.
     */
    private fun parseStyles(): String {
        logger.info("Читаем стили из файла styles.css.")
        return try {
            File("src/main/resources/styles.css").readText()
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при чтении файла стилей: ${e.message}", e)
        }
    }

    /**
     * Загружает JavaScript.
     *
     * @param src Путь к файлу JavaScript, который необходимо загрузить.
     */
    private fun HEAD.loadScript(src: String) {
        script {
            unsafe { raw(File(src).readText()) }
        }
    }

    /**
     * Создает таблицу метрик на основе переданных данных.
     *
     * @param versionsList список версий, которые будут отображены в таблице отчета.
     */
    private fun BODY.createMetricsTable(versionsList: List<JiraVersion>) {
        // Создаем таблицу
        table {
            id = "metricsTable"
            thead {
                tr {
                    th { +"Название метрики" } // Первый столбец
                    versionsList.forEach { version ->
                        th { +version.name } // Заголовки для каждой версии
                    }
                }
            }
            tbody {
                metricNames.forEach { metricName ->
                    tr {
                        td { +metricName } // Название метрики в первом столбце

                        versionsList.forEach { version ->
                            td {
                                // Находим значение метрики для текущей версии
                                +version.metrics.find { it.name == metricName }?.value.toString()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun BODY.createStatBoxes(currentVersion: String) {
        val workingDays = DateUtil().getWorkingDaysCount(configs, JiraVersionStore.get(currentVersion))
        val workLogTypes = listOf(configs.devsList, configs.qaList, configs.devOpsList, configs.analyticsList)
        val totalWorkLogs = workLogTypes.map { JiraVersionStore.get(currentVersion).issues.getTotalWorklogByUsers(it) }
        val totalWorkLogsInDays = totalWorkLogs.map { MathUtil().getFormattedWorkLog(it, "days") }
        val estimationsByRole = listOf("dev", "qa", "devops", "analytics")
        val totalEstimations = estimationsByRole.map { JiraVersionStore.get(currentVersion).issues.getTotalEstimationByRole(it) }
        val devFTE = MathUtil().getFTE(totalWorkLogsInDays[0], workingDays)
        val qaFTE = MathUtil().getFTE(totalWorkLogsInDays[1], workingDays)
        val devOpsFTE = MathUtil().getFTE(totalWorkLogsInDays[2], workingDays)
        val analyticsFTE = MathUtil().getFTE(totalWorkLogsInDays[3], workingDays)
        div("stat-box-container") {
            div("stat-box first-box") { +"Фактическая длительность релиза в днях"
                span("tooltip") { +"без учета праздников и выходных дней" }
                span("number") { +workingDays.toString() }
            }
            div("stat-box-group") {
                div("stat-box") { +"План разработки в днях"
                    span("tooltip") { +"сумма фактических трудозатрат разработчиков" }
                    span("number") { +totalEstimations[0].toString() }
                }
                div("stat-box") { +"План тестирования в днях"
                    span("tooltip") { +"сумма фактических трудозатрат разработчиков" }
                    span("number") { +totalEstimations[1].toString() }
                }
                div("stat-box") { +"План инженерных в днях"
                    span("tooltip") { +"сумма фактических трудозатрат разработчиков" }
                    span("number") { +totalEstimations[2].toString() }
                }
                div("stat-box") { +"План аналитики в днях"
                    span("tooltip") { +"сумма фактических трудозатрат разработчиков" }
                    span("number") { +totalEstimations[3].toString() }
                }
                div("stat-box") { +"Факт разработки в днях"
                    span("tooltip") { +"сумма фактических трудозатрат разработчиков" }
                    span("number") { +totalWorkLogsInDays[0].toString() }
                }
                div("stat-box") { +"Факт тестирования в днях"
                    span("tooltip") { +"сумма фактических трудозатрат тестировщиков" }
                    span("number") { +totalWorkLogsInDays[1].toString() }
                }
                div("stat-box") { +"Факт инженерных работ в днях"
                    span("tooltip") { +"сумма фактических трудозатрат инженеров" }
                    span("number") { +totalWorkLogsInDays[2].toString() }
                }
                div("stat-box") { +"Факт аналитики в днях"
                    span("tooltip") { +"сумма фактических трудозатрат аналитиков" }
                    span("number") { +totalWorkLogsInDays[3].toString() }
                }
                div("stat-box") { +"Дельта соответствия плана и факта разработки"
                    span("tooltip") { +"сумма фактических трудозатрат аналитиков" }
                    span("number") {
                        style = formatSpan(totalEstimations[0], totalWorkLogsInDays[0])
                        +(totalEstimations[0] - totalWorkLogsInDays[0]).toString()
                    }
                }
                div("stat-box") { +"Дельта соответствия плана и факта тестирования"
                    span("tooltip") { +"сумма фактических трудозатрат аналитиков" }
                    span("number") {
                        style = formatSpan(totalEstimations[1], totalWorkLogsInDays[1])
                        +(totalEstimations[1] - totalWorkLogsInDays[1]).toString()
                    }
                }
                div("stat-box") { +"Дельта соответствия плана и факта инженерных работ"
                    span("tooltip") { +"сумма фактических трудозатрат аналитиков" }
                    span("number") {
                        style = formatSpan(totalEstimations[2], totalWorkLogsInDays[2])
                        +(totalEstimations[2] - totalWorkLogsInDays[2]).toString()
                    }
                }
                div("stat-box") { +"Дельта соответствия плана и факта аналитики"
                    span("tooltip") { +"сумма фактических трудозатрат аналитиков" }
                    span("number") {
                        style = formatSpan(totalEstimations[3], totalWorkLogsInDays[3])
                        +(totalEstimations[3] - totalWorkLogsInDays[3]).toString()
                    }
                }
                div("stat-box") { +"Фактический FTE ресурсов разработки"
                    span("tooltip") { +"трудозатраты разработчиков / длительность релиза" }
                    span("number") { +devFTE.toString() }
                }
                div("stat-box") { +"Фактический FTE ресурсов тестирования"
                    span("tooltip") { +"трудозатраты тестировщиков / длительность релиза" }
                    span("number") { +qaFTE.toString() }
                }
                div("stat-box") { +"Фактический FTE инженерных ресурсов"
                    span("tooltip") { +"трудозатраты инженеров / длительность релиза" }
                    span("number") { +devOpsFTE.toString() }
                }
                div("stat-box") { +"Фактический FTE ресурсов аналитики"
                    span("tooltip") { +"трудозатраты аналитиков / длительность релиза" }
                    span("number") { +analyticsFTE.toString() }
                }
            }
        }
    }

    /**
     * Создает таблицу трудозатрат на основе переданных данных.
     *
     */
    private fun BODY.createWorkLogTable(currentVersion: String) {
        table {
            id = "worklogTable"
            thead {
                tr {
                    th { +"Название задачи" }
                    th { +"Плановые трудозатраты по разработке, дн." }
                    th { +"Фактические трудозатраты по разработке, дн."
                        span("tooltip") { + "подсвечивается зеленым, если значение не превышает плановые трудозатраты; подсвечивается красным, если превышает" }
                    }
                    th { +"Плановые трудозатраты по тестированию, дн." }
                    th { +"Фактические трудозатраты по тестированию, дн."
                        span("tooltip") { + "подсвечивается зеленым, если значение не превышает плановые трудозатраты; подсвечивается красным, если превышает" }
                    }
                    th { +"Плановые трудозатраты по инженерным работам, дн." }
                    th { +"Фактические трудозатраты по инженерным работам, дн."
                        span("tooltip") { + "подсвечивается зеленым, если значение не превышает плановые трудозатраты; подсвечивается красным, если превышает" }
                    }
                    th { +"Плановые трудозатраты по аналитике, дн." }
                    th { +"Фактические трудозатраты по аналитике, дн."
                        span("tooltip") { + "подсвечивается зеленым, если значение не превышает плановые трудозатраты; подсвечивается красным, если превышает" }
                    }
                }
            }
            tbody {
                val parentIssues = JiraVersionStore.get(currentVersion).issues.getSorted()
                parentIssues.forEach { createRow(it) }
            }
        }
    }

    /**
     * Создает контейнер для графиков на основе списка метрик.
     *
     * Для каждой метрики создается элемент canvas с уникальным идентификатором,
     * который формируется на основе названия метрики.
     */
    private fun BODY.createChartContainer() {
        metricNames.forEach { metric ->
            canvas {
                id = "chart_${metric.replace(" ", "_")}"
            }
        }
    }

    /**
     * Создает строку таблицы с данными о задаче и ее трудозатратами.
     *
     */
    private fun TBODY.createRow(jiraIssue: JiraIssue) {
        val formattedDevEstimation = jiraIssue.estimations.getByRole("dev")
        val formattedDevWorklog = MathUtil().getFormattedWorkLog(jiraIssue.getWorklogByUsers(configs.devsList), "days")
        val formattedQaEstimation = jiraIssue.estimations.getByRole("qa")
        val formattedQaWorklog = MathUtil().getFormattedWorkLog(jiraIssue.getWorklogByUsers(configs.qaList), "days")
        val formattedDevOpsEstimation = jiraIssue.estimations.getByRole("devops")
        val formattedDevOpsWorklog = MathUtil().getFormattedWorkLog(jiraIssue.getWorklogByUsers(configs.devOpsList), "days")
        val formattedAnalyticsEstimation = jiraIssue.estimations.getByRole("analytics")
        val formattedAnalyticsWorklog = MathUtil().getFormattedWorkLog(jiraIssue.getWorklogByUsers(configs.analyticsList), "days")

        val highlightClass = when (jiraIssue.type) {
            "Epic" -> "highlight highlight-epic"
            "История" -> "highlight highlight-story"
            "Задача" -> "highlight highlight-task"
            "Ошибка" -> "highlight highlight-bug"
            else -> "highlight highlight-other"
        }

        tr {
            td { classes += highlightClass
                a(href = "${jiraURL}browse/${jiraIssue.key}", target = "_blank") {
                    +jiraIssue.summary
                }
            }
            td { +formattedDevEstimation.toString() }
            td { unsafe { +formatCell(formattedDevWorklog, formattedDevEstimation) } }
            td { +formattedQaEstimation.toString() }
            td { unsafe { +formatCell(formattedQaWorklog, formattedQaEstimation) } }
            td { +formattedDevOpsEstimation.toString() }
            td { unsafe { +formatCell(formattedDevOpsWorklog, formattedDevOpsEstimation) } }
            td { +formattedAnalyticsEstimation.toString() }
            td { unsafe { +formatCell(formattedAnalyticsWorklog, formattedAnalyticsEstimation) } }
        }
    }

    /**
     * Форматирует значение трудозатрат с изменением цвета в зависимости от результата сравнения.
     *
     * @param value Значение фактических трудозатрат для отображения.
     * @param compareValue Значение плановых трудозатрат.
     *
     * @return HTML-строка с цветом текста.
     */
    private fun formatCell(value: Double, compareValue: Double?): String {
        val color = if (compareValue != null && value > compareValue) "red" else "green"
        return """<span style="color: $color;">$value</span>"""
    }

    /**
     * Форматирует значение трудозатрат с изменением цвета в зависимости от результата сравнения.
     *
     * @param value Значение фактических трудозатрат для отображения.
     * @param compareValue Значение плановых трудозатрат.
     *
     * @return HTML-строка с цветом текста.
     */
    private fun formatSpan(value: Double, compareValue: Double): String {
        val color = if (value >= compareValue) "color: green;" else "color: red;"
        return color
    }
}