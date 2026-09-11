package com.github.ilife798

expect class PersistentStorage {
    fun saveString(key: String, value: String)
    fun getString(key: String): String?
    fun saveBoolean(key: String, value: Boolean)
    fun getBoolean(key: String): Boolean
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
}

object StorageKeys {
    const val PHONE = "account_phone"
    const val IS_LOGGED_IN = "account_is_logged_in"
    const val POINTS_LOGIN_DONE = "account_points_login_done"
    const val UID = "account_uid"
    const val EID = "account_eid"
    const val TOKEN = "account_token"
    const val APP_TOKEN = "account_app_token"
    const val DYNAMIC_COLOR = "setting_dynamic_color"
    const val FLOATING_NAV = "setting_floating_nav"
    const val APP_BLUR = "setting_app_blur"
    const val PREDICTIVE_BACK = "setting_predictive_back"
    const val THEME_MODE = "setting_theme_mode"
    const val HOME_DEVICE_TYPE = "home_device_type"
}

object AppStorage {
    lateinit var instance: PersistentStorage
}
