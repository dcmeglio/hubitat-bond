/**
 *  BOND Fireplace Fan
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fireplace", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "Switch"
		
		command "setFlame", [[name:"Height*", type: "ENUM", description: "Flame height", constraints: ["off","low","medium", "high"] ] ]
		attribute "flame", "string"
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