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
	private var echelonDeploymentNumber: Int = 1

	/**
	 * Prepare for the combat operation by zooming in/out the map as needed and deploying echelons.
	 *
	 */
	fun prepareAndStartOperation() {
		echelonDeploymentNumber = 1

		game.wait(3.0)

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
				"deploy_dummy" -> {
					if (!game.configData.enableSetup) {
						game.printToLog("[PREPARATION] Deploying dummy at (${init.coordinates[0]}, ${init.coordinates[1]})", tag = tag)
						game.gestureUtils.tap(init.coordinates[0].toDouble(), init.coordinates[1].toDouble(), "node")
						game.wait(1.0)
						deployEchelon(echelonDeploymentNumber)
					}
				}
				"deploy_echelon" -> {
					if (!game.configData.enableSetup) {
						game.printToLog("[PREPARATION] Deploying echelon at (${init.coordinates[0]}, ${init.coordinates[1]})", tag = tag)
						game.gestureUtils.tap(init.coordinates[0].toDouble(), init.coordinates[1].toDouble(), "node")
						game.wait(1.0)
						deployEchelon(echelonDeploymentNumber)
					}
				}
				else -> {
					throw Exception("Invalid action")
				}
			}
		}

		game.printToLog("\n[PREPARATION] Finished preparation for operation.", tag = tag)
		game.printToLog("* * * * * * * * * * * * * * * * *", tag = tag)

		// Now that the echelons are deployed, start the operation.
		game.findAndPress("start_operation", tries = 30)
		game.wait(3.0)
	}

	/**
	 * Deploys a echelon on the already selected node.
	 *
	 * @param echelonNumber The required echelon to deploy.
	 * @return True if the echelon was deployed.
	 */
	private fun deployEchelon(echelonNumber: Int): Boolean {
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
					if (echelonLocation != null) {
						break
					}
				}

				tempEchelonNumber++
			}
		}

		if (echelonLocation != null && game.gestureUtils.tap(echelonLocation.x, echelonLocation.y, "node")) {
			echelonDeploymentNumber++
			return game.findAndPress("choose_echelon_ok")
		} else throw Exception("Failed to deploy echelon $echelonDeploymentNumber in preparation phase.")
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
				game.printToLog("[EXECUTE_PLAN] The End Round button has vanished. Bot must be in combat so waiting several seconds for it to end before retrying checks...", tag = tag)
				tries = 3
				game.tdoll.startDetection()
				game.wait(5.0)
			} else {
				game.printToLog("[EXECUTE_PLAN] The End Round button is still here. Operation will be considered ended in $tries tries.", tag = tag)
				game.wait(2.0)
				tries -= 1
			}
		}

		game.printToLog("\n[EXECUTE_PLAN] Stopping checks for operation end.", tag = tag)
		val result = game.findAndPress("end_round")
		return if (result) {
			game.wait(10.0)
			result
		} else {
			result
		}
	}
}