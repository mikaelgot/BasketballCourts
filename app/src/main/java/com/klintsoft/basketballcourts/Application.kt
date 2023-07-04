package com.klintsoft.basketballcourts

import android.app.Application
import android.content.Context

class BasketCourtsApplication: Application() {
    init { app = this }
    companion object {
        private lateinit var app: BasketCourtsApplication
        fun getAppContext(): Context = app.applicationContext
    }
}