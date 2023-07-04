package com.klintsoft.basketballcourts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.GlideLazyListPreloader
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.klintsoft.basketballcourts.ui.theme.BasketballCourtsTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BasketballCourtsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier,//.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Navigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BasketCourtsScreen(navController: NavController, vm: BasketCourtsViewModel) {
    Log.i("MyInfo", "BasketCourtsScreen composed")
    val ui = vm.ui
    val ctx = LocalContext.current

    LaunchedEffect(Unit){
        vm.stopLocationTracking()
        vm.resetNewCourt()
        Log.i("MyInfo", "BasketCourtsScreen In Launched effect")
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )


    Scaffold(
        topBar = { TopBar(title = "Basketball Courts") },
        bottomBar = { BottomBar(
            navController = navController,
            resetCourtForm = vm::resetNewCourt,
            saveJson = vm::saveJson,
            uploadFile = vm::uploadImageFromFile,
            downloadCourt = vm::downloadCourt,
            uploadCourt = vm::uploadBasketCourt,
            deleteCourt = vm::deleteBasketCourt
        ) }) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {

            Log.i("MyInfo", "Is Coarse Location permission granted?: ${permissionsState.permissions.first().status.isGranted}")

            if (!permissionsState.permissions.first().status.isGranted) {
                permissionsState.permissions.forEach {
                    if (!it.status.isGranted) {
                        Log.i("MyInfo", "Asking permission ${it.permission}")
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "The app needs to know your location")
                            MultiplePermissionHandler(permissionsState, permissionsState.permissions.first().status.isGranted)
                        }
                    }
                }
            }
            else { // Coarse location Permission is granted
                LaunchedEffect(Unit){
                    vm.getBasketCourts()
                }
                Column(modifier = Modifier.fillMaxWidth()){
                   LazyColumn(modifier = Modifier){
                       items(ui.setOfCourts.toList()) {item ->
                           CourtCard(
                               court = item,
                               updateActiveCourt = vm::updateActiveCourt,
                               showOnMap = vm::showOnMap,
                               goToDetails = { navController.navigate(Screens.CourtDetailsScreen.route) },
                               goToEditing = { navController.navigate(Screens.AddCourtScreen.route) }
                           )
                       }
                   }
                }
            }
        }
    }
}

/*val urlList = listOf(
    "https://michaelgotsopoulos.com/basketcourts/basketCourt0.jpg",
    "https://michaelgotsopoulos.com/basketcourts/basketCourt1.jpg",
    "https://michaelgotsopoulos.com/basketcourts/basketCourt2.jpg",
    "https://michaelgotsopoulos.com/basketcourts/basketCourt3.jpg",
    "https://michaelgotsopoulos.com/basketcourts/basketCourt4.jpg",
)*/

//const val baseUrl = "https://michaelgotsopoulos.com/basketcourts/"
//const val courtsBaseUrl = "http://192.168.248.46:8080/basketcourts/"
const val baseUrl = "http://192.168.52.46:8080"
const val courtsBaseUrl = baseUrl + "/basketcourts/"

