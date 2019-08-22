/**
 *  BOND Home Integration
 *
 *  Copyright 2019 Dominick Meglio
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
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


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
	}
}

def prefListDevices() {
	getDevices();
	return dynamicPage(name: "prefListDevices", title: "Devices", nextPage: "prefPowerSensors", install: false, uninstall: false) {
		section("Devices") {
			if (state.fireplaceList.size() > 0)
				input(name: "fireplaces", type: "enum", title: "Fireplaces", required:false, multiple:true, options:state.fireplaceList, hideWhenEmpty: true)
			if (state.fanList.size() > 0)
				input(name: "fans", type: "enum", title: "Fans", required:false, multiple:true, options:state.fanList, hideWhenEmpty: true)
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
	state.deviceList = [:]
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	try
	{
		httpGet(params) { resp ->
			log.debug resp.data
			for (deviceid in resp.data) {
				if (deviceid.key == "_")
					continue
				getDeviceById(deviceid);
			}
		}
	}
	catch (e)
	{
		log.debug "HTTP Exception Received on GET: $e"
	}
}

def getDeviceById(id) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${id}",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	try
	{
		httpGet(params) { resp ->
			log.debug resp.data
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
		}
	}
	catch (e)
	{
		log.debug "HTTP Exception Received on GET: $e"
	}
}

def getDeviceProperties(id) {
	def params = [
		uri: "http://${hubIp}",
		path: "/v2/devices/${id}/properties",
		contentType: "application/json",
		headers: [ 'BOND-Token': hubToken ]
	]
	def result = null
	try
	{
		httpGet(params) { resp ->
			result = resp.data
		}
	}
	catch (e)
	{
		log.debug "HTTP Exception Received on GET: $e"
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
				fanDevice = addChildDevice("bond", "BOND Fan", "bond:" + fan, 1234, ["name": state.fanList[fan], isComponent: false])
			}
			if (state.fanDetails[fan].contains("TurnUpLightOn") && state.fanDetails[fan].contains("TurnDownLightOn"))
			{
				if (!fanDevice.getChildDevice("bond:" + fan + ":uplight"))
					fanDevice.addChildDevice("bond", "BOND Fan Light", "bond:" + fan + ":uplight", ["name": state.fanList[fan] + " Up Light", isComponent: true])
				if (!fanDevice.getChildDevice("bond:" + fan + ":downlight"))
					fanDevice.addChildDevice("bond", "BOND Fan Light", "bond:" + fan + ":downlight", ["name": state.fanList[fan] + " Down Light", isComponent: true])
			}
			else if (state.fanDetails[fan].contains("TurnLightOn"))
			{
				if (!fanDevice.getChildDevice("bond:" + fan + ":light"))
					fanDevice.addChildDevice("bond", "BOND Fan Light", "bond:" + fan + ":light", ["name": state.fanList[fan] + " Light", isComponent: true])
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
        def state = getState(fan)
        def device = getChildDevice("bond:" + fan)
        def deviceLight = device.getChildDevice("bond:" + fan + ":light")
		def deviceUpLight = device.getChildDevice("bond:" + fan + ":uplight")
		def deviceDownLight = device.getChildDevice("bond:" + fan + ":downlight")
        if (state.power > 0)
        {
            device.sendEvent(name: "switch", value: "on")
			device.sendEvent(name: "speed", value: translateBondFanSpeedToHE(state.fanProperties[fan].max_speed ?: 3, state.speed))
        }
        else
        {
            device.sendEvent(name: "switch", value: "off")
			device.sendEvent(name: "speed", value: "off")
        }
        if (deviceLight)
        {
            if (state.light > 0)
                deviceLight.sendEvent(name: "switch", value: "on")
            else
                deviceLight.sendEvent(name: "switch", value: "off")
        }
		if (deviceUpLight)
		{
			if (state.light > 0 && state.up_light > 0)
				deviceUpLight.sendEvent(name: "switch", value: "on")
            else
                deviceUpLight.sendEvent(name: "switch", value: "off")
		}
		if (deviceDownLight)
		{
			if (state.light > 0 && state.down_light > 0)
				deviceDownLight.sendEvent(name: "switch", value: "on")
            else
                deviceDownLight.sendEvent(name: "switch", value: "off")		
		}
    }
    
	if (fireplaces != null)
	{
		for (def i = 0; i < fireplaces.size(); i++)
		{
			def state = getState(fireplaces[i])
			def device = getChildDevice("bond:" + fireplaces[i])
			def deviceFan = device.getChildDevice("bond:" + fireplaces[i] + ":fan")
			def deviceLight = device.getChildDevice("bond:" + fireplaces[i] + ":light")
			
			if (state.power > 0)
			{
				if (this.getProperty("fireplaceSensor${i}") == null)
				{
					device.sendEvent(name: "switch", value: "on")
				}
				if (deviceFan)
				{
					deviceFan.sendEvent(name: "speed", value: translateBondFanSpeedToHE(state.fireplaceProperties[fireplaces[i]].max_speed ?: 3, state.fpfan_speed))
				}
				
				if (deviceLight)
				{
					if (state.light == 1)
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
}

def handleOn(device, bondId) {
	logDebug "Handling On event for ${bondId}"
	if (hasAction(bondId, "TurnOn")) 
	{
		if (executeAction(bondId, "TurnOn") && shouldSendEvent(bondId))
		{
			device.sendEvent(name: "switch", value: "on")
		}
	}
}

def handleLightOn(device, bondId) {
    logDebug "Handling Light On event for ${bondId}"
	if (device.deviceNetworkId.contains("uplight") && hasAction(bondId, "TurnUpLightOn"))
	{
	        if (executeAction(bondId, "TurnUpLightOn")) 
		{
			device.sendEvent(name: "switch", value: "on")
		}
	}
	else if (device.deviceNetworkId.contains("downlight") && hasAction(bondId, "TurnDownLightOn"))
	{
	        if (executeAction(bondId, "TurnDownLightOn")) 
		{
			device.sendEvent(name: "switch", value: "on")
		}
	}
    else if (hasAction(bondId, "TurnLightOn")) 
	{
        if (executeAction(bondId, "TurnLightOn")) 
		{
			device.sendEvent(name: "switch", value: "on")
		}
    }
}

def handleLightOff(device, bondId) {
    logDebug "Handling Light Off event for ${bondId}"   
	if (device.deviceNetworkId.contains("uplight") && hasAction(bondId, "TurnUpLightOff"))
	{
	        if (executeAction(bondId, "TurnUpLightOff")) 
		{
			device.sendEvent(name: "switch", value: "off")
		}
	}
	else if (device.deviceNetworkId.contains("downlight") && hasAction(bondId, "TurnDownLightOff"))
	{
	        if (executeAction(bondId, "TurnDownLightOff")) 
		{
			device.sendEvent(name: "switch", value: "off")
		}
	}	
    else if (hasAction(bondId, "TurnLightOff")) 
	{
        if (executeAction(bondId, "TurnLightOff")) 
		{
			device.sendEvent(name: "switch", value: "off")
		}
    }
}

def translateBondFanSpeedToHE(max_speeds, speed)
{
	if (!speed.isNumber())
		return speed
		
	// So the array indices match
	speed--
	
	def twoSpeeds = ["low", "high"]
	def threeSpeeds = ["low", "medium", "high"]
	def fourSpeeds = ["low", "medium-low", "medium", "high"]
	def fiveSpeeds = ["low", "medium-low", "medium", "medium-high", "high"]
	
	if (max_speeds == 2 && speed < 2)
		return twoSpeeds[speed]
	else if (max_speeds == 3 && speed < 3)
		return threeSpeeds[speed]	
	else if (max_speeds == 4 && speed < 4)
		return fourSpeeds[speed]	
	else if (max_speeds == 5 && speed < 5)
		return fiveSpeeds[speed]
		
	return 0
}

def translateHEFanSpeedToBond(max_speeds, speed)
{
	if (speed.isNumber())
		return speed.toInteger()
		
	def twoSpeeds = ["low", "high"]
	def threeSpeeds = ["low", "medium", "high"]
	def fourSpeeds = ["low", "medium-low", "medium", "high"]
	def fiveSpeeds = ["low", "medium-low", "medium", "medium-high", "high"]
	
	if (max_speeds == 2)
		return twoSpeeds.findIndexOf { it == speed }+1
	else if (max_speeds == 3)
		return threeSpeeds.findIndexOf { it == speed }+1
	else if (max_speeds == 4)
		return fourSpeeds.findIndexOf { it == speed }+1
	else if (max_speeds == 5)
		return fiveSpeeds.findIndexOf { it == speed }+1
}

def handleFanSpeed(device, bondId, speed) {
    logDebug "Handling Fan Speed event for ${bondId}"

	if (speed == "off")
	{
		if (handleOff(device, bondId))
			device.sendEvent(name: "speed", value: "off")
	}	
	else if (speed == "on")
		handleOn(device, bondId)
    else if (hasAction(bondId, "SetSpeed")) 
	{
        if (executeAction(bondId, "SetSpeed", translateHEFanSpeedToBond(state.fanProperties?.getAt(bondId)?.max_speed ?: 3, speed))) 
		{
			device.sendEvent(name: "switch", value: "on")
			device.sendEvent(name: "speed", value: speed)
		}
    }
}

def handleFPFanSpeed(device, bondId, speed) {
    logDebug "Handling Fireplace Fan Speed event for ${bondId}"

	if (speed == "off")	
		handleOff(device, bondId)
	else if (speed == "on")
		handleOn(device, bondId)
    else if (hasAction(bondId, "SetSpeed")) 
	{
        if (executeAction(bondId, "SetSpeed", translateHEFanSpeedToBond(state.fireplaceProperties?.getAt(bondId)?.max_speed ?: 3, speed))) 
		{
			device.sendEvent(name: "speed", value: speed)
		}
    }
}

def handleOff(device, bondId) {
	logDebug "Handling Off event for ${bondId}"

	if (hasAction(bondId, "TurnOff")) 
	{
		if (executeAction(bondId, "TurnOff") && shouldSendEvent(bondId)) 
		{
			device.sendEvent(name: "switch", value: "off")
			if (device.hasCapability("FanControl"))
				device.sendEvent(name: "speed", value: "off")
			return true
		}
	}
	return false
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
            stateToReturn = resp.data
		}
	}
	catch (e)
	{
		log.debug "HTTP Exception Received on GET: $e"
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
	catch (e)
	{
		log.debug "HTTP Exception Received on GET: $e"
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
	logDebug "calling action ${action}"
	try
	{
		httpPut(params) { resp ->
			isSuccessful = (resp.status == 204)
		}
	}
	catch (e) 
	{
		log.debug "HTTP Exception Received on PUT: $e"
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
			isSuccessful = (resp.status == 204)
		}
	}
	catch (e) 
	{
		log.debug "HTTP Exception Received on PUT: $e"
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