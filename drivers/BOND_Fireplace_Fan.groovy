/**
 *  BOND Fireplace Fan
 *
 *  Copyright 2019-2020 Dominick Meglio
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
		
		command "fixPower", [[name:"Power*", type: "ENUM", description: "Power", constraints: ["off","on"] ] ]
		command "fixSpeed", [[name:"Speed*", type: "ENUM", description: "Speed", constraints: ["off","low", "medium-low", "medium", "medium-high", "high", "on"] ] ]
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

def fixPower(power) {
	parent.fixFPFanPower(device, device.deviceNetworkId.split(":")[1], power)
}

def fixSpeed(speed) {
	parent.fixFPFanSpeed(device, device.deviceNetworkId.split(":")[1], speed)
}