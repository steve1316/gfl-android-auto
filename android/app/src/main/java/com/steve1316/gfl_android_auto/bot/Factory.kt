package com.steve1316.gfl_android_auto.bot

/**
 * This class handles the Factory screen including dismantling excess T-Dolls.
 */
class Factory(val game: Game) {
	private val tag = "[Factory]"

	fun disassemble(): Boolean {
		game.printToLog("[Disassemble] Starting the process to disassemble T-dolls...", tag)
		val result: Boolean = if (game.imageUtils.findImage("factory", tries = 30) != null && game.findAndPress("dismantle_retirement")) {
			game.findAndPress("dismantle_select_tdoll")
			game.findAndPress("dismantle_smart_select")
			game.findAndPress("dismantle_ok")
			game.printToLog("[Disassemble] Factory successfully entered. Dismantling now...", tag)
			game.findAndPress("dismantle")
		} else {
			game.printToLog("[Disassemble] Process failed to start.", tag)
			false
		}

		game.goBack()
		return result
	}
}