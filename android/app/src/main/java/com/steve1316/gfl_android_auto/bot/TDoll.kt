package com.steve1316.gfl_android_auto.bot

import com.steve1316.gfl_android_auto.utils.MediaProjectionService
import info.debatty.java.stringsimilarity.JaroWinkler

/**
 * This class handles text detection to determine what T-Doll dropped.
 */
class TDoll(val game: Game) {
	private val tag = "TDoll"

	val acquiredDolls = arrayListOf<String>()

	/**
	 * Starts the text detection on the T-Doll screen after combat or operation end.
	 *
	 * @return String from Tesseract or empty string in the case of ImageUtils being unable to get a screenshot.
	 */
	fun startDetection(): String {
		// In order to start the process, it needs to see the T-Doll screen with the Share button at the top left.
		val shareLocation = game.imageUtils.findImage("tdoll_share", tries = 10, region = intArrayOf(0, 0, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 3))
		return if (shareLocation != null) {
			game.printToLog("\n[DETECTION] T-Doll is on screen. Now attempting to determine name of T-Doll...", tag = tag)
			var result = game.imageUtils.findTextTesseract(shareLocation.x.toInt() - 80, shareLocation.y.toInt() + 90, 630, 90)
			if (result == "") {
				game.printToLog("[DETECTION] Detected nothing. Trying one more time...", tag = tag)
				game.wait(0.5)
				result = game.imageUtils.findTextTesseract(shareLocation.x.toInt() - 80, shareLocation.y.toInt() + 90, 630, 90)
			}

			var similarityObj = calculateSimilarity(result)

			// It is 0.79 instead of 0.80 to avoid certain situations where the wrong result was accepted at around 0.79 instead of the next one at around 0.80
			if (similarityObj.second < 0.79) {
				// Try again but without thresholding the screenshot.
				if (game.configData.debugMode) game.printToLog("[DEBUG] Confidence of ${similarityObj.second} < 0.79, so retrying one more time without thresholding...", tag = tag)
				val nameSecondTry = game.imageUtils.findTextTesseract(shareLocation.x.toInt() - 80, shareLocation.y.toInt() + 90, 630, 90, thresh = false)
				val resultSecondTry = game.tdoll.calculateSimilarity(nameSecondTry)
				if (game.configData.debugMode) game.printToLog("[DEBUG] Secondary detection of ${resultSecondTry.first}, confidence = ${resultSecondTry.second}")
				if (resultSecondTry.second >= 0.79) similarityObj = resultSecondTry
			}

			game.printToLog("[DETECTION] Detected text: $result most similar to ${similarityObj.first} with a score of ${similarityObj.second}\n", tag = tag)

			acquiredDolls.add(similarityObj.first)
			result
		} else {
			""
		}
	}

	/**
	 * Calculate the similarity score between the detected text and the names of T-Dolls using Jaro-Winkler.
	 *
	 * @param testString Detected text from Tesseract
	 * @return Pair object of the most similar string with its similarity score.
	 */
	fun calculateSimilarity(testString: String): Pair<String, Double> {
		var resultString = ""
		var resultConfidence = 0.0

		val stringCompare = JaroWinkler()

		// Now determine text similarity by comparing the result with each T-Doll in data.
		game.configData.tdolls.forEach { tdoll ->
			val tempConfidence = stringCompare.similarity(tdoll.name, testString)
			if (tempConfidence > resultConfidence) {
				resultConfidence = tempConfidence
				resultString = tdoll.name
			}
		}

		return Pair(resultString, resultConfidence)
	}
}