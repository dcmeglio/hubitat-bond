/**
 *  BOND Fan
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fan", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "Switch"
        capability "FanControl"
    }
}

def on() {
	parent.handleOn(device, device.deviceNetworkId.split(":")[1])
	if (state.lastSpeed != null)
	{
		pauseExecution(500)
		parent.handleFanSpeed(device, device.deviceNetworkId.split(":")[1], state.lastSpeed)
	}
}

def off() {
	parent.handleOff(device, device.deviceNetworkId.split(":")[1])
}

def handleLightOn(device, id) {
    parent.handleLightOn(device, id)
}

def handleLightOff(device, id) {
    parent.handleLightOff(device, id)
}

def setSpeed(speed) {
	state.lastSpeed = speed
    parent.handleFanSpeed(device, device.deviceNetworkId.split(":")[1], speed)
}