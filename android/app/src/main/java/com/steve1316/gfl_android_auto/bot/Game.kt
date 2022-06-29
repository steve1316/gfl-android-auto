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
	var runsCompleted = 0
	val maxChapterNumber: Int = 11

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

		var failureTries = 5
		while (runsCompleted <= configData.amount) {
			// Start the logic loop here.
			val prevRunsCompleted = runsCompleted

			// Navigate to the map.
			// TODO: Cover all instances where the bot might be anywhere in the app. It must get to the home screen first before continuing.
			nav.enterMap(configData.mapName)
			wait(3.0)

			// Now prepare for the operation by deploying the necessary echelons.
			op.prepareOperation()

			// Now that the echelons are deployed, start the operation.
			findAndPress("start_operation")

			// Setup the moves in Planning Mode.
			op.setupPlanningMode()

			// Finally, execute Planning Mode.
			op.executeOperation()

			if (prevRunsCompleted == runsCompleted) {
				Log.d(loggerTag, "[DEBUG] The bot did not complete a run. $failureTries tries left before stopping the bot.")
				failureTries -= 1
				if (failureTries < 0) throw Exception("Bot failed 5 times to complete any run.")
			}
		}

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