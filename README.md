# hubitat-bond
BOND Home Integration for Hubitat. This currently supports both fireplaces and fans but fans are not tested.
 
## Fans
Fans currently support on/off, speed settings, and a single light. Fans have currently not been tested.
 
## Fireplaces
Fireplaces currently support on/off (which can also be optionally monitored by a power meter), fan speed settings, and a light.

## Devices
You must install all of the device drivers for the integration to work properly.
* BOND Fan
* BOND Fan Light
* BOND Fireplace
* BOND Fireplace Fan
* BOND Fireplace Light

## Apps
The BOND Home Integration app is what actually communicates with the BOND Hub device. This relies on the BOND Home Local API which is only available in the v2 BOND Home firmware which is currently in beta.

### Configuration
To connect to the API you will need to specify the IP address of your BOND Hub and the access token. If you have the v2 app (currently in beta) you can obtain the token by going to the Hub device in the app, then copy the _Local token_ value.

You will see the list of devices available from your hub on the next screen. Select the devices you wish to integrate.

If you chose any fireplaces and you have power meters setup, you will optionally be able to associate a power meter with the fireplace. This can be used to detect on/off status more reliably than BOND can (IR/RF aren't always the most reliable). You can then set a mimimum threshold for the power meter to detect that it is on.
