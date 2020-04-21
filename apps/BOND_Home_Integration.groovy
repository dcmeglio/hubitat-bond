/**
 *  https://raw.githubusercontent.com/dcmeglio/hubitat-bond/master/apps/BOND_Home_Integration.groovy
 *
 *  BOND Home Integration
 *
 *  Copyright 2019-2020 Dominick Meglio
 *
 * Revision History
 * v 2019.12.01 - Fixed an issue where dimmers wouldn't work with fans that support direction controls, fixed an issue setting flame height
 * v 2019.11.24 - Added support for timer based fan light dimmers and flame height adjustment for fireplaces
 * v 2019.12.14 - Added support for Switch capability to the motorized shades for compatibility
 * v 2020.01.02 - Fixed an issue where fan speed wouldn't be set properly (thanks jchurch for the troubleshooting!)
 * v 2020.02.01 - Fixed an issue where looking for devices was incorrect which broke Smart By BOND devices (thanks mcneillk for the fix!)
 * v 2020.03.23 - Added the ability to fix device state when it's out of sync (thanks stephen_nutt for the suggestion)
 * v 2020.04.13 - Added a stop command to motorized shades to stop an open/close at the current position (suggested by jchurch)
 * v 2020.04.21 - Added better logging for connection issues to the hub
 *
 */

definition(
    name: "BOND Home Integration",
    namespace: "dcm.bond",
    author: "Dominick Meglio",
    description: "Connects to BOND Home hub",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	documentationLink: "https://github.com/dcmeglio/hubitat-bond/blob/master/README.md")


preferences {
	page(name: "prefHub", title: "BOND")
	page(name: "prefListDevices", title: "BOND")
	page(name: "prefPowerSensors", title: "BOND")
}

def prefHub() {
	return dynamicPage(name: "prefHub", title: "Connect to BOND", nextPage:"prefListDevices", uninstall:false, install: false) {
		section("Hub Information"){
			input("hubIp", "text", title: "BOND Hub IP", description: "BOND Hub IP Address")
			input("hubToken", "text", title: "BOND Hub Token", description: "BOND Hub Token")
            input("debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false)
		}
		displayFooter()
	}
}

def prefListDevices() {
	if (!getDevices())
	{
		return dynamicPage(name: "prefListDevices", title: "Connection Error", install: false, uninstall: false) {
			section("Error") {
				paragraph "Unable to retrieve devices. Please verify your BOND Hub ID and Token"
			}
			displayFooter()
		}
	}
	else
	{
		return dynamicPage(name: "prefListDevices", title: "Devices", nextPage: "prefPowerSensors", install: false, uninstall: false) {
			section("Devices") {
				if (state.fireplaceList.size() > 0)
					input(name: "fireplaces", type: "enum", title: "Fireplaces", required:false, multiple:true, options:state.fireplaceList, hideWhenEmpty: true)
				if (state.fanList.size() > 0)
					input(name: "fans", type: "enum", title: "Fans", required:false, multiple:true, options:state.fanList, hideWhenEmpty: true)
				if (state.shadeList.size() > 0)
					input(name: "shades", type: "enum", title: "Shades", required:false, multiple:true, options:state.shadeList, hideWhenEmpty: true)
				if (state.genericList.size() > 0)
					input(name: "genericDevices", type: "enum", title: "Generic Devices", required:false, multiple:true, options:state.genericList, hideWhenEmpty: true)
			}
			displayFooter()
		}
	}
}

def prefPowerSensors() {
	return dynamicPage(name: "prefPowerSensors", title: "Fireplace Power Meters", install: true, uninstall: true, hideWhenEmpty: true) {
		section("Fireplace Power Meters") {
			paragraph "For each fireplace device you can associate a power meter to more accurately tell when it is powered on"
			if (fireplaces != null) {
				for (def i = 0; i < fireplaces.size(); i++) {
					input(name: "fireplaceSensor${i}", type: "capability.powerMeter", title: "Sensor for ${state.fireplaceList[fireplaces[i]]}", required: false, submitOnChange: true)
				}
				for (def i = 0; i < fireplaces.size(); i++) {
					if (this.getProperty("fireplaceSensor${i}") != null)
					input(name: "fireplaceSensorThreshold${i}", type: "number", title: "Sensor threshold for ${state.fireplaceList[fireplaces[i]]}", required: false)
				}
			}
		}
		displayFooter()
	}
}

def installed() {
	logDebug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	logDebug "Updated with settings: ${settings}"
    unschedule()
	unsubscribe()
	initialize()
}

def uninstalled() {
	logDebug "Uninstalled app"

	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}	
}

def initialize() {
	logDebug "initializing"

	cleanupChildDevices()
	createChildDevices()
	subscribeSensorEvents()	
    schedule("0/30 * * * * ? *", updateDevices)
}

def getDevices() {
	state.fireplaceList = [:]
    state.fireplaceDetails = [:]
	state.fireplaceProperties = [:]
	state.fanList = [:]
    state.fanDetails = [:]
	state.fanProperties = [:]
	state.shadeList = [:]
	state.shadeDetails = [:]
	state.shadeProperties = [:]
	state.genericList = [:]
	state.genericDetails = [:]
	state.deviceList = [:]
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	try
	{
		def result = false
		httpGet(params) { resp ->
			if (checkHttpResponse("getDevices", resp))
			{
				for (deviceid in resp.data) {
					if (deviceid.key == "_")
						continue
					getDeviceById(deviceid);
				}
				result = true
			}
		}
		return result
	}
	catch (e)
	{
		checkHttpResponse("getDevices", e.getResponse())
		return false
	}
}

