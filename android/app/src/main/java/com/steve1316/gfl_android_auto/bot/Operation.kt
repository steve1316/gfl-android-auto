package com.steve1316.gfl_android_auto.bot

import com.steve1316.gfl_android_auto.data.PlanningModeData
import com.steve1316.gfl_android_auto.data.SetupData
import com.steve1316.gfl_android_auto.utils.MediaProjectionService
import org.opencv.core.Point

/**
 * This class takes care of preparing for and executing operations including deploying echelons.
 */
class Operation(val game: Game) {
	private val tag = "[Operation]"
	private var dummyDeploymentIndex: Int = 0
	private var echelonDeploymentIndex: Int = 0
	private var inactiveCorpseDragger: String = ""
	private var activeCorpseDragger: String = ""
	private var isMod: Boolean = false

	/**
	 * Prepare for the combat operation by zooming in/out the map as needed and deploying echelons.
	 *
	 */
	fun prepareAndStartOperation() {
		if (!game.configData.enableSetup) game.wait(3.0)

		if (game.imageUtils.findImage(
				"start_operation",
				tries = 30,
				region = intArrayOf(0, MediaProjectionService.displayHeight / 2, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 2)
			) == null
		) {
			throw Exception("Game failed to load into the map.")
		}

		game.printToLog("\n* * * * * * * * * * * * * * * * *", tag = tag)
		game.printToLog("[PREPARATION] Starting preparation for operation.", tag = tag)

		SetupData.setupSteps.forEach { init ->
			if (game.configData.debugMode) game.printToLog("[DEBUG] Setup executing: $init", tag = tag)
			when (init.action) {
				"pinch_in" -> {
					game.printToLog("[PREPARATION] Zooming in the map now...", tag = tag)
					game.gestureUtils.zoom(MediaProjectionService.displayWidth / 2f, MediaProjectionService.displayHeight / 2f, init.spacing[0].toFloat(), init.spacing[1].toFloat())
					game.wait(2.0)
				}
				"pinch_out" -> {
					game.printToLog("[PREPARATION] Zooming out the map now...", tag = tag)
					game.gestureUtils.zoom(MediaProjectionService.displayWidth / 2f, MediaProjectionService.displayHeight / 2f, init.spacing[0].toFloat(), init.spacing[1].toFloat())
					game.wait(2.0)
				}
				"swipe_up" -> {
					game.printToLog("[PREPARATION] Swiping the map up now...", tag = tag)
					game.gestureUtils.swipe(
						MediaProjectionService.displayWidth.toFloat() / 2, MediaProjectionService.displayHeight.toFloat() / 2,
						MediaProjectionService.displayWidth.toFloat() / 2,
						(MediaProjectionService.displayHeight.toFloat() / 2) + init.spacing[0].toFloat()
					)
					game.wait(2.0)
				}
				"swipe_down" -> {
					game.printToLog("[PREPARATION] Swiping the map down now...", tag = tag)
					game.gestureUtils.swipe(
						MediaProjectionService.displayWidth.toFloat() / 2, MediaProjectionService.displayHeight.toFloat() / 2,
						MediaProjectionService.displayWidth.toFloat() / 2,
						(MediaProjectionService.displayHeight.toFloat() / 2) - init.spacing[0].toFloat()
					)
					game.wait(2.0)
				}
				"deploy_dummy" -> {
					if (!game.configData.enableSetup) {
						game.printToLog("[PREPARATION] Deploying dummy at (${init.coordinates[0]}, ${init.coordinates[1]})", tag = tag)
						game.gestureUtils.tap(init.coordinates[0].toDouble(), init.coordinates[1].toDouble(), "node")
						game.wait(1.0)
						if (selectEchelon(game.configData.dummyEchelons[dummyDeploymentIndex].toInt(), deployEchelon = true)) dummyDeploymentIndex++
					}
				}
				"deploy_echelon" -> {
					if (!game.configData.enableSetup) {
						game.printToLog("[PREPARATION] Deploying echelon at (${init.coordinates[0]}, ${init.coordinates[1]})", tag = tag)
						game.gestureUtils.tap(init.coordinates[0].toDouble(), init.coordinates[1].toDouble(), "node")
						game.wait(1.0)
						if (selectEchelon(game.configData.dummyEchelons[echelonDeploymentIndex].toInt(), deployEchelon = true)) echelonDeploymentIndex++
					}
				}
				else -> {
					throw Exception("Invalid action. Available actions are: pinch_in, pinch_out, swipe_up, swipe_down, deploy_dummy, deploy_echelon")
				}
			}
		}

		game.printToLog("\n[PREPARATION] Finished preparation for operation.", tag = tag)
		game.printToLog("* * * * * * * * * * * * * * * * *", tag = tag)

		// Now that the echelons are deployed, start the operation.
		if (!game.configData.enableSetup) {
			game.findAndPress("start_operation", tries = 30)
			game.wait(3.0)
		}
	}

