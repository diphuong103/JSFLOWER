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
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.jsflower.R

class AddEditAddressActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var btnSelectOnMap: Button
    private lateinit var etAddress: EditText
    private var mapVisible = false
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Phải load config osmdroid trước setContentView
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))

        setContentView(R.layout.activity_add_edit_address)

        etAddress = findViewById(R.id.etAddress)
        btnSelectOnMap = findViewById(R.id.btnSelectOnMap)
        mapView = findViewById(R.id.map)

        // Khởi tạo bản đồ
        initMap()

        btnSelectOnMap.setOnClickListener {
            toggleMapVisibility()
        }
    }

    private fun initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Thêm khả năng xoay bản đồ
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)

        val mapController = mapView.controller
        mapController.setZoom(5.0)
        mapController.setCenter(GeoPoint(16.0, 106.0)) // Trung tâm Việt Nam

        // Bắt sự kiện chạm bản đồ để đặt marker
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
        currentMarker?.let { mapView.overlays.remove(it) }
        currentMarker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Vị trí đã chọn"
        }
        mapView.overlays.add(currentMarker)
        mapView.invalidate()
    }

    private fun updateAddressFromGeoPoint(geoPoint: GeoPoint) {
        // Bạn có thể tích hợp Geocoder để lấy địa chỉ thật
        val fakeAddress = "Vị trí: ${geoPoint.latitude}, ${geoPoint.longitude}"
        etAddress.setText(fakeAddress)
    }

    private fun toggleMapVisibility() {
        mapVisible = !mapVisible
        if (mapVisible) {
            mapView.visibility = View.VISIBLE
            btnSelectOnMap.text = "Đóng bản đồ"
            // Zoom đến vị trí hiện tại hoặc mặc định
            mapView.controller.setZoom(15.0)
        } else {
            mapView.visibility = View.GONE
            btnSelectOnMap.text = "Chọn trên bản đồ"
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
