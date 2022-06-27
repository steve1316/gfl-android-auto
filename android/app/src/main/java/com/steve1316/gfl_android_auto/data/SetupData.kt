package com.steve1316.gfl_android_auto.data

class SetupData {
	companion object {
		data class Init(val action: String, val spacing: ArrayList<Int>, val coordinates: ArrayList<Int>)

		val setupSteps = arrayListOf<Init>()
	}
}