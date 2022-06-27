package com.steve1316.gfl_android_auto.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.steve1316.gfl_android_auto.MainActivity.loggerTag

class ConfigData(myContext: Context) {
	private val tag = "${loggerTag}ConfigData"

	val debugMode: Boolean

	// GFL
	val mapName: String
	val amount: Int
	val dummyEchelons: List<String>
	val dpsEchelons: List<String>

	// Discord
	val enableDiscordNotifications: Boolean
	val discordToken: String
	val discordUserID: String

	// Android
	val enableDelayTap: Boolean
	val delayTapMilliseconds: Int
	val confidence: Double
	val confidenceAll: Double
	val customScale: Double

	init {
		Log.d(tag, "Loading settings from SharedPreferences to memory...")

		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)

		debugMode = sharedPreferences.getBoolean("debugMode", false)

		// GFL settings.
		mapName = sharedPreferences.getString("map", "")!!
		amount = sharedPreferences.getInt("amount", 1)
		dummyEchelons = sharedPreferences.getString("dummyEchelons", "")!!.split("|")
		dpsEchelons = sharedPreferences.getString("dpsEchelons", "")!!.split("|")

		// Token and user ID for use with the Discord API.
		enableDiscordNotifications = sharedPreferences.getBoolean("enableDiscordNotifications", false)
		discordToken = sharedPreferences.getString("discordToken", "")!!
		discordUserID = sharedPreferences.getString("discordUserID", "")!!

		// Android-specific settings.
		enableDelayTap = sharedPreferences.getBoolean("enableDelayTap", false)
		delayTapMilliseconds = sharedPreferences.getInt("delayTapMilliseconds", 1000)
		confidence = sharedPreferences.getFloat("confidence", 0.8f).toDouble() / 100.0
		confidenceAll = sharedPreferences.getFloat("confidenceAll", 0.8f).toDouble() / 100.0
		customScale = sharedPreferences.getFloat("customScale", 1.0f).toDouble()

		Log.d(tag, "Successfully loaded settings from SharedPreferences to memory.")
	}
}