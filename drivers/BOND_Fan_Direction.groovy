/**
 *  BOND Fan Direction
 *
 *  Copyright 2019 Dominick Meglio
 *
 */

metadata {
    definition (name: "BOND Fan Direction", namespace: "bond", author: "dmeglio@gmail.com") {
		capability "Actuator"
        command "setDirection", [[name:"Direction", type: "ENUM", description: "Direction", constraints: ["forward","reverse"] ] ]
		attribute "direction", "enum", ["forward", "reverse"]

    }
}

def setDirection(direction) {
	parent.handleDirection(device, device.deviceNetworkId.split(":")[1], direction)
}