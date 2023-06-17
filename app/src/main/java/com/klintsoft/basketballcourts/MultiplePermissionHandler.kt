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
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MultiplePermissionHandler(permissionsState: MultiplePermissionsState, minimumGranted: Boolean){
    Log.i("MyInfo", "MultiplePermission handler")
    val context = LocalContext.current
    //val granted = permissionState.status.isGranted
    val shouldShowRationale = permissionsState.permissions.first().status.shouldShowRationale
    Log.i("MyInfo", "Minimum permission granted?: $minimumGranted")
    //Log.i("MyInfo", "Should show rationale?: $shouldShowRationale")
    if (minimumGranted) {
        //do nothing
    } else if (shouldShowRationale) { // permission not granted and shouldShowRationale = true
        Button(onClick = {
            permissionsState.launchMultiplePermissionRequest()
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
            permissionsState.launchMultiplePermissionRequest()
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