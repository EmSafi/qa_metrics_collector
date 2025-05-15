package utils

import com.google.gson.Gson
import com.google.gson.JsonParseException
import models.Configurations
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileNotFoundException

/**
 * Класс, реализующий взаимодействие с конфигом.
 */
class Configs {

    private val logger = LogManager.getLogger(this::class.java)

    /**
     * Читает конфигурацию из файла JSON.
     *
     * Если файл существует и его содержимое корректно, функция возвращает объект типа Configurations.
     *
     * @return объект типа [Configurations], если файл существует и успешно разобран.
     *
     * @throws FileNotFoundException если файл конфигурации не найден.
     * @throws JsonParseException если содержимое файла не может быть разобрано как JSON.
     */
    fun readConfig(): Configurations {
        return try {
            logger.info("Получаем файл конфигурации.")
            val file = File("data/Configuration.json")
            val config: String
            if (file.exists()) {
                logger.info("Файл получен.")
                config = file.readText()
            }
            else {
                throw FileNotFoundException("Файл конфигурации не найден.")
            }
            val gson = Gson()
            logger.info("Пробуем разобрать файл конфигурации.")
            gson.fromJson(config, Configurations::class.java)
        }
        catch(e: JsonParseException) {
            throw RuntimeException("$e: не удалось разобрать файл конфигурации.")
        }
    }

    /**
     * Получает значение указанного поля из объекта конфигурации.
     * Если поле существует, функция возвращает его значение в виде строки.
     *
     * @param configs объект конфигурации типа [Configurations], из которого нужно получить значение поля.
     * @param fieldName имя поля, значение которого требуется получить.
     *
     * @return строковое представление значения указанного поля.
     */
    fun getFieldValue(configs: Configurations, fieldName: String): String {
        return configs::class.java.getDeclaredField(fieldName).get(configs).toString()
    }
}