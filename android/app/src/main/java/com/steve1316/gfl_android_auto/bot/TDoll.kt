package com.steve1316.gfl_android_auto.bot

import com.steve1316.gfl_android_auto.utils.MediaProjectionService
import info.debatty.java.stringsimilarity.JaroWinkler

/**
 * This class handles text detection to determine what T-Doll dropped.
 */
class TDoll(val game: Game) {
	val acquiredDolls = arrayListOf<String>()

	private val listOfDolls = arrayListOf("L85A1", "MP-446", "SVT-38", "FN FNP9", "SIG-510", "M38", "SKS", "DP28", "MG42", "Spectre M4", "Galil", "")

	/**
	 * Starts the text detection on the T-Doll screen after combat or operation end.
	 *
	 * @return String from Tesseract or empty string in the case of ImageUtils being unable to get a screenshot.
	 */
	fun startDetection(): String {
		val shareLocation = game.imageUtils.findImage("tdoll_share", tries = 5, region = intArrayOf(0, 0, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 3))
		return if (shareLocation != null) {
			game.printToLog("\n[DETECTION] T-Doll is on screen. Now attempting to determine name of T-Doll...")
			var result = game.imageUtils.findTextTesseract(shareLocation.x.toInt() - 80, shareLocation.y.toInt() + 90, 630, 90)
			if (result == "") {
				game.printToLog("[DETECTION] Detected nothing. Trying one more time...")
				game.wait(0.5)
				result = game.imageUtils.findTextTesseract(shareLocation.x.toInt() - 80, shareLocation.y.toInt() + 90, 630, 90)
			}

			val similarityObj = calculateSimilarity(result)

			game.printToLog("[DETECTION] Detected text: $result most similar to ${similarityObj.first} with a score of ${similarityObj.second}\n")

			acquiredDolls.add(result)
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

		// Now determine text similarity.
		val stringCompare = JaroWinkler()

		listOfDolls.forEach { tdoll ->
			val tempConfidence = stringCompare.similarity(tdoll, testString)
			if (tempConfidence > resultConfidence) {
				resultConfidence = tempConfidence
				resultString = tdoll
			}
		}

		return Pair(resultString, resultConfidence)
	}
}