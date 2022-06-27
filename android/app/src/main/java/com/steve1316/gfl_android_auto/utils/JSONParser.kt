package com.steve1316.gfl_android_auto.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.steve1316.gfl_android_auto.MainActivity.loggerTag
import com.steve1316.gfl_android_auto.data.SetupData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class JSONParser {
	/**
	 * Initialize settings from the JSON file.
	 *
	 * @param myContext The application context.
	 */
	fun initializeSettings(myContext: Context) {
		Log.d(loggerTag, "Loading settings from JSON file to SharedPreferences...")

		// Grab the JSON object from the file.
		val jString = File(myContext.getExternalFilesDir(null), "settings.json").bufferedReader().use { it.readText() }
		val jObj = JSONObject(jString)

		//////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////

		// Here you can parse out each property from the JSONObject via key iteration. You can create a static class
		// elsewhere to hold the JSON data. Or you can save them all into SharedPreferences.

		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)

		try {
			val discordObj = jObj.getJSONObject("discord")

			sharedPreferences.edit {
				discordObj.keys().forEach { key ->
					when (key) {
						"enableDiscordNotifications" -> {
							putBoolean(key, discordObj[key] as Boolean)
						}
						else -> {
							putString(key, discordObj[key] as String)
						}
					}
				}

				commit()
			}
		} catch (e: Exception) {
		}

		try {
			val gflObj = jObj.getJSONObject("gfl")
			sharedPreferences.edit {
				putString("map", gflObj.getString("map"))
				putInt("amount", gflObj.getInt("amount"))
				putString("dummyEchelons", toStringArrayList(gflObj.getJSONArray("dummyEchelons")).joinToString("|"))
				putString("dpsEchelons", toStringArrayList(gflObj.getJSONArray("dpsEchelons")).joinToString("|"))
				commit()
			}

			// Load the map data.
			loadMap(gflObj.getString("map"), myContext)
		} catch (e: Exception) {
		}

		try {
			val androidObj = jObj.getJSONObject("android")
			sharedPreferences.edit {
				putBoolean("enableDelayTap", androidObj.getBoolean("enableDelayTap"))
				putInt("delayTapMilliseconds", androidObj.getInt("delayTapMilliseconds"))
				putFloat("confidence", androidObj.getDouble("confidence").toFloat())
				putFloat("confidenceAll", androidObj.getDouble("confidenceAll").toFloat())
				putFloat("customScale", androidObj.getDouble("customScale").toFloat())
				putBoolean("enableTestForHomeScreen", androidObj.getBoolean("enableTestForHomeScreen"))
				commit()
			}
		} catch (e: Exception) {
		}
	}

	/**
	 * Convert JSONArray to ArrayList<String> object.
	 *
	 * @param jsonArray The JSONArray object to be converted.
	 * @return The converted ArrayList<String> object.
	 */
	private fun toStringArrayList(jsonArray: JSONArray): ArrayList<String> {
		val newArrayList: ArrayList<String> = arrayListOf()

		var i = 0
		while (i < jsonArray.length()) {
			newArrayList.add(jsonArray.get(i) as String)
			i++
		}

		return newArrayList
	}

	/**
	 * Loads the map data which includes the initial setup steps and subsequent Planning Mode moves.
	 *
	 * @param mapName Name of the map to run.
	 * @param myContext The application context.
	 */
	private fun loadMap(mapName: String, myContext: Context) {
		val mapString = myContext.assets?.open("maps/$mapName.json")?.bufferedReader().use { it?.readText() } ?: throw Exception("Could not load map data for $mapName.")
		val mapObj = JSONObject(mapString)

		try {
			val initObj = mapObj.getJSONObject("init")
			SetupData.setupSteps.clear()
			initObj.keys().forEach { key ->
				val jsonObj = initObj.get(key) as JSONObject
				@Suppress("UNCHECKED_CAST")
				SetupData.setupSteps.add(SetupData.Companion.Init(jsonObj.getString("action"), jsonObj.get("spacing") as ArrayList<Int>, jsonObj.get("coordinates") as ArrayList<Int>))
			}
		} catch (e: Exception) {
		}
	}
}