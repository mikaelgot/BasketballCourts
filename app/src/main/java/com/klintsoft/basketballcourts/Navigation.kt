package com.klintsoft.basketballcourts

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@Composable
fun Navigation(){
    val navController = rememberNavController()
    val vm: BasketCourtsViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screens.BasketCourtsScreen.route){
        composable(route = Screens.BasketCourtsScreen.route){
            BasketCourtsScreen(navController = navController, vm = vm)
        }
        composable(route = Screens.AddCourtScreen.route){
            AddCourtScreen(navController = navController, vm = vm)
        }
    }
}

sealed class Screens(val route: String){
    object BasketCourtsScreen: Screens("basketcourtsscreen")
    object AddCourtScreen: Screens("addcourtscreen")
}