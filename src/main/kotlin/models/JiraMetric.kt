package models

data class JiraMetric(
    val name: String,
    val value: Int
)

/**
 * Получает параметры цвета для построения графика,
 * исходя из названия метрики.
 *
 * @return Список цветов, соответствующий метрике.
 */
fun JiraMetric.getColorForCharts(): List<String> {
    return when (name) {
        "Количество найденных ошибок" -> listOf("'rgba(255, 99, 132, 0.2)'", "'rgba(255, 99, 132, 1)'")
        "Количество критических ошибок" -> listOf("'rgba(54, 162, 235, 0.2)'", "'rgba(54, 162, 235, 1)'")
        "Количество исправленных ошибок" -> listOf("'rgba(255, 206, 86, 0.2)'", "'rgba(255, 206, 86, 1)'")
        "Длина бэклога" -> listOf("'rgba(75, 192, 192, 0.2)'", "'rgba(75, 192, 192, 1)'")
        "Количество ошибок с прода" -> listOf("'rgba(153, 102, 255, 0.2)'", "'rgba(153, 102, 255, 1)'")
        else -> throw IllegalArgumentException("Метрика с названием $name не найдена.")
    }
}