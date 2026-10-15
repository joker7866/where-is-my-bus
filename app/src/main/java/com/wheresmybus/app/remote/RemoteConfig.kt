package com.wheresmybus.app.remote

object RemoteConfig {
    /**
     * 10.0.2.2 is the Android emulator's alias for the host machine's localhost,
     * where `backend/` (`npm start`) listens on port 4000 by default.
     * On a physical device, change this to your machine's LAN IP, e.g.
     * "http://192.168.1.23:4000/api".
     */
    const val BASE_URL = "http://10.0.2.2:4000/api"
}
