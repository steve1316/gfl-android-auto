package com.steve1316.gfl_android_auto.data

class PlanningModeData {
	companion object {
		data class Moves(val action: String, val coordinates: ArrayList<Int>)

		val moves: ArrayList<Moves> = arrayListOf()
	}
}