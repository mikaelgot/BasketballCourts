package com.klintsoft.basketballcourts

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(permissionState: PermissionState){
    Log.i("MyInfo", "Permission handler for ${permissionState.permission}")
    val context = LocalContext.current
    val granted = permissionState.status.isGranted
    val shouldShowRationale = permissionState.status.shouldShowRationale
    Log.i("MyInfo", "Permission for ${permissionState.permission} granted?: $granted")
    //Log.i("MyInfo", "Should show rationale?: $shouldShowRationale")
    if (granted) {
        //do nothing
    } else if (shouldShowRationale) { // permission not granted and shouldShowRationale = true
        Button(onClick = {
            permissionState.launchPermissionRequest()
        },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary),
            modifier = Modifier.padding(8.dp))
        {
            Text(text = "Request permission")
        }
    } else {    //permission not granted and shouldShowRationale = false
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
        Button(onClick = {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextCompat.startActivity(context, intent, null)
        },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary),
            modifier = Modifier.padding(8.dp))
        {
            Text(text = "Open app settings")
        }
    }
}