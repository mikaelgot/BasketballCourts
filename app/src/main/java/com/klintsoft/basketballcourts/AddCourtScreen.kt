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
fun AddCourtScreen(navController: NavController, vm: BasketCourtsViewModel) {
    Log.i("MyInfo", "Add court composed")
    val ui = vm.ui
    val ctx = LocalContext.current

    var tempUri by remember { mutableStateOf(value = Uri.EMPTY) }


    val court = ui.activeCourt
    val isEditMode = court.id != null

    Log.i("MyInfo", "AddCourtScreen composed:, activeCourt: $court")
    Log.i("MyInfo", "AddCourtScreen composed:, activeImageUri: ${ui.imageUri}")

    LaunchedEffect(Unit){
        vm.startLocationTracking()
    }

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    Scaffold(
        topBar = { TopBar(title = if(isEditMode) "Edit court" else "Add new court") },
        bottomBar = { BottomEditBar(
            isSaveEnabled = vm.isSaveEnabled(),
            saveCourt = vm::saveCourt,
            uploadImageFromUri = vm::uploadImageFromUri,
            upDateShowDeleteAlert = vm::upDateShowDeleteAlert,
            uploadCourtDataAndImage = vm::uploadCourtAndImage,
            goToMainScreen = { navController.navigate(Screens.BasketCourtsScreen.route){popUpTo(Screens.BasketCourtsScreen.route)} }
        ) }
    ) { contentPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth()
        ) {
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
                    }
                }

                /** Image grabber using camera , Camera permission required, need to ask for it **/
                val cameraLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.TakePicture()
                ) {success ->
                    if (success) {
                        vm.updateImageUri(tempUri)
                    }
                    Log.i("MyInfo", "Image Capture?: $success, imageUri: ${ui.imageUri}")
                }

                val cameraPermissionState = rememberPermissionState(
                    permission = Manifest.permission.CAMERA,
                    onPermissionResult = { granted ->
                        if (granted) {
                            tempUri = vm.createTempPictureUri(ctx)
                            cameraLauncher.launch(tempUri)
                        }
                        else print("camera permission is denied")
                    }
                )

                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)){

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxWidth()
                            .padding(10.dp)
                            .then(
                                if (!isEditMode) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colors.onBackground
                                ) else Modifier
                            )
                    ) {
                        /** IMAGE **/
                            AsyncImage(
                                model = if (ui.imageUri == Uri.EMPTY) ui.activeCourt.imageUrl else ui.imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth(),
                                contentScale = ContentScale.Fit
                            )
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
                                    //vm.createPictureUri(ctx)
                                    cameraPermissionState.launchPermissionRequest()
                                }) {
                                Icon(
                                    painterResource(R.drawable.ic_outline_photo_camera_24),
                                    contentDescription = null)
                            }
                        }
                    }

                    /** ID **/
                    Text(text = "ID: ${court.id}")

                    /** NAME **/
                    OutlinedTextField(
                        value = court.name,
                        singleLine = true,
                        onValueChange = { vm.updateName(it) },
                        label = { Text(text = "Name")}
                    )

                    /** LOCATION **/
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            value = court.latitude,
                            onValueChange = { vm.updateLatitude(it) },
                            label = { Text(text = "Latitude")},
                            trailingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.clickable { vm.updateLatitude("") })}
                        )
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(end = 8.dp),
                            singleLine = true,
                            value = court.longitude,
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

                    /** OPEN/CLOSED **/
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Open court")
                        Switch(checked = court.isClosedCourt, onCheckedChange = { vm.updateOpenClosed(it) })
                        Text(text = "Closed court")
                    }
                    /** NUMBER OF BASKETS **/
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Number of baskets:", modifier = Modifier.padding(end = 10.dp))
                        Icon(painterResource(R.drawable.ic_baseline_remove_24), contentDescription = null, modifier = Modifier.clickable { vm.updateNumberOfBaskets(-1) })
                        Text(text = court.numberOfBaskets.toString(),
                            modifier = Modifier
                                .padding(6.dp)
                                .border(1.dp, MaterialTheme.colors.onBackground)
                                .width(50.dp)
                                .padding(6.dp),
                            textAlign = TextAlign.Center
                        )
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.clickable { vm.updateNumberOfBaskets(1) })
                    }
                    /** SURFACE TYPE **/
                    Column() {
                        Text(text = "Surface material:")
                        LazyRow(verticalAlignment = Alignment.CenterVertically) {
                            items(terrainTypes){type ->
                                Text(text = type,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(shape = RoundedCornerShape(50))
                                        .background(color = if (type == court.terrain) MaterialTheme.colors.secondary else MaterialTheme.colors.background)
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
                    /** FREE/PAID **/
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Free")
                        Switch(checked = court.isPaid, onCheckedChange = { vm.updateFreePaid(it) })
                        Text(text = "Paid")
                    }
                }
            }
            if (ui.showDeleteAlert) DeleteAlert(
                showAlert = vm::upDateShowDeleteAlert,
                deleteCourt = { vm.deleteBasketCourt(id = court.id.toString()) },
                goToMainScreen = { navController.navigate(Screens.BasketCourtsScreen.route) {popUpTo(Screens.BasketCourtsScreen.route)} }
            )
        }
    }
}

@Composable
fun DeleteAlert(
    showAlert: (Boolean) -> Unit,
    deleteCourt: () -> Unit,
    goToMainScreen: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { showAlert(false) },
        buttons = {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = {
                    deleteCourt()
                    showAlert(false)
                    goToMainScreen()
                    }
                ) {
                    Text(text = "Delete")
                }
                Button(onClick = {
                    showAlert(false)
                }
                ) {
                    Text(text = "Cancel")
                }
            }
        },
        text = { Text(text = "Are you sure you want to delete this court?") }
    )
}

@Composable
fun BottomEditBar(
    isSaveEnabled: Boolean,
    saveCourt: (Context) -> Unit,
    goToMainScreen: () -> Unit,
    uploadImageFromUri: (Context) -> Unit,
    uploadCourtDataAndImage: (Context) -> Unit,
    upDateShowDeleteAlert: (Boolean) -> Unit
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
            upDateShowDeleteAlert(true)
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_remove_24), contentDescription = "Delete")
                Text(text = "Delete", style = MaterialTheme.typography.caption)
            }
        }
        IconButton(
            enabled = isSaveEnabled,
            onClick = {
            saveCourt(ctx)
            goToMainScreen()
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_save_24), contentDescription = "Save")
                Text(text = "Save", style = MaterialTheme.typography.caption)
            }
        }
        IconButton(onClick = {
            uploadImageFromUri(ctx)
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_file_upload_24), contentDescription = "Upload image")
                Text(text = "Upload image", style = MaterialTheme.typography.caption)
            }
        }
        IconButton(onClick = {
            uploadCourtDataAndImage(ctx)
        }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_baseline_file_upload_24), contentDescription = "Upload Court Data and Image")
                Text(text = "Upload All", style = MaterialTheme.typography.caption)
            }
        }
    }
}

val terrainTypes = listOf("Wood", "Asphalt", "Concrete", "PVC", "Vinyl", "Sport tiles")
