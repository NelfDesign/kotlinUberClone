package fr.nelfdesign.kotlinuberclone

import fr.nelfdesign.kotlinuberclone.model.DriverInfoModel
import java.lang.StringBuilder

/**
 * Created by fabricedesign at 5/21/21
 * fr.nelfdesign.kotlinuberclone
 */
object Common {
    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentUser!!.firstName)
            .append(" ")
            .append(currentUser!!.lastName)
            .toString()
    }

    //Database name
    val DRIVERS_LOCATION_REFERENCE: String = "DriversLocation"
    val DRIVER_INFO_REFERENCE: String = "DriverInfo"

    var currentUser: DriverInfoModel? = null
}