def getDeviceById(id) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${id.key}",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	try
	{
		httpGet(params) { resp ->
			if (checkHttpResponse("getDeviceById", resp))
			{
				if (resp.data.type == "FP")
				{
					state.fireplaceList[id.key] = resp.data.name
					state.fireplaceDetails[id.key] = resp.data.actions
					state.fireplaceProperties[id.key] = getDeviceProperties(id)
				}
				else if (resp.data.type == "CF")
				{
					state.fanList[id.key] = resp.data.name
					state.fanDetails[id.key] = resp.data.actions
					state.fanProperties[id.key] = getDeviceProperties(id)
				}
				else if (resp.data.type == "MS")
				{
					state.shadeList[id.key] = resp.data.name
					state.shadeDetails[id.key] = resp.data.actions
					state.shadeProperties[id.key] = getDeviceProperties(id)
				}
				else if (resp.data.type == "GX")
				{
					state.genericList[id.key] = resp.data.name
					state.genericDetails[id.key] = resp.data.actions
				}
			}
		}
	}
	catch (e)
	{
		checkHttpResponse("getDeviceById", e.getResponse())
	}
}

def getDeviceProperties(id) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${id.key}/properties",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	def result = null
	try
	{
		httpGet(params) { resp ->
			if (checkHttpResponse("getDeviceProperties", resp))
			{
				result = resp.data
			}
		}
	}
	catch (e)
	{
		checkHttpResponse("getDeviceProperties", e.getResponse())
	}
	return result
}

def createChildDevices() {
	if (fireplaces != null) 
	{
		for (fireplace in fireplaces)
		{
			def fpDevice = getChildDevice("bond:" + fireplace)
			if (!fpDevice)
            {
				fpDevice = addChildDevice("bond", "BOND Fireplace", "bond:" + fireplace, 1234, ["name": state.fireplaceList[fireplace], isComponent: false])\
			}
			if (state.fireplaceDetails[fireplace].contains("TurnFpFanOn"))
			{
				if (!fpDevice.getChildDevice("bond:" + fireplace + ":fan"))
					fpDevice.addChildDevice("bond", "BOND Fireplace Fan", "bond:" + fireplace + ":fan", ["name": state.fireplaceList[fireplace] + " Fan", isComponent: true])
			}
			if (state.fireplaceDetails[fireplace].contains("TurnLightOn"))
			{
				if (!fpDevice.getChildDevice("bond:" + fireplace + ":light"))
					fpDevice.addChildDevice("bond", "BOND Fireplace Light", "bond:" + fireplace + ":light", ["name": state.fireplaceList[fireplace] + " Light", isComponent: true])
			}
		}
	}
	
	if (fans != null) 
	{
		for (fan in fans)
		{
			def fanDevice = getChildDevice("bond:" + fan)
			if (!fanDevice)
            {
				if (state.fanDetails[fan].contains("SetDirection"))
					fanDevice = addChildDevice("bond", "BOND Fan With Direction", "bond:" + fan, 1234, ["name": state.fanList[fan], isComponent: false])
				else
					fanDevice = addChildDevice("bond", "BOND Fan", "bond:" + fan, 1234, ["name": state.fanList[fan], isComponent: false])
			}
			if (state.fanDetails[fan].contains("TurnUpLightOn") && state.fanDetails[fan].contains("TurnDownLightOn"))
			{
				if (state.fanDetails[fan].contains("SetUpLightBrightness") && state.fanDetails[fan].contains("SetDownLightBrightness"))
				{
					if (!fanDevice.getChildDevice("bond:" + fan + ":uplight"))
						fanDevice.addChildDevice("bond", "BOND Fan Dimmable Light", "bond:" + fan + ":uplight", ["name": state.fanList[fan] + " Up Light", isComponent: true])
					if (!fanDevice.getChildDevice("bond:" + fan + ":downlight"))
						fanDevice.addChildDevice("bond", "BOND Fan Dimmable Light", "bond:" + fan + ":downlight", ["name": state.fanList[fan] + " Down Light", isComponent: true])

				}
				else if (state.fanDetails[fan].contains("StartUpLightDimmer") && state.fanDetails[fan].contains("StartDownLightDimmer"))
				{
					if (!fanDevice.getChildDevice("bond:" + fan + ":uplight"))
						fanDevice.addChildDevice("bond", "BOND Fan Timer Light", "bond:" + fan + ":uplight", ["name": state.fanList[fan] + " Up Light", isComponent: true])
					if (!fanDevice.getChildDevice("bond:" + fan + ":downlight"))
						fanDevice.addChildDevice("bond", "BOND Fan Timer Light", "bond:" + fan + ":downlight", ["name": state.fanList[fan] + " Down Light", isComponent: true])
				}
				else
				{
					if (!fanDevice.getChildDevice("bond:" + fan + ":uplight"))
						fanDevice.addChildDevice("bond", "BOND Fan Light", "bond:" + fan + ":uplight", ["name": state.fanList[fan] + " Up Light", isComponent: true])
					if (!fanDevice.getChildDevice("bond:" + fan + ":downlight"))
						fanDevice.addChildDevice("bond", "BOND Fan Light", "bond:" + fan + ":downlight", ["name": state.fanList[fan] + " Down Light", isComponent: true])
				}
			}
			else if (state.fanDetails[fan].contains("TurnLightOn"))
			{
				if (!fanDevice.getChildDevice("bond:" + fan + ":light"))
				{
					if (state.fanDetails[fan].contains("SetBrightness"))
					{
						fanDevice.addChildDevice("bond", "BOND Fan Dimmable Light", "bond:" + fan + ":light", ["name": state.fanList[fan] + " Light", isComponent: true])
					}
					else if (state.fanDetails[fan].contains("StartDimmer"))
					{
						fanDevice.addChildDevice("bond", "BOND Fan Timer Light", "bond:" + fan + ":light", ["name": state.fanList[fan] + " Light", isComponent: true])
					}
					else
						fanDevice.addChildDevice("bond", "BOND Fan Light", "bond:" + fan + ":light", ["name": state.fanList[fan] + " Light", isComponent: true])
				}
			}
		}
	}
	
	if (shades != null)
	{
		for (shade in shades)
		{
			def shadeDevice = getChildDevice("bond:" + shade)
			if (!shadeDevice)
            {
				shadeDevice = addChildDevice("bond", "BOND Motorized Shade", "bond:" + shade, 1234, ["name": state.shadeList[shade], isComponent: false])
			}
		}
	}
	
	if (genericDevices != null)
	{
		for (generic in genericDevices)
		{
			def genericDevice = getChildDevice("bond:" + generic)
			if (!genericDevice)
            {
				genericDevice = addChildDevice("bond", "BOND Generic Device", "bond:" + generic, 1234, ["name": state.genericList[generic], isComponent: false])
			}
		}
	}
}

