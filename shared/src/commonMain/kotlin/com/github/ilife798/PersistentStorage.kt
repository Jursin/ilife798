package com.github.ilife798

expect class PersistentStorage {
    fun saveString(key: String, value: String)
    fun getString(key: String): String?
    fun saveBoolean(key: String, value: Boolean)
    fun getBoolean(key: String): Boolean
}

object StorageKeys {
    const val PHONE = "account_phone"
    const val IS_LOGGED_IN = "account_is_logged_in"
    const val POINTS_LOGIN_DONE = "account_points_login_done"
    const val UID = "account_uid"
    const val EID = "account_eid"
    const val TOKEN = "account_token"
    const val APP_TOKEN = "account_app_token"
}

object AppStorage {
    lateinit var instance: PersistentStorage
}