	/**
	 * Swap the corpse dragger T-Doll between the DPS and dummy echelons.
	 *
	 */
	fun swapCorpseDragger() {
		// Enter the Formation screen.
		if (game.configData.enableCorpseDrag && swapDraggerNow && game.findAndPress("echelon_formation")) {
			game.waitScreenTransition()

			game.printToLog("\n[SWAP] Swapping dragger between Dummy and DPS echelons now...", tag = tag)

			val corpseDraggerLocation = findCorpseDraggerLocation()
			if (corpseDraggerLocation == null) {
				game.printToLog("[SWAP] Could not find the position of the corpse dragger ${game.configData.corpseDragger1} or ${game.configData.corpseDragger2}. " +
						"Skipping corpse dragging...", tag = tag)
				return
			}

			// Go to the Selection screen by tapping on the portrait of the corpse dragger.
			game.gestureUtils.tap(corpseDraggerLocation.x, corpseDraggerLocation.y - 200, "node")
			game.wait(1.0)

			// Mark the other corpse dragger as the inactive one.
			if (game.configData.corpseDragger1 == activeCorpseDragger) {
				inactiveCorpseDragger = game.configData.corpseDragger2
				isMod = game.configData.enableCorpseDragger2Mod
				activeCorpseDragger = game.configData.corpseDragger1
			} else {
				inactiveCorpseDragger = game.configData.corpseDragger1
				isMod = game.configData.enableCorpseDragger1Mod
				activeCorpseDragger = game.configData.corpseDragger2
			}

			// Open the Filter By menu and fetch the data on the inactive corpse dragger.
			game.findAndPress("echelon_filter_by")
			val corpseDraggerData = game.configData.tdolls.find {
				it.name == inactiveCorpseDragger
			}

			// Filter by the type and rarity of the inactive corpse dragger.
			game.findAndPress("echelon_${corpseDraggerData?.type?.lowercase()}")
			val newRarity: Int = if (isMod) {
				corpseDraggerData!!.rarity + 1
			} else {
				corpseDraggerData!!.rarity
			}
			game.findAndPress("echelon_${newRarity}_star")
			game.findAndPress("echelon_formation_filter_by_confirm")

			// Now find all locations of Captains and perform OCR to determine the location of the Captain of the echelon to swap with.
			var tries = 5
			while (tries > 0) {
				val captainLocations = game.imageUtils.findAll("echelon_captain")
				var captainLocation: Point? = null
				var isDone = false
				captainLocations.forEach { location ->
					if (!isDone) {
						val name = game.imageUtils.findTextTesseract(location.x.toInt() + 31, location.y.toInt() - 10, 165, 40)
						var result = game.tdoll.calculateSimilarity(name)
						if (game.configData.debugMode) game.printToLog("[DEBUG] Initial detection of ${result.first}, confidence = ${result.second}", tag = tag)

						if (result.second < 0.79) {
							if (game.configData.debugMode) game.printToLog("[DEBUG] Confidence of ${result.second} < 0.79, so retrying one more time without thresholding...", tag = tag)
							val nameSecondTry = game.imageUtils.findTextTesseract(location.x.toInt() - 30, location.y.toInt() - 100, 240, 42, thresh = false)
							val resultSecondTry = game.tdoll.calculateSimilarity(nameSecondTry)
							if (game.configData.debugMode) game.printToLog("[DEBUG] Secondary detection of ${resultSecondTry.first}, confidence = ${resultSecondTry.second}")
							if (resultSecondTry.second >= 0.79) result = resultSecondTry
						}

						game.printToLog("[SWAP] Initial detection of ${result.first}, confidence = ${result.second}", tag = tag)
						if (inactiveCorpseDragger == result.first) {
							game.printToLog("[SWAP] Found the corpse dragger ${result.first} at $location. Swapping ${result.first} and $activeCorpseDragger now...", tag = tag)
							isDone = true
							captainLocation = location
						}
					}
				}

				// If found, press it to swap the inactive and active corpse dragger T-Dolls.
				if (captainLocation != null) {
					game.gestureUtils.tap(captainLocation!!.x, captainLocation!!.y, "echelon_captain")
					break
				} else {
					tries -= 1
					game.gestureUtils.swipe(
						MediaProjectionService.displayWidth.toFloat() / 2, MediaProjectionService.displayHeight.toFloat() / 2,
						MediaProjectionService.displayWidth.toFloat() / 2, (MediaProjectionService.displayHeight.toFloat() / 2) - 200
					)
					game.wait(2.0)
				}
			}

			game.goBack()
			game.goBack()
			game.printToLog("[SWAP] Successfully swapped corpse draggers.", tag = tag)
			swapDraggerNow = false
		}
	}

