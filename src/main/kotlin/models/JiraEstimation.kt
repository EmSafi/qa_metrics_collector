package models

data class JiraEstimation(
    val role: String,
    val estimation: Double
)

fun List<JiraEstimation>.getByRole(role: String): Double {
    return this.first { it.role == role }.estimation
}
