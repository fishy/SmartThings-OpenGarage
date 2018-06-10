/**
 *  SmartThings device handler for OpenGarage.
 *
 *  Copyright 2018 Yuxuan "fishy" Wang
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  Some of the code are from https://github.com/adamheinmiller/ST_MyQ by Adam Heinmiller,
 *  used under Apache License.
 *
 */

metadata {
	definition(name: "OpenGarage", namespace: "fishy", author: "Yuxuan Wang") {
		capability "Contact Sensor"
		capability "Door Control"
		capability "Garage Door Control"
		capability "Momentary"
		capability "Polling"
		capability "Refresh"
		capability "Switch"

		attribute "doorStatus", "string"
	}

	preferences {
		input(
			name: "auth_token",
			type: "text",
			title: "Blynk Auth Token",
			description: "Your Auth Token from Blynk",
			required: true,
		)

		input(
			name: "blynk_prefix",
			type: "text",
			title: "Blynk URL prefix",
			description: "Example: \"http://blynk-cloud.com\" or \"https://blynk-proxy.herokuapp.com\"",
			required: true,
		)

		input(
			name: "refresh_rate",
			type: "enum",
			title: "State Refresh Rate",
			options: [
				"Every minute",
				"Every 5 minutes",
				"Every 10 minutes",
				"Every 15 minutes",
				"Every 30 minutes",
				"Every hour",
				"Disabled",
			],
			description: "Only disable it if you have another contact sensor hooked on the garage door",
			required: true,
		)
	}

	tiles {
		standardTile("sDoorToggle", "device.doorStatus", width: 1, height: 1, canChangeIcon: false) {
			state(
				"default",
				label: "",
			)

			state(
				"unknown",
				label: "Unknown",
				icon: "st.unknown.unknown.unknown",
				action: "refresh.refresh",
				backgroundColor: "#afafaf",
			)
			state(
				"door_not_found",
				label: "Not Found",
				backgroundColor: "#CC1821",
			)

			state(
				"stopped",
				label: "Stopped",
				icon: "st.contact.contact.open",
				action: "close",
				backgroundColor: "#ffdd00",
			)
			state(
				"closed",
				label: "Closed",
				icon: "st.doors.garage.garage-closed",
				action: "open",
				backgroundColor: "#79b820",
			)
			state(
				"closing",
				label: "Closing",
				icon: "st.doors.garage.garage-closing",
				backgroundColor: "#ffdd00",
			)
			state(
				"open",
				label: "Open",
				icon: "st.doors.garage.garage-open",
				action: "close",
				backgroundColor: "#ffdd00",
			)
			state(
				"opening",
				label: "Opening",
				icon: "st.doors.garage.garage-opening",
				backgroundColor: "#ffdd00",
			)
			state(
				"moving",
				label: "Moving",
				icon: "st.motion.motion.active",
				action: "refresh.refresh",
				backgroundColor: "#ffdd00",
			)
		}

		standardTile("sRefresh", "device.doorStatus", inactiveLabel: false, decoration: "flat") {
			state(
				"default",
				label: "",
				action: "refresh.refresh",
				icon: "st.secondary.refresh",
			)
		}

		standardTile("sContact", "device.contact") {
			state(
				"open",
				label: "${name}",
				icon: "st.contact.contact.open",
				backgroundColor: "#ffa81e",
			)
			state(
				"closed",
				label: "${name}",
				icon: "st.contact.contact.closed",
				backgroundColor: "#79b821",
			)
		}

		main(["sDoorToggle"])
		details(["sDoorToggle", "sRefresh"])
	}
}


def installed() {
	log.debug "Installing OpenGarage Door"

	checkRefresh()
	refresh()
}

def updated() {
	log.debug "Updating OpenGarage Door"

	checkRefresh()
	refresh()
}

def parse(description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'contact' attribute
	// TODO: handle 'switch' attribute
}

def push() {
	log.debug "Executing 'push'"

	def initStatus
	getDoorStatus() { status -> initStatus = status }

	def target
	if (initStatus == "closed" || initStatus == "closing" || initStatus == "stopped" || initStatus == "moving") {
		log.debug "Door is in a closed status, opening"
		target = "open"
	} else if (initStatus == "open" || initStatus == "opening") {
		log.debug "Door is in an open status, closing"
		target = "closed"
	} else if (initStatus == "unknown") {
		log.debug "Door is in an unknown state, doing nothing"
		return
	}

	flipDoor()
	refreshUntil(target)
}

def poll() {
	log.debug "OpenGarage Polling"
	refresh()
}

def refresh() {
	doRefresh()
}