	/**
	 * Finds the location of either of the corpse draggers in the selected echelon.
	 *
	 * @return Point object of the corpse dragger location.
	 */
	private fun findCorpseDraggerLocation(): Point? {
		// Select the DPS echelon.
		selectEchelon(game.configData.dpsEchelons[echelonDeploymentIndex].toInt())

		// Find all instances of the "HP" text for each T-Doll in the echelon.
		val hpLocations = game.imageUtils.findAll(
			"echelon_formation_hp", region = intArrayOf(
				0, MediaProjectionService.displayHeight / 2, MediaProjectionService.displayWidth,
				MediaProjectionService.displayHeight / 2
			)
		)
		if (hpLocations.isEmpty()) throw Exception("Could not find any T-Dolls in Echelon ${echelonDeploymentIndex + 1}.")

		hpLocations.forEach { location ->
			// Crop out the name text region right above it.
			val name = game.imageUtils.findTextTesseract(location.x.toInt() - 30, location.y.toInt() - 100, 240, 42)
			var result = game.tdoll.calculateSimilarity(name)
			if (game.configData.debugMode) game.printToLog("[DEBUG] Initial detection of ${result.first}, confidence = ${result.second}", tag = tag)

			if (result.second < 0.79) {
				if (game.configData.debugMode) game.printToLog("[DEBUG] Confidence of ${result.second} < 0.79, so retrying one more time without thresholding...", tag = tag)
				val nameSecondTry = game.imageUtils.findTextTesseract(location.x.toInt() - 30, location.y.toInt() - 100, 240, 42, thresh = false)
				val resultSecondTry = game.tdoll.calculateSimilarity(nameSecondTry)
				if (game.configData.debugMode) game.printToLog("[DEBUG] Secondary detection of ${resultSecondTry.first}, confidence = ${resultSecondTry.second}")
				if (resultSecondTry.second >= 0.79) result = resultSecondTry
			}

			game.printToLog("[SWAP] Initial detection of ${result.first}, confidence = ${result.second}", tag = tag)
			if (game.configData.corpseDragger1 == result.first || game.configData.corpseDragger2 == result.first) {
				game.printToLog("[SWAP] Found the corpse dragger ${result.first} at $location.", tag = tag)

				// Mark this T-Doll as the active corpse dragger that is present in the DPS echelon.
				activeCorpseDragger = result.first
				return location
			}
		}

		return null
	}

	/**
	 * Select and deploys a echelon on the already selected node. If swapDraggerNow is set, then it will only select the echelon.
	 *
	 * @param echelonNumber The echelon to deploy.
	 * @param deployEchelon If true, deploys the selected echelon onto the currently selected node. Defaults to false.
	 * @return True if the echelon was deployed.
	 */
	private fun selectEchelon(echelonNumber: Int, deployEchelon: Boolean = false): Boolean {
		// If the initial check failed, then attempt to find it in the list.
		var echelonLocation: Point? = game.imageUtils.findImage(
			"echelon$echelonNumber", tries = 1,
			region = intArrayOf(0, 0, MediaProjectionService.displayWidth / 2, MediaProjectionService.displayHeight)
		)
		if (echelonLocation == null) {
			// If the echelon cannot be found, then find the nearest one and scroll up or down the list.
			var tempEchelonNumber = 1
			while (tempEchelonNumber <= 10) {
				echelonLocation = game.imageUtils.findImage(
					"echelon$tempEchelonNumber", region = intArrayOf(
						0, 0, MediaProjectionService.displayWidth / 2,
						MediaProjectionService.displayHeight
					)
				)

				// Scroll the echelon list up or down.
				if (echelonLocation != null) {
					if (tempEchelonNumber < echelonNumber) {
						game.printToLog("[DEPLOY_ECHELON] Nearest echelon of $tempEchelonNumber is less than the required echelon number so scrolling the list down.", tag = tag)
						game.gestureUtils.swipe(
							echelonLocation.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, echelonLocation.x.toFloat(),
							(MediaProjectionService.displayHeight.toFloat() / 2) - 400
						)

						// Wait for the scrolling animation to settle.
						game.wait(2.0)
					} else if (tempEchelonNumber > echelonNumber) {
						game.printToLog("[DEPLOY_ECHELON] Nearest echelon of $tempEchelonNumber is more than the required echelon number so scrolling the list up.", tag = tag)
						game.gestureUtils.swipe(
							echelonLocation.x.toFloat(), MediaProjectionService.displayHeight.toFloat() / 2, echelonLocation.x.toFloat(),
							(MediaProjectionService.displayHeight.toFloat() / 2) + 400
						)

						// Wait for the scrolling animation to settle.
						game.wait(2.0)
					}

					// Now check if the bot has the correct echelon in view.
					echelonLocation = game.imageUtils.findImage("echelon$echelonNumber", region = intArrayOf(0, 0, MediaProjectionService.displayWidth / 2, MediaProjectionService.displayHeight))
					if (echelonLocation != null) break
				}

				tempEchelonNumber++
			}
		}

		if (echelonLocation != null && game.gestureUtils.tap(echelonLocation.x, echelonLocation.y, "echelon$echelonNumber")) {
			return if (deployEchelon) {
				game.findAndPress("choose_echelon_ok")
			} else {
				false
			}
		} else throw Exception("Failed to deploy echelon $echelonNumber in preparation phase.")
	}

