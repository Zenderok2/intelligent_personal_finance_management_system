package com.example.project5

import android.app.Application
import com.example.project5.utils.RepositoryProvider

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        RepositoryProvider.init(this)
    }

}