/**
 *  BOND Fan
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (
		name: "BOND Fan", 
		namespace: "bond", 
		author: "dmeglio@gmail.com", 
		importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/drivers/BOND_Fan.groovy"
	) {
		capability "Switch"
        capability "FanControl"
    }
}

def on() {
	parent.handleOn(device, device.deviceNetworkId.split(":")[1])
	if (state.lastSpeed != null)
	{
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
	if (speed != "off" && speed != "on")
		state.lastSpeed = speed
    parent.handleFanSpeed(device, device.deviceNetworkId.split(":")[1], speed)
}

def handleLightLevel(device, id, level)
{
	parent.handleLightLevel(device, id, level)
}

def handleDim(device, bondId, duration) {
	parent.handleDim(device, bondId, duration)
}

def handleStartDimming(device, bondId) {
	parent.handleStartDimming(device, bondId)
}

def handleStopDimming(device, bondId) {
	parent.handleStopDimming(device, bondId)
}