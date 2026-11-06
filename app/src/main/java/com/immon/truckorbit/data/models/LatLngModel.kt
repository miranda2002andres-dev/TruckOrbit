package com.immon.truckorbit.data.models

import java.util.UUID

data class LatLngModel(
    var id: String = UUID.randomUUID().toString(),
    var latitude: Double,
    var longitude: Double
) {
    constructor() : this(latitude = 0.0, longitude = 0.0)
}
