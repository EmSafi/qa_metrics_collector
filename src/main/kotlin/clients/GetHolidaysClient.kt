package clients

import io.restassured.RestAssured
import io.restassured.response.Response
import models.Configurations
import org.apache.logging.log4j.LogManager
import utils.JsonParser
import java.time.LocalDate

/**
 * Класс для получения праздничных дней
 * за указанный год через API.
 *
 * @param configs конфигурационные данные
 */
class GetHolidaysClient(configs: Configurations) {

    private val logger = LogManager.getLogger(this::class.java)

    private val holidaysApiURL = configs.holidaysApiURL

    /**
     * Получает список праздничных дней за указанный год.
     *
     * @param year Год, для которого требуется получить список праздничных дней.
     *
     * @return Список объектов LocalDate, представляющих праздничные дни за указанный год.
     *
     * @throws IllegalArgumentException Если год является пустым или null.
     */
    fun getHolidaysByYear(year: String): List<LocalDate> {
        if (year.isBlank()) {
            throw IllegalArgumentException("Год не может быть пустым или null.")
        }
        logger.info("Отправляем запрос для получения праздничных дней за $year год.")
        val response = getRequest(year)
        return JsonParser().getHolidaysList(response.jsonPath())
    }

    /**
     * Выполняет GET-запрос к API для получения данных о праздничных днях за указанный год.
     *
     * @param year Год, для которого выполняется запрос.
     *
     * @return Объект Response, содержащий ответ от API.
     *
     * @throws RuntimeException Если произошла ошибка при выполнении запроса или если код статуса ответа не равен 200.
     */
    private fun getRequest(year: String): Response {
        return try {
            RestAssured.given()
                .get("${holidaysApiURL}calendar/$year/holidays")
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

}