package com.steve1316.gfl_android_auto.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.steve1316.gfl_android_auto.MainActivity.loggerTag
import org.json.JSONArray

class ConfigData(myContext: Context) {
	private val tag = "${loggerTag}ConfigData"

	data class TDollInterface(
		val id: Int,
		val name: String,
		val type: String,
		val rarity: Int
	)

	val debugMode: Boolean

	// GFL
	val tdolls: ArrayList<TDollInterface> = arrayListOf()
	val mapName: String
	val amount: Int
	val dummyEchelons: List<String>
	val dpsEchelons: List<String>
	val enableSetup: Boolean
	val enableRepair: Boolean
	val repairInterval: Int
	val enableCorpseDrag: Boolean
	val corpseDragger1: String
	val corpseDragger2: String

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
		enableSetup = sharedPreferences.getBoolean("enableSetup", false)
		enableRepair = sharedPreferences.getBoolean("enableRepair", true)
		repairInterval = sharedPreferences.getInt("amount", 3)
		enableCorpseDrag = sharedPreferences.getBoolean("enableCorpseDrag", false)
		corpseDragger1 = sharedPreferences.getString("corpseDragger1", "")!!
		corpseDragger2 = sharedPreferences.getString("corpseDragger2", "")!!

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

		// Grab the JSON object from the file.
		Log.d(tag, "Now loading tdolls from the tdolls.json file...")
		val tdollString = myContext.assets.open("data/tdolls.json").bufferedReader().use { it.readText() }
		val tdollArray = JSONArray(tdollString)

		var i = 0
		while (i < tdollArray.length()) {
			val tdollObj = tdollArray.getJSONObject(i)
			if (tdollObj["id"] != "") {
				val tdoll = TDollInterface(tdollObj["id"].toString().toInt(), tdollObj["name"] as String, tdollObj["type"] as String, tdollObj["rarity"].toString().toInt())
				tdolls.add(tdoll)
			}

			i++
		}

		Log.d(tag, "Loaded ${tdolls.size} T-Dolls from the tdolls.json file.")
	}
}