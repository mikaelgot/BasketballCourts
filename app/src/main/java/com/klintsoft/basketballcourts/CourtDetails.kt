package com.klintsoft.basketballcourts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import java.io.File


@OptIn(ExperimentalPermissionsApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun CourtDetailsScreen(navController: NavController, vm: BasketCourtsViewModel) {
    Log.i("MyInfo", "Add court composed")
    val ui = vm.ui
    val ctx = LocalContext.current

    val court = ui.activeCourt

    LaunchedEffect(Unit){
        //vm.startLocationTracking()
        vm.getGeoLocation(court)
    }


    Scaffold(topBar = { TopBar(title = "Court Details") }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            /** IMAGE **/
            AsyncImage(
                model = court.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .weight(0.3f)
                    .padding(4.dp)
                    .fillMaxWidth(),
            )
            Column(modifier = Modifier.weight(0.6f)) {
                /** Name **/
                Text(
                    text = court.name,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    textAlign = TextAlign.Center
                )
                /** Description **/
                Text(text = court.description, modifier = Modifier.padding(6.dp))

                /** Location Info **/
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column() {
                        Text(text = "Location: ${court.latitude}, ${court.longitude}")
                        ui.geoInfo?.let {
                            Text(text = "${it.streetName} ${it.streetNumber}, ${it.postalCode}")
                            Text(text = "${it.locality}, ${it.adminArea}, ${it.countryName}")
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier
                            .padding(vertical = 4.dp)
                            .offset(y = 4.dp)
                            .clickable { vm.showOnMap(ctx, ui.activeCourt) })
                        Text(text = "See on map", style = MaterialTheme.typography.caption)
                    }
                }
                Text(text = "")
                /** Open/Closed **/
                Text(text = "Open/Closed court: ${if (court.isClosedCourt) "Closed" else "Open"}")
                /** Number of baskets **/
                Text(text = "Number of baskets: ${court.numberOfBaskets}")
                /** Surface type **/
                Text(text = "Surface: ${court.terrain}")
                /** Free/Paid **/
                Text(text = "Free/Paid: ${if (court.isPaid) "Paid" else "Free"}")
            }
        }
    }
}