def cleanupChildDevices()
{
	for (device in getChildDevices())
	{
		def deviceId = device.deviceNetworkId.replace("bond:","")
		
		def deviceFound = false
		for (fireplace in fireplaces)
		{
			if (fireplace == deviceId)
			{
				deviceFound = true
				cleanupFPComponents(device, fireplace)
				break
			}
		}
		
		if (deviceFound == true)
			continue
		
		for (fan in fans)
		{
			if (fan == deviceId)
			{
				deviceFound = true
				cleanupFanComponents(device, fan)
				break
			}
		}
		if (deviceFound == true)
			continue
			
		for (shade in shades)
		{
			if (shade == deviceId)
			{
				deviceFound = true
				break
			}
		}
		if (deviceFound == true)
			continue
			
		for (generic in genericDevices)
		{
			if (generic == deviceId)
			{
				deviceFound = true
				break
			}
		}
		if (deviceFound == true)
			continue
		
		deleteChildDevice(device.deviceNetworkId)
	}
}

def cleanupFPComponents(device, fireplace)
{
	if (!state.fireplaceDetails[fireplace].contains("TurnFpFanOn"))
	{
		device.deleteChildDevice("bond:" + fireplace + ":fan")
	}
	if (!state.fireplaceDetails[fireplace].contains("TurnLightOn"))
	{
		device.deleteChildDevice("bond:" + fireplace + ":light")
	}
}

def cleanupFanComponents(device, fan)
{
	if (!state.fanDetails[fan].contains("TurnUpLightOn") || !state.fanDetails[fan].contains("TurnDownLightOn"))
	{
		device.deleteChildDevice("bond:" + fan + ":uplight")
		device.deleteChildDevice("bond:" + fan + ":downlight")
	}
	if (!state.fanDetails[fan].contains("TurnLightOn") || (state.fanDetails[fan].contains("TurnUpLightOn") && state.fanDetails[fan].contains("TurnDownLightOn")))
	{
		device.deleteChildDevice("bond:" + fan + ":light")
	}
}

def subscribeSensorEvents() {
	if (fireplaces != null)
	{
		for (def i = 0; i < fireplaces.size(); i++)
		{
			def sensorDevice = this.getProperty("fireplaceSensor${i}")
			if (sensorDevice != null)
			{
				logDebug "subscribing to power event for ${sensorDevice}"
				subscribe(sensorDevice, "power", powerMeterEventHandler)
			}
		}
	}
}
				  
def powerMeterEventHandler(evt) {
	logDebug "Received power meter event ${evt}"
	for (def i = 0; i < fireplaces.size(); i++)
	{
		def sensorDevice = this.getProperty("fireplaceSensor${i}")
		if (evt.device.id == sensorDevice.id)
		{
			def fireplace = fireplaces[i];
			def fireplaceDevice = getChildDevice("bond:" + fireplace)
			def threshold = 10
			def value = "on"
			if (evt.integerValue < threshold)
				value = "off"
			if (value != fireplaceDevice.currentValue("switch"))
			{
				logDebug "current state ${fireplaceDevice.currentValue("switch")} changing to ${value}"
				fireplaceDevice.sendEvent(name: "switch", value: value)
			}
            if (value == "off")
            {
                def fanDevice = fireplaceDevice.getChildDevice("bond:" + fireplace + ":fan")
                if (fanDevice)
                    fanDevice.sendEvent(name: "speed", value: "off")
            }
			break;
		}
	}
}

