package builders

import org.apache.logging.log4j.LogManager

/**
 * Абстрактный класс, реализующий базовый билдер.
 */
abstract class AbstractBuilder {

    /** Логгер для записи логов. */
    protected val logger =  LogManager.getLogger(this::class.java)
}