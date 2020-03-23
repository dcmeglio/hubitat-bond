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
    }
}

def setLevel(level, duration) {
	parent.handleLightLevel(device, device.deviceNetworkId.split(":")[1], level)
}
def setLevel(level) {
	parent.handleLightLevel(device, device.deviceNetworkId.split(":")[1], level)
}

def on() {
	parent.handleLightOn(device, device.deviceNetworkId.split(":")[1])
}

def off() {
    parent.handleLightOff(device, device.deviceNetworkId.split(":")[1])
}

def fixPower(power) {
	parent.fixLightPower(device, device.deviceNetworkId.split(":")[1], power)
}

def fixLevel(power) {
	parent.fixLightLevel(device, device.deviceNetworkId.split(":")[1], power)
}