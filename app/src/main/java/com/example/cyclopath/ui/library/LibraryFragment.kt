package com.example.cyclopath.ui.library

import RouteObj
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cyclopath.R
import com.example.cyclopath.items.DirectionsRouteAdapter
import com.example.cyclopath.ui.history.HistoryAdapter
import com.facebook.AccessTokenManager.Companion.TAG
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import com.google.gson.*
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.*
import com.mapbox.maps.MapView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Type
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.sqrt

/**
 * A simple [Fragment] subclass.
 * Use the [LibraryFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LibraryFragment : Fragment() {
    val routeObjList = mutableListOf<RouteObj>()
    // List to store the name of the activity
    var routeList: ArrayList<RouteObj> = arrayListOf()

    // Connect to the Firebase storage
    var storage = Firebase.storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    class GeoJsonTypeAdapter : JsonDeserializer<GeoJson>, JsonSerializer<GeoJson> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): GeoJson? {
            json?.let {
                val jsonObject = it.asJsonObject
                val type = jsonObject.get("type").asString

                return when (type) {
                    "Feature" -> context?.deserialize<Feature>(jsonObject, Feature::class.java)
                    "Point" -> context?.deserialize<Point>(jsonObject, Point::class.java)
                    "LineString" -> context?.deserialize<LineString>(jsonObject, LineString::class.java)
                    "Polygon" -> context?.deserialize<Polygon>(jsonObject, Polygon::class.java)
                    // Add support for other GeoJson types as needed
                    else -> throw IllegalArgumentException("Unsupported GeoJson type: $type")
                }
            }
            return null
        }

        override fun serialize(
            src: GeoJson?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return context!!.serialize(src)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val root = inflater.inflate(R.layout.fragment_library, container, false)

        val storageRef = storage.reference
        var routeRef = storageRef.child("routes")
        var routeGeoRef = storageRef.child("routegeojson")


        // Create a Gson instance with the custom TypeAdapter registered
        val gson = GsonBuilder()
            .registerTypeAdapter(GeoJson::class.java, GeoJsonTypeAdapter())
            .create()



        val adapter = RouteFrameAdapter(routeObjList)
        val llm = LinearLayoutManager(context)
        val view = root.findViewById<RecyclerView>(R.id.r_view)
        view.layoutManager = llm
        view.adapter = adapter
        routeRef.listAll()
            .addOnSuccessListener { listResult ->
                // Iterate over each item in the list and download its contents
                listResult.items.forEach { item ->
                        item.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                            // Convert the downloaded bytes to a String
                            val jsonString = String(bytes)

                            // Parse the JSON String into a Route object
                            val route = gson.fromJson(jsonString, RouteObj::class.java)

                            // Create a reference to the image with the same name as the RouteObj
                            val imageRef = storageRef.child("images/${route.route_name_text}.png")

                            // Download the image as a byte array
                            imageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { imageBytes ->
                                // Convert the downloaded bytes to a Bitmap
                                val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                                // Set the Bitmap to the Route object
                                route.staticimage = imageBitmap

                                // Add the Route object to the list
                                routeObjList.add(route)

                                // Update the RecyclerView on the main thread
                                view.post {
                                    adapter.notifyItemChanged(routeObjList.size - 1)
                                }
                            }.addOnFailureListener {
                                // Handle any errors that occur while downloading the image
                                Log.e(TAG, "Failed to download image for route: ${route.route_name_text}", it)
                            }

                        }.addOnFailureListener {
                            // Handle any errors that occur while downloading the file
                            Log.e(TAG, "Failed to download route: ${item.name}", it)
                        }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to list routes", it)
            }

        val myrouteBTN : FloatingActionButton = root.findViewById<FloatingActionButton>(R.id.myroutes)
        myrouteBTN.setOnClickListener{
            val intent = Intent(context, myRoutesActivity::class.java)
            startActivity(intent)
        }

        val sortSpinner = root.findViewById<Spinner>(R.id.sort_spinner)
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedOption = parent?.getItemAtPosition(position).toString()
                when (selectedOption) {
                    "Distance (low to high)" -> {
                        // Sort by distance
                        adapter.sortByDistance()
                    }
                    "Distance (high to low)"-> {
                        // Sort by distance
                        adapter.sortByDistanceD()
                    }
                    "Difficulty (low to high)" -> {
                        // Sort by difficulty
                        adapter.sortByDifficulty()
                    }
                    "Difficulty (high to low)" -> {
                        // Sort by ratings
                        adapter.sortByDifficultyD()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyDjAFFs1s-IfgS7-sFsK1E2n9DVtYNIvXU", Locale("en","GB"));
        }



        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // TODO: Get info about the selected place.
//                val geocoder = Geocoder(requireContext(), Locale.getDefault())
//                val address = "1600 Amphitheatre Parkway, Mountain View, CA"
//
//                val results = geocoder.getFromLocationName(address, 1)
//
//                if (results.isNotEmpty()) {
//                    val latitude = results[0].latitude
//                    val longitude = results[0].longitude
//                    println(latitude)
//                    println(longitude)
//                    Log.i(TAG, "Latitude: $latitude, Longitude: $longitude")
//                }

                val searchLatLng = place.latLng
                println(place.toString())
                println(searchLatLng)
                Log.i(TAG, "Place: ${place.name}, ${place.id}")
                if (searchLatLng != null) {
                    updateNear(searchLatLng.longitude,searchLatLng.latitude)
                }
                adapter.sortByNearest()
            }

            override fun onError(status: Status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: $status")
            }
        })


        return root
    }

    fun updateNear(lng: Double, lat: Double){
        routeObjList.forEach { route ->
            route.near = sqrt((route.startLng-lng) *(route.startLng-lng) + (route.startLat-lat)*(route.startLat-lat))
        }
    }

}