package com.example.suntime.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.suntime.databinding.FragmentCompassBinding
import com.example.suntime.util.AddressResolver
import java.util.Locale

class CompassFragment : Fragment(), SensorEventListener, LocationListener {

    private var _binding: FragmentCompassBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var accel: Sensor? = null
    private var magnet: Sensor? = null
    private var rotationVector: Sensor? = null

    /** 是否使用厂商融合传感器（TYPE_ROTATION_VECTOR）。可用时优先，精度与稳定性都更好。 */
    private var useFused = false

    private val gravity = FloatArray(3)
    private val geomag = FloatArray(3)
    private var haveAccel = false
    private var haveMagnet = false

    private val rot = FloatArray(9)
    private val rotRemapped = FloatArray(9)
    private val orientation = FloatArray(3)

    /** 磁北方向（平滑后的原始方位角），NaN 表示尚未获得首个有效值。 */
    private var magAzimuth = Float.NaN

    /** 磁偏角：磁北 → 真北（东偏为正）。 */
    private var declination = 0f

    /** 磁场校准精度（来自 onAccuracyChanged）。 */
    private var magAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    /** 当前显示的方位角（真北基准，已做磁偏角修正）。 */
    private var azimuth = 0f
    private var lastShown = -1f
    private var lastAccuracy = -1

    private lateinit var locationManager: LocationManager

