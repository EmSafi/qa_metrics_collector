package utils

import clients.GetHolidaysClient
import models.Configurations
import models.JiraVersion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Утилитный класс для работы с датами.
 */
class DateUtil {

    /**
     * Преобразует строку в объект LocalDate.
     *
     * @param date строка, представляющая дату в формате "yyyy-MM-dd".
     *
     * @return объект LocalDate, соответствующий переданной строке.
     *
     * @throws DateTimeParseException если строка не может быть распознана как дата.
     */
    fun getFormatedDate(date: String): LocalDate {
        return try {
            LocalDate.parse(date.substring(0, 10))
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Неверный формат даты: $date. Ожидается формат yyyy-MM-dd.", e)
        }
    }

    /**
     * Подсчитывает количество рабочих дней между начальной датой и датой релиза.
     *
     * @param configs конфигурационные данные.
     * @param currentVersion версия, для которой необходимо получить рабочие дни.
     *
     * @return количество рабочих дней между начальной датой и датой релиза.
     */
    fun getWorkingDaysCount(configs: Configurations, version: JiraVersion): Int {
        val startDate = version.startDate
        val releaseDate = version.releaseDate

        if (startDate >= releaseDate) {
            return 0
        }

        val holidays = GetHolidaysClient(configs).getHolidaysByYear(LocalDate.now().year.toString())

        return startDate.datesUntil(releaseDate.plusDays(1))
            .filter { date ->
                date.dayOfWeek != DayOfWeek.SATURDAY &&
                        date.dayOfWeek != DayOfWeek.SUNDAY &&
                        !holidays.contains(date)
            }
            .count().toInt()
    }
}