/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

@file:Suppress("DEPRECATION")

package com.example.myapplication10.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberPickerState
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.myapplication10.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.sqrt


class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    //Sensors
    private val STEP_SENSOR_LISTENER = 20
    private lateinit var mSensorManager: SensorManager
    var mAccelerometer: Sensor? = null

    //Location
    var locationCallback: LocationCallback? = null
    var fusedLocationClient: FusedLocationProviderClient? = null
    var locationRequired = false
    var currentLocation by mutableStateOf(LocationDetails(0.toDouble(), 0.toDouble()))
    var distance_calculated by mutableStateOf(0f)

    var heartRateList: MutableList<Double> = mutableListOf()


    private lateinit var ambientController: AmbientModeSupport.AmbientController

    private val googleSignInClient by lazy {
        GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //Sensors
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        super.onCreate(savedInstanceState)

        setupPermissions()
        ambientController = AmbientModeSupport.attach(this)


        setContent {

            val context = LocalContext.current
            val startLocation = Location("Lugar A")
            var endLocation: Location = remember {
                Location("Lugar B")
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    for (lo in p0.locations) {
                        // Update UI with location data
                        Log.e("Location", "Location: " + lo.latitude + " " + lo.longitude)
                        currentLocation = LocationDetails(lo.latitude, lo.longitude)
                        if (currentLocation != null) {
                            val lugarB = LatLng(currentLocation.latitude, currentLocation.longitude)
                            endLocation.latitude = lugarB.latitude
                            endLocation.longitude = lugarB.longitude
                            distance_calculated = startLocation.distanceTo(endLocation)
                            //distance_calculated /= 1000
                            Log.e("endLocation", endLocation.toString())
                            Log.e("distance", "Distancia: $distance_calculated")

                        }
                    }
                }
            }

            val launcherMultiplePermissions = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
                if (areGranted) {
                    locationRequired = true
                    startLocationUpdates()
                    Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startLocationUpdates()
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient!!.lastLocation.addOnCompleteListener { loc ->
                    if (loc.result != null) {
                        startLocation.latitude = loc.result.latitude
                        startLocation.longitude = loc.result.longitude
                    }
                }
            }


            // Google Sign In
            var googleSignInAccount by remember {
                mutableStateOf(GoogleSignIn.getLastSignedInAccount(this))
            }

            val signInRequestLauncher = rememberLauncherForActivityResult(
                contract = GoogleSignInContract(googleSignInClient)
            ) {
                googleSignInAccount = it
                if (googleSignInAccount != null) {
                    startLocationUpdates()
                    Toast.makeText(
                        this,
                        "Bienvenido ${googleSignInAccount?.displayName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            val navController = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = NavRoutes.GoogleSignIn
            ) {
                composable(NavRoutes.GoogleSignIn) {
                    GoogleSignInScreen(
                        googleSignInAccount = googleSignInAccount,
                        onSignInClicked = { signInRequestLauncher.launch(Unit) },
                        navController = navController
                    )
                }
                composable(NavRoutes.SCREEN_1) {
                    CaptureScreen(
                        googleSignInAccount = googleSignInAccount,
                        navController = navController
                    )
                }
                composable(NavRoutes.SCREEN_2) {
                    val coroutineScope = rememberCoroutineScope()
                    MainScreen(
                        googleSignInAccount = googleSignInAccount,
                        navController = navController,
                        onSignOutClicked = {
                            coroutineScope.launch {
                                try {
                                    googleSignInClient.signOut()

                                    googleSignInAccount = null

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Sesión cerrada",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate(NavRoutes.GoogleSignIn)
                                } catch (apiException: ApiException) {
                                    Log.w("GoogleSignInActivity", "Sign out failed: $apiException")
                                }
                            }
                        }
                    )
                }
                composable(NavRoutes.SCREEN_3) {
                    ConfigRunning(
                        navController = navController
                    )
                }
                composable(NavRoutes.SCREEN_4) {
                    val distance = it.arguments?.getString("distance")?.toInt() ?: 0
                    RunningScreen(
                        distance = distance,
                        navController = navController,
                        currentLocation = currentLocation,
                        startLocation = startLocation,
                        onClickPermissions = {
                            val permissions = arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            if (permissions.all {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        it
                                    ) == PackageManager.PERMISSION_GRANTED
                                }) {
                                // Get the location
                                startLocationUpdates()
                            } else {
                                launcherMultiplePermissions.launch(permissions)
                            }
                        },
                        distance_calculated = distance_calculated,
                        heartRateList = heartRateList
                    )
                }
                composable(NavRoutes.SCREEN_5) {
                    val data: String? = it.arguments?.getString("data")
                    Log.e("data", data.toString())
                    var datos_separados = data?.split(",")
                    var datosCarrera = DatosCarrera(
                        distancia_total = datos_separados!![0].toInt(),
                        tiempo_total = datos_separados[1].toInt(),
                        latitudInicial = datos_separados[2].toDouble(),
                        longitudInicial = datos_separados[3].toDouble(),
                        latitudFinal = datos_separados[4].toDouble(),
                        longitudFinal = datos_separados[5].toDouble(),
                        ritmoMinimo = datos_separados[6].toDouble(),
                        ritmoMaximo = datos_separados[7].toDouble(),
                        ritmoPromedio = datos_separados[8].toDouble(),
                        no_pasos = datos_separados[9].toInt(),
                        fecha_actual = datos_separados[10],
                        hora_actual = datos_separados[11],
                    )
                    runSummary(
                        datosCarrera = datosCarrera,
                        googleSignInAccount = googleSignInAccount,
                        navController = navController
                    )
                }
                composable(NavRoutes.SCREEN_6) {
                    ProfileScreen(
                        googleSignInAccount = googleSignInAccount,
                        navController = navController
                    )
                }
                composable(NavRoutes.SCREEN_7) {
                    HistoricalRunning(googleSignInAccount = googleSignInAccount)
                }
            }


        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let {
            val locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationRequired) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(
            //this, Manifest.permission.ACTIVITY_RECOGNITION
            this, Manifest.permission.ACTIVITY_RECOGNITION
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("permisos", "Permiso denegado...")
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            STEP_SENSOR_LISTENER
        )
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback()
}

class MyAmbientCallback : AmbientModeSupport.AmbientCallback() {
    override fun onEnterAmbient(ambientDetails: Bundle?) {
        super.onEnterAmbient(ambientDetails)
    }

    override fun onExitAmbient() {
        super.onExitAmbient()
    }

    override fun onUpdateAmbient() {
        super.onUpdateAmbient()
    }
}

object NavRoutes {
    const val GoogleSignIn = "GoogleSignIn"
    const val SCREEN_1 = "screen1"
    const val SCREEN_2 = "screen2"
    const val SCREEN_3 = "screen3"
    const val SCREEN_4 = "screen4/{distance}"
    const val SCREEN_5 = "screen5/{data}"
    const val SCREEN_6 = "screen6"
    const val SCREEN_7 = "screen7"
}

@Composable
fun GoogleSignInScreen(
    googleSignInAccount: GoogleSignInAccount?,
    onSignInClicked: () -> Unit,
    navController: NavController
) {
    Log.e("GoogleSignInActivity", "Signed in as ${googleSignInAccount?.photoUrl.toString()}")
    val context = LocalContext.current
    Scaffold() {
        val listState = rememberScalingLazyListState()
        Scaffold(timeText = {
            if (!listState.isScrollInProgress) {
                TimeText()
            }
        }, vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }, positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
        ) {
            if (googleSignInAccount == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AndroidView(::SignInButton) { signInButton ->
                        signInButton.setOnClickListener {
                            onSignInClicked()
                        }
                    }
                }

            } else {
                val uid = googleSignInAccount?.id
                val database = Firebase.database
                val ref = database.getReference("carreras").child(uid.toString()).child("datos")
                ref.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val value = task.result?.value
                        if (value != null) {
                            Log.e("GoogleSignInActivity", "Signed in as ${value.toString()}")
                            navController.navigate(NavRoutes.SCREEN_2)
                        } else {
                            Log.e("GoogleSignInActivity", "Signed in as ${value.toString()}")
                            navController.navigate(NavRoutes.SCREEN_1)
                        }
                    } else {
                        Log.e("GoogleSignInActivity", "Signed in as ${task.exception.toString()}")
                        navController.navigate(NavRoutes.SCREEN_1)
                    }
                }
            }

        }
    }
}

