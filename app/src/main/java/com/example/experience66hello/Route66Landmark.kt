package com.example.experience66hello

import com.mapbox.geojson.Point

/**
 * Data class representing a Route 66 landmark in Arizona
 */
data class Route66Landmark(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 200f  // Default geofence radius
) {
    fun toPoint(): Point = Point.fromLngLat(longitude, latitude)
}

/**
 * Arizona Route 66 Landmarks - West to East
 */
object ArizonaLandmarks {
    
    val landmarks = listOf(
        Route66Landmark(
            id = "oatman",
            name = "Oatman Ghost Town",
            description = "Historic gold mining town famous for wild burros roaming the streets",
            latitude = 35.0264,
            longitude = -114.3823,
            radiusMeters = 300f
        ),
        Route66Landmark(
            id = "kingman",
            name = "Kingman",
            description = "Heart of Historic Route 66 - Powerhouse Visitor Center & Route 66 Museum",
            latitude = 35.1894,
            longitude = -114.0530,
            radiusMeters = 500f
        ),
        Route66Landmark(
            id = "hackberry",
            name = "Hackberry General Store",
            description = "Iconic Route 66 general store with vintage cars and memorabilia",
            latitude = 35.3707,
            longitude = -113.7301,
            radiusMeters = 200f
        ),
        Route66Landmark(
            id = "seligman",
            name = "Seligman",
            description = "Birthplace of Historic Route 66 Association - The Delgadillo's Snow Cap Drive-In",
            latitude = 35.3274,
            longitude = -112.8767,
            radiusMeters = 400f
        ),
        Route66Landmark(
            id = "williams",
            name = "Williams",
            description = "Gateway to the Grand Canyon - Last town bypassed by Interstate 40",
            latitude = 35.2494,
            longitude = -112.1910,
            radiusMeters = 500f
        ),
        Route66Landmark(
            id = "flagstaff",
            name = "Flagstaff",
            description = "Mountain town at 7,000 ft - Historic downtown with Route 66 heritage",
            latitude = 35.1983,
            longitude = -111.6513,
            radiusMeters = 600f
        ),
        Route66Landmark(
            id = "meteor_crater",
            name = "Meteor Crater",
            description = "World's best-preserved meteorite impact site - 50,000 years old",
            latitude = 35.0275,
            longitude = -111.0225,
            radiusMeters = 400f
        ),
        Route66Landmark(
            id = "winslow",
            name = "Standin' on the Corner Park",
            description = "Famous Eagles 'Take It Easy' song tribute in Winslow, Arizona",
            latitude = 35.0242,
            longitude = -110.6974,
            radiusMeters = 150f
        ),
        Route66Landmark(
            id = "jackrabbit",
            name = "Jack Rabbit Trading Post",
            description = "Iconic yellow rabbit billboard and trading post since 1949",
            latitude = 35.0245,
            longitude = -110.1042,
            radiusMeters = 200f
        ),
        Route66Landmark(
            id = "holbrook",
            name = "Wigwam Motel",
            description = "Sleep in a concrete teepee - Classic Route 66 roadside attraction",
            latitude = 34.9014,
            longitude = -110.1580,
            radiusMeters = 200f
        ),
        Route66Landmark(
            id = "petrified_forest",
            name = "Petrified Forest National Park",
            description = "Ancient petrified wood and Painted Desert - Route 66 runs through the park",
            latitude = 35.0657,
            longitude = -109.7890,
            radiusMeters = 1000f
        )
    )
    
    fun findById(id: String): Route66Landmark? = landmarks.find { it.id == id }
}
