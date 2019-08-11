/**
 *  BOND Fireplace Fan
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fireplace", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "Switch"
    }
}

def on() {
	parent.handleOn(device, device.deviceNetworkId.split(":")[1])
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

def handleFPFanSpeed(device, id, speed) {
    parent.handleFPFanSpeed(device, id, speed)
}
    