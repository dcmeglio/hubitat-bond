/**
 *  BOND Dimmable Light
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fan Dimmable Light", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "SwitchLevel"
    }
}

def setLevel(level, duration) {
	parent.handleLightLevel(device, device.deviceNetworkId.split(":")[1], level)
}