/**
 *  BOND Generic Device
 *
 *  Copyright 2019 Dominick Meglio
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
    }
}


def on() {
	parent.handleOn(device, device.deviceNetworkId.split(":")[1])
}

def off() {
	parent.handleOff(device, device.deviceNetworkId.split(":")[1])
}