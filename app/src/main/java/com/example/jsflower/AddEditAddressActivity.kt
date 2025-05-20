package com.example.jsflower

import android.os.Bundle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.location.Geocoder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


class AddEditAddressActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var btnSelectOnMap: Button
    private lateinit var btnMyLocation: Button
    private lateinit var btnSave: Button
    private lateinit var etAddress: EditText
    private lateinit var cbDefaultAddress: CheckBox
    private var mapVisible = false
    private var currentMarker: Marker? = null
    private lateinit var locationManager: LocationManager
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var addressId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Constant for database path
    private val DATABASE_PATH = "users"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load osmdroid config before setContentView
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))

        setContentView(R.layout.activity_add_edit_address)

        etAddress = findViewById(R.id.etAddress)
        btnSelectOnMap = findViewById(R.id.btnSelectOnMap)
        btnMyLocation = findViewById(R.id.btnMyLocation)
        btnSave = findViewById(R.id.btnSave)
        cbDefaultAddress = findViewById(R.id.cbDefaultAddress)
        mapView = findViewById(R.id.map)

        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Get data from intent if editing an address
        addressId = intent.getStringExtra("address_id")
        if (addressId != null) {
            // Load existing address for editing
            loadExistingAddress(addressId!!)
        }

        // Initialize map
        initMap()

        btnSelectOnMap.setOnClickListener {
            toggleMapVisibility()
        }

        // Add click listener for My Location button
        btnMyLocation.setOnClickListener {
            centerMapToCurrentLocation()
        }

        // Handle save address event
        btnSave.setOnClickListener {
            saveAddress()
        }

        // Check location permission
        checkLocationPermission()
    }

    private fun initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Add map rotation capability
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)

        val mapController = mapView.controller
        mapController.setZoom(5.0)
        mapController.setCenter(GeoPoint(16.0, 106.0)) // Vietnam center default

        // Capture map touch event to place marker
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                placeMarker(geoPoint)
                updateAddressFromGeoPoint(geoPoint)
                return true
            }
        })
    }

    private fun placeMarker(geoPoint: GeoPoint) {
        // Remove old marker if exists
        currentMarker?.let { mapView.overlays.remove(it) }

        // Create new marker
        currentMarker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected location"
        }
        mapView.overlays.add(currentMarker)
        mapView.invalidate()

        // Save selected coordinates
        selectedLatitude = geoPoint.latitude
        selectedLongitude = geoPoint.longitude
    }

    private fun updateAddressFromGeoPoint(geoPoint: GeoPoint) {
//        val geocoder = Geocoder(this, Locale.getDefault())
//        try {
//            val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
//            if (!addresses.isNullOrEmpty()) {
//                val address = addresses[0]
//
//                // Lấy thoroughfare (tên đường), fallback các trường khác nếu thoroughfare trống
//                val road = address.thoroughfare ?: ""
//                val subLocality = address.subLocality ?: ""
//                val locality = address.locality ?: ""
//                val adminArea = address.adminArea ?: ""
//                val country = address.countryName ?: ""
//
//                val displayAddress = when {
//                    road.isNotEmpty() -> road
//                    subLocality.isNotEmpty() -> subLocality
//                    locality.isNotEmpty() -> locality
//                    adminArea.isNotEmpty() -> adminArea
//                    country.isNotEmpty() -> country
//                    else -> "Location: ${geoPoint.latitude}, ${geoPoint.longitude}"
//                }
//
//                // Gán địa chỉ ra EditText
//                etAddress.setText(displayAddress)
//                android.util.Log.d("Geocoder", "Address found: $displayAddress")
//
//            } else {
//                etAddress.setText("Location: ${geoPoint.latitude}, ${geoPoint.longitude}")
//                android.util.Log.w("Geocoder", "No address found for location")
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            etAddress.setText("Location: ${geoPoint.latitude}, ${geoPoint.longitude}")
//        }
        getAddressFromCoordinatesOSM(geoPoint.latitude, geoPoint.longitude) { address ->
            etAddress.setText(address)
        }
    }


    fun getAddressFromCoordinatesOSM(latitude: Double, longitude: Double, callback: (String) -> Unit) {
        val client = OkHttpClient()
        val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$latitude&lon=$longitude"

        Thread {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "JSFlowerApp/1.0")
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!responseBody.isNullOrEmpty()) {
                    val json = JSONObject(responseBody)
                    val displayName = json.optString("display_name", "Không tìm thấy địa chỉ")
                    runOnUiThread {
                        callback(displayName)
                    }
                } else {
                    runOnUiThread {
                        callback("Không tìm thấy địa chỉ")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    callback("Lỗi khi lấy địa chỉ")
                }
            }
        }.start()
    }


    private fun toggleMapVisibility() {
        mapVisible = !mapVisible
        if (mapVisible) {
            mapView.visibility = View.VISIBLE
            btnMyLocation.visibility = View.VISIBLE
            btnSelectOnMap.text = "Close map"

            // If marker exists, move to marker position
            currentMarker?.let {
                mapView.controller.animateTo(it.position)
                mapView.controller.setZoom(15.0)
            } ?: run {
                // If no marker, try to use current location
                checkLocationPermission()
            }
        } else {
            mapView.visibility = View.GONE
            btnMyLocation.visibility = View.GONE
            btnSelectOnMap.text = "Select on map"
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            centerMapToCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            centerMapToCurrentLocation()
        } else {
            Toast.makeText(this, "You need to grant location permission to use the map", Toast.LENGTH_SHORT).show()
        }
    }

    private fun centerMapToCurrentLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                // Check if GPS is enabled
                val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (isGPSEnabled || isNetworkEnabled) {
                    // Prioritize GPS
                    val provider = if (isGPSEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER

                    // Get last known location
                    val lastKnownLocation = locationManager.getLastKnownLocation(provider)
                    if (lastKnownLocation != null) {
                        // We have location, show on map
                        handleLocationFound(lastKnownLocation)
                    } else {
                        // No last known location, request update
                        Toast.makeText(this, "Determining location...", Toast.LENGTH_SHORT).show()
                        locationManager.requestLocationUpdates(provider, 0, 0f, locationListener)
                    }
                } else {
                    Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error determining location", Toast.LENGTH_SHORT).show()
        }
    }

    // Listener for location update events
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // New location received
            handleLocationFound(location)

            // Unregister location updates to save battery
            locationManager.removeUpdates(this)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    private fun handleLocationFound(location: Location) {
        val currentLocation = GeoPoint(location.latitude, location.longitude)

        // Move map to current location
        mapView.controller.setZoom(15.0)
        mapView.controller.animateTo(currentLocation)

        // Display marker and update address
        placeMarker(currentLocation)
        updateAddressFromGeoPoint(currentLocation)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Unregister location updates when Activity is destroyed
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadExistingAddress(addressId: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val addressRef = database.reference
                .child(DATABASE_PATH)
                .child(currentUser.uid)
                .child("listaddress")
                .child(addressId)

            addressRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val address = snapshot.child("address").getValue(String::class.java) ?: ""
                    val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val isDefault = snapshot.child("isDefault").getValue(Boolean::class.java) ?: false

                    // Update UI
                    etAddress.setText(address)
                    cbDefaultAddress.isChecked = isDefault
                    selectedLatitude = latitude
                    selectedLongitude = longitude

                    // Set marker if coordinates exist
                    if (latitude != 0.0 && longitude != 0.0) {
                        val geoPoint = GeoPoint(latitude, longitude)
                        placeMarker(geoPoint)
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error loading address: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAddress() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to save address", Toast.LENGTH_SHORT).show()
            return
        }

        val addressText = etAddress.text.toString().trim()
        if (addressText.isEmpty()) {
            Toast.makeText(this, "Please enter an address", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show()
            return
        }

        val addressId = this.addressId ?: UUID.randomUUID().toString()
        val uid = currentUser.uid

        val userAddressesRef = database.reference
            .child(DATABASE_PATH)
            .child(uid)
            .child("listaddress")

        val addressData = HashMap<String, Any>()
        addressData["address"] = addressText
        addressData["latitude"] = selectedLatitude
        addressData["longitude"] = selectedLongitude
        addressData["isDefault"] = cbDefaultAddress.isChecked

        userAddressesRef.child(addressId).setValue(addressData)
            .addOnSuccessListener {
                if (cbDefaultAddress.isChecked) {
                    updateDefaultAddress(uid, addressId)
                } else {
                    Toast.makeText(this, "Address saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving address: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDefaultAddress(userId: String, currentAddressId: String) {
        // Reference to the listaddress node
        val userAddressesRef = database.reference
            .child(DATABASE_PATH)
            .child(userId)
            .child("listaddress")

        // Get all user addresses
        userAddressesRef.get().addOnSuccessListener { snapshot ->
            // List of addresses to update (excluding current address)
            val updateTasks = mutableListOf<Pair<String, Boolean>>()

            // Iterate through all addresses
            for (addressSnapshot in snapshot.children) {
                val addressId = addressSnapshot.key ?: continue
                if (addressId != currentAddressId) {
                    val isDefault = addressSnapshot.child("isDefault").getValue(Boolean::class.java) ?: false
                    if (isDefault) {
                        // Add to update list
                        updateTasks.add(Pair(addressId, false))
                    }
                }
            }

            // Update all other addresses to non-default
            if (updateTasks.isEmpty()) {
                Toast.makeText(this, "Default address saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                var completedTasks = 0
                for ((addressId, isDefault) in updateTasks) {
                    userAddressesRef.child(addressId).child("isDefault").setValue(isDefault)
                        .addOnCompleteListener {
                            completedTasks++
                            if (completedTasks == updateTasks.size) {
                                Toast.makeText(this, "Default address saved", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error updating default address: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}