def updateDevices() {
    for (fan in fans) {
        def deviceState = getState(fan)
        def device = getChildDevice("bond:" + fan)
        def deviceLight = device.getChildDevice("bond:" + fan + ":light")
		def deviceUpLight = device.getChildDevice("bond:" + fan + ":uplight")
		def deviceDownLight = device.getChildDevice("bond:" + fan + ":downlight")
        if (deviceState.power > 0)
        {
            device.sendEvent(name: "switch", value: "on")
			device.sendEvent(name: "speed", value: translateBondFanSpeedToHE(fan, state.fanProperties[fan].max_speed ?: 3, deviceState.speed))
        }
        else
        {
            device.sendEvent(name: "switch", value: "off")
			device.sendEvent(name: "speed", value: "off")
        }
        if (deviceLight)
        {
			if (deviceState.brightness != null)
			{
				if (deviceState.light == 0)
					deviceLight.sendEvent(name: "level", value: 0)
				else
					deviceLight.sendEvent(name: "level", value: deviceState.brightness)
			}
			else
			{
				if (deviceState.light > 0)
					deviceLight.sendEvent(name: "switch", value: "on")
				else
					deviceLight.sendEvent(name: "switch", value: "off")
			}
        }
		if (deviceUpLight)
		{
			if (deviceState.up_light_brightness != null)
			{
				if (deviceState.up_light == 0)
					deviceUpLight.sendEvent(name: "level", value: 0)
				else
					deviceUpLight.sendEvent(name: "level", value: deviceState.up_light_brightness)
			}
			else
			{
				if (deviceState.light > 0 && deviceState.up_light > 0)
					deviceUpLight.sendEvent(name: "switch", value: "on")
				else
					deviceUpLight.sendEvent(name: "switch", value: "off")
			}
		}
		if (deviceDownLight)
		{
			if (deviceState.down_light_brightness != null)
			{
				if (deviceState.down_light == 0)
					deviceDownLight.sendEvent(name: "level", value: 0)
				else
					deviceDownLight.sendEvent(name: "level", value: deviceState.down_light_brightness)
			}
			else
			{
				if (deviceState.light > 0 && deviceState.down_light > 0)
					deviceDownLight.sendEvent(name: "switch", value: "on")
				else
					deviceDownLight.sendEvent(name: "switch", value: "off")	
			}				
		}
		if (device.hasAttribute("direction"))
		{
			if (deviceState.direction == 1)
				device.sendEvent(name: "direction", value: "forward")
			else if (deviceState.direction == -1)
				device.sendEvent(name: "direction", value: "reverse")
		}
    }
    
	if (fireplaces != null)
	{
		for (def i = 0; i < fireplaces.size(); i++)
		{
			def deviceState = getState(fireplaces[i])
			def device = getChildDevice("bond:" + fireplaces[i])
			def deviceFan = device.getChildDevice("bond:" + fireplaces[i] + ":fan")
			def deviceLight = device.getChildDevice("bond:" + fireplaces[i] + ":light")
			
			if (deviceState.flame > 0 && deviceState.power > 0)
			{
				if (deviceState.flame <= 25)
					device.sendEvent(name: "flame", value: "low")
				else if (deviceState.flame <= 50)
					device.sendEvent(name: "flame", value: "medium")
				else
					device.sendEvent(name: "flame", value: "high")
			}
			else
			{
				device.sendEvent(name: "flame", value: "off")
			}
			
			if (deviceState.power > 0)
			{
				if (this.getProperty("fireplaceSensor${i}") == null)
				{
					device.sendEvent(name: "switch", value: "on")
				}
				if (deviceFan)
				{
					deviceFan.sendEvent(name: "speed", value: translateBondFanSpeedToHE(fireplaces[i], state.fireplaceProperties?.getAt(fireplaces[i])?.max_speed ?: 3, deviceState.fpfan_speed))
				}
				
				if (deviceLight)
				{
					if (deviceState.light == 1)
						deviceLight.sendEvent(name: "switch", value: "on")
					else
						deviceLight.sendEvent(name: "switch", value: "off")
				}
			}
			else 
			{
				if (this.getProperty("fireplaceSensor${i}") == null)
				{
					device.sendEvent(name: "switch", value: "off")
				}
				if (deviceFan)
				{
					deviceFan.sendEvent(name: "speed", value: "off")
				}
				if (deviceLight)
				{
					deviceLight.sendEvent(name: "switch", value: "off")
				}
			}
			
		}
	}
	
	if (shades != null)
	{
		for (shade in shades)
		{
			def deviceState = getState(shade)
			def device = getChildDevice("bond:" + shade)
			
			if (deviceState.open == 1)
			{
				device.sendEvent(name: "switch", value: "on")
				device.sendEvent(name: "windowShade", value: "open")
			}
			else
			{
				device.sendEvent(name: "switch", value: "off")
				device.sendEvent(name: "windowShade", value: "closed")
			}
		}
	}
	
	if (genericDevices != null)
	{
		for (generic in genericDevices)
		{
			def deviceState = getState(generic)
			def device = getChildDevice("bond:" + generic)
			
			if (deviceState.power > 0)
			{
				device.sendEvent(name: "switch", value: "on")
			}
			else
			{
				device.sendEvent(name: "switch", value: "off")
			}
		}
	}
}