	/**
	 * Sets up the Planning Mode moves.
	 *
	 */
	fun setupPlanningMode() {
		game.wait(2.0)

		game.printToLog("\n= = = = = = = = = = = = = = = =", tag = tag)
		game.printToLog("[SETUP_PLANNING_MODE] Laying out the moves for Planning Mode now...", tag = tag)

		if (game.imageUtils.findImage("planning_mode") == null) throw Exception("Failed to find Planning Mode.")

		PlanningModeData.moves.forEach { move ->
			if (game.configData.debugMode) game.printToLog("[DEBUG] Move executing: $move", tag = tag)
			when (move.action) {
				"start" -> {
					// Resupply DPS echelon.
					game.gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
					game.wait(0.5)
					game.gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
					game.findAndPress("resupply")

					// Activate Planning Mode.
					if (!game.findAndPress("planning_mode")) throw Exception("Unable to proceed with Planning Mode as the button was not found or was obscured by something else.")
					game.gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
				}
				"start_no_resupply" -> {
					// Activate Planning Mode.
					if (!game.findAndPress("planning_mode")) throw Exception("Unable to proceed with Planning Mode as the button was not found or was obscured by something else.")
					game.gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
				}
				"move" -> {
					game.gestureUtils.tap(move.coordinates[0].toDouble(), move.coordinates[1].toDouble(), "node")
				}
			}
		}

		game.printToLog("\n[SETUP_PLANNING_MODE] Finished preparation for operation.", tag = tag)
		game.printToLog("= = = = = = = = = = = = = = = =", tag = tag)
	}

	/**
	 * Executes the Planning Mode and wait for the operation to end after some time of inactivity.
	 *
	 * @return True if the bot finished up with pressing the End Round button itself at the end of all of the checks.
	 */
	fun executeOperation(): Boolean {
		// Press the button to execute the plan.
		game.findAndPress("planning_mode_execute_plan", tries = 30)
		game.printToLog("\n[EXECUTE_PLAN] Planning Mode is now being executed. Waiting for operation to end...", tag = tag)

		var tries = 3
		while (tries > 0) {
			// If the End Round button vanished, then the bot might be in combat so wait for it to end before retrying checks.
			if (game.imageUtils.waitVanish(
					"end_round", region =
					intArrayOf(0, MediaProjectionService.displayHeight / 2, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 2), timeout = 2, suppressError = true
				)
			) {
				game.printToLog("[EXECUTE_PLAN] The End Round button has vanished. Bot must be in combat so waiting for combat end before retrying checks...", tag = tag)
				tries = 3

				// Wait until combat ends.
				while (!game.imageUtils.waitVanish("combat_pause", timeout = 1, region = intArrayOf(0, 0, MediaProjectionService.displayWidth, MediaProjectionService.displayHeight / 3))) {
					game.wait(0.5)
				}

				game.tdoll.startDetection()
				game.wait(5.0)
			} else {
				game.printToLog("[EXECUTE_PLAN] The End Round button is still here. Operation will be considered ended in $tries tries.", tag = tag)
				tries -= 1
			}
		}

		game.printToLog("\n[EXECUTE_PLAN] Stopping checks for operation end.", tag = tag)
		swapDraggerNow = true
		val result = game.findAndPress("end_round")
		return if (result) {
			game.wait(10.0)
			result
		} else {
			result
		}
	}
}