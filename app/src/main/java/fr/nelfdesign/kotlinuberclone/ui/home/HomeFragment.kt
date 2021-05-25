package fr.nelfdesign.kotlinuberclone.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import fr.nelfdesign.kotlinuberclone.Common
import fr.nelfdesign.kotlinuberclone.R

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mapFragment: SupportMapFragment

    //Location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //onLine system
    private lateinit var onLineRef : DatabaseReference
    private lateinit var currentUserRef : DatabaseReference
    private lateinit var driversLocationRef : DatabaseReference
    private lateinit var geoFire : GeoFire

    private val onLineValueEventListener = object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists())
                currentUserRef.onDisconnect().removeValue()
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_SHORT).show()
        }

    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onLineRef.removeEventListener(onLineValueEventListener)

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        registerOnLineSystem()
    }

    private fun registerOnLineSystem() {
        onLineRef.addValueEventListener(onLineValueEventListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        init()

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }

    @SuppressLint("MissingPermission")
    private fun init() {

        onLineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected")
        driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE)
        currentUserRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        geoFire = GeoFire(driversLocationRef)
        registerOnLineSystem()

        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 10f
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val latLng = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                //Update location
                geoFire.setLocation(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    GeoLocation(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                ){key:String?, error:DatabaseError? ->
                    if (error != null)
                        Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
                    else
                        Snackbar.make(mapFragment.requireView(), "You're online", Snackbar.LENGTH_LONG).show()
                }
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        Dexter.withContext(context)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener{
                @SuppressLint("MissingPermission")
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    //enable button
                   mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationButtonClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener{
                                    e -> Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                            .addOnSuccessListener { location ->
                                val userLatlon = LatLng(location.latitude, location.longitude)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatlon, 18f))

                            }
                        true
                    }
                    //Layout
                    val view = mapFragment.requireView().findViewById<View>("1".toInt())?.parent as View
                    val locationButton = view.findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 50
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context, "Permission" + p0?.permissionName + " was denied", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    TODO("Not yet implemented")
                }

            }).check()

        mMap.uiSettings.isZoomControlsEnabled = true

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.uber_maps_style))
            if(!success){
                Log.e("Map_error", "Style parsing error")
            }
        }catch (e:Resources.NotFoundException){
            Log.e("Map_error", e.message.toString())
        }

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}