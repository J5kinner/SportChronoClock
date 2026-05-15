package com.sportchronoclock.format

import com.sportchronoclock.settings.SpeedUnits

private const val KMH_TO_MPH = 0.6213711922
private const val METERS_TO_FEET = 3.280839895
private const val METERS_TO_YARDS = 1.0936132983
private const val METERS_TO_MILES = 0.0006213711922

object UnitFormatter {

    fun displaySpeed(speedKmh: Float, units: SpeedUnits): Int =
        when (units) {
            SpeedUnits.KMH -> speedKmh.toInt()
            SpeedUnits.MPH -> (speedKmh * KMH_TO_MPH).toInt()
        }

    fun speedLabel(units: SpeedUnits): String = when (units) {
        SpeedUnits.KMH -> "KM/H"
        SpeedUnits.MPH -> "MPH"
    }

    fun maxScaleSpeed(units: SpeedUnits): Float = when (units) {
        SpeedUnits.KMH -> 200f
        SpeedUnits.MPH -> 120f
    }

    /**
     * Returns major tick labels (0, 25%, 50%, 75%, 100%) for the current unit.
     */
    fun majorTickLabels(units: SpeedUnits): List<String> = when (units) {
        SpeedUnits.KMH -> listOf("0", "50", "100", "150", "200")
        SpeedUnits.MPH -> listOf("0", "30", "60", "90", "120")
    }

    fun formatDistance(meters: Double, units: SpeedUnits): String = when (units) {
        SpeedUnits.KMH -> {
            val km = meters / 1000.0
            when {
                km >= 100.0 -> "${km.toInt()}km"
                km >= 1.0 -> "${(km * 10).toInt() / 10.0}km"
                else -> "${meters.toInt()}m"
            }
        }
        SpeedUnits.MPH -> {
            val miles = meters * METERS_TO_MILES
            when {
                miles >= 100.0 -> "${miles.toInt()}mi"
                miles >= 0.1 -> "${(miles * 10).toInt() / 10.0}mi"
                else -> "${(meters * METERS_TO_YARDS).toInt()}yd"
            }
        }
    }

    fun shortDistanceLabel(meters: Double, units: SpeedUnits): String = when (units) {
        SpeedUnits.KMH -> "${meters.toInt()}m"
        SpeedUnits.MPH -> "${(meters * METERS_TO_FEET).toInt()}ft"
    }
}
