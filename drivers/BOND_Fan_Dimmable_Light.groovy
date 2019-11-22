/**
 *  BOND Fan Dimmable Light
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fan Dimmable Light", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "SwitchLevel"
		capability "Switch"
        capability "Light"
    }
}

def setLevel(level, duration) {
	parent.handleLightLevel(device, device.deviceNetworkId.split(":")[1], level)
}
def setLevel(level) {
	parent.handleLightLevel(device, device.deviceNetworkId.split(":")[1], level)
}

def on() {
	parent.handleLightOn(device, device.deviceNetworkId.split(":")[1])
}

def off() {
    parent.handleLightOff(device, device.deviceNetworkId.split(":")[1])
}