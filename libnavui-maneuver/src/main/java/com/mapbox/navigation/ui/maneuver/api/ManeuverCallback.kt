package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError

/**
 * Interface definition for a callback to be invoked when a current maneuver data is processed.
 */
interface ManeuverCallback {

    /**
     * Invoked when [Maneuver] is ready.
     * @param maneuver CurrentManeuver represents maneuver to be represented on the view.
     */
    fun onManeuver(maneuver: Expected<Maneuver, ManeuverError>)
}
