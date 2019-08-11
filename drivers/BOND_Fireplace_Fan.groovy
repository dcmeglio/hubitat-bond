/**
 *  BOND Fireplace Fan
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fireplace Fan", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "FanControl"
    }
}

def setSpeed(speed) {
    parent.handleFPFanSpeed(device, device.deviceNetworkId.split(":")[1], speed)
}

