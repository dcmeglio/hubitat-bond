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
		command "toggle"
    }
}

def setSpeed(speed) {
    parent.handleFPFanSpeed(device, speed)
}

def on() {
	parent.handleFPFanOn(device)
}

def off () {
	parent.handleFPFanOff(device)
}

def toggle() {
	if (device.currentState("switch") == "on")
		off()
	else
		on()
}

def fixPower(power) {
	parent.fixFPFanPower(device, power)
}

def fixSpeed(speed) {
	parent.fixFPFanSpeed(device, speed)
}