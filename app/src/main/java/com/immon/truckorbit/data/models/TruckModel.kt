package com.immon.truckorbit.data.models

import com.immon.truckorbit.data.enums.DrivingStatusModel
import java.util.UUID

data class TruckModel(
    var id: String = UUID.randomUUID().toString(),
    var truckName: String,
    var licenseNo: String,
    var currentDriver: UserModel? = null,
    var drivingStatus: DrivingStatusModel = DrivingStatusModel.STOPPED,
    var location: LatLngModel? = null,
    var destination: String? = null
) {
    constructor() : this(truckName = "", licenseNo = "")
}
