package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.getGesturesPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.databinding.LayoutActivityStyleBinding
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maneuver.api.ManeuverCallback
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.api.StepDistanceRemainingCallback
import com.mapbox.navigation.ui.maneuver.api.UpcomingManeuverListCallback
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.StepDistance
import com.mapbox.navigation.ui.maneuver.model.StepDistanceError
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineApiExtensions.setRoutes
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.Objects

class MapboxCustomStyleActivity : AppCompatActivity(), OnMapLongClickListener {

    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapCamera: CameraAnimationsPlugin
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var binding: LayoutActivityStyleBinding
    private lateinit var locationComponent: LocationComponentPlugin

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    private val speedLimitFormatter: SpeedLimitFormatter by lazy {
        SpeedLimitFormatter(this)
    }

    private val speedLimitApi: MapboxSpeedLimitApi by lazy {
        MapboxSpeedLimitApi(speedLimitFormatter)
    }

    private val tripProgressFormatter: TripProgressUpdateFormatter by lazy {
        val distanceFormatterOptions =
            DistanceFormatterOptions.Builder(this).build()
        TripProgressUpdateFormatter.Builder(this)
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(this))
            .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(this))
            .build()
    }

    private val tripProgressApiApi: MapboxTripProgressApi by lazy {
        MapboxTripProgressApi(tripProgressFormatter)
    }

    private val distanceFormatter: DistanceFormatterOptions by lazy {
        DistanceFormatterOptions.Builder(this).build()
    }

    private val maneuverApi: MapboxManeuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatter))
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder().build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeArrowApi: MapboxRouteArrowApi by lazy {
        MapboxRouteArrowApi()
    }

    private val routeArrowView: MapboxRouteArrowView by lazy {
        MapboxRouteArrowView(RouteArrowOptions.Builder(this).build())
    }

    private val currentManeuverCallback = object : ManeuverCallback {
        override fun onManeuver(maneuver: Expected<Maneuver, ManeuverError>) {
            if (binding.maneuverView.visibility != View.VISIBLE) {
                binding.maneuverView.visibility = View.VISIBLE
            }
            binding.maneuverView.renderManeuver(maneuver)
        }
    }

    private val stepDistanceRemainingCallback = object : StepDistanceRemainingCallback {
        override fun onStepDistanceRemaining(
            distanceRemaining: Expected<StepDistance, StepDistanceError>
        ) {
            when (distanceRemaining) {
                is Expected.Success -> {
                    binding.maneuverView.renderDistanceRemaining(distanceRemaining.value)
                }
                is Expected.Failure -> {
                    // Not handled
                }
            }
        }
    }

    private val upcomingManeuversCallback = object : UpcomingManeuverListCallback {
        override fun onUpcomingManeuvers(maneuvers: Expected<List<Maneuver>, ManeuverError>) {
            when (maneuvers) {
                is Expected.Success -> {
                    binding.maneuverView.renderUpcomingManeuvers(maneuvers.value)
                }
                is Expected.Failure -> {
                    // Not handled
                }
            }
        }
    }

    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {}
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            navigationLocationProvider.changePosition(
                enhancedLocation,
                keyPoints,
            )
            updateCamera(enhancedLocation)
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                binding.tripProgressView.visibility = VISIBLE
                CoroutineScope(Dispatchers.Main).launch {
                    routeLineApi.setRoutes(
                        listOf(RouteLine(routes[0], null))
                    ).apply {
                        routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, this)
                    }
                }
                startSimulation(routes[0])
            } else {
                binding.tripProgressView.visibility = GONE
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            routeArrowApi.updateUpcomingManeuverArrow(routeProgress).apply {
                routeArrowView.render(mapboxMap.getStyle()!!, this)
            }
            tripProgressApiApi.getTripProgress(routeProgress).let { update ->
                binding.tripProgressView.render(update)
            }
            maneuverApi.getUpcomingManeuverList(routeProgress, upcomingManeuversCallback)
            ifNonNull(routeProgress.currentLegProgress) { legProgress ->
                ifNonNull(legProgress.currentStepProgress) {
                    maneuverApi.getStepDistanceRemaining(it, stepDistanceRemainingCallback)
                }
            }
        }
    }

    private val bannerInstructionsObserver = object : BannerInstructionsObserver {
        override fun onNewBannerInstructions(bannerInstructions: BannerInstructions) {
            maneuverApi.getManeuver(bannerInstructions, currentManeuverCallback)
        }
    }

    private val mapMatcherObserver = object : MapMatcherResultObserver {
        override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
            val value = speedLimitApi.updateSpeedLimit(mapMatcherResult.speedLimit)
            binding.speedLimitView.render(value)
        }
    }

    private fun init() {
        initNavigation()
        initStyle()
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
        mapboxNavigation.startTripSession()
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.play()
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS
        ) {
            getGesturePlugin().addOnMapLongClickListener(this)
        }
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = ReplayRouteMapper().mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun findRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultParams()
                .accessToken(Objects.requireNonNull<String>(getMapboxAccessTokenFromResources()))
                .coordinates(listOf(origin, destination))
                .alternatives(false)
                .annotationsList(Collections.singletonList(DirectionsCriteria.ANNOTATION_MAXSPEED))
                .build()
        )
    }

    private fun getMapCamera(): CameraAnimationsPlugin {
        return binding.mapView.getCameraAnimationsPlugin()
    }

    private fun getGesturePlugin(): GesturesPlugin {
        return binding.mapView.getGesturesPlugin()
    }

    private fun updateCamera(location: Location) {
        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration(1500L)
        mapCamera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .pitch(45.0)
                .zoom(17.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityStyleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapboxMap = binding.mapView.getMapboxMap()
        locationComponent = binding.mapView.getLocationComponentPlugin().apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        mapCamera = getMapCamera()
        init()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerMapMatcherResultObserver(mapMatcherObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::mapboxNavigation.isInitialized) {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.unregisterMapMatcherResultObserver(mapMatcherObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
            mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
        }
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onMapLongClick(point: Point): Boolean {
        ifNonNull(navigationLocationProvider.lastLocation) { currentLocation ->
            val originPoint = Point.fromLngLat(
                currentLocation.longitude,
                currentLocation.latitude
            )
            findRoute(originPoint, point)
        }
        return false
    }
}
