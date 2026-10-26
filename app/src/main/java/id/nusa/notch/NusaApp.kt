package id.nusa.notch

import android.app.Application

class NusaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }
}
