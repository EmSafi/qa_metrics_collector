package builders

import models.JiraVersion
import models.getColorForCharts

/**
 * Класс отвечает за создание JavaScript кода для экспорта данных в Excel
 * и генерации графиков на основе метрик.
 */
class ScriptBuilder: AbstractBuilder() {

    /**
     * Создает JavaScript код для экспорта данных и построения графиков.
     *
     *
     * @return Строка, содержащая сгенерированный JavaScript код.
     */
    fun create(versionsList: List<JiraVersion>, metricNames: Set<String>): String {
        logger.info("Начинаем создание JavaScript кода для экспорта данных и построения графиков.")
        val versionNames = versionsList.map { it.name }
        logger.info("Получен список версий для построения графиков: $versionNames")

        val script =
            """
            function exportToExcel() {
                const metricsTable = document.getElementById('metricsTable');
                const worklogTable = document.getElementById('worklogTable');
                
                // Создаем книгу и добавляем первый лист
                const workbook = XLSX.utils.table_to_book(metricsTable, { sheet: "Статистика по метрикам" });
                
                // Добавляем второй лист
                const worklogTableSheet = XLSX.utils.table_to_sheet(worklogTable);
                XLSX.utils.book_append_sheet(workbook, worklogTableSheet, "Детализация трудозатрат");
                
                // Создаем файл и инициируем его скачивание
                XLSX.writeFile(workbook, 'report.xlsx');
            }

            // Данные для графиков
            const labels = ${versionNames.joinToString(prefix = "[", postfix = "]", separator = ", ") { "\"$it\"" }};
            const data = ${buildData(versionsList, metricNames)};

            // Создание графиков
            data.forEach(dataset => {
                const canvasId = 'chart_' + dataset.label.replace(/ /g, "_");
                const canvas = document.getElementById(canvasId)
                const ctx = canvas.getContext('2d');
                new Chart(ctx, {
                    type: 'bar',
                    data: {
                         labels: labels,
                         datasets: [dataset]
                    },
                    options: {
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function(value) {
                                        if (value >= 0 && Number.isInteger(value)) {
                                            return value;
                                        }
                                    }
                                }
                            }
                        },
                        animation: {
                            duration: 1000,
                            easing: 'easeOutBounce'
                        }
                    }
                });
            });
            """.trimIndent()
        logger.info("JavaScript код успешно сгенерирован.")
        return script
    }

    /**
     * Строит список значений метрик для заданного названия метрики.
     *
     * @param metricName Название метрики на русском языке.
     *
     * @return Список значений метрики для всех релизов.
     */
    private fun buildMetricValuesList(versionsList: List<JiraVersion>, metricName: String): List<Int> {
        val valuesList = versionsList.mapNotNull { version ->
            version.metrics.find { it.name == metricName }?.value
        }
        logger.info("Сформирован список значений для метрики '$metricName': $valuesList")
        return valuesList
    }

    /**
     * Строит строку JS из списка метрик, разделяя их заданным разделителем.
     *
     *
     * @return Строка JS.
     */
    private fun buildData(versionsList: List<JiraVersion>, metricNames: Set<String>): String {
        return buildString {
            appendLine("[")
            metricNames.joinTo(this, separator = ",\n") { metricLabel ->
                val metricValues = buildMetricValuesList(versionsList, metricLabel)
                val backgroundColor = versionsList.asSequence()
                    .mapNotNull { version -> version.metrics.find { it.name == metricLabel }?.getColorForCharts()?.get(0) }
                    .firstOrNull() ?: "'rgba(0, 0, 0, 0.1)'" // Значение по умолчанию, если цвет не найден
                val borderColor = versionsList.asSequence()
                    .mapNotNull { version -> version.metrics.find { it.name == metricLabel }?.getColorForCharts()?.get(1) }
                    .firstOrNull() ?: "'rgba(0, 0, 0, 0.1)'" // Значение по умолчанию, если цвет не найден

                """
            {
                label: '$metricLabel',
                data: $metricValues,
                backgroundColor: $backgroundColor,
                borderColor: $borderColor,
                borderWidth: 1
            }
            """.trimIndent()
            }
            append("\n]")
        }.also {
            logger.info("Генерация data завершена.")
        }
    }
}