def handleOn(device, bondId) {
	logDebug "Handling On event for ${bondId}"

    if (executeAction(bondId, "TurnOn") && shouldSendEvent(bondId))
    {
        device.sendEvent(name: "switch", value: "on")
    }
	
}

def handleLightOn(device, bondId) {
    logDebug "Handling Light On event for ${bondId}"
	if (device.deviceNetworkId.contains("uplight"))
	{
		if (executeAction(bondId, "TurnUpLightOn")) 
		{
			device.sendEvent(name: "switch", value: "on")
		}
	}
	else if (device.deviceNetworkId.contains("downlight"))
	{
		if (executeAction(bondId, "TurnDownLightOn")) 
		{
			device.sendEvent(name: "switch", value: "on")
		}
	}
    else
	{
        if (executeAction(bondId, "TurnLightOn")) 
		{
			device.sendEvent(name: "switch", value: "on")
		}
    }
}

def handleLightOff(device, bondId) {
    logDebug "Handling Light Off event for ${bondId}"   
	if (device.deviceNetworkId.contains("uplight"))
	{
		if (executeAction(bondId, "TurnUpLightOff")) 
		{
			device.sendEvent(name: "switch", value: "off")
		}
	}
	else if (device.deviceNetworkId.contains("downlight"))
	{
		if (executeAction(bondId, "TurnDownLightOff")) 
		{
			device.sendEvent(name: "switch", value: "off")
		}
	}	
    else
	{
        if (executeAction(bondId, "TurnLightOff")) 
		{
			device.sendEvent(name: "switch", value: "off")
		}
    }
}

def handleDim(device, bondId, duration)
{
	if (device.deviceNetworkId.contains("uplight"))
	{
		dimUsingTimer(device, bondId, duration, "StartUpLightDimmer")
	}
	else if (device.deviceNetworkId.contains("downlight"))
	{
		dimUsingTimer(device, bondId, duration, "StartDownLightDimmer")
	}
	else
	{
		dimUsingTimer(device, bondId, duration, "StartDimmer")
	}
}

def dimUsingTimer(device, bondId, duration, command)
{
	if (executeAction(bondId, command))
	{
		runInMillis((duration*1000).toInteger(), stopDimmer, [data: [device: device, bondId: bondId]])
	}
}

def stopDimmer(data)
{
	executeAction(data.bondId, "Stop")
}

def handleStartDimming(device, bondId)
{
	if (device.deviceNetworkId.contains("uplight"))
	{
		executeAction(bondId, "StartUpLightDimmer")
	}
	else if (device.deviceNetworkId.contains("downlight"))
	{
		executeAction(bondId, "StartDownLightDimmer")
	}
	else
	{
		executeAction(bondId, "StartDimmer")
	}
}

def handleStopDimming(device, bondId)
{
	executeAction(bondId, "Stop")
}

def handleLightLevel(device, bondId, level) {
	logDebug "Handling Light Level event for ${bondId}"
	if (device.deviceNetworkId.contains("uplight"))
	{
		if (executeAction(bondId, "SetUpLightBrightness", level)) 
		{
			device.sendEvent(name: "level", value: level)
		}
	}
	else if (device.deviceNetworkId.contains("downlight"))
	{
		if (executeAction(bondId, "SetDownLightBrightness", level)) 
		{
			device.sendEvent(name: "level", value: level)
		}
	}
    else 
	{
		if (executeAction(bondId, "SetBrightness", level)) 
		{
			device.sendEvent(name: "level", value: level)
		}
    }
}

def handleSetFlame(device, bondId, height)
{
	logDebug "Handling Flame event for ${bondId}"
	
	if (height == "off")
	{
		if (handleOff(device, bondId))
			device.sendEvent(name: "flame", value: "off")
	}
	else 
	{
		def flameHeight = 0
		if (height == "low")
			flameHeight = 1
		else if (height == "medium")
			flameHeight = 50
		else if (height == "high")
			flameHeight = 100
			
		if (executeAction(bondId, "SetFlame", flameHeight))
		{
			device.sendEvent(name: "flame", value: height)
		}
	}
}

def handleOpen(device, bondId)
{
	logDebug "Handling Open event for ${bondId}"
	
    if (executeAction(bondId, "Open")) 
    {
        device.sendEvent(name: "windowShade", value: "open")
    }
}

def handleClose(device, bondId)
{
	logDebug "Handling Close event for ${bondId}"
	
    if (executeAction(bondId, "Close")) 
    {
        device.sendEvent(name: "windowShade", value: "closed")
    }
}

def handleStop(device, bondId)
{
	logDebug "Handling Stop event for ${bondId}"
	
    executeAction(bondId, "Hold")
}

