package com.steve1316.gfl_android_auto.bot

import com.steve1316.gfl_android_auto.utils.MediaProjectionService

/**
 * This class handles text detection to determine what T-Doll dropped.
 */
class TDoll(val game: Game) {
	val acquiredDolls = arrayListOf<String>()

	/**
	 * Starts the text detection on the T-Doll screen after combat or operation end.
	 *
	 * @return String from Tesseract or empty string in the case of ImageUtils being unable to get a screenshot.
	 */
	fun startDetection(): String {
		val shareLocation = game.imageUtils.findImage("tdoll_share", tries = 5, region = intArrayOf(0, 0, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 3))
		return if(shareLocation != null) {
			var result = game.imageUtils.findTextTesseract(shareLocation.x.toInt() - 80, shareLocation.y.toInt() + 90, 630, 90)
			if (result == "") {
				game.wait(0.5)
				result = game.imageUtils.findTextTesseract(shareLocation.x.toInt() - 80, shareLocation.y.toInt() + 90, 630, 90)
			}

			game.printToLog("\n[DETECTION] Detected: $result")
			acquiredDolls.add(result)
			result
		} else {
			""
		}
	}
}