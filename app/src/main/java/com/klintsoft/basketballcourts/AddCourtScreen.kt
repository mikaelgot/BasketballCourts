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
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import java.io.File


@OptIn(ExperimentalPermissionsApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun AddCourtScreen(navController: NavController, vm: BasketCourtsViewModel) {
    Log.i("MyInfo", "Add court composed")
    val ui = vm.ui
    val ctx = LocalContext.current

    LaunchedEffect(Unit){
        vm.startLocationTracking()
    }

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    Scaffold(topBar = { TopBar() }) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            if (!permissionState.status.isGranted) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "The app needs to know your precise location")
                    PermissionHandler(permissionState)
                }
            } else { // Fine Location permission is granted
                //Image or video picker launcher, can use PickMultipleVisualMedia for multiple file picking
                val pickVisualMediaLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()) {
                    if (it != null) {
                        vm.updateImageUri(it)
                        vm.updateCourtImage(ctx)
                    }
                }

                /** Image grabber using camera , Camera permission required, need to ask for it **/
                val cameraLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.TakePicture()
                ) {success ->
                    if (success) { vm.updateCourtImage(ctx) }
                    Log.i("MyInfo", "Image Capture?: $success, imageUri: ${ui.imageUri}")
                }

                val cameraPermissionState = rememberPermissionState(
                    permission = Manifest.permission.CAMERA,
                    onPermissionResult = { granted ->
                        if (granted) {
                            cameraLauncher.launch(ui.imageUri)
                        }
                        else print("camera permission is denied")
                    }
                )

                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)){
                    //Text(text = "${ui.currentLocation}")
                    //val boxModifier = if(ui.imageUri == Uri.EMPTY) Modifier

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxWidth()
                            .padding(10.dp)
                            .then(
                                if (ui.imageUri == Uri.EMPTY) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colors.onBackground
                                ) else Modifier
                            )
                    ) {
                        ui.courtImage?.let {
                            /*Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2))
                            )
                            Text(text = "Image width: ${ui.courtImage.width}, height: ${ui.courtImage.height}")*/
                            GlideImage(
                                model = ui.imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(shape = CircleShape)
                                    .background(color = MaterialTheme.colors.background.copy(alpha = 0.7f)),
                                onClick = { pickVisualMediaLauncher.launch(
                                PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )}) {
                                Icon(
                                    painterResource(R.drawable.ic_outline_upload_24),
                                    contentDescription = null)
                            }
                            IconButton(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(shape = CircleShape)
                                    .background(color = MaterialTheme.colors.background.copy(alpha = 0.7f)),
                                onClick = {
                                    /**Take photo**/
                                    //vm.createTempPictureUri(ctx)
                                    vm.createPictureUri(ctx)
                                    cameraPermissionState.launchPermissionRequest()
                                }) {
                                Icon(
                                    painterResource(R.drawable.ic_outline_photo_camera_24),
                                    contentDescription = null)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ui.newCourt.name,
                        singleLine = true,
                        onValueChange = { vm.updateName(it) },
                        label = { Text(text = "Name")}
                    )

                    /** Location Info **/
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            value = ui.newCourt.latitude,
                            onValueChange = { vm.updateLatitude(it) },
                            label = { Text(text = "Latitude")},
                            trailingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.clickable { vm.updateLatitude("") })}
                        )
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            value = ui.newCourt.longitude,
                            onValueChange = { vm.updateLongitude(it) },
                            label = { Text(text = "Longitude")},
                            trailingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.clickable { vm.updateLongitude("") })}
                        )
                        Icon(painterResource(R.drawable.ic_baseline_my_location_24), contentDescription = null, modifier = Modifier
                            .offset(y = 4.dp)
                            .clickable { vm.getCurrentLocation() })
                    }
                    ui.geoInfo?.let {
                        Text(text = "${it.streetName} ${it.streetNumber}, ${it.postalCode}, ${it.locality}, ${it.adminArea}, ${it.countryName}")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Closed court")
                        Switch(checked = ui.newCourt.isClosedCourt, onCheckedChange = { vm.updateOpenClosed(it) })
                        Text(text = "Open court")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Number of baskets:", modifier = Modifier.padding(end = 10.dp))
                        Icon(painterResource(R.drawable.ic_baseline_remove_24), contentDescription = null, modifier = Modifier.clickable { vm.updateNumberOfBaskets(-1) })
                        Text(text = ui.newCourt.numberOfBaskets.toString(),
                            modifier = Modifier
                                .padding(6.dp)
                                .border(1.dp, MaterialTheme.colors.onBackground)
                                .width(50.dp)
                                .padding(6.dp),
                            textAlign = TextAlign.Center
                        )
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.clickable { vm.updateNumberOfBaskets(1) })
                    }
                    Column() {
                        Text(text = "Terrain type:")
                        LazyRow(verticalAlignment = Alignment.CenterVertically) {
                            items(terrainTypes){type ->
                                Text(text = type,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(shape = RoundedCornerShape(50))
                                        .background(color = if (type == ui.newCourt.terrain) MaterialTheme.colors.secondary else MaterialTheme.colors.background)
                                        .clickable { vm.updateTerrain(type) }
                                        .border(
                                            1.dp,
                                            MaterialTheme.colors.onBackground,
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Free")
                        Switch(checked = ui.newCourt.isPaid, onCheckedChange = { vm.updateFreePaid(it) })
                        Text(text = "Paid")
                    }
                    Button(
                        enabled = vm.isSaveEnabled(),
                        modifier = Modifier
                            .padding(horizontal = 25.dp)
                            .fillMaxWidth(),
                        onClick = {
                            vm.saveCourt()
                            vm.stopLocationTracking()
                            navController.navigate(Screens.BasketCourtsScreen.route) {popUpTo(Screens.BasketCourtsScreen.route)}
                    }) {
                        Text(text = "SAVE COURT")
                    }
                }

            }
        }
    }
}

val terrainTypes = listOf("Wood", "Asphalt", "Concrete", "PVC", "Vinyl", "Sport tiles")
