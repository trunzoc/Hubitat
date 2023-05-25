/**
 *  ****************  Anycubic Photon Mono X Poller  ****************
*   
*   File: Anycubic_Photon_MonoX_Poller.groovy
*   Platform: Hubitat
*   Modification History:
*       Date       Who                   What
*       2023-05-01 Craig Trunzo          Built it
*       2023-05-09 Craig Trunzo          Added "sysinfo
*       2023-05-11 Craig Trunzo          Changed numeric attributes to strings
*       2023-05-12 Craig Trunzo          Fixed stuff
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*
*/
import java.util.concurrent.TimeUnit

def version() {"v1.0.20230512"}


metadata {
	definition (name: "Anycubic Photon Mono X Poller", namespace: "trunzoc", author: "Craig Trunzo", importUrl: "https://raw.githubusercontent.com/trunzoc/Hubitat/master/Drivers/Anycubic_Photon_MonoX_Poller/Anycubic_Photon_MonoX_Poller.groovy") {
	capability "Contact Sensor"
        capability "PresenceSensor"

    	command "GetStatus"
    	command "SystemInfo"

        attribute "CurrentStatus", "string"
        attribute "CurrentFile", "string"
        attribute "LastStatusDate", "string"
	attribute "LayerHeight", "string"
        attribute "Layers_Total", "string"
        attribute "Layers_Current", "string"
        attribute "PercentComplete", "string"
	attribute "Time_Elapsed", "string"
	attribute "Time_Remaining", "string"
        attribute "TotalVolume", "string"
}
    
preferences() {    	
        section(""){
            input "ipaddress", "text", required: true, title: "IP/Host", defaultValue: "0.0.0.0"
            input "delayCheckIdle", "number", title:"Number of seconds between checking printer while idle", description: "", required: true, displayDuringSetup: true, defaultValue: "300"
            input "delayCheckPrinting", "number", title:"Number of seconds between checking printer while printing", description: "", required: true, displayDuringSetup: true, defaultValue: "60"
            input "logEnable", "bool", title: "Enable logging", required: true, defaultValue: true
        }
    }
}

def installed () {
	log.info "${device.displayName}.installed()"
    updated()
}


def updated () {
	log.info "${device.displayName}.updated()"
    runIn(2, SystemInfo)

    pauseExecution(5000)

    runEvery1Minute(GetStatus)
    runIn(2, GetStatus)
}

def sendMsg(msg) {
	try {
        def dateNow = new Date()
		dateNow = dateNow.format("yyyy-MM-dd HH:mm:ss") 
        state.LastAttempt = dateNow

        if(logEnable) log.debug "Opening connection"
		sendEvent([name: "Connection", value: "Opening"])
		
        interfaces.rawSocket.connect("${ipaddress}", 6000)

		//give it a chance to start
		//pauseExecution(1000)
		if(logEnable) log.debug "Connection established"
		sendEvent([name: "Connection", value: "Connection established"])

		//pauseExecution(1000)
        if(logEnable) log.debug "Sending Message: ${msg}"
        interfaces.rawSocket.sendMessage("${msg}")        
		sendEvent([name: "Connection", value: "Message Sent"])
        sendEvent(name: "presence", value: "present")
        
    } catch(e) {
		if(logEnable) 
		{
			log.debug "Initialize Error: ${e.message}"
			log.debug "(${ipaddress}, 6000)"
		}
		sendEvent([name: "Connection", value: "Connection Failed"])
        sendEvent(name: "presence", value: "not present")
        state.LastResult = "Connection Failed: ${e.message}"
    }

}

def GetStatus() {
    sendMsg("getstatus")
}

def SystemInfo() {
    sendMsg("sysinfo")
}
def parse(String msg) {
	try {
		sendEvent([name: "Connection", value: "Response Received"])

        byte[] bytes = hubitat.helper.HexUtils.hexStringToByteArray(msg)
        def newstring = new String(bytes)

        def dateNow = new Date()
		dateNow = dateNow.format("yyyy-MM-dd HH:mm:ss") 
        
		if(logEnable) log.debug "Response: ${newstring}"
        
        String[] strResults;
        strResults = newstring.split(',');
        
        if (strResults[0] == "getstatus") {
            if (strResults[1] == "print") {
                state.IsPrintng = true
                sendEvent(name: "contact", value: "open")
                sendEvent(name: "LastStatusDate", value: dateNow)
                sendEvent(name: "CurrentStatus", value: strResults[1])
                sendEvent(name: "CurrentFile", value: strResults[2].split('/')[0])
                sendEvent(name: "Layers_Total", value: strResults[3])
                sendEvent(name: "Layers_Current", value: strResults[5])
                sendEvent(name: "PercentComplete", value: "${strResults[4]}%")
                sendEvent(name: "Time_Elapsed", value: SecondsToTime(strResults[6].toInteger()))
                sendEvent(name: "Time_Remaining", value: SecondsToTime(strResults[7].toInteger()))
                sendEvent(name: "TotalVolume", value: strResults[8])
                sendEvent(name: "LayerHeight", value: strResults[11])
            } else {
                state.IsPrintng = false
                sendEvent(name: "contact", value: "close")
                sendEvent(name: "LastStatusDate", value: dateNow)
                sendEvent(name: "CurrentStatus", value: "Not Printing")
            
                sendEvent(name: "CurrentFile", value: "null")
                sendEvent(name: "Layers_Total", value: "null")
                sendEvent(name: "Layers_Current", value: "null")
                sendEvent(name: "PercentComplete", value: "null")
                sendEvent(name: "Time_Elapsed", value: "null")
                sendEvent(name: "Time_Remaining", value: "null")
                sendEvent(name: "TotalVolume", value: "null")
                sendEvent(name: "LayerHeight", value: "null")
            }
        }
		else if (strResults[0] == "sysinfo") {
			state.Printer_Model = strResults[1]
			state.Firmware_Version = strResults[2]
			state.Serial_No = strResults[3]
			state.WiFi_SSID = strResults[4]
		}
        state.LastResult = "success"

    } catch(e) {
		log.debug "parse error: ${e}"
	}
    
    //pauseExecution(1000)
    sendEvent([name: "Connection", value: "Closing"])
    interfaces.rawSocket.close()

    //pauseExecution(1000)
    if(logEnable) log.debug "Connection Closed"
    sendEvent([name: "Connection", value: "Closed"])

    //SET DELAY FOR NEXT CHECK
    if(state.isPrinting){
		if (autoUpdate) runIn(delayCheckPrinting.toInteger(), CheckPrinter)
	} else {
		if (autoUpdate) runIn(delayCheckIdle.toInteger(), CheckPrinter)
	}
}

def SecondsToTime(secondsToConvert) {
    long millis = secondsToConvert * 1000;
    long hours = TimeUnit.MILLISECONDS.toHours(millis);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1);

    String timestamp = String.format("%02d H, %02d M, %02d S", Math.abs(hours), Math.abs(minutes), Math.abs(seconds));
    return timestamp
}
