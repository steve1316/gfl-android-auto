package com.steve1316.gfl_android_auto.bot

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.steve1316.gfl_android_auto.MainActivity.loggerTag
import com.steve1316.gfl_android_auto.StartModule
import com.steve1316.gfl_android_auto.data.ConfigData
import com.steve1316.gfl_android_auto.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opencv.core.Point
import java.util.concurrent.TimeUnit

/**
 * Main driver for bot activity and navigation.
 */
class Game(private val myContext: Context) {
	private val tag: String = "Game"

	private val startTime: Long = System.currentTimeMillis()

	val configData: ConfigData = ConfigData(myContext)
	val imageUtils: ImageUtils = ImageUtils(myContext, this)
	val gestureUtils: MyAccessibilityService = MyAccessibilityService.getInstance()
	private val nav: Navigation = Navigation(this)
	private val op: Operation = Operation(this)
	private val factory: Factory = Factory(this)
	val tdoll: TDoll = TDoll(this)

	var runsCompleted = 0
	val maxChapterNumber: Int = 11
	var is1920 = false

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
			Log.w("${loggerTag}${tag}", message)
		} else if (isError && !isWarning) {
			Log.e("${loggerTag}${tag}", message)
		} else {
			Log.d("${loggerTag}${tag}", message)
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
	 * Perform a dimensions check of the device to check if it has one of the supported resolutions.
	 *
	 */
	private fun dimensionCheck() {
		is1920 = if (MediaProjectionService.displayWidth == 1920 && MediaProjectionService.displayHeight == 1080) {
			true
		} else if (MediaProjectionService.displayWidth == 2400 && MediaProjectionService.displayHeight == 1080) {
			false
		} else {
			throw Exception("Wrong system resolution set to ${MediaProjectionService.displayWidth}x${MediaProjectionService.displayHeight}. Note that supported dimensions are 1920x1080 and 2560x1080.")
		}
	}

	/**
	 * Starts the process to start taking setup screenshots.
	 *
	 */
	private fun takeSetupScreenshots() {
		op.prepareAndStartOperation()
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
	 * Wait a hard coded amount of seconds to account for screen transition delays.
	 *
	 */
	fun waitScreenTransition() {
		wait(3.0)
	}

	/**
	 * Back out of the current screen.
	 *
	 */
	fun goBack() {
		findAndPress("back", tries = 2)
		waitScreenTransition()
	}

	/**
	 * Checks if the bot is at the Home screen.
	 *
	 */
	fun checkHomeScreen(): Boolean {
		return imageUtils.findImage("home_combat", tries = 2) != null
	}

	/**
	 * Checks if the bot is at the Combat screen.
	 *
	 */
	fun checkCombatScreen(): Boolean {
		return imageUtils.findImage("combat_screen", tries = 2) != null
	}

	/**
	 * Checks if there are echelons to be repaired and repairs them using tickets.
	 *
	 * @return True if the bot repaired echelons.
	 */
	fun repair(): Boolean {
		if (configData.enableRepair && configData.repairInterval != 0 && runsCompleted % configData.repairInterval == 0) {
			printToLog("\n[Repair] Starting process to repair echelons...")
			val result: Boolean = if (findAndPress("repair", tries = 2)) {
				waitScreenTransition()
				findAndPress("repair_one_click")
				wait(2.0)
				printToLog("[Repair] Repair Bay entered. Repairing echelons using tickets now...")
				findAndPress("repair_ok")
			} else {
				printToLog("[Repair] Repair Bay was not entered or no echelons need repairs. Moving on...")
				false
			}

			wait(1.0)

			goBack()
			return result
		} else {
			return false
		}
	}

	/**
	 * Bot will begin automation here.
	 *
	 * @return True if all automation goals have been met. False otherwise.
	 */
	fun start(): Boolean {
		val startTime: Long = System.currentTimeMillis()

		// Perform pre-checks.
		landscapeCheck()
		dimensionCheck()

		if (!configData.enableSetup) wait(1.0) else wait(3.0)

		if (configData.debugMode) {
			printToLog("\n[DEBUG] I am starting here but as a debugging message!")
		} else {
			printToLog("\n[INFO] I am starting here!!!")
		}

		if (configData.enableSetup) {
			takeSetupScreenshots()
			return true
		}

		// Start the logic loop here.
		var failureTries = 5
		while (runsCompleted < configData.amount) {
			// Navigate to the map.
			// TODO: Cover all instances where the bot might be anywhere in the app. It must get to the home screen first before continuing.
			val skipLocationCheck = checkCombatScreen()
			if (!skipLocationCheck) {
				if (!checkHomeScreen()) {
					printToLog("[ERROR] Failed to determine if the bot is at the Home screen. Stopping bot...", isError = true)
					break
				}

				// Check if there are echelons that need repair.
				repair()
			}

			nav.enterMap(configData.mapName, skipInitialLocationCheck = skipLocationCheck)
			if (imageUtils.findImage("insufficient_slots", tries = 2) != null) {
				// Handle the case where there are too many T-Dolls in the inventory.
				val moveToLocations = imageUtils.findAll("dismantle_move_to")
				gestureUtils.tap(moveToLocations[1].x, moveToLocations[1].y, "dismantle_move_to")
				waitScreenTransition()
				factory.disassemble()
				goBack()
				continue
			}

			waitScreenTransition()

			// Now prepare for the operation by deploying the necessary echelons.
			op.prepareAndStartOperation()

			// Setup the moves in Planning Mode.
			op.setupPlanningMode()

			// Finally, execute Planning Mode.
			if (op.executeOperation()) {
				// Check if the operation ended in success or failure.
				if (imageUtils.findImage("result_settlement", tries = 10) != null) {
					gestureUtils.tap(MediaProjectionService.displayWidth.toDouble() / 2, MediaProjectionService.displayHeight.toDouble() / 2, "node")
					wait(3.0)

					// Start detection of acquired T-Doll.
					printToLog("[INFO] Analyzing the final T-Doll reward...")
					tdoll.startDetection()

					// Now close out the screens.
					gestureUtils.tap(MediaProjectionService.displayWidth.toDouble() / 2, MediaProjectionService.displayHeight.toDouble() / 2, "node")
					wait(1.0)
					gestureUtils.tap(MediaProjectionService.displayWidth.toDouble() / 2, MediaProjectionService.displayHeight.toDouble() / 2, "node")
					wait(1.0)
					gestureUtils.tap(MediaProjectionService.displayWidth.toDouble() / 2, MediaProjectionService.displayHeight.toDouble() / 2, "node")
					wait(5.0)
				} else {
					gestureUtils.tap(MediaProjectionService.displayWidth.toDouble() / 2, MediaProjectionService.displayHeight.toDouble() / 2, "node")
				}

				runsCompleted += 1
			} else {
				Log.d(loggerTag, "[DEBUG] The bot did not complete a run. $failureTries tries left before stopping the bot.")
				failureTries -= 1
				if (failureTries < 0) throw Exception("Bot failed 5 times to complete any run.")
			}
		}

		printToLog("\n[INFO] Acquired T-Dolls: ${tdoll.acquiredDolls}")

		printToLog("\n[INFO] I am ending here!")

		val endTime: Long = System.currentTimeMillis()
		val runTime: Long = endTime - startTime
		printToLog("[INFO] Total Runtime: ${runTime}ms or ${printTime()}")

		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)
		if (sharedPreferences.getBoolean("enableDiscordNotifications", false)) {
			wait(1.0)
			DiscordUtils.queue.add("Total Runtime: ${runTime}ms or ${printTime()}")
			wait(1.0)
		}

		return true
	}
}