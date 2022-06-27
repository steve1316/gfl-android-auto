package com.steve1316.gfl_android_auto.bot

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.steve1316.gfl_android_auto.MainActivity.loggerTag
import com.steve1316.gfl_android_auto.StartModule
import com.steve1316.gfl_android_auto.data.ConfigData
import com.steve1316.gfl_android_auto.data.PlanningModeData
import com.steve1316.gfl_android_auto.data.SetupData
import com.steve1316.gfl_android_auto.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opencv.core.Point
import java.util.concurrent.TimeUnit

/**
 * Main driver for bot activity and navigation.
 */
class Game(private val myContext: Context) {
	private val tag: String = "${loggerTag}Game"

	private val startTime: Long = System.currentTimeMillis()

	val configData: ConfigData = ConfigData(myContext)
	val imageUtils: ImageUtils = ImageUtils(myContext, this)
	val gestureUtils: MyAccessibilityService = MyAccessibilityService.getInstance()

	val maxChapterNumber: Int = 11
	var echelonDeploymentNumber: Int = 1

	/**
	 * Returns a formatted string of the elapsed time since the bot started as HH:MM:SS format.
	 *
	 * Source is from https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format/9027379
	 *
	 * @return String of HH:MM:SS format of the elapsed time.
	 */
	private fun printTime(): String {
		val elapsedMillis: Long = System.currentTimeMillis() - startTime

		return String.format(
			"%02d:%02d:%02d",
			TimeUnit.MILLISECONDS.toHours(elapsedMillis),
			TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsedMillis)),
			TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMillis))
		)
	}

	/**
	 * Print the specified message to debug console and then saves the message to the log.
	 *
	 * @param message Message to be saved.
	 * @param tag Distinguish between messages for where they came from. Defaults to Game's tag.
	 * @param isWarning Flag to determine whether to display log message in console as debug or warning.
	 * @param isError Flag to determine whether to display log message in console as debug or error.
	 */
	fun printToLog(message: String, tag: String = this.tag, isWarning: Boolean = false, isError: Boolean = false) {
		if (!isError && isWarning) {
			Log.w(tag, message)
		} else if (isError && !isWarning) {
			Log.e(tag, message)
		} else {
			Log.d(tag, message)
		}

		// Remove the newline prefix if needed and place it where it should be.
		val newMessage = if (message.startsWith("\n")) {
			"\n" + printTime() + " " + message.removePrefix("\n")
		} else {
			printTime() + " " + message
		}

		MessageLog.messageLog.add(newMessage)

		// Send the message to the frontend.
		StartModule.sendEvent("MessageLog", newMessage)
	}

	/**
	 * Wait the specified seconds to account for ping or loading.
	 *
	 * @param seconds Number of seconds to pause execution.
	 */
	fun wait(seconds: Double) {
		runBlocking {
			delay((seconds * 1000).toLong())
		}
	}

	/**
	 * Finds and presses the image's location.
	 *
	 * @param imageName Name of the button image file.
	 * @param tries Number of tries to find the specified image. Defaults to 0 which will use ImageUtil's default.
	 * @param suppressError Whether or not to suppress saving error messages to the log in failing to find the image.
	 * @return True if the image was found and clicked. False otherwise.
	 */
	fun findAndPress(imageName: String, tries: Int = 0, suppressError: Boolean = false): Boolean {
		if (configData.debugMode) {
			printToLog("[DEBUG] Now attempting to find and press the \"$imageName\" button.")
		}

		val tempLocation: Point? = if (tries > 0) {
			imageUtils.findImage(imageName, tries = tries, suppressError = suppressError)
		} else {
			imageUtils.findImage(imageName, suppressError = suppressError)
		}

		return if (tempLocation != null) {
			if (configData.enableDelayTap) {
				val newDelay: Double = ((configData.delayTapMilliseconds - 100)..(configData.delayTapMilliseconds + 100)).random().toDouble() / 1000
				if (configData.debugMode) printToLog("[DEBUG] Adding an additional delay of ${newDelay}s...")
				wait(newDelay)
			}

			gestureUtils.tap(tempLocation.x, tempLocation.y, imageName)
			wait(1.0)
			true
		} else {
			false
		}
	}

	/**
	 * Check rotation of the Virtual Display and if it is stuck in Portrait Mode, destroy and remake it.
	 *
	 */
	private fun landscapeCheck() {
		if (MediaProjectionService.displayHeight > MediaProjectionService.displayWidth) {
			Log.d(tag, "Virtual display is not correct. Recreating it now...")
			MediaProjectionService.forceGenerateVirtualDisplay(myContext)
		} else {
			Log.d(tag, "Skipping recreation of Virtual Display as it is correct.")
		}
	}

	/**
	 * Takes 5 screenshots back to back.
	 *
	 */
	private fun takeSetupScreenshots() {
		var tries = 1
		while (tries <= 5) {
			val result: Boolean = imageUtils.saveScreenshot("$tries")
			if (result) {
				tries += 1
				wait(1.0)
			}
		}
	}

	/**
	 * Deploys a echelon on the already selected node.
	 *
	 * @param echelonNumber The required echelon to deploy.
	 * @return True if the echelon was deployed.
	 */
	private fun deployEchelon(echelonNumber: Int): Boolean {
		// If the echelon cannot be found, then find the nearest one and scroll up or down the list.
		var echelonLocation = imageUtils.findImage("echelon$echelonDeploymentNumber")
		if (echelonLocation != null) {
			var tempEchelonNumber = 1
			while (tempEchelonNumber <= 10) {
				echelonLocation = imageUtils.findImage(
					"echelon$tempEchelonNumber", region = intArrayOf(
						0, 0, MediaProjectionService.displayWidth / 2,
						MediaProjectionService.displayHeight
					)
				)

				// Scroll the echelon list up or down.
				if (echelonLocation != null) {
					if (tempEchelonNumber < echelonNumber) {
						printToLog("[DEPLOY_ECHELON] Nearest echelon of $tempEchelonNumber is less than the required echelon number so scrolling the list down.")
						gestureUtils.swipe(
							echelonLocation.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, echelonLocation.x.toFloat(),
							(MediaProjectionService.displayHeight.toFloat() / 2) - 400
						)
					} else {
						printToLog("[DEPLOY_ECHELON] Nearest echelon of $tempEchelonNumber is more than the required echelon number so scrolling the list up.")
						gestureUtils.swipe(
							echelonLocation.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, echelonLocation.x.toFloat(),
							(MediaProjectionService.displayHeight.toFloat() / 2) + 400
						)
					}

					// Wait for the scrolling animation to settle.
					wait(2.0)

					// Now check if the bot has the correct echelon in view.
					echelonLocation = imageUtils.findImage(
						"echelon$echelonNumber", confidence = 0.95,
						region = intArrayOf(0, 0, MediaProjectionService.displayWidth / 2, MediaProjectionService.displayHeight)
					)
					if (echelonLocation != null) {
						break
					}
				}

				tempEchelonNumber++
			}
		}

		if (echelonLocation != null && gestureUtils.tap(echelonLocation.x, echelonLocation.y, "node")) {
			echelonDeploymentNumber++
			return findAndPress("choose_echelon_ok")
		} else throw Exception("Failed to deploy echelon $echelonDeploymentNumber in preparation phase.")
	}

	/**
	 * Starts the process to verify and/or enter the correct map.
	 *
	 * @param mapName Name of the map to run.
	 * @return True if the bot was able to enter the specified map.
	 */
	private fun enterMap(mapName: String): Boolean {
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
		val epLocation = imageUtils.findImage("ep$episodeString", region = intArrayOf(0, 0, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 2))
		if (epLocation != null) {
			printToLog("\n[INFO] We are at the correct episode.")
		} else {
			printToLog("\n[INFO] Bot is not at the correct episode. Navigating to Episode $episodeString...")
			val chapterNumber: Int = mapName.split("-")[0].toInt()
			navigateToCorrectEpisode(episodeString, chapterNumber) ?: throw Exception("Bot could not find the correct episode at the end.")
		}

		////////////////// TODO: Implement scrolling the map list if needed.

		// Now that the correct episode is now active, select the map.
		return findAndPress("map$mapName") && findAndPress("normal_battle")
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
				printToLog("[INFO] Nearest chapter button of ${chapterButtonPairLocation.second} is less than the required chapter number so scrolling the list down.")
				gestureUtils.swipe(
					chapterButtonPairLocation.second!!.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, chapterButtonPairLocation.second!!.x.toFloat(),
					(MediaProjectionService.displayHeight.toFloat() / 2) - 400
				)
			} else {
				printToLog("[INFO] Nearest chapter button of ${chapterButtonPairLocation.second} is more than the required chapter number so scrolling the list up.")
				gestureUtils.swipe(
					chapterButtonPairLocation.second!!.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, chapterButtonPairLocation.second!!.x.toFloat(),
					(MediaProjectionService.displayHeight.toFloat() / 2) + 400
				)
			}

			// Wait for the scrolling animation to settle.
			wait(2.0)

			// Now check if the bot has the correct chapter button in view.
			val chapterButtonLocation = imageUtils.findImage("ch$chapter", confidence = 0.95, region = intArrayOf(0, 0, MediaProjectionService.displayWidth / 2, MediaProjectionService.displayHeight))
			if (chapterButtonLocation != null) {
				// If so, then press it.
				gestureUtils.tap(chapterButtonLocation.x, chapterButtonLocation.y, "ch$chapter")
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
			while (chapterNumber < maxChapterNumber) {
				val chapterString: String = if (chapterNumber < 10) "0$chapterNumber" else "$chapterNumber"
				val tempLocation = imageUtils.findImage(
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

	/**
	 * Prepare for the combat operation by zooming in/out the map as needed and deploying echelons.
	 *
	 */
	private fun prepareOperation() {
		if (imageUtils.findImage(
				"start_operation",
				tries = 30,
				region = intArrayOf(0, MediaProjectionService.displayHeight / 2, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 2)
			) == null
		) {
			throw Exception("Game failed to load into the map.")
		}

		printToLog("\n* * * * * * * * * * * * * * * * *")
		printToLog("[PREPARATION] Starting preparation for operation.")

		SetupData.setupSteps.forEach { init ->
			when (init.action) {
				"pinch_in" -> {
					printToLog("[PREPARATION] Zooming in the map now...")
					gestureUtils.zoom(MediaProjectionService.displayWidth / 2f, MediaProjectionService.displayHeight / 2f, init.spacing[0].toFloat(), init.spacing[1].toFloat())
					wait(2.0)
				}
				"pinch_out" -> {
					printToLog("[PREPARATION] Zooming out the map now...")
					gestureUtils.zoom(MediaProjectionService.displayWidth / 2f, MediaProjectionService.displayHeight / 2f, init.spacing[0].toFloat(), init.spacing[1].toFloat())
					wait(2.0)
				}
				"deploy_dummy" -> {
					printToLog("[PREPARATION] Deploying dummy at (${init.coordinates[0]}, ${init.coordinates[1]})")
					gestureUtils.tap(init.coordinates[0].toDouble(), init.coordinates[1].toDouble(), "node")
					wait(1.0)
					deployEchelon(echelonDeploymentNumber)
				}
				"deploy_echelon" -> {
					printToLog("[PREPARATION] Deploying echelon at (${init.coordinates[0]}, ${init.coordinates[1]})")
					gestureUtils.tap(init.coordinates[0].toDouble(), init.coordinates[1].toDouble(), "node")
					wait(1.0)
					deployEchelon(echelonDeploymentNumber)
				}
				else -> {
					throw Exception("Invalid action")
				}
			}
		}

		printToLog("\n[PREPARATION] Finished preparation for operation.")
		printToLog("* * * * * * * * * * * * * * * * *")
	}

	/**
	 * Sets up the Planning Mode moves.
	 *
	 */
	private fun setupPlanningMode() {
		wait(2.0)

		printToLog("\n= = = = = = = = = = = = = = = =")
		printToLog("[SETUP_PLANNING_MODE] Laying out the moves for Planning Mode now...")

		PlanningModeData.moves.forEach { move ->
			when (move.action) {
				"start" -> {
					// Resupply DPS echelon.
					gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
					wait(0.5)
					gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
					findAndPress("resupply")

					// Activate Planning Mode.
					if (!findAndPress("planning_mode")) throw Exception("Unable to proceed with Planning Mode as the button was not found or was obscured by something else.")
					gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
				}
				"start_no_resupply" -> {
					// Activate Planning Mode.
					if (!findAndPress("planning_mode")) throw Exception("Unable to proceed with Planning Mode as the button was not found or was obscured by something else.")
					gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
				}
				"move" -> {
					gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
				}
			}
		}

		printToLog("\n[SETUP_PLANNING_MODE] Finished preparation for operation.")
		printToLog("= = = = = = = = = = = = = = = =")
	}

	/**
	 * Executes the Planning Mode and wait for the operation to end after some time of inactivity.
	 *
	 */
	private fun executeOperation() {
		// Press the button to execute the plan.
		findAndPress("planning_mode_execute_plan")
		printToLog("\n[EXECUTE_PLAN] Planning Mode is now being executed. Waiting for operation to end...")

		var tries = 10
		while (tries > 0) {
			// If the End Round button vanished, then the bot might be in combat so wait for it to end before retrying checks.
			if (imageUtils.waitVanish(
					"end_round", region =
					intArrayOf(0, MediaProjectionService.displayHeight / 2, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 2), timeout = 3, suppressError = true
				)
			) {
				printToLog("[EXECUTE_PLAN] The End Round button has vanished. Bot must be in combat so waiting 30 seconds for it to end before retrying checks...")
				tries = 10
				wait(30.0)
			} else {
				printToLog("[EXECUTE_PLAN] The End Round button is still here. Operation will be considered ended in $tries tries.")
				tries -= 1
			}
		}

		printToLog("\n[EXECUTE_PLAN] Stopping checks for operation end.")
	}

	/**
	 * Bot will begin automation here.
	 *
	 * @return True if all automation goals have been met. False otherwise.
	 */
	fun start(): Boolean {
		val startTime: Long = System.currentTimeMillis()

		landscapeCheck()

		wait(1.0)

		if (configData.debugMode) {
			printToLog("\n[DEBUG] I am starting here but as a debugging message!")
		} else {
			printToLog("\n[INFO] I am starting here!!!")
		}

		enterMap(configData.mapName)

		wait(3.0)

		prepareOperation()

		findAndPress("start_operation")

		setupPlanningMode()

		executeOperation()

		printToLog("\n[INFO] I am ending here!")

		val endTime: Long = System.currentTimeMillis()
		val runTime: Long = endTime - startTime
		printToLog("Total Runtime: ${runTime}ms")

		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)
		if (sharedPreferences.getBoolean("enableDiscordNotifications", false)) {
			wait(1.0)
			DiscordUtils.queue.add("Total Runtime: ${runTime}ms")
			wait(1.0)
		}

		return true
	}
}