package com.tugulu.scanner

import android.app.Application
import com.tugulu.scanner.data.ApiClient
import com.tugulu.scanner.data.SessionStore

class TuguluApp : Application() {
    lateinit var session: SessionStore
        private set
    lateinit var api: ApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        session = SessionStore(this)
        api = ApiClient(session)
    }

    companion object {
        lateinit var instance: TuguluApp
            private set
    }
}
