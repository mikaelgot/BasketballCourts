package com.klintsoft.basketballcourts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.net.UnknownHostException

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
            .baseUrl("https://michaelgotsopoulos.com/basketcourts/")
            .build()
        restInterface = retrofit.create(BasketCourtsApiService::class.java)
        getBasketCourts()

        Log.i("MyInfo", "init block ended")
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
    fun getGeoLocation(){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firstAddress = ui.newCourt?.let {
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

    /**------------------------------- UPDATING NEW COURT --------------------------------------**/
    fun resetNewCourt(){
        val court = BasketCourt(latitude = ui.currentLocation?.latitude?.toString() ?: "0.0", longitude = ui.currentLocation?.longitude?.toString() ?: "0.0")
        _ui.value = ui.copy(courtImage = null, imageUri = Uri.EMPTY, newCourt = court)
    }
    fun updateName(name: String){
        val newCourt = ui.newCourt.copy(name = name)
        _ui.value = ui.copy(newCourt = newCourt)
    }
    fun updateLatitude(latitude: String){
        val newCourt =  ui.newCourt.copy(latitude = latitude)
        getGeoLocation()
        _ui.value = ui.copy(newCourt = newCourt)
    }
    fun updateLongitude(longitude: String){
        val newCourt =  ui.newCourt.copy(longitude = longitude)
        getGeoLocation()
        _ui.value = ui.copy(newCourt = newCourt)
    }
    fun updateOpenClosed(isOpen: Boolean){
        val newCourt = ui.newCourt.copy(isClosedCourt = isOpen)
        _ui.value = ui.copy(newCourt = newCourt)
    }
    fun getCurrentLocation(){
        ui.currentLocation?.let {
            val newCourt =  ui.newCourt.copy(latitude = it.latitude.toString(), longitude = it.longitude.toString())
            getGeoLocation()
            _ui.value = ui.copy(newCourt = newCourt)
        }
    }
    fun updateNumberOfBaskets(add: Int){
        val numberOfBaskets = ui.newCourt.numberOfBaskets + add
        val newCourt = ui.newCourt.copy(numberOfBaskets = if (numberOfBaskets in 1..19) numberOfBaskets else 0 )
        _ui.value = ui.copy(newCourt = newCourt)
    }
    fun updateTerrain(material: String){
        val newCourt = ui.newCourt.copy(terrain = material)
        _ui.value = ui.copy(newCourt = newCourt)
    }
    fun updateFreePaid(isPaid: Boolean){
        val newCourt = ui.newCourt.copy(isPaid = isPaid)
        _ui.value = ui.copy(newCourt = newCourt)
    }
    fun saveCourt(){
        val courtList = ui.setOfCourts.toMutableSet()
        courtList.add(ui.newCourt)
        _ui.value = ui.copy(setOfCourts = courtList)
    }
    fun updateImageUri(uri: Uri){
        _ui.value = ui.copy(imageUri = uri)
    }
    fun updateCourtImage(ctx: Context){
        viewModelScope.launch(Dispatchers.IO) {
            val uploadedBitmap = when (ui.imageUri) {
                null -> null
                else -> ctx.contentResolver.openInputStream(ui.imageUri)
                    .use { data -> BitmapFactory.decodeStream(data) }
            }
            _ui.value = ui.copy(courtImage = uploadedBitmap)
            Log.i("MyInfo", "updateCourtImage: image bytecount: ${uploadedBitmap?.byteCount}, uri: ${ui.imageUri}")
        }
    }
    fun createTempPictureUri(ctx: Context) {
        val provider = "${BuildConfig.APPLICATION_ID}.provider"
        val fileName = "basketCourt"
        val fileExtension: String = ".jpg"
        val tempFile = File.createTempFile(fileName, fileExtension, ctx.cacheDir).apply { createNewFile() }
        val uri =  FileProvider.getUriForFile(ctx, provider, tempFile)
        _ui.value = ui.copy(imageUri = uri)
        Log.i("MyInfo", "createTempPictureUri: Uri: $uri")
    }
    fun createPictureUri(ctx: Context) {
        val folder: File = File(ctx.getExternalFilesDir(null), "images")
        val provider = "${BuildConfig.APPLICATION_ID}.provider"
        val filename = "basketCourt.png"
        if(!folder.exists()) folder.mkdir()
        val file = File(folder, filename)
        if (file.exists()) file.delete()
        val uri = FileProvider.getUriForFile(ctx, provider, file)
        _ui.value = ui.copy(imageUri = uri)
        Log.i("MyInfo", "createPictureUri: uri: $uri")
    }
    fun isSaveEnabled(): Boolean{
        val enableSaveButton = with(ui.newCourt){
            name.isNotEmpty() && latitude.toDouble() in 60.1..60.35 && longitude.toDouble() in 24.6..25.15 && numberOfBaskets > 0
        }
        _ui.value = ui.copy(isSaveEnabled = enableSaveButton)
        return enableSaveButton
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
            val basketCourts = getRemoteCourts()
            Log.i("MyInfo", "getBasketCourts: basketCourts: $basketCourts ")
            _ui.value = ui.copy(setOfCourts = basketCourts.toSet())
        }
    }
    private suspend fun getRemoteCourts(): List<BasketCourt>{
        return withContext(Dispatchers.IO){
            try {
                val basketCourts = restInterface.getBasketCourts()
                Log.i("MyInfo", "getRemoteCourts: Successfully downloaded json")
                return@withContext basketCourts
            }catch (e: Exception){
                Log.i("MyInfo", "getRemoteCourts: Error downloading")
                throw e
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