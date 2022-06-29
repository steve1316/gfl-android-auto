package com.steve1316.gfl_android_auto.bot

import com.steve1316.gfl_android_auto.utils.MediaProjectionService
import org.opencv.core.Point

/**
 *
 */
class Navigation(val game: Game) {
	/**
	 * Starts the process to verify and/or enter the correct map.
	 *
	 * @param mapName Name of the map to run.
	 * @return True if the bot was able to enter the specified map.
	 */
	fun enterMap(mapName: String): Boolean {
		// Determine the correct chapter.
		val episodeString = when (mapName) {
			"0-2" -> {
				"00"
			}
			////////////////// TODO: Implement the rest of the maps.
			else -> {
				""
			}
		}

		if (episodeString == "") throw Exception("Invalid map name.")

		// First check if the bot is at the correct episode. If not, navigate to it.
		val epLocation = game.imageUtils.findImage("ep$episodeString", region = intArrayOf(0, 0, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 2))
		if (epLocation != null) {
			game.printToLog("\n[INFO] We are at the correct episode.")
		} else {
			game.printToLog("\n[INFO] Bot is not at the correct episode. Navigating to Episode $episodeString...")
			val chapterNumber: Int = mapName.split("-")[0].toInt()
			navigateToCorrectEpisode(episodeString, chapterNumber) ?: throw Exception("Bot could not find the correct episode at the end.")
		}

		////////////////// TODO: Implement scrolling the map list if needed.

		// Now that the correct episode is now active, select the map.
		return game.findAndPress("map$mapName") && game.findAndPress("normal_battle")
	}

	/**
	 * Navigate to the correct Episode.
	 *
	 * @param chapter String version of the Chapter that the Episode resides in.
	 * @param chapterNumber Integer version of the Chapter.
	 * @return Point object of the chapter button's location or null.
	 */
	private fun navigateToCorrectEpisode(chapter: String, chapterNumber: Int): Point? {
		// Determine the location of the first found location of a episode by searching for its corresponding chapter button.
		var tries = 10
		while (tries > 0) {
			val chapterButtonPairLocation = findNearestChapterButton()

			if (chapterButtonPairLocation.first == -1 || chapterButtonPairLocation.second == null) throw Exception("Bot could not seem to find any episode locations.")
			else if (chapterButtonPairLocation.first < chapterNumber) {
				game.printToLog("[INFO] Nearest chapter button of ${chapterButtonPairLocation.second} is less than the required chapter number so scrolling the list down.")
				game.gestureUtils.swipe(
					chapterButtonPairLocation.second!!.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, chapterButtonPairLocation.second!!.x.toFloat(),
					(MediaProjectionService.displayHeight.toFloat() / 2) - 400
				)
			} else {
				game.printToLog("[INFO] Nearest chapter button of ${chapterButtonPairLocation.second} is more than the required chapter number so scrolling the list up.")
				game.gestureUtils.swipe(
					chapterButtonPairLocation.second!!.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, chapterButtonPairLocation.second!!.x.toFloat(),
					(MediaProjectionService.displayHeight.toFloat() / 2) + 400
				)
			}

			// Wait for the scrolling animation to settle.
			game.wait(2.0)

			// Now check if the bot has the correct chapter button in view.
			val chapterButtonLocation = game.imageUtils.findImage(
				"ch$chapter", confidence = 0.95, region = intArrayOf(
					0, 0, MediaProjectionService.displayWidth / 2, MediaProjectionService
						.displayHeight
				)
			)
			if (chapterButtonLocation != null) {
				// If so, then press it.
				game.gestureUtils.tap(chapterButtonLocation.x, chapterButtonLocation.y, "ch$chapter")
				return chapterButtonLocation
			}

			tries -= 1
		}

		return null
	}

	/**
	 * Finds the nearest chapter button.
	 *
	 * @return Pair object containing the number of the nearest chapter button and its Point object.
	 */
	private fun findNearestChapterButton(): Pair<Int, Point?> {
		var chapterNumber = 0
		var tries = 5
		while (tries > 0) {
			// Find the nearest unselected chapter button.
			while (chapterNumber < game.maxChapterNumber) {
				val chapterString: String = if (chapterNumber < 10) "0$chapterNumber" else "$chapterNumber"
				val tempLocation = game.imageUtils.findImage(
					"ch$chapterString", tries = 1, confidence = 0.95,
					region = intArrayOf(0, 0, MediaProjectionService.displayWidth / 2, MediaProjectionService.displayHeight)
				)
				if (tempLocation != null) return Pair(chapterNumber, tempLocation) else chapterNumber++
			}

			// Reset the chapter number.
			chapterNumber = 0
			tries -= 1
		}

		return Pair(-1, null)
	}
}