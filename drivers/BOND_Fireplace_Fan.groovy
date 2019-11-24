/**
 *  BOND Fireplace Fan
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (
		name: "BOND Fireplace Fan", 
		namespace: "bond", 
		author: "dmeglio@gmail.com",
		importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/drivers/BOND_Fireplace_Fan.groovy"
	) {
		capability "FanControl"
		capability "Switch"
    }
}

def setSpeed(speed) {
    parent.handleFPFanSpeed(device, device.deviceNetworkId.split(":")[1], speed)
}

def on() {
	parent.handleFPFanOn(device, device.deviceNetworkId.split(":")[1])
}

def off () {
	parent.handleFPFanOff(device, device.deviceNetworkId.split(":")[1])
}