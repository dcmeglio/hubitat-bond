/**
 *  BOND Fireplace
 *
 *  Copyright 2019-2020 Dominick Meglio
 *
 */

metadata {
    definition (
		name: "BOND Fireplace", 
		namespace: "bond", 
		author: "dmeglio@gmail.com",
		importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/drivers/BOND_Fireplace.groovy"
	) {
		capability "Switch"
		
		command "setFlame", [[name:"Height*", type: "ENUM", description: "Flame height", constraints: ["off","low","medium", "high"] ] ]
		attribute "flame", "string"
		
		command "fixPower", [[name:"Power*", type: "ENUM", description: "Power", constraints: ["off","on"] ] ]
		command "fixFlame", [[name:"Height*", type: "ENUM", description: "Flame height", constraints: ["off","low","medium", "high"] ] ]
		command "toggle"
    }
}

def on() {
	parent.handleOn(device)
}

def off() {
	parent.handleOff(device)
}

def toggle() {
	if (device.currentState("switch") == "on")
		off()
	else
		on()
}

def setFlame(height) {
	parent.handleSetFlame(device, height)
}

def handleLightOn(device, id) {
    parent.handleLightOn(device, id)
}

def handleLightOff(device, id) {
    parent.handleLightOff(device, id)
}

def handleFPFanSpeed(device, id, speed) {
    parent.handleFPFanSpeed(device, id, speed)
}
    
def handleFPFanOn(device, id) {
    parent.handleFPFanOn(device, id)
}

def handleFPFanOff(device, id) {
    parent.handleFPFanOff(device, id)
}

def fixPower(power) {
	parent.fixPowerState(device, power)
}

def fixFlame(flame) {
	parent.fixFlameState(device, flame)
}

def fixFPFanPower(device, id, state)
{
	parent.fixFPFanPower(device, state)
}

def fixFPFanSpeed(device, id, state)
{
	parent.fixFPFanSpeed(device, state)
}

def fixLightPower(device, id, state)
{
	parent.fixLightPower(device, state)
}