/*@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun RandomCourtFromApi(randomCourt: BasketCourt) {
    Column() {
        CourtCard(court = randomCourt)
    }
}*/


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CourtCard(
    court: BasketCourt,
    updateActiveCourt: (Int) -> Unit,
    showOnMap: (Context, BasketCourt) -> Unit,
    goToEditing: () -> Unit,
    goToDetails: () -> Unit) {
    val ctx = LocalContext.current
    val modifier1 = Modifier
        .padding(end = 2.dp)
        .clip(shape = RoundedCornerShape(50))
        .border(1.dp, MaterialTheme.colors.onBackground, RoundedCornerShape(50))
        .padding(horizontal = 6.dp, vertical = 1.dp)

    Card(
        elevation = 4.dp,
        modifier = Modifier
            .padding(2.dp)
            .clickable {
            court.id?.let {
                updateActiveCourt(it)
                goToDetails()
            } },
        backgroundColor = MaterialTheme.colors.secondary,
        border = BorderStroke(1.dp, MaterialTheme.colors.onBackground)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {

            Log.i("MyInfo", "${courtsBaseUrl}${court.id}.jpg")
            Log.i("MyInfo", "Court image Url: ${court.imageUrl}")
            AsyncImage(
                model = court.imageUrl,
                contentDescription = null,
                modifier = Modifier.weight(0.4f).padding(4.dp),
                contentScale = ContentScale.Fit
            )
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(4.dp)

            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = court.name, style = MaterialTheme.typography.h6)

                }

                Text(text = "Number of baskets: ${court.numberOfBaskets}")
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(text = if (court.isClosedCourt) "Closed" else "Open", modifier = modifier1, style = MaterialTheme.typography.caption)
                    Text(text = if (court.isPaid) "Paid" else "Free", modifier = modifier1, style = MaterialTheme.typography.caption)
                    Text(text = court.terrain, modifier = modifier1, style = MaterialTheme.typography.caption)
                }
            }
            Column(
                modifier = Modifier.fillMaxHeight().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween) {
                Text(text = "ID:${court.id}", style = MaterialTheme.typography.caption)
                Icon(Icons.Default.Place, contentDescription = null,
                    modifier = Modifier.clickable { showOnMap(ctx, court) })
                Icon(Icons.Default.Edit, contentDescription = null,
                    modifier = Modifier.clickable {
                        court.id?.let {
                            updateActiveCourt(it)
                            goToEditing()
                        }
                    })

            }
        }
    }
}

fun navigateToCourt(ctx: Context, court: BasketCourt){
    val uri = Uri.parse("google.navigation:q=" + court.latitude.toString() + "," + court.longitude.toString() + "&mode=d")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    ctx.startActivity(intent)
}
fun showOnMap(ctx: Context, court: BasketCourt){
    //Show location on Google Maps using intent
    //val packageManager = ctx.packageManager
    val uri = Uri.parse("geo:0,0?q=" + court.latitude + "," + court.longitude)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    //To verify that an app is available to receive the intent, call resolveActivity() on your Intent object.
    //otherwise it may crash if there's no app for this intent
    //intent.resolveActivity(packageManager)?.let{ ctx.startActivity(intent) }
    ctx.startActivity(intent)
}

@Composable
fun TopBar(title: String){
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(painter = painterResource(id = R.drawable.basketball), contentDescription = null, modifier = Modifier
            .height(45.dp)
            .padding(4.dp))
        Text(text = title,
            modifier = Modifier,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Image(painter = painterResource(id = R.drawable.basketball), contentDescription = null, modifier = Modifier
            .height(45.dp)
            .alpha(0f)
            .padding(4.dp))
    }
}

@Composable
fun BottomBar(
    navController: NavController,
    resetCourtForm: () -> Unit,
    saveJson: (Context) -> Boolean,
    uploadFile: (Context) -> Unit,
    downloadCourt: (String) -> Unit,
    uploadCourt: (BasketCourt) -> Unit,
    deleteCourt: () -> Unit
) {
    val ctx = LocalContext.current
    Divider(thickness = 2.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.background)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        IconButton(onClick = {
            resetCourtForm()
            navController.navigate(Screens.AddCourtScreen.route) { popUpTo(Screens.AddCourtScreen.route) }
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = "Add Court")
                Text(text = "Add Court", style = MaterialTheme.typography.caption)
            }
        }
        /*IconButton(onClick = {
            deleteCourt()
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Clear, contentDescription = "Delete Court")
                Text(text = "Delete Court", style = MaterialTheme.typography.caption)
            }
        }*/
        /*IconButton(onClick = {
            saveJson(ctx)
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_save_24), contentDescription = "Save Json")
                Text(text = "Save Json", style = MaterialTheme.typography.caption)
            }
        }*/
        IconButton(onClick = {
            uploadFile(ctx)
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_file_upload_24), contentDescription = "Upload image")
                Text(text = "Upload image", style = MaterialTheme.typography.caption)
            }
        }
        /*IconButton(onClick = {
            uploadCourt(BasketCourt(name = "uploaded"))
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_file_upload_24), contentDescription = "Upload court")
                Text(text = "Upload court", style = MaterialTheme.typography.caption)
            }
        }*/
        /*IconButton(onClick = {
            downloadCourt("3")
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_download_24), contentDescription = "Download court")
                Text(text = "Download court", style = MaterialTheme.typography.caption)
            }
        }*/
    }
}