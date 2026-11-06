package com.immon.truckorbit.data.models

import com.immon.truckorbit.data.enums.AccountStatusModel
import com.immon.truckorbit.data.enums.AccountTypeModel
import com.immon.truckorbit.data.enums.DrivingStatusModel
import java.util.UUID

data class UserModel(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var email: String,
    var accountType: AccountTypeModel = AccountTypeModel.DRIVER,
    var accountStatus: AccountStatusModel = AccountStatusModel.INACTIVE,
    var drivingStatus: DrivingStatusModel = DrivingStatusModel.STOPPED,
    var colorHex: String? = "#FF0000" // <--- AGREGADO: Color por defecto (Rojo)
) {
    constructor() : this(name = "", email = "")
}