/**
 *  BOND Generic Device
 *
 *  Copyright 2019-2020 Dominick Meglio
 *
 */

metadata {
    definition (
		name: "BOND Generic Device", 
		namespace: "bond", 
		author: "dmeglio@gmail.com",
		importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/drivers/BOND_Generic_Device.groovy"
	) {
		capability "Switch"
		command "fixPower", [[name:"Power*", type: "ENUM", description: "Power", constraints: ["off","on"] ] ]
    }
}

def on() {
	parent.handleOn(device, device.deviceNetworkId.split(":")[1])
}

def off() {
	parent.handleOff(device, device.deviceNetworkId.split(":")[1])
}

def fixPower(power) {
	parent.fixPowerState(device, device.deviceNetworkId.split(":")[1], power)
}