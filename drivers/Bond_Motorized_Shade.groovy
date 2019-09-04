/**
 *  BOND Motorized Shade
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Motorized Shade", namespace: "bond", author: "dmeglio@gmail.com") {
        capability "WindowShade"
    }
}

def open() {
	parent.handleOpen(device, device.deviceNetworkId.split(":")[1])
}

def close() {
	parent.handleClose(device, device.deviceNetworkId.split(":")[1])
}