package com.xxy.activitystack

import android.app.Application
import com.xxy.activitystack.data.Settings
import com.xxy.activitystack.data.ActivityTracker
import com.xxy.activitystack.shizuku.ShizukuHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        ActivityTracker.init(this)
        ShizukuHelper.attach()
    }
}
