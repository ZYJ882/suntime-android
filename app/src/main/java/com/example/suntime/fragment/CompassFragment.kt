package com.example.suntime.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.suntime.databinding.FragmentCompassBinding
import java.util.Locale

class CompassFragment : Fragment(), SensorEventListener, LocationListener {

    private var _binding: FragmentCompassBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accel: Sensor? = null
    private var magnet: Sensor? = null

    private val gravity = FloatArray(3)
    private val geomag = FloatArray(3)
    private var haveAccel = false
    private var haveMagnet = false

    private var azimuth = 0f

    private lateinit var locationManager: LocationManager
    private val dirs = arrayOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")

    private val reqLoc = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startLocation() else Toast.makeText(requireContext(), "未授权定位，仅显示方向", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCompassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.btnLocate.setOnClickListener { ensureLocation() }
    }

    override fun onResume() {
        super.onResume()
        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magnet?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        ensureLocation()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
    }

    private fun ensureLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            startLocation()
        } else {
            reqLoc.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocation() {
        try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { showLocation(it) }
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { showLocation(it) }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, this)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "定位不可用：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> { event.values.copyInto(gravity); haveAccel = true }
            Sensor.TYPE_MAGNETIC_FIELD -> { event.values.copyInto(geomag); haveMagnet = true }
        }
        if (haveAccel && haveMagnet) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravity, geomag)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                deg = (deg + 360) % 360
                // 简单平滑
                azimuth = azimuth * 0.85f + deg * 0.15f
                binding.compassView.setAzimuth(azimuth)
                binding.tvHeading.text = String.format(Locale.CHINA, "%.0f°", azimuth)
                val idx = (((azimuth + 22.5f) / 45).toInt()) % 8
                binding.tvHeadingDir.text = "${dirs[idx]} ${if (azimuth <= 0.5f || azimuth >= 359.5f) "N" else dirs[idx].first()}"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun showLocation(loc: Location) {
        binding.tvLat.text = "纬度：${String.format(Locale.CHINA, "%.5f", loc.latitude)}°"
        binding.tvLng.text = "经度：${String.format(Locale.CHINA, "%.5f", loc.longitude)}°"
        // 反查地址（需网络）
        try {
            val geocoder = Geocoder(requireContext(), Locale.CHINA)
            val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            if (!list.isNullOrEmpty()) {
                val a = list[0]
                val parts = listOf(a.countryName, a.adminArea, a.locality, a.thoroughfare).filterNotNull().filter { it.isNotBlank() }
                binding.tvAddress.text = parts.joinToString(" ")
            } else binding.tvAddress.text = ""
        } catch (e: Exception) {
            binding.tvAddress.text = "（无法解析地址）"
        }
    }

    override fun onLocationChanged(loc: Location) = showLocation(loc)
    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
