package com.example.cyclopath.ui.history

import com.example.cyclopath.R
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style

class ViewHistoryActivity : AppCompatActivity() {

    private lateinit var text: TextView
    private var storage = Firebase.storage
    val ONE_MEGABYTE: Long = 1024 * 1024
    private lateinit var mapView : MapView
    private lateinit var mapboxMap : MapboxMap
    private lateinit var ttt: TextView
    private lateinit var td: TextView
    private lateinit var pb: ProgressBar

    private val ZOOM = 14.0

    private var originLong = 0.0
    private var originLat = 0.0
    private var destinationLong = 0.0
    private var destinationLat = 0.0
    private var duration = "0H0M0S"
    private var distance = "0M"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_history)

        mapView = findViewById(R.id.mapView)

        val name = intent.getStringExtra("name")

        text = findViewById(R.id.title)
        text.setText(name)
        ttt = findViewById(R.id.ttt)
        td = findViewById(R.id.td)

        val sharedPreferences = this.getSharedPreferences("user_data", AppCompatActivity.MODE_PRIVATE)
        val username = sharedPreferences?.getString("username", "user")

        val storageRef = storage.reference
        var dataRef = storageRef.child("history/$username/$name.geojson")
        dataRef.downloadUrl.addOnSuccessListener {
            mapView.getMapboxMap().loadStyle(
                    (
                            style(styleUri = Style.MAPBOX_STREETS) {
                                +geoJsonSource("line") {
                                    url(it.toString())
                                }
                                +lineLayer("linelayer", "line") {
                                    lineCap(LineCap.ROUND)
                                    lineJoin(LineJoin.ROUND)
                                    lineOpacity(0.9)
                                    lineWidth(8.0)
                                    lineColor("#F55C5C")
                                }
                            }
                            )
            )
        }.addOnFailureListener {

        }

        var infoRef = storageRef.child("history/$username/$name.txt")
        infoRef.getBytes(ONE_MEGABYTE).addOnSuccessListener { it ->
            val data = String(it).lines()
            data.forEach {
                if (it != "") {
                    if ("origin" in it) {
                        val strs = it.substring(7).split(",").toTypedArray()
                        originLat = strs[0].toDouble()
                        originLong = strs[1].toDouble()
                    } else if ("destination" in it) {
                        val strs = it.substring(12).split(",").toTypedArray()
                        destinationLat = strs[0].toDouble()
                        destinationLong = strs[1].toDouble()
                    } else if ("duration" in it) {
                        duration = it.substring(9)
                    } else if ("distance" in it) {
                        distance = it.substring(9)
                    }
                }
            }
            val midLat = (originLat+destinationLat)/2
            val midLong = (originLong+destinationLong)/2
            ttt.setText("Total Time Taken: $duration")
            td.setText("Total Distance: $distance")

            mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder().center(
                            Point.fromLngLat(
                                    midLong,
                                    midLat
                            )
                    ).zoom(ZOOM).build()
            )

        }.addOnFailureListener {
        }

    }
}