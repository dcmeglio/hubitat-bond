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
    }
}

def on() {
	parent.handleOn(device, device.deviceNetworkId.split(":")[1])
}

def off() {
	parent.handleOff(device, device.deviceNetworkId.split(":")[1])
}

def setFlame(height) {
	parent.handleSetFlame(device, device.deviceNetworkId.split(":")[1], height)
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
	parent.fixPowerState(device, device.deviceNetworkId.split(":")[1], power)
}

def fixFlame(flame) {
	parent.fixFlameState(device, device.deviceNetworkId.split(":")[1], flame)
}

def fixFPFanPower(device, id, state)
{
	parent.fixFPFanPower(device, device.deviceNetworkId.split(":")[1], state)
}

def fixFPFanSpeed(device, id, state)
{
	parent.fixFPFanSpeed(device, device.deviceNetworkId.split(":")[1], state)
}

def fixLightPower(device, id, state)
{
	parent.fixLightPower(device, device.deviceNetworkId.split(":")[1], state)
}