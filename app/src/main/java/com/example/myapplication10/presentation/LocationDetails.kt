package com.example.myapplication10.presentation

data class LocationDetails(
    val latitude: Double,
    val longitude: Double
) {}

data class DatosIniciales(
    val peso: Int,
    val estatura: Double,
) {}


data class DatosCarrera(
    val distancia_total: Int,
    val tiempo_total: Int,
    val latitudInicial: Double,
    val longitudInicial: Double,
    val latitudFinal: Double,
    val longitudFinal: Double,
    val ritmoMinimo: Double?,
    val ritmoMaximo: Double?,
    val ritmoPromedio: Double,
    val no_pasos: Int,
    val fecha_actual: String,
    val hora_actual: String,
) {}

data class DatosFinales(
    var distancia_total: Int = 0,
    var tiempo_total: Int = 0,
    var latitudInicial: Double = 0.0,
    var longitudInicial: Double = 0.0,
    var direccionInicial: String = "",
    var latitudFinal: Double = 0.0,
    var longitudFinal: Double = 0.0,
    var direccionFinal: String = "",
    var ritmoMinimo: Double? = 0.0,
    var ritmoMaximo: Double? = 0.0,
    var ritmoPromedio: Double = 0.0,
    var no_pasos: Int = 0,
    var fecha_actual: String = "",
    var hora_actual: String = "",
    var calorias_quemadas: Double = 0.0,
) {
    constructor() : this(0, 0, 0.0, 0.0, "", 0.0, 0.0, "", 0.0, 0.0, 0.0, 0, "", "", 0.0)
}

