package utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Утилита для выполнения математических операций.
 */
class MathUtil {

    /**
     * Форматирует трудозатраты в соответствии с указанным форматом.
     *
     * @param workLog Общее количество трудозатрат в секундах.
     * @param format Формат, в который нужно конвертировать трудозатраты.
     *
     * @return Отформатированное значение трудозатрат в соответствии с указанным форматом.
     *
     * @throws IllegalArgumentException Если формат не поддерживается.
     */
    fun getFormattedWorkLog(workLog: Int, format: String): Double {
        return when (format) {
            "days" -> BigDecimal(workLog.toDouble() / 28800)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
            "hours" -> BigDecimal(workLog.toDouble() / 3600)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
            else -> throw IllegalArgumentException("Неверный формат для конвертации трудозатрат.")
        }
    }

    /**
     * Рассчитывает полное эквивалентное время (FTE) на основе общего рабочего времени и количества рабочих дней.
     *
     * @param totalWorkLog Общее количество трудозатрат в секундах.
     * @param workingDays Количество рабочих дней.
     *
     * @return Значение FTE, округленное до двух знаков после запятой.
     */
    fun getFTE(totalWorkLog: Double, workingDays: Int): Double {
        return when {
            workingDays > 0 -> BigDecimal(totalWorkLog / workingDays)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
            else -> 0.0
        }
    }
}