def fixPowerState(device, bondId, state) 
{
	logDebug "Setting power state for ${bondId} to ${state}"
	
	def power
	if (state == "on")
		power = 1
	else 
		power = 0

	if (executeFixState(bondId, '{"power": ' + power + '}'))
    {
		if (power == 1)
			device.sendEvent(name: "switch", value: "on")
		else
		{
			device.sendEvent(name: "switch", value: "off")
			if (device.hasAttribute("speed"))
				device.sendEvent(name: "speed", value: "off")
			if (device.hasAttribute("flame"))
				device.sendEvent(name: "flame", value: "off")
		}
    }
}

def fixFlameState(device, bondId, state) 
{
	logDebug "Setting flame state for ${bondId} to ${state}"
	
	def flameHeight = 0
	if (height == "low")
		flameHeight = 1
	else if (height == "medium")
		flameHeight = 50
	else if (height == "high")
		flameHeight = 100

	if (executeFixState(bondId, '{"flame": ' + flameHeight + '}'))
    {
		device.sendEvent(name: "flame", value: height)
    }
}

def fixFanSpeed(device, bondId, fanState) 
{
	def speed = translateHEFanSpeedToBond(bondId, state.fanProperties?.getAt(bondId)?.max_speed ?: 3, fanState)
	logDebug "Setting fan speed for ${bondId} to ${fanState}"

	if (fanState == "off") 
	{
		if (executeFixState(bondId, '{"power": 0}'))
		{
			device.sendEvent(name: "speed", value: "off")
		}
	}
	else 
	{
		if (executeFixState(bondId, '{"speed": ' + speed + '}'))
		{
			device.sendEvent(name: "speed", value: fanState)
		}
    }
}

def fixShadeState(device, bondId, state) 
{
	logDebug "Setting shade state for ${bondId} to ${state}"

	def open
	if (state == "open")
		open = 1
	else
		open = 0
		
	if (executeFixState(bondId, '{"open": ' + open + '}'))
    {
		if (open == 1)
		{
			device.sendEvent(name: "switch", value: "on")
			device.sendEvent(name: "windowShade", value: "open")
		}
		else
		{
			device.sendEvent(name: "switch", value: "off")
			device.sendEvent(name: "windowShade", value: "closed")
		}
    }
}

def fixDirection(device, bondId, state) 
{
	logDebug "Setting direction state for ${bondId} to ${state}"

	def direction
	if (state == "forward")
		direction = 1
	else
		direction = -1
		
	if (executeFixState(bondId, '{"direction": ' + direction + '}'))
    {
		if (direction == 1)
		{
			device.sendEvent(name: "direction", value: "forward")
		}
		else
		{
			device.sendEvent(name: "direction", value: "reverse")
		}
    }
}

def fixFPFanPower(device, bondId, state) 
{
	logDebug "Setting FP fan power state for ${bondId} to ${state}"

	def fppower
	if (state == "on")
		fppower = 1
	else
		fppower = 0
		
	if (executeFixState(bondId, '{"fpfan_power": ' + fppower + '}'))
    {
		if (fppower == 1)
		{
			device.sendEvent(name: "switch", value: "on")
			device.sendEvent(name: "speed", value: "on")
		}
		else
		{
			device.sendEvent(name: "switch", value: "off")
			device.sendEvent(name: "speed", value: "off")
		}
    }
}

def fixFPFanSpeed(device, bondId, fanState) 
{
	logDebug "Setting FP fan speed state for ${bondId} to ${fanState}"
	
	def speed = translateHEFanSpeedToBond(bondId, state.fireplaceProperties?.getAt(bondId)?.max_speed ?: 3, fanState)

	if (fanState == "off") 
	{
		if (executeFixState(bondId, '{"fpfan_power": 0}'))
		{
			device.sendEvent(name: "speed", value: "off")
			device.sendEvent(name: "switch", value: "off")
		}
	}
	else 
	{
		if (executeFixState(bondId, '{"fpfan_speed": ' + speed + '}'))
		{
			device.sendEvent(name: "speed", value: fanState)
			device.sendEvent(name: "switch", value: "on")
		}
    }
}

def fixLightPower(device, bondId, state) {
    logDebug "Setting light state for ${bondId} to ${state}"
	
	def power
	if (state == "on")
		power = 1
	else
		power = 0
		
	if (device.deviceNetworkId.contains("uplight"))
	{
		if (executeFixState(bondId, '{"up_light": ' + power + '}'))
		{
			device.sendEvent(name: "switch", value: state)
		}
	}
	else if (device.deviceNetworkId.contains("downlight"))
	{
		if (executeFixState(bondId, '{"down_light": ' + power + '}'))
		{
			device.sendEvent(name: "switch", value: state)
		}
	}
    else
	{
        if (executeFixState(bondId, '{"light": ' + power + '}'))
		{
			device.sendEvent(name: "switch", value: state)
		}
    }
}