    private val dirs = arrayOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    private val dirsEn = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

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
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.btnLocate.setOnClickListener { ensureLocation() }
    }

    override fun onResume() {
        super.onResume()
        // 优先使用融合传感器：厂商已做倾角补偿与滤波，读数与系统指南针一致
        useFused = rotationVector != null
        magAzimuth = Float.NaN
        haveAccel = false
        haveMagnet = false
        lastShown = -1f

        if (useFused) {
            rotationVector?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } else {
            accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnet?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
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
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onCoord(it) }
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { onCoord(it) }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 5f, this)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "定位不可用：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== 传感器 ====================

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rot, event.values)
                handleRotationMatrix(rot)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (useFused) return
                lowPass(event.values, gravity, if (haveAccel) 0.20f else 1f)
                haveAccel = true
                computeFromRaw()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                if (useFused) return
                lowPass(event.values, geomag, if (haveMagnet) 0.25f else 1f)
                haveMagnet = true
                computeFromRaw()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            magAccuracy = accuracy
        }
    }

    private fun lowPass(input: FloatArray, out: FloatArray, alpha: Float) {
        for (i in 0..2) out[i] += alpha * (input[i] - out[i])
    }

    private fun computeFromRaw() {
        if (!haveAccel || !haveMagnet) return
        if (SensorManager.getRotationMatrix(rot, null, gravity, geomag)) {
            handleRotationMatrix(rot)
        }
    }

    private fun handleRotationMatrix(matrix: FloatArray) {
        val rotated = remapForScreenRotation(matrix, rotRemapped)
        SensorManager.getOrientation(if (rotated) rotRemapped else matrix, orientation)

        var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        deg = normalize(deg)

        magAzimuth = smoothAngle(magAzimuth, deg, 0.25f)
        publish()
    }

    /**
     * 屏幕旋转时把设备坐标重映射到「当前屏幕坐标系」，
     * 否则横屏握持时读数会整体偏移 90°。
     */
    @Suppress("DEPRECATION")
    private fun remapForScreenRotation(src: FloatArray, dst: FloatArray): Boolean {
        val rotation = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                (activity?.display ?: requireActivity().windowManager.defaultDisplay).rotation
            } else {
                requireActivity().windowManager.defaultDisplay.rotation
            }
        } catch (_: Exception) {
            Surface.ROTATION_0
        }
        return when (rotation) {
            Surface.ROTATION_90 -> {
                SensorManager.remapCoordinateSystem(
                    src, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, dst
                ); true
            }
            Surface.ROTATION_180 -> {
                SensorManager.remapCoordinateSystem(
                    src, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, dst
                ); true
            }
            Surface.ROTATION_270 -> {
                SensorManager.remapCoordinateSystem(
                    src, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, dst
                ); true
            }
            else -> false
        }
    }

    /**
     * 角度低通滤波。必须先取 180° 以内的最短角度差再做插值，
     * 否则跨 0°/360° 边界（359° → 1°）时会瞬间跳到 300° 附近。
     */
    private fun smoothAngle(prev: Float, next: Float, alpha: Float): Float {
        if (prev.isNaN()) return next
        var delta = next - prev
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f
        return normalize(prev + delta * alpha)
    }

    private fun normalize(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    private fun publish() {
        if (magAzimuth.isNaN()) return
        val trueAzimuth = normalize(magAzimuth + declination)

        // 角度变化很小且校准状态未变，就不刷新 UI，避免高频 setText 造成无谓开销
        val accuracyChanged = magAccuracy != lastAccuracy
        if (!accuracyChanged && lastShown >= 0f &&
            Math.abs(shortestDelta(trueAzimuth, lastShown)) < 0.3f) return
        lastAccuracy = magAccuracy
        lastShown = trueAzimuth
        azimuth = trueAzimuth

        binding.compassView.setAzimuth(trueAzimuth)
        binding.tvHeading.text = String.format(Locale.CHINA, "%.0f°", trueAzimuth)

        val idx = (((trueAzimuth + 22.5f) % 360f) / 45f).toInt() % 8
        val hint = if (magAccuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) " · 需校准" else ""
        binding.tvHeadingDir.text = "${dirs[idx]}  ${dirsEn[idx]}$hint"
    }

    private fun shortestDelta(a: Float, b: Float): Float {
        var d = a - b
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }

    // ==================== 定位与地址 ====================

    // 地址反查节流：位移 > 50m 或间隔 > 30s 才重新请求，避免频繁打网络
    private var lastAddrLat = Double.NaN
    private var lastAddrLng = Double.NaN
    private var lastAddrTime = 0L
    private val distHolder = FloatArray(1)

    private fun onCoord(loc: Location) {
        binding.tvLat.text = String.format(Locale.CHINA, "%.5f", loc.latitude)
        binding.tvLng.text = String.format(Locale.CHINA, "%.5f", loc.longitude)
        val acc = loc.accuracy
        binding.tvAccuracy.text = if (acc >= 0f && acc.isFinite()) "定位精度 ±${acc.toInt()} 米" else ""

        updateDeclination(loc)

        val now = System.currentTimeMillis()
        if (!lastAddrLat.isNaN()) {
            Location.distanceBetween(lastAddrLat, lastAddrLng, loc.latitude, loc.longitude, distHolder)
        }
        val moved = lastAddrLat.isNaN() || distHolder[0] > 50f
        val stale = now - lastAddrTime > 30_000
        if (moved || stale) {
            lastAddrLat = loc.latitude
            lastAddrLng = loc.longitude
            lastAddrTime = now
            requestAddress(loc)
        }
    }

    /**
     * 磁偏角修正：传感器给出的是「磁北」方位，与地图/系统指南针的「真北」存在偏差。
     * 用 WMM 模型按当前经纬度与日期计算，修正后读数才能与系统指南针对齐。
     */
    private fun updateDeclination(loc: Location) {
        try {
            val altitude = if (loc.hasAltitude()) loc.altitude.toFloat() else 0f
            val field = GeomagneticField(
                loc.latitude.toFloat(),
                loc.longitude.toFloat(),
                altitude,
                System.currentTimeMillis()
            )
            declination = field.declination
            lastShown = -1f   // 强制刷新一次显示
        } catch (_: Exception) {
            // 无法计算时保持上一次的值（未定位时为 0，即按磁北显示）
        }
    }

    private fun requestAddress(loc: Location) {
        binding.tvAddress.text = "正在解析地址…"
        binding.tvAddressSource.text = ""
        AddressResolver.resolve(requireContext(), loc.latitude, loc.longitude) { res ->
            if (_binding == null) return@resolve
            binding.tvAddress.text = when {
                res.address != null -> res.address
                res.error != null -> "地址解析失败：${res.error}"
                else -> "（无网络或未能解析地址）"
            }
            binding.tvAddressSource.text = if (res.provider != null) "地址来源：${res.provider}" else ""
        }
    }

    override fun onLocationChanged(loc: Location) = onCoord(loc)
    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