@Composable
fun CaptureScreen(
    googleSignInAccount: GoogleSignInAccount?,
    navController: NavController
) {
    val context = LocalContext.current
    val itemsWeight = mutableListOf<Int>()
    for (i in 30..100) {
        itemsWeight.add(i)
    }
    val itemsHeight = mutableListOf<Double>()
    for (i in 50..205) {
        itemsHeight.add(i.toDouble() / 100)
    }
    val stateWeight = rememberPickerState(itemsWeight.size)
    val stateHeight = rememberPickerState(itemsHeight.size)
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = {
        if (!listState.isScrollInProgress) {
            TimeText()
        }
    }, vignette = {
        Vignette(vignettePosition = VignettePosition.TopAndBottom)
    }, positionIndicator = {
        PositionIndicator(scalingLazyListState = listState)
    }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            state = listState
        ) {
            item {
                Text(
                    text = "Hola ${googleSignInAccount?.displayName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item { Spacer(modifier = Modifier.height(5.dp)) }
            item { Text(text = "Ingrese los siguientes datos:", fontSize = 10.sp) }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = "Peso (kg):", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Picker(state = stateWeight, modifier = Modifier.size(50.dp, 15.dp)) {
                        Text(
                            text = itemsWeight[it].toString(),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Estatura (metros):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Picker(state = stateHeight, modifier = Modifier.size(50.dp, 15.dp)) {
                        Text(
                            text = itemsHeight[it].toString(),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                var positionWeight = stateWeight.selectedOption
                var positionHeight = stateHeight.selectedOption
                val weight = itemsWeight[positionWeight]
                val height = itemsHeight[positionHeight]
                //Text(text = "Peso: $weight kg. Estatura: $height m.", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Button(onClick = {
                    val uid = googleSignInAccount?.id
                    val database = Firebase.database
                    val ref = database.getReference("carreras").child(uid.toString()).child("datos")
                    val datos = DatosIniciales(peso = weight, estatura = height)
                    ref.setValue(datos).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Datos enviados correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate(NavRoutes.SCREEN_2)
                        } else {
                            Toast.makeText(
                                context,
                                "Error al enviar datos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
                    Text(
                        text = "Enviar datos",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    googleSignInAccount: GoogleSignInAccount?,
    navController: NavController,
    onSignOutClicked: () -> Unit
) {
    Scaffold() {
        val listState = rememberScalingLazyListState()
        Scaffold(timeText = {
            if (!listState.isScrollInProgress) {
                TimeText()
            }
        }, vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }, positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                state = listState
            ) {
                item {
                    Text(
                        text = "Hola ${googleSignInAccount?.displayName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item {
                    Button(
                        onClick = {
                            // Navigate to the second screen
                            navController.navigate(NavRoutes.SCREEN_3)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.width(150.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.run_icon),
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Box(
                                modifier = Modifier.weight(1F),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Iniciar carrera", color = Color.Black)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item {
                    Button(
                        onClick = {
                            navController.navigate(NavRoutes.SCREEN_7)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.width(150.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.historial),
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Box(
                                modifier = Modifier.weight(1F),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Historial de carreras",
                                    color = Color.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item {
                    Button(
                        onClick = {
                            // Navigate to the profile screen
                            navController.navigate(NavRoutes.SCREEN_6)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.width(150.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.profile),
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Box(
                                modifier = Modifier.weight(1F),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Ajustes perfil", color = Color.Black, fontSize = 13.sp)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
                item {
                    Button(
                        onClick = {
                            onSignOutClicked()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.width(150.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.logout),
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Box(
                                modifier = Modifier.weight(1F),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Cerrar sesión", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigRunning(
    navController: NavController
) {
    val items = listOf("50", "100", "200", "300", "500", "700", "800", "1000", "1200", "1500")
    val state = rememberPickerState(items.size)
    Scaffold() {
        val listState = rememberScalingLazyListState()
        Scaffold(timeText = {
            if (!listState.isScrollInProgress) {
                TimeText()
            }
        }, vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }, positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                state = listState
            ) {
                item {
                    Text(
                        text = "Configura la carrera:",
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item { Text(text = "Distancia en metros a recorrer:", fontSize = 10.sp) }
                item {
                    Picker(state = state, modifier = Modifier.size(75.dp, 50.dp)) {
                        Text(text = "${items[it]}m.", modifier = Modifier.padding(10.dp))
                    }
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    var position = state.selectedOption
                    val distance = items[position]
                    Button(
                        onClick = {
                            navController.navigate("screen4/$distance")
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.width(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.play),
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Box(
                                modifier = Modifier.weight(1F),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Iniciar", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RunningScreen(
    distance: Int,
    navController: NavController,
    startLocation: Location,
    currentLocation: LocationDetails?,
    onClickPermissions: () -> Unit,
    distance_calculated: Float,
    heartRateList: MutableList<Double>,
) {
    var context = LocalContext.current
    var stepCount by remember { mutableStateOf(0) }
    val ctx = LocalContext.current
    val sensorManager: SensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val mAccelerometer: Sensor = sensorManager.getDefaultSensor(
        Sensor.TYPE_ACCELEROMETER
    )
    var dvalue = remember {
        mutableStateOf("0")
    }
    val SensorListener1 = object : SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            p0 ?: return
            if (p0 != null) {
                if (p0.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val values = p0.values
                    val x = values[0]
                    val y = values[1]
                    val z = values[2]

                    val acceleration = sqrt((x * x + y * y + z * z).toDouble())
                    Log.e("acceleration", acceleration.toString())
                    if (acceleration > 12.0) {
                        stepCount++
                    }
                }
            }
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            println("onAccuracyChanged: Sensor: $p0: accuracy: $p1 ")
        }
    }
    sensorManager.registerListener(
        SensorListener1,
        mAccelerometer,
        SensorManager.SENSOR_DELAY_GAME
    )
    val heartSensor: Sensor = sensorManager.getDefaultSensor(
        Sensor.TYPE_HEART_RATE
    )
    var heartSensorValue = remember {
        mutableStateOf("0")
    }
    val HeartRateSensorListener = object : SensorEventListener {
        override fun onSensorChanged(p0: SensorEvent?) {
            p0 ?: return
            if (p0 != null) {
                p0.values.firstOrNull()?.let {
                    if (p0.sensor.type == Sensor.TYPE_HEART_RATE) {
                        heartSensorValue.value = p0.values[0].toString()
                        Log.d("value", "Valor Ritmo: ${heartSensorValue.value}")
                        if (p0.values[0].toDouble() != 0.0) {
                            heartRateList.add(p0.values[0].toDouble())
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            println("onAccuracyChanged: Sensor: $p0: accuracy: $p1 ")
        }
    }
    sensorManager.registerListener(
        HeartRateSensorListener,
        heartSensor,
        SensorManager.SENSOR_DELAY_NORMAL
    )


    Scaffold() {
        val listState = rememberScalingLazyListState()
        Scaffold(timeText = {
            if (!listState.isScrollInProgress) {
                TimeText()
            }
        }, vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }, positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
        ) {
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                state = listState
            ) {
                item {
                    Button(
                        onClick = {
                            // Navigate to the second screen
                            onClickPermissions()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.width(80.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.location),
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Box(
                                modifier = Modifier.weight(1F),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Ubicación", color = Color.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }
                item {
                    val totalTime = 100L
                    val initialValue: Float = 1f
                    var size by remember {
                        mutableStateOf(IntSize.Zero)
                    }
                    // create variable for value
                    var value by remember {
                        mutableStateOf(initialValue)
                    }
                    // create variable for current time
                    var currentTime by remember {
                        mutableStateOf(totalTime)
                    }
                    // create variable for isTimerRunning
                    var isTimerRunning by remember {
                        mutableStateOf(false)
                    }
                    LaunchedEffect(key1 = currentTime, key2 = isTimerRunning) {
                        if (currentTime >= 0 && isTimerRunning) {
                            delay(100L)
                            currentTime += 100L
                            value = currentTime / totalTime.toFloat()
                        }
                    }
                    if ((currentTime / 1000L) > 0 && (currentTime / 1000L).toInt() % 2 == 0) {
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = contentModifier
                            .onSizeChanged {
                                size = it
                            }
                    )
                    {
                        // add value of the timer
                        Text(
                            text = (currentTime / 1000L).toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        // create button to start or stop the timer
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(7.dp)
                                .height(20.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentTime <= 0L) {
                                        currentTime = totalTime
                                        isTimerRunning = true
                                    } else {
                                        isTimerRunning = !isTimerRunning
                                    }
                                },
                                //modifier = Modifier.align(Alignment.BottomCenter),
                                // change button color
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (!isTimerRunning || currentTime <= 0L) {
                                        Color.Green
                                    } else {
                                        Color.Red
                                    }
                                )
                            ) {
                                Text(
                                    // change the text of button based on values
                                    text = if (isTimerRunning && currentTime >= 0L) "Detener"
                                    else if (!isTimerRunning && currentTime >= 0L) "Iniciar"
                                    else "Restart",
                                    fontSize = 10.sp,
                                )
                            }
                            Button(
                                onClick = {
                                    if (currentTime >= 0L) {
                                        currentTime = totalTime
                                        isTimerRunning = true
                                    } else {
                                        isTimerRunning = !isTimerRunning
                                    }
                                    heartRateList.clear()
                                },
                                //modifier = Modifier.align(Alignment.BottomCenter),
                                // change button color
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.Blue
                                )
                            )
                            {
                                Text(text = "Reiniciar", fontSize = 9.sp)
                            }
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = contentModifier
                            .onSizeChanged {
                                size = it
                            }
                    ) {
                        Spacer(modifier = Modifier.height(100.dp))
                        Text(text = "Distancia a recorrer: $distance metros", fontSize = 10.sp)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = contentModifier
                            .onSizeChanged {
                                size = it
                            }
                    ) {
                        Spacer(modifier = Modifier.height(130.dp))
                        Text(
                            text = "Distancia recorrida: ${distance_calculated.toInt()} metros",
                            fontSize = 10.sp
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = contentModifier
                            .onSizeChanged {
                                size = it
                            }
                    ) {
                        Spacer(modifier = Modifier.height(190.dp))
                        Text(
                            text = "Localizacion: ${currentLocation?.latitude}, ${currentLocation?.longitude}",
                            fontSize = 11.sp
                        )
                    }

                    if (distance_calculated >= distance) {
                        Log.e("heartRateList", "$heartRateList")
                        val horaActual = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val minutoActual = Calendar.getInstance().get(Calendar.MINUTE)
                        val segundoActual = Calendar.getInstance().get(Calendar.SECOND)
                        val timestamp =
                            ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyy"))

                        val distancia_total = distance_calculated.toInt()
                        val tiempo_total = (currentTime / 1000L).toInt()
                        val latitudInicial = startLocation.latitude
                        val longitudInicial = startLocation.longitude
                        val latitudFinal = currentLocation?.latitude ?: 0.0
                        val longitudFinal = currentLocation?.longitude ?: 0.0
                        val ritmoMinimo = heartRateList.minOrNull()
                        val ritmoMaximo = heartRateList.maxOrNull()
                        val ritmoPromedio = heartRateList.average()
                        val no_pasos = stepCount
                        val fecha_actual = timestamp
                        val hora_actual = "$horaActual:$minutoActual:$segundoActual"

                        val data =
                            "$distancia_total,$tiempo_total,$latitudInicial,$longitudInicial,$latitudFinal,$longitudFinal,$ritmoMinimo,$ritmoMaximo,$ritmoPromedio,$no_pasos,$fecha_actual,$hora_actual"
                        //navController.previousBackStackEntry?.savedStateHandle?.set("data", "$data")
                        navController.navigate("screen5/$data")
                        {
                            popUpTo("screen5/$data") {
                                inclusive = true
                            }
                        }
                        Toast.makeText(
                            context,
                            "Carrera terminada",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}


@SuppressLint("UnrememberedMutableState")
@Composable
fun runSummary(
    datosCarrera: DatosCarrera?,
    googleSignInAccount: GoogleSignInAccount?,
    navController: NavController,
) {
    var _carreraData = mutableStateOf<List<DatosFinales>>(emptyList())
    val carreraData: State<List<DatosFinales>> = _carreraData
    val uid = googleSignInAccount?.id
    val database = Firebase.database
    var addresses: List<Address>?
    var address: Address?
    var fulladdressInicial = ""
    var fulladdressFinal = ""
    val context = LocalContext.current
    val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    var caloriesBurned: Float = 0.0F
    Scaffold() {
        val listState = rememberScalingLazyListState()
        Scaffold(timeText = {
            if (!listState.isScrollInProgress) {
                TimeText()
            }
        }, vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }, positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                state = listState
            ) {
                item {
                    Text(
                        text = "Resumen de la carrera",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Text(
                        text = "Fecha y hora: ${datosCarrera?.fecha_actual} ${datosCarrera?.hora_actual}",
                        fontSize = 8.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Text(
                        text = "Distancia total: ${datosCarrera?.distancia_total} metros",
                        fontSize = 8.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Text(
                        text = "Tiempo total: ${datosCarrera?.tiempo_total} segundos",
                        fontSize = 8.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    try {
                        addresses = geocoder.getFromLocation(
                            datosCarrera?.latitudInicial!!,
                            datosCarrera?.longitudInicial!!,
                            1
                        )
                        if (!addresses.isNullOrEmpty()) {
                            address = addresses!![0]
                            fulladdressInicial = address!!.getAddressLine(0)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    Text(
                        text = "Dirección inicial: $fulladdressInicial",
                        fontSize = 6.sp,
                        textAlign = TextAlign.Center
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    try {
                        addresses = geocoder.getFromLocation(
                            datosCarrera?.latitudFinal!!,
                            datosCarrera?.longitudFinal!!,
                            1
                        )
                        if (!addresses.isNullOrEmpty()) {
                            address = addresses!![0]
                            fulladdressFinal = address!!.getAddressLine(0)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    Text(
                        text = "Dirección final: $fulladdressFinal",
                        fontSize = 6.sp,
                        textAlign = TextAlign.Center
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Text(
                        text = "Ritmo cardiaco mínimo: ${datosCarrera?.ritmoMinimo}",
                        fontSize = 8.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Text(
                        text = "Ritmo cardiaco máximo: ${datosCarrera?.ritmoMaximo}",
                        fontSize = 8.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Text(
                        text = "Ritmo cardiaco promedio: ${datosCarrera?.ritmoPromedio}",
                        fontSize = 8.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Text(text = "Número de pasos: ${datosCarrera?.no_pasos}", fontSize = 8.sp)
                }
                item { Spacer(modifier = Modifier.height(5.dp)) }
                var peso = 0.0F
                database.getReference("carreras").child("$uid").child("datos").child("peso").get()
                    .addOnSuccessListener {
                        peso = it.value.toString().toFloat()
                        val distancia = datosCarrera?.distancia_total?.toFloat()
                        val tiempo = datosCarrera?.tiempo_total?.toFloat()
                        caloriesBurned = calcularCalorias(peso, distancia, tiempo)
                    }
                item {

                    Text(text = "Calorías quemadas: $caloriesBurned", fontSize = 8.sp)
                }

                item { Spacer(modifier = Modifier.height(5.dp)) }
                item {
                    Button(
                        onClick = {
                            var datos_Finales = DatosFinales(
                                distancia_total = datosCarrera?.distancia_total!!,
                                tiempo_total = datosCarrera?.tiempo_total!!,
                                latitudInicial = datosCarrera?.latitudInicial!!,
                                longitudInicial = datosCarrera?.longitudInicial!!,
                                direccionInicial = fulladdressInicial,
                                latitudFinal = datosCarrera?.latitudFinal!!,
                                longitudFinal = datosCarrera?.longitudFinal!!,
                                direccionFinal = fulladdressFinal,
                                ritmoMinimo = datosCarrera?.ritmoMinimo!!,
                                ritmoMaximo = datosCarrera?.ritmoMaximo!!,
                                ritmoPromedio = datosCarrera?.ritmoPromedio!!,
                                no_pasos = datosCarrera?.no_pasos!!,
                                fecha_actual = datosCarrera?.fecha_actual!!,
                                hora_actual = datosCarrera?.hora_actual!!,
                                calorias_quemadas = caloriesBurned.toDouble()
                            )
                            val database = Firebase.database
                            val uid = googleSignInAccount?.id
                            val ref = database.getReference("carreras").child(uid.toString())
                                .child("historial")
                            var contador_items = 0
                            ref.get().addOnCompleteListener {
                                contador_items = it.result?.childrenCount?.toInt()!!
                                Log.e("Contador data", "Data: ${it.result?.childrenCount}")
                                ref.child(contador_items.toString()).setValue(datos_Finales)
                                    .addOnCompleteListener {
                                        Toast.makeText(
                                            context,
                                            "Datos almacenados correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al almacenar los datos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                navController.navigate(NavRoutes.SCREEN_2) {
                                    popUpTo(NavRoutes.SCREEN_2) { inclusive = true }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.width(90.dp),
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Box(
                                modifier = Modifier.weight(1F),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cerrar y guardar",
                                    color = Color.Black,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun calcularCalorias(peso: Float, distancia: Float?, tiempo: Float?): Float {
    val distanciaKm = distancia!! / 1000 // convertir metros a kilómetros
    val velocidad = distanciaKm / (tiempo!! / 3600.0f) // calcular velocidad promedio en km/h
    val factorMET = 9.8f // factor MET para correr
    return peso * distanciaKm * factorMET // calcular calorías quemadas
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun HistoricalRunning(
    googleSignInAccount: GoogleSignInAccount?
) {
    var _carreraData = mutableStateOf<List<DatosFinales>>(emptyList())
    val carreraData: State<List<DatosFinales>> = _carreraData
    Scaffold() {
        val listState = rememberScalingLazyListState()
        Scaffold(timeText = {
            if (!listState.isScrollInProgress) {
                TimeText()
            }
        }, vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        }, positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                state = listState
            ) {
                val database = Firebase.database
                val uid = googleSignInAccount?.id
                val ref = database.getReference("carreras").child(uid.toString()).child("historial")
                ref.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        _carreraData.value = snapshot.getValue<List<DatosFinales>>()!!
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Error", "Error: ${error.message}")
                    }
                })
                var index = carreraData.value.size
                if (index > 0) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    item {
                        Text(
                            text = "Historial de carreras:",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }
                items(carreraData.value) { carrera ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .background(Color.White)
                            .clip(RoundedCornerShape(0.dp))
                            .padding(
                                10.dp
                            )){
                        CardData(data = carrera)
                    }
                    Spacer(modifier = Modifier.width(5.dp))

                }
            }
        }
    }
}

@Composable
fun CardData(data: DatosFinales) {
    Text(text = "Fecha: ${data.fecha_actual} ${data.hora_actual}", style = MaterialTheme.typography.title1, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(modifier = Modifier.width(5.dp))
    Row {
        Text(text = "Tiempo total: ", style = MaterialTheme.typography.body1, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = "${data.tiempo_total} segundos", style = MaterialTheme.typography.caption1, fontSize = 10.sp, color = Color.Black)
        Spacer(modifier = Modifier.width(3.dp))
    }
    Row {
        Text(text = "Distancia total: ", style = MaterialTheme.typography.body1, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = "${data.distancia_total} metros", style = MaterialTheme.typography.caption1, fontSize = 10.sp, color = Color.Black)
        Spacer(modifier = Modifier.width(3.dp))
    }
    Row {
        Text(text = "Ritmo cardiaco promedio: ",style = MaterialTheme.typography.body1, fontSize = 10.sp, fontWeight = FontWeight.Bold , color = Color.Black)
        Text(text = "${data.ritmoPromedio.toInt()} bpm", style = MaterialTheme.typography.caption1, fontSize = 10.sp, color = Color.Black)
        Spacer(modifier = Modifier.width(3.dp))
    }
    Row {
        Text(text = "No. pasos: ", style = MaterialTheme.typography.body1, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = "${data.no_pasos}", style = MaterialTheme.typography.caption1, fontSize = 10.sp, color = Color.Black)
        Spacer(modifier = Modifier.width(3.dp))
    }
    Row {
        Text(text = "Calorias quemadas: ", style = MaterialTheme.typography.body1, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = "${data.calorias_quemadas.toInt()}", style = MaterialTheme.typography.caption1, fontSize = 10.sp, color = Color.Black)
        Spacer(modifier = Modifier.width(3.dp))
    }

}

@Composable
fun ProfileScreen(
    googleSignInAccount: GoogleSignInAccount?,
    navController: NavController
) {
    val database = Firebase.database
    val uid = googleSignInAccount?.id
    val ref = database.getReference("carreras").child(uid.toString()).child("datos")
    var peso_item = 0
    var estatura_item: Double = 0.0
    var itemsWeightModificado = mutableListOf<Int>()
    var itemsHeightModificado = mutableListOf<Double>()
    val context = LocalContext.current
    val itemsWeight = mutableListOf<Int>()
    for (i in 30..100) {
        itemsWeight.add(i)
    }
    val itemsHeight = mutableListOf<Double>()
    for (i in 50..205) {
        itemsHeight.add(i.toDouble() / 100)
    }
    Log.d("TAG", "Value is: $itemsWeight")
    Log.d("TAG", "Value is: $itemsHeight")
    ref.child("peso").get().addOnSuccessListener {
        peso_item = it.value.toString().toInt()
        Log.d("TAG", "Value is: $peso_item")
        val IndexWeight = itemsWeight.indexOf(peso_item)
        Log.d("TAG", "IndexWeight is: $IndexWeight")
        if (IndexWeight != -1) {
            itemsWeight.removeAt(IndexWeight)
            itemsWeight.add(0, peso_item)
        }
    }
    ref.child("estatura").get().addOnSuccessListener {
        estatura_item = it.value.toString().toDouble()
        Log.d("TAG", "Value is: $estatura_item")
        val IndexHeight = itemsHeight.indexOf(estatura_item)
        Log.d("TAG", "IndexHeight is: $IndexHeight")
        if (IndexHeight != -1) {
            itemsHeight.removeAt(IndexHeight)
            itemsHeight.add(0, estatura_item)
        }
    }

    val stateWeight = rememberPickerState(itemsWeight.size)
    val stateHeight = rememberPickerState(itemsHeight.size)
    val listState = rememberScalingLazyListState()
    Scaffold(timeText = {
        if (!listState.isScrollInProgress) {
            TimeText()
        }
    }, vignette = {
        Vignette(vignettePosition = VignettePosition.TopAndBottom)
    }, positionIndicator = {
        PositionIndicator(scalingLazyListState = listState)
    }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            state = listState
        ) {
            item {
                Text(
                    text = "Ajustes de perfil",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item { Spacer(modifier = Modifier.height(5.dp)) }
            item {
                Text(text = "Nombre: ${googleSignInAccount?.displayName}", fontSize = 10.sp)
            }
            item { Spacer(modifier = Modifier.height(5.dp)) }
            item {
                Text(text = "Correo: ${googleSignInAccount?.email}", fontSize = 10.sp)
            }
            item { Spacer(modifier = Modifier.height(5.dp)) }
            item { Text(text = "Actualice los siguientes datos:", fontSize = 10.sp) }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = "Peso (kg):", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Picker(state = stateWeight, modifier = Modifier.size(50.dp, 15.dp)) {
                        Text(
                            text = itemsWeight[it].toString(),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Estatura (metros):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Picker(state = stateHeight, modifier = Modifier.size(50.dp, 15.dp)) {
                        Text(
                            text = itemsHeight[it].toString(),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                var positionWeight = stateWeight.selectedOption
                var positionHeight = stateHeight.selectedOption
                val weight = itemsWeight[positionWeight]
                val height = itemsHeight[positionHeight]
                //Text(text = "Peso: $weight kg. Estatura: $height m.", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Button(onClick = {
                    val uid = googleSignInAccount?.id
                    val database = Firebase.database
                    val ref = database.getReference("carreras").child(uid.toString()).child("datos")
                    val datos = DatosIniciales(peso = weight, estatura = height)
                    ref.setValue(datos).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Datos actualizados correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.navigate(NavRoutes.SCREEN_2)
                        } else {
                            Toast.makeText(
                                context,
                                "Error al enviar datos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
                    Text(
                        text = "Actualizar",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

private class GoogleSignInContract(
    private val googleSignInClient: GoogleSignInClient
) : ActivityResultContract<Unit, GoogleSignInAccount?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        googleSignInClient.signInIntent

    override fun parseResult(resultCode: Int, intent: Intent?): GoogleSignInAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
        // As documented, this task must be complete
        check(task.isComplete)

        return if (task.isSuccessful) {
            task.result
        } else {
            val exception = task.exception
            check(exception is ApiException)
            Log.w(
                "GoogleSignInContract",
                "Sign in failed: code=${
                    exception.statusCode
                }, message=${
                    GoogleSignInStatusCodes.getStatusCodeString(exception.statusCode)
                }"
            )
            null
        }
    }
}