def fixLightLevel(device, bondId, state) {
	logDebug "Setting light level for ${bondId} to ${state}"
	
	if (device.deviceNetworkId.contains("uplight"))
	{
		if (executeFixState(bondId, '{"up_light_brightness": ' + state + '}')) 
		{
			device.sendEvent(name: "level", value: state)
		}
		if (state == 0)
		{
			if (executeFixState(bondId, '{"up_light": 0}')) 
			{
				device.sendEvent(name: "switch", value: "off")
			}
		}
	}
	else if (device.deviceNetworkId.contains("downlight"))
	{
		if (executeFixState(bondId, '{"down_light_brightness": ' + state + '}'))
		{
			device.sendEvent(name: "level", value: state)
		}
		if (state == 0)
		{
			if (executeFixState(bondId, '{"down_light": 0}')) 
			{
				device.sendEvent(name: "switch", value: "off")
			}
		}
	}
    else 
	{
		if (executeFixState(bondId, '{"brightness": ' + state + '}'))
		{
			device.sendEvent(name: "level", value: state)
		}
		if (state == 0)
		{
			if (executeFixState(bondId, '{"light": 0}')) 
			{
				device.sendEvent(name: "switch", value: "off")
			}
		}
    }
}

def translateBondFanSpeedToHE(id, max_speeds, speed)
{
	def speedTranslations = 
	[
		10: [10: "high", 9: "high", 8: "medium-high", 7: "medium-high", 6: "medium", 5: "medium", 4: "medium-low", 3: "medium-low", 2: 1, 1: "low"],
		9: [9: "high", 8: "medium-high", 7: "medium-high", 6: "medium", 5: "medium", 4: "medium-low", 3: "medium-low", 2: "low", 1: "low"],
		8: [8: "high", 7: "medium-high", 6: "medium-high", 5: "medium", 4: "medium", 3: "medium-low", 2: "medium-low", 1: "low"],
		7: [7: "high", 6: "medium-high", 5: "medium", 4: "medium", 3: "medium-low", 2: "medium-low", 1: "low"],
		6: [6: "high", 5: "medium-high", 4: "medium", 3: "medium", 2: "medium-low", 1: "low"],
		5: [5: "high", 4: "medium-high", 3: "medium", 2: "medium-low", 1: "low"],
		4: [4: "high", 3: "medium", 2: "medium-low", 1: "low"],
		3: [3: "high", 2: "medium", 1: "low"],
		2: [2: "high", 1: "low" ]
	]
	
	if (!speed.toString().isNumber())
		return speed
		
	if (max_speeds > 10 || speed > max_speeds)
		return 0
		
	logDebug "${id} -> Translating ${speed}:${max_speeds} to HE ${speedTranslations[max_speeds][speed]}"
		
	return speedTranslations[max_speeds][speed]
}

def translateHEFanSpeedToBond(id, max_speeds, speed)
{
	if (speed.isNumber())
		return speed.toInteger()
		
		
	def speedTranslations =
	[
		10: ["high": 10, "medium-high": 8, "medium": 5, "medium-low": 3, "low": 1],
		9: ["high": 9, "medium-high": 7, ":medium": 5, "medium-low": 3, "low": 1],
		8: ["high": 8, "medium-high": 6, "medium": 4, "medium-low": 3, "low": 1],
		7: ["high": 7, "medium-high": 6, "medium": 4, "medium-low": 3, "low": 1 ],
		6: ["high": 6, "medium-high": 5, "medium": 3, "medium-low": 2, "low": 1],
		5: ["high": 5, "medium-high": 4, "medium": 3, "medium-low": 2, "low": 1],
		4: ["high": 4, "medium": 3, "medium-low": 2, "low": 1],
		3: ["high": 3, "medium": 2, "low": 1],
		2: ["high": 2, "low": 1]
	]
	
	if (max_speeds > 10)
		return null
		
	logDebug "${id} -> Translating ${speed}:${max_speeds} to BOND ${speedTranslations[max_speeds][speed]}"
		
	return speedTranslations[max_speeds][speed]
}

def handleFanSpeed(device, bondId, speed) {
    logDebug "Handling Fan Speed event for ${bondId}"

	if (speed == "off")
	{
		if (handleOff(device, bondId))
		{
			device.sendEvent(name: "speed", value: "off")
		}
	}	
	else if (speed == "on")
		handleOn(device, bondId)
    else
	{
        if (executeAction(bondId, "SetSpeed", translateHEFanSpeedToBond(bondId, state.fanProperties?.getAt(bondId)?.max_speed ?: 3, speed))) 
		{
			device.sendEvent(name: "switch", value: "on")
			device.sendEvent(name: "speed", value: speed)
		}
    }
}

def handleFPFanSpeed(device, bondId, speed) {
    logDebug "Handling Fireplace Fan Speed event for ${bondId}"

	if (speed == "off")	
		handleFPFanOff(device, bondId)
	else if (speed == "on")
		handleFPFanOn(device, bondId)
    else
	{
        if (executeAction(bondId, "SetSpeed", translateHEFanSpeedToBond(bondId, state.fireplaceProperties?.getAt(bondId)?.max_speed ?: 3, speed))) 
		{
			device.sendEvent(name: "speed", value: speed)
		}
    }
}

def handleFPFanOn(device, bondId) {
	logDebug "Handling Fan On event for ${bondId}"
	
    if (executeAction(bondId, "TurnFpFanOn")) 
    {
        device.sendEvent(name: "switch", value: "on")
        device.sendEvent(name: "speed", value: "on")
        return true
    }
	
	return false
}

def handleFPFanOff(device, bondId) {
	logDebug "Handling Fan Off event for ${bondId}"
	
	
    if (executeAction(bondId, "TurnFpFanOff")) 
    {
        device.sendEvent(name: "switch", value: "off")
        device.sendEvent(name: "speed", value: "off")
        return true
    }
	
	return false
}

