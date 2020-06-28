/**
 *  BOND Fan Dimmable Light
 *
 *  Copyright 2019-2020 Dominick Meglio
 *
 */

metadata {
    definition (
		name: "BOND Fan Dimmable Light", 
		namespace: "bond", 
		author: "dmeglio@gmail.com",
		importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/drivers/BOND_Fan_Dimmable_Light.groovy"
	) {
		capability "SwitchLevel"
		capability "Switch"
        capability "Light"
		
		command "fixPower", [[name:"Power*", type: "ENUM", description: "Power", constraints: ["off","on"] ] ]
		command "fixLevel", [[name:"Level*", type: "NUMBER", description: "Level"]]
		command "toggle"
    }
}

def setLevel(level, duration) {
	parent.handleLightLevel(device, level)
}
def setLevel(level) {
	parent.handleLightLevel(device, level)
}

def on() {
	parent.handleLightOn(device)
}

def off() {
    parent.handleLightOff(device)
}

def toggle() {
	if (device.currentValue("switch") == "on")
		off()
	else
		on()
}

def fixPower(power) {
	parent.fixLightPower(device, power)
}

def fixLevel(power) {
	parent.fixLightLevel(device, power)
}