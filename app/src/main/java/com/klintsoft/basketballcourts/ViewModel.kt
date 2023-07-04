package com.klintsoft.basketballcourts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class BasketCourtsViewModel(private val stateHandle: SavedStateHandle): ViewModel() {
    //restInterface variable for Retrofit
    private var restInterface: BasketCourtsApiService
    private lateinit var locationManager: LocationManager
    private lateinit var geocoder: Geocoder

    //error handler for all coroutines automatically, so no need to put them in try/catch blocks
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        //_ui.value = ui.copy(coroutineError = true)
        exception.printStackTrace()
    }

    private val _ui = mutableStateOf(UiState())
    val ui get() = _ui.value

    init {
        Log.i("MyInfo", "init block started")

        //Instantiate Retrofit builder object
        val retrofit: Retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            //.baseUrl("https://michaelgotsopoulos.com/basketcourts/")
            .baseUrl(baseUrl)
            .build()
        restInterface = retrofit.create(BasketCourtsApiService::class.java)
        getBasketCourts()
        //getRandomCourt()

        initLocationManager(BasketCourtsApplication.getAppContext())

        Log.i("MyInfo", "init block ended")
    }
    /**-------------------------- Court details -------------------------------------------------**/
    fun updateActiveCourt(id: Int){
        val activeCourt = ui.setOfCourts.firstOrNull { it.id == id } ?: BasketCourt()
        _ui.value = ui.copy(activeCourt = activeCourt)
    }

    /**--------------------------- LOCATION -----------------------------------------------------**/

    fun initLocationManager(ctx: Context){
        val timeInterval = 3000L
        locationManager = LocationManager(ctx, timeInterval, 0f, refresh = { refreshLocation(it) })
        geocoder = Geocoder(ctx)
    }
    fun startLocationTracking(){
        locationManager.startLocationTracking()
    }
    fun stopLocationTracking(){
        locationManager.stopLocationTracking()
    }
    fun refreshLocation(location: Location) {
        _ui.value = ui.copy(currentLocation = location)
    }
    fun getGeoLocation(court: BasketCourt = ui.activeCourt){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firstAddress = court.let {
                    geocoder.getFromLocation(it.latitude.toDouble(), it.longitude.toDouble(), 1)
                }
                if (firstAddress != null && firstAddress.isNotEmpty()) {
                    with(firstAddress.first()) {
                        val geoInfo = GeoLocationInfo(
                            countryName = countryName,
                            postalCode = postalCode,
                            locality = locality,
                            streetName = thoroughfare,
                            streetNumber = subThoroughfare,
                            adminArea = adminArea
                        )
                        withContext(Dispatchers.Main) {
                            _ui.value = ui.copy(geoInfo = geoInfo)
                        }
                    }
                }
            }
            catch (e: Exception){
                Log.i("MyInfo", "Can't determine GeoLocation")
                _ui.value = ui.copy(geoInfo = null)
            }
        }
    }

    fun showOnMap(context: Context, court: BasketCourt){
        //Show location on Google Maps using intent
        //val packageManager = ctx.packageManager
        //val context = BasketCourtsApplication.getAppContext()
        val uri = Uri.parse("geo:0,0?q=" + court.latitude + "," + court.longitude)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        //To verify that an app is available to receive the intent, call resolveActivity() on your Intent object.
        //otherwise it may crash if there's no app for this intent
        //intent.resolveActivity(packageManager)?.let{ ctx.startActivity(intent) }
        context.startActivity(intent)
    }

    /**------------------------------- UPDATING NEW COURT --------------------------------------**/
    fun resetNewCourt(){
        val newCourt = BasketCourt(latitude = ui.currentLocation?.latitude?.toString() ?: "0.0", longitude = ui.currentLocation?.longitude?.toString() ?: "0.0")
        _ui.value = ui.copy(/*courtImage = null,*/ imageUri = Uri.EMPTY, activeCourt = newCourt)
    }
    fun updateName(name: String){
        val activeCourt = ui.activeCourt.copy(name = name)
        _ui.value = ui.copy(activeCourt = activeCourt)
    }
    fun updateLatitude(latitude: String){
        val activeCourt =  ui.activeCourt.copy(latitude = latitude)
        getGeoLocation()
        _ui.value = ui.copy(activeCourt = activeCourt)
    }
    fun updateLongitude(longitude: String){
        val activeCourt =  ui.activeCourt.copy(longitude = longitude)
        getGeoLocation()
        _ui.value = ui.copy(activeCourt = activeCourt)
    }
    fun updateOpenClosed(isClosed: Boolean){
        val activeCourt = ui.activeCourt.copy(isClosedCourt = isClosed)
        _ui.value = ui.copy(activeCourt = activeCourt)
    }
    fun getCurrentLocation(){
        ui.currentLocation?.let {
            val activeCourt =  ui.activeCourt.copy(latitude = it.latitude.toString(), longitude = it.longitude.toString())
            getGeoLocation()
            _ui.value = ui.copy(activeCourt = activeCourt)
        }
    }
    fun updateNumberOfBaskets(add: Int){
        val numberOfBaskets = ui.activeCourt.numberOfBaskets + add
        val activeCourt = ui.activeCourt.copy(numberOfBaskets = if (numberOfBaskets in 1..19) numberOfBaskets else 0 )
        _ui.value = ui.copy(activeCourt = activeCourt)
    }
    fun updateTerrain(material: String){
        val activeCourt = ui.activeCourt.copy(terrain = material)
        _ui.value = ui.copy(activeCourt = activeCourt)
    }
    fun updateFreePaid(isPaid: Boolean){
        val activeCourt = ui.activeCourt.copy(isPaid = isPaid)
        _ui.value = ui.copy(activeCourt = activeCourt)
    }
    fun saveCourt(ctx: Context){
        Log.i("MyInfo", "saveCourt: Invoked")
        if(ui.imageUri == Uri.EMPTY) {
            /** UPLOAD ONLY COURT DATA - In edit mode when image is not changed - JSON POST **/
            uploadBasketCourt(ui.activeCourt)
        }
        else {
            /** UPLOAD COURT DATA & IMAGE - New court or edit where image has changed - MULTIPART POST**/
            uploadCourtAndImage(ctx)
        }
    }
    fun updateImageUri(uri: Uri){
        _ui.value = ui.copy(imageUri = uri)
    }
    /*fun updateCourtImage(ctx: Context){
        viewModelScope.launch(Dispatchers.IO) {
            val uploadedBitmap = when (ui.imageUri) {
                null -> null
                else -> ctx.contentResolver.openInputStream(ui.imageUri)
                    .use { data -> BitmapFactory.decodeStream(data) }
            }
            _ui.value = ui.copy(courtImage = uploadedBitmap)
            Log.i("MyInfo", "updateCourtImage: image bytecount: ${uploadedBitmap?.byteCount}, uri: ${ui.imageUri}")
        }
    }*/
    fun createTempPictureUri(ctx: Context): Uri {
        val provider = "${BuildConfig.APPLICATION_ID}.provider"
        val fileName = "picture_${System.currentTimeMillis()}"
        val fileExtension: String = ".jpg"
        val tempFile = File.createTempFile(fileName, fileExtension, ctx.cacheDir).apply { createNewFile() }
        val uri = FileProvider.getUriForFile(ctx, provider, tempFile)
        //_ui.value = ui.copy(imageUri = uri)
        Log.i("MyInfo", "createTempPictureUri: Uri: $uri")
        return uri
    }
    fun createPictureUri(ctx: Context) {
        val folder: File = File(ctx.getExternalFilesDir(null), "images")
        val provider = "${BuildConfig.APPLICATION_ID}.provider"
        val filename = "basketCourt.jpg"
        if(!folder.exists()) folder.mkdir()
        val file = File(folder, filename)
        if (file.exists()) file.delete()
        val uri = FileProvider.getUriForFile(ctx, provider, file)
        _ui.value = ui.copy(imageUri = uri)
        Log.i("MyInfo", "createPictureUri: uri: $uri")
    }
    fun isSaveEnabled(): Boolean{
        val enableSaveButton = with(ui.activeCourt){
            name.isNotEmpty() && latitude.toDouble() in 60.1..60.35 && longitude.toDouble() in 24.6..25.15 && numberOfBaskets > 0 && terrain.isNotEmpty()
        }
        _ui.value = ui.copy(isSaveEnabled = enableSaveButton)
        return enableSaveButton
    }
    fun upDateShowDeleteAlert(show: Boolean){
        _ui.value = ui.copy(showDeleteAlert = show)
    }

    /**-------------------------- JSON & Web ----------------------------------------------------**/
    fun saveJson(ctx: Context): Boolean {
        val folder = File(ctx.getExternalFilesDir(null), "JSON")
        val filename = "basketcourts.json"
        val destination = File(folder, filename)
        if (!folder.exists()) {
            try {
                folder.mkdirs()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.i("MyInfo", "Error creating folder")
                return false
            }
        }
        try {
            Log.i("MyInfo", "Saving courts file to $destination")
            val jsonObject = Json.encodeToString(ui.setOfCourts)
            Log.i("MyInfo", "Json object created")
            destination.writeText(jsonObject)
            Log.i("MyInfo", "Json written to file")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("MyInfo", "Error writing to file")
        }
        return false
    }
    fun getBasketCourts() {
        Log.i("MyInfo", "getBasketCourts invoked")
        //Don't forget Internet permission in manifest!!!!!
        viewModelScope.launch(errorHandler) {
            val basketCourts = getAllBasketCourts()
            Log.i("MyInfo", "getBasketCourts: basketCourts: $basketCourts ")
            _ui.value = ui.copy(setOfCourts = basketCourts.toSet())
        }
    }
    private suspend fun getAllBasketCourts(): List<BasketCourt>{
        return withContext(Dispatchers.IO){
            try {
                val basketCourts = restInterface.getAllBasketCourts()
                Log.i("MyInfo", "getRemoteCourts: Successfully downloaded json")
                return@withContext basketCourts
            }catch (e: Exception){
                Log.i("MyInfo", "getRemoteCourts: Error downloading")
                throw e
            }
        }
    }
    /*private fun getRandomCourt(){
        Log.i("MyInfo", "getRandomCourt: Invoked")
        viewModelScope.launch() {
            val randomCourt = withContext(Dispatchers.IO) {
                try {
                    val basketCourts = restInterface.getRandomBasketCourt()
                    Log.i("MyInfo", "getRandomCourt: Successfully downloaded court: $basketCourts")
                    basketCourts
                } catch (e: Exception) {
                    Log.i("MyInfo", "getRandomCourt: Error downloading")
                    throw e
                }
            }
            _ui.value = ui.copy(oneBasketCourt = randomCourt)
        }
    }*/
    fun downloadCourt(id: String){
        Log.i("MyInfo", "getRandomCourt: Invoked")
        viewModelScope.launch() {
            val firstCourt = withContext(Dispatchers.IO) {
                try {
                    val basketCourt = restInterface.getBasketCourtById( id )
                    Log.i("MyInfo", "downloadCourt: Successfully downloaded court: $basketCourt")
                } catch (e: Exception) {
                    Log.i("MyInfo", "downloadCourt: Error downloading")
                    //throw e
                }
            }
            //_ui.value = ui.copy(oneBasketCourt = firstCourt)
        }
    }
    fun uploadImageFromFile(ctx: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i("MyInfo", "uploadImageFromFile: Invoked")
            val folder = File(ctx.getExternalFilesDir(null), "images")
            val file = File(folder, "basketCourt.jpg")

            val fileFromUri = ui.imageUri.path?.let { File(it) }

            Log.i("MyInfo", "uploadFileToApi: file ${file} exists?: ${file.exists()}")
            Log.i("MyInfo", "uploadFileToApi: fileFromUri ${fileFromUri} exists?: ${file.exists()}")
            try {
                restInterface.uploadImage(
                    image = MultipartBody.Part.createFormData(
                        name = "image",
                        filename = file.name,
                        body = file.asRequestBody()
                    )
                )

                Log.i("MyInfo", "uploadFileToApi: Uploaded successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.i("MyInfo", "uploadFileToApi: Error uploading")
            }
        }
    }
    fun uploadImageFromUri(ctx: Context) {
        Log.i("MyInfo", "uploadImageFromUri: Invoked")
        viewModelScope.launch(Dispatchers.IO) {
            if (ui.imageUri != Uri.EMPTY) {
                val file = getRealPathFromURI(ui.imageUri, ctx)
                Log.i("MyInfo", "uploadImageFromUri: file: $file")

                //val requestBody = InputStreamRequestBody(ctx, ui.imageUri)

                if (file != null) {
                    try {
                        restInterface.uploadImage(
                            image = MultipartBody.Part.createFormData(
                                name = "image",
                                filename = file.name,
                                body = file.asRequestBody()
                            )
                        )

                        Log.i("MyInfo", "uploadFileToApi: Uploaded successfully")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.i("MyInfo", "uploadFileToApi: Error uploading")
                    }
                }
            }
        }
    }
    fun uploadCourtAndImage(ctx: Context) {
        Log.i("MyInfo", "uploadCourtAndImage: Invoked")
        viewModelScope.launch(Dispatchers.IO) {
            if (ui.imageUri != Uri.EMPTY) {

                val jsonObject = Json.encodeToString(ui.activeCourt)

                val file = getRealPathFromURI(ui.imageUri, ctx)
                Log.i("MyInfo", "uploadCourtAndImage: file: $file")

                //val requestBody = InputStreamRequestBody(ctx, ui.imageUri)

                if (file != null) {
                    try {
                        restInterface.uploadCourtAndImage(
                            courtData = MultipartBody.Part.createFormData(
                                "court_data",
                                jsonObject//.toRequestBody("application/json".toMediaTypeOrNull())
                            ),
                            image = MultipartBody.Part.createFormData(
                                name = "image",
                                filename = file.name,
                                body = file.asRequestBody()
                            )
                        )

                        Log.i("MyInfo", "uploadCourtAndImage: Uploaded successfully")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.i("MyInfo", "uploadCourtAndImage: Error uploading")
                    }
                }
            }
            else {
                Log.i("MyInfo", "No image chosen for the court")
                //Toast.makeText(ctx, "No image chosen for the court", Toast.LENGTH_SHORT).show()
            }
        }
    }