def handleOff(device, bondId) {
	logDebug "Handling Off event for ${bondId}"

	
    if (executeAction(bondId, "TurnOff") && shouldSendEvent(bondId)) 
    {
        device.sendEvent(name: "switch", value: "off")
        if (device.hasCapability("FanControl"))
        device.sendEvent(name: "speed", value: "off")
        return true
    }
	
	return false
}

def handleDirection(device, bondId, direction)
{
	logDebug "Handling Direction event for ${bondId}"

    def bondDirection = 1
    if (direction == "reverse")
    bondDirection = -1
    if (executeAction(bondId, "SetDirection", bondDirection)) 
    {
        device.sendEvent(name: "direction", value: direction)
    }
    
}

def getState(bondId) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${bondId}/state",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	def stateToReturn = null
	try
	{
		httpGet(params) { resp ->
			if (checkHttpResponse("getState", resp))
				stateToReturn = resp.data
		}
	}
	catch (org.apache.http.conn.ConnectTimeoutException e)
	{
		log.error "getState: connection to BOND hub appears to be down. Check if the IP is correct."
	}
	catch (Exception e)
	{
		checkHttpResponse("getState", e.getResponse())
	}
	return stateToReturn
}

def hasAction(bondId, commandType) {
	logDebug "searching for ${commandType} for ${bondId}"
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${bondId}/actions",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	def commandToReturn = false
	try
	{
		httpGet(params) { resp ->
			if (checkHttpResponse("hasAction", resp))
			{
				for (commandId in resp.data) {
					if (commandId.key == "_")
						continue
					if (commandId.key == commandType) {
						logDebug "found command ${commandId.key} for ${bondId}"
						commandToReturn = true
						break
					}
				}
			}
		}
	}
	catch (org.apache.http.conn.ConnectTimeoutException e)
	{
		log.error "hasAction: connection to BOND hub appears to be down. Check if the IP is correct."
	}
	catch (Exception e)
	{
		checkHttpResponse("hasAction", e.getResponse())
	}
	return commandToReturn
}

def executeAction(bondId, action) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${bondId}/actions/${action}",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ],
		body: "{}"
	]
	def isSuccessful = false
	logDebug "${bondId} -> calling action ${action}"
	try
	{
		httpPut(params) { resp ->
			isSuccessful = checkHttpResponse("executeAction", resp)
		}
	}
	catch (org.apache.http.conn.ConnectTimeoutException e)
	{
		log.error "executeAction: connection to BOND hub appears to be down. Check if the IP is correct."
	}
	catch (Exception e)
	{
		checkHttpResponse("executeAction", e.getResponse())
	}
	return isSuccessful
}

def executeAction(bondId, action, argument) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${bondId}/actions/${action}",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ],
		body: '{"argument": ' + argument +'}'
	]
	def isSuccessful = false
	logDebug "calling action ${action} ${params.body}"
	try
	{
		httpPut(params) { resp ->
			isSuccessful = checkHttpResponse("executeAction", resp)
		}
	}
	catch (org.apache.http.conn.ConnectTimeoutException e)
	{
		log.error "executeAction: connection to BOND hub appears to be down. Check if the IP is correct."
	}
	catch (Exception e) 
	{
		checkHttpResponse("executeAction", e.getResponse())
	}
	return isSuccessful
}

def executeFixState(bondId, body) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${bondId}/state",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ],
		body: body
	]
	def isSuccessful = false
	logDebug "calling fix state ${params.body}"
	try
	{
		httpPatch(params) { resp ->
			isSuccessful = checkHttpResponse("executeFixState", resp)
		}
	}
	catch (org.apache.http.conn.ConnectTimeoutException e)
	{
		log.error "executeFixState: connection to BOND hub appears to be down. Check if the IP is correct."
	}
	catch (Exception e) 
	{
		checkHttpResponse("executeFixState", e.getResponse())
	}
	return isSuccessful
}

def shouldSendEvent(bondId) {
	for (fan in fans) 
	{
		if (fan == bondId)
			return true;
	}
	
	if (fireplaces != null)
	{
		for (def i = 0; i < fireplaces.size(); i++)
		{
			if (fireplaces[i] == bondId)
			{
				if (this.getProperty("fireplaceSensor${i}") != null)
					return false;
				return true;
			}
		}
	}
	return true;
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}

def checkHttpResponse(action, resp) {
	if (resp.status == 200 || resp.status == 201 || resp.status == 204)
		return true
	else if (resp.status == 400 || resp.status == 401 || resp.status == 404 || resp.status == 409 || resp.status == 500)
	{
		log.error "${action}: ${resp.getData()}"
		return false
	}
	else
	{
		log.error "${action}: unexpected HTTP response: ${resp.status}"
		return false
	}
}

def displayFooter(){
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center'>BOND Home Integration<br><a href='https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7LBRPJRLJSDDN&source=url' target='_blank'><img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo'></a><br><br>Please consider donating. This app took a lot of work to make.<br>If you find it valuable, I'd certainly appreciate it!</div>"
	}       
}

def getFormat(type, myText=""){			// Modified from @Stephack Code   
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}