/**
 *  BOND Motorized Shade
 *
 *  Copyright 2019-2020 Dominick Meglio
 *
 */

metadata {
    definition (
		name: "BOND Motorized Shade", 
		namespace: "bond", 
		author: "dmeglio@gmail.com",
		importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/drivers/BOND_Motorized_Shade.groovy"
	) {
        capability "WindowShade"
		capability "Switch"
		
        command "stop"
        command "fixShade", [[name:"Shade*", type: "ENUM", description: "Shade", constraints: ["open","close"] ] ]
		command "toggle"
		
    }
}

def open() {
	parent.handleOpen(device)
}

def close() {
	parent.handleClose(device)
}

def on() {
	open()
}

def off() {
	close()
}

def toggle() {
	if (device.currentValue("windowShade") == "open")
		close()
	else
		open()
}

def stop() {
	parent.handleStop(device)
}

def fixShade(shade) {
	parent.fixShadeState(device, shade)
}