/*@SuppressLint("Range")
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    val fileName: String?
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.moveToFirst()
    fileName = cursor?.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
    cursor?.close()
    return fileName
}*/

    fun getRealPathFromURI(uri: Uri, context: Context): File? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex =  returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val size = returnCursor.getLong(sizeIndex).toString()
        val file = File(context.filesDir, name)
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable: Int = inputStream?.available() ?: 0
            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream?.read(buffers).also {
                    if (it != null) {
                        read = it
                    }
                } != -1) {
                outputStream.write(buffers, 0, read)
            }
            Log.e("File Size", "Size " + file.length())
            inputStream?.close()
            outputStream.close()
            Log.e("File Path", "Path " + file.path)

        } catch (e: java.lang.Exception) {
            Log.e("Exception", e.message!!)
        }
        return file
    }

    fun uploadBasketCourt(activeCourt: BasketCourt) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i("MyInfo", "uploadBasketCourt: new court to be uploaded $activeCourt")
            val jsonObject = Json.encodeToString(activeCourt)
            // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
            val requestBody = jsonObject.toRequestBody("application/json".toMediaTypeOrNull())
            try {

                restInterface.uploadCourt(requestBody)
                /*restInterface.uploadImage(
                    image = MultipartBody.Part.createFormData(
                        name = "image",
                        filename = file.name,
                        body = file.asRequestBody()
                    )
                )*/
                Log.i("MyInfo", "uploadBasketCourt: Uploaded successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.i("MyInfo", "uploadBasketCourt: Error uploading")
            }
        }
    }

    fun deleteBasketCourt(id: String = "4") {
        Log.i("MyInfo", "deleteBasketCourt: invoked")
        viewModelScope.launch(Dispatchers.IO) {
            // Do the DELETE request and get response
            val response = restInterface.deleteBasketCourt( id )
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    // Convert raw JSON to pretty JSON using GSON library
                    /*val gson = GsonBuilder().setPrettyPrinting().create()
                    val prettyJson = gson.toJson(
                        JsonParser.parseString(
                            response.body()
                                ?.string() // About this thread blocking annotation : https://github.com/square/retrofit/issues/3255
                        )
                    )*/

                    Log.i("MyInfo", "deleteBasketCourt: Deleted successfully")
                    //Log.d("Pretty Printed JSON :", prettyJson)

                } else {
                    Log.i("MyInfo", "deleteBasketCourt: FAILURE")
                    Log.e("RETROFIT_ERROR", response.code().toString())

                }
            }
        }
    }


    /*private suspend fun getAllCourts(): List<BasketCourt>{
        Log.i("MyInfo", "getAllRestaurants invoked")
        return withContext(Dispatchers.IO){
            try {
                val remoteCourts = restInterface.getBasketCourts()
                //We can't add them to the database right away, to update it, because this way all the
                // favorited would turn to false, the only source who knows about the favorites is the local DB
                //So, we first query the favorited restaurants from the database
                val favRestaurants = restaurantsDao.getAllFavorited()
                //Next we add to the DB the list from internet (without favorited)
                restaurantsDao.addAll(remoteRestaurants)
                //Finally we update the DB with the favorited list we saved above mapped as Partial Restaurants
                //We use partial so to not interfere with the rest of data like name, description etc.
                //These might have changed in the meanwhile on the internet, who knows?
                //We only care about what's local and user-dependent (i.e the favorite ones)
                val partialFavRestaurants = favRestaurants.map { PartialRestaurant(id = it.id, isFavorite = it.isFavorite) }
                restaurantsDao.updateAll(partialFavRestaurants)
                Log.i("MyInfo", "getAllRestaurants: Written results to local database ")
            }
            catch (e: Exception){
                when(e) {
                    is UnknownHostException, is UnknownHostException, is UnknownHostException -> {
                        //See if the database is empty and if it is throw exception
                        if(basketCourtsDao.getAll().isEmpty()) throw  Exception("Something went wrong. The database is empty")
                    }
                    else -> throw e
                }
            }
            return@withContext basketCourtsDao.getAll()
        }
    }*/
}