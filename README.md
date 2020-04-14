# hubitat-bond
BOND Home Integration for Hubitat. This currently supports fireplaces, fans, motorized shades, and generic devices.
 
## Fans
Fans currently support on/off, speed settings, direction, and lights (both a single light and up/down lights) as well as dimmable lights. Please note that some fans do not support setting dimmer levels for their lights. These are usually lights where you have to hold the button to dim. The app will recognize these as a "BOND Fan Timer Light" which has custom commands that allow you to start and stop dimming but do not implement the true capability to be detected as a dimmer. This is because there is no way to set these devices to a specific dim level.
 
## Fireplaces
Fireplaces currently support on/off (which can also be optionally monitored by a power meter), flame height adjustment, fan speed settings, and a light.

## Shades
Shades support open/closed.

## Generic Devices
Generic devices support on/off.

## Devices
You must install all of the device drivers for the integration to work properly.
* BOND Fan
* BOND Fan Light
* BOND Fan Dimmable Light
* BOND Fan Timer Light
* BOND Fan With Direction
* BOND Fireplace
* BOND Fireplace Fan
* BOND Fireplace Light
* BOND Motorized Shade
* BOND Generic Device

## Apps
The BOND Home Integration app is what actually communicates with the BOND Hub device. This relies on the BOND Home Local API which is only available in the v2 BOND Home firmware.

### Configuration
To connect to the API you will need to specify the IP address of your BOND Hub and the access token. If you have the v2 app you can obtain the token by going to the Hub device in the app, then copy the _Local token_ value.

You will see the list of devices available from your hub on the next screen. Select the devices you wish to integrate.

If you chose any fireplaces and you have power meters setup, you will optionally be able to associate a power meter with the fireplace. This can be used to detect on/off status more reliably than BOND can (IR/RF aren't always the most reliable). You can then set a mimimum threshold for the power meter to detect that it is on.

## Limitations
* Only supports a single BOND hub per app installation

## Donations
If you find this app useful, please consider making a [donation](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=7LBRPJRLJSDDN&source=url)! 

## Revision History
* 2019.12.01 - Fixed an issue where dimmers wouldn't work with fans that support direction controls, fixed an issue setting flame height
* 2019.11.24 - Added support for timer based fan light dimmers and flame height adjustment for fireplaces
* 2019.12.14 - Added support for Switch capability to the motorized shades for compatibility
* 2020.01.02 - Fixed an issue where fan speed wouldn't be set properly (thanks jchurch for the troubleshooting!)
* 2020.02.01 - Fixed an issue where looking for devices was incorrect which broke Smart By BOND devices (thanks mcneillk for the fix!)
* 2020.03.23 - Added the ability to fix device state when it's out of sync (thanks stephen_nutt for the suggestion)
* 2020.04.13 - Added a stop command to motorized shades to stop an open/close at the current position (suggested by jchurch)