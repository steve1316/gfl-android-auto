package com.steve1316.gfl_android_auto.bot

import com.steve1316.gfl_android_auto.utils.MediaProjectionService
import org.opencv.core.Point

/**
 * This class handles navigation to and from the Combat screen.
 */
class Navigation(val game: Game) {
	private val tag = "[Navigation]"

	// TODO: rename enterMap and separate out logic for selecting the mission.

	/**
	 * Starts the process to verify and/or enter the correct map.
	 *
	 * @param mapName Name of the map to run.
	 * @param skipInitialLocationCheck Skip the initial check for the Combat button on the Home screen. Defaults to false.
	 * @param retreated Skips the entire navigation process if the bot previously retreated and terminated a run. Defaults to false.
	 * @return True if the bot was able to enter the specified map.
	 */
	fun enterMap(mapName: String, skipInitialLocationCheck: Boolean = false, retreated: Boolean = false): Boolean {
		if (retreated) {
			// Now that the correct episode is now active, select the map.
			if (!game.findAndPress("map$mapName")) {
				game.printToLog("\n[Navigation] Map $mapName was not found. Scrolling list of available maps...", tag = tag)
				var tries = 3
				while (tries > 0) {
					game.gestureUtils.swipe(
						MediaProjectionService.displayWidth.toFloat() / 2, MediaProjectionService.displayHeight.toFloat() / 2, MediaProjectionService.displayWidth.toFloat() / 2,
						(MediaProjectionService.displayHeight.toFloat() / 2) - 400
					)

					// Wait for the scrolling animation to settle.
					game.wait(2.0)

					if (game.findAndPress("map$mapName")) {
						break
					}

					tries -= 1
				}
			}

			return game.findAndPress("normal_battle")
		}

		// Enter the Combat screen.
		game.printToLog("\n[Navigation] Entering the Combat screen now...", tag = tag)
		if (!skipInitialLocationCheck && !game.findAndPress("home_combat", tries = 30)) throw Exception("Failed to enter Combat screen from the Home screen.")
		game.waitScreenTransition()

		// Determine the correct chapter.
		val episodeString = when (mapName) {
			"0-2" -> {
				"00"
			}
			"0-4" -> {
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
			game.printToLog("\n[Navigation] We are at the correct episode.", tag = tag)
		} else {
			game.printToLog("\n[Navigation] Bot is not at the correct episode. Navigating to Episode $episodeString...", tag = tag)
			val chapterNumber: Int = mapName.split("-")[0].toInt()
			navigateToCorrectEpisode(episodeString, chapterNumber) ?: throw Exception("Bot could not find the correct episode at the end.")
		}

		////////////////// TODO: Implement scrolling the map list if needed.

		// Now that the correct episode is now active, select the map.
		if (!game.findAndPress("map$mapName")) {
			game.printToLog("\n[Navigation] Map $mapName was not found. Scrolling list of available maps...", tag = tag)
			var tries = 3
			while (tries > 0) {
				game.gestureUtils.swipe(
					MediaProjectionService.displayWidth.toFloat() / 2, MediaProjectionService.displayHeight.toFloat() / 2, MediaProjectionService.displayWidth.toFloat() / 2,
					(MediaProjectionService.displayHeight.toFloat() / 2) - 400
				)

				// Wait for the scrolling animation to settle.
				game.wait(2.0)

				if (game.findAndPress("map$mapName")) {
					break
				}

				tries -= 1
			}
		}

		return game.findAndPress("normal_battle")
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
				game.printToLog("[Navigation] Nearest chapter button of ${chapterButtonPairLocation.second} is less than the required chapter number so scrolling the list down.", tag = tag)
				game.gestureUtils.swipe(
					chapterButtonPairLocation.second!!.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, chapterButtonPairLocation.second!!.x.toFloat(),
					(MediaProjectionService.displayHeight.toFloat() / 2) - 400
				)
			} else {
				game.printToLog("[Navigation] Nearest chapter button of ${chapterButtonPairLocation.second} is more than the required chapter number so scrolling the list up.", tag = tag)
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