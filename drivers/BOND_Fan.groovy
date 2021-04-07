​/**
 *  BOND Fan
 *
 *  Copyright 2019-2020 Dominick Meglio
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
		
		command "fixPower", [[name:"Power*", type: "ENUM", description: "Power", constraints: ["off","on"] ] ]
		command "fixSpeed", [[name:"Speed*", type: "ENUM", description: "Speed", constraints: ["off","low", "medium-low", "medium", "medium-high", "high", "on"] ] ]
		command "toggle"
        command "cycleSpeed"
    }
}

def on() {
	parent.handleOn(device)
	if (state.lastSpeed != null)
	{
		parent.handleFanSpeed(device, state.lastSpeed)
	}
}

def off() {
	parent.handleOff(device)
}

def toggle() {
	if (device.currentValue("switch") == "on")
		off()
	else
		on()
}

def handleLightOn(device) {
    parent.handleLightOn(device)
}

def handleLightOff(device) {
    parent.handleLightOff(device)
}
def cycleSpeed(speed) {
    if(state.lastSpeed == "off")
        speed = "low"
    else if(state.lastSpeed == "low")
        speed = "medium-low"
    else if(state.lastSpeed == "medium-low")
        speed = "medium"
    else if(state.lastSpeed == "medium")
        speed = "medium-high"
    else if(state.lastSpeed == "medium-high")
        speed = "high"
    else if(state.lastSpeed == "high")
       speed = "off"
    state.lastSpeed = speed
    parent.handleFanSpeed(device, speed)
}
def setSpeed(speed) {
	if (speed != "off" && speed != "on")
		state.lastSpeed = speed
    parent.handleFanSpeed(device, speed)
}

def handleLightLevel(device, level)
{
	parent.handleLightLevel(device, level)
}

def handleDim(device, duration) {
	parent.handleDim(device, duration)
}

def handleStartDimming(device) {
	parent.handleStartDimming(device)
}

def handleStopDimming(device) {
	parent.handleStopDimming(device)
}

def fixPower(power) {
	parent.fixPowerState(device, power)
}

def fixSpeed(speed) {
	parent.fixFanSpeed(device, speed)
}

def fixLightPower(device, power) {
	parent.fixLightPower(device, power)
}

def fixLightLevel(device, level) {
	parent.fixLightLevel(device, level)
}
