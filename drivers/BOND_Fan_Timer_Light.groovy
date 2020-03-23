/**
 *  BOND Fan Timer Light
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (
		name: "BOND Fan Timer Light", 
		namespace: "bond", 
		author: "dmeglio@gmail.com",
		importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/drivers/BOND_Fan_Timer_Light.groovy"
	) {
		capability "Switch"
        capability "Light"

		
		command "dim", ["number"]
		command "startDimming"
		command "stopDimming"
		
		command "fixPower", [[name:"Power*", type: "ENUM", description: "Power", constraints: ["off","on"] ] ]
    }
}

def dim(duration) {
	parent.handleDim(device, device.deviceNetworkId.split(":")[1], duration)
}

def startDimming() {
	parent.handleStartDimming(device, device.deviceNetworkId.split(":")[1])
}

def stopDimming() {
	parent.handleStopDimming(device, device.deviceNetworkId.split(":")[1])
}

def on() {
	parent.handleLightOn(device, device.deviceNetworkId.split(":")[1])
}

def off() {
    parent.handleLightOff(device, device.deviceNetworkId.split(":")[1])
}

def installed() {
	sendEvent(name: "numberOfButtons", value: "1")
}

def updated() {
	sendEvent(name: "numberOfButtons", value: "1")
}

def fixPower(power) {
	parent.fixLightPower(device, device.deviceNetworkId.split(":")[1], power)
}