package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import okhttp3.Request

internal object RouteBuilderProvider {

    fun getBuilder(
        context: Context,
        urlSkuTokenProvider: UrlSkuTokenProvider?
    ): MapboxDirections.Builder =
        MapboxDirections.builder()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .language(context.inferDeviceLocale())
            .continueStraight(true)
            .roundaboutExits(true)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .steps(true)
            .annotations(
                listOf(
                    DirectionsCriteria.ANNOTATION_CONGESTION,
                    DirectionsCriteria.ANNOTATION_DISTANCE
                )
            )
            .voiceInstructions(true)
            .bannerInstructions(true)
            .voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale())
            .also { builder ->
                if (urlSkuTokenProvider != null) {
                    builder.interceptor {
                        val httpUrl = (it.request() as Request).url
                        val skuUrl = urlSkuTokenProvider.obtainUrlWithSkuToken(httpUrl.toUrl())
                        it.proceed(it.request().newBuilder().url(skuUrl).build())
                    }
                }
            }

    fun getRefreshBuilder(): MapboxDirectionsRefresh.Builder =
        MapboxDirectionsRefresh.builder()
}
