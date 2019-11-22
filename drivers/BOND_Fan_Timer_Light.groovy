/**
 *  BOND Fan Timer Light
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fan Timer Light", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "Switch"
        capability "Light"
		
		command "dim", ["number"]
    }
}

def dim(duration) {
	parent.handleDim(device, device.deviceNetworkId.split(":")[1], duration)
}

def on() {
	parent.handleLightOn(device, device.deviceNetworkId.split(":")[1])
}

def off() {
    parent.handleLightOff(device, device.deviceNetworkId.split(":")[1])
}