def checkRefresh() {
	switch (refresh_rate.toLowerCase()) {
	case "disabled":
		unschedule(doRefresh)
		doRefresh()
		break
	case "every 5 minutes":
		runEvery5Minutes(doRefresh)
		break
	case "every 10 minutes":
		runEvery10Minutes(doRefresh)
		break
	case "every 15 minutes":
		runEvery15Minutes(doRefresh)
		break
	case "every 30 minutes":
		runEvery30Minutes(doRefresh)
		break
	case "every hour":
		runEvery1Hour(doRefresh)
		break
	case "every minute":
	default:
		runEvery1Minute(doRefresh)
		break
	}
}

def doRefresh() {
	log.debug "Refreshing Door State"

	getDoorStatus() { status ->
		setDoorState(status)
		log.debug "Door Status: $status"
	}
}

def afterForceRefresh(status, startTime) {
	time = (now() - startTime) / 1000
	log.debug "Final Door Status: $status, took $time seconds"
	setDoorState(status)
}

def forceRefreshUntil(data) {
	def timestamp = now()
	def target = data.targetStatus
	log.debug "forceRefreshUntil: ${new Date()}, timestamp: $timestamp, stops at ${data.stopAt}, target status: $target"
	def scheduleNext = true
	if (timestamp >= data.stopAt) {
		log.debug "Stopping refreshing..."
		getDoorStatus() { status ->
			afterForceRefresh(status, data.startTime)
		}
		scheduleNext = false
		return
	}
	getDoorStatus() { status ->
		log.debug "forceRefreshUntil: get door status: $status"
		if (status == target) {
			log.debug "Got target status $status, stopping refreshing..."
			afterForceRefresh(status, data.startTime)
			scheduleNext = false
			return
		}
	}
	if (scheduleNext) {
		def options = [
			overwrite: true,
			data: data,
		]
		runIn(1, forceRefreshUntil, options)
	} else {
		unschedule(forceRefreshUntil)
	}
}

def refreshUntil(target) {
	log.debug "refreshUntil: $target"
	def maxMin = 5
	def timestamp = now() + 60 * 1000 * maxMin
	def data = [
		startTime: now(),
		stopAt: timestamp,
		targetStatus: target,
	]
	forceRefreshUntil(data)
}

def on() {
	log.debug "Executing 'on'"

	open()
}

def off() {
	log.debug "Executing 'off'"

	close()
}

def open() {
	log.debug "Opening Door"

	def initStatus
	getDoorStatus() { status -> initStatus = status }

	if (initStatus == "opening" || initStatus == "open" || initStatus == "moving") {
		log.debug "Current status is $initStatus, ignore open command"
		return
	}

	setDoorState("opening")

	flipDoor()
	refreshUntil("open")
}

def close() {
	log.debug "Closing Door"

	def initStatus
	getDoorStatus() { status -> initStatus = status }

	if (initStatus == "closing" || initStatus == "closed" || initStatus == "moving") {
		log.debug "Current status is $initStatus, ignore open command"
		return
	}

	setDoorState("closing")

	flipDoor()
	refreshUntil("closed")
}

def getDoorStatus(callback = {}) {
	callApiGet("/get/V0") { resp ->
		def value = resp.data[0] as long
		def state
		if (value == 0) {
			state = "closed"
		} else {
			state = "open"
		}
		log.debug "getDoorStatus: value = $value, state = $state"
		callback(state)
	}
}

def flipDoor() {
	def body = [
		"value": 1,
	]
	callApiGet("/update/V1", body) { resp ->
		// if error, do something?
	}
}

def setContactSensorState(status) {
	// Sync contact sensor
	if (status == "open" || status == "opening" || status == "stopped") {
		sendEvent(name: "contact", value: "open", display: true, descriptionText: "Contact is open")
		sendEvent(name: "switch", value: "on", display: true, descriptionText: "Switch is on")
	} else if (status == "closed" || status == "closing") {
		sendEvent(name: "contact", value: "closed", display: true, descriptionText: "Contact is closed")
		sendEvent(name: "switch", value: "off", display: true, descriptionText: "Switch is off")
	}
}

def setDoorState(status) {
	sendEvent(name: "doorStatus", value: status, display: true, descriptionText: "Door is $status")
	setContactSensorState(status)
}

def callApiGet(apipath, callback) {
	return callApiGet(apipath, null, callback)
}

def callApiGet(apipath, query, callback) {
	def params = [
		uri: blynk_prefix,
		path: "/$auth_token$apipath",
		query: query,
	]

	log.debug "http get params: $params"

	try {
		httpGet(params) { resp ->
			log.debug "response data: ${resp.data}"
			callback(resp)
		}
	} catch (e) {
		log.debug "API Error: $e"
		setDoorState("unknown")
	}
}
