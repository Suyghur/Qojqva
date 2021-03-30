package com.suyghur.qojqva.demo

import android.app.Application
import android.content.Context
import com.suyghur.qojqva.Qojqva

/**
 * @author #Suyghur.
 * Created on 3/30/21
 */
class DemoApplication : Application() {


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        Qojqva.scopedStorage = true
    }
}