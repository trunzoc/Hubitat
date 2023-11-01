
/**
*   
*   File: HTTPPOSTer.groovy
*   Platform: Hubitat
*   Modification History:
*       Date       Who                   What
*       2023-04-14 Craig Trunzo          Built it
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
def version() {"v1.0.20230416"}

metadata {
    definition (name: "HTTP POSTer", namespace: "trunzoc", author: "Craig Trunzo", importUrl: "https://raw.githubusercontent.com/trunzoc/Hubitat/master/Drivers/HTTPPOSTer/HTTPPOSTer.groovy") {
        capability "Notification"
        capability "Actuator"
    }
    
    preferences {
        input(name: "deviceIP", type: "string", title:"Device IP/HOST Address", description: "Enter IP or Host Address of your HTTP server", required: true, displayDuringSetup: true)
        input(name: "devicePort", type: "string", title:"Device Port", description: "Enter Port of your HTTP server (defaults to 80)", defaultValue: "80", required: false, displayDuringSetup: true)
        input(name: "devicePath", type: "string", title:"URL Path", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
        input(name: "sourceName", type: "string", title:"Source Name", description: "The name of the source of the data being sent.", displayDuringSetup: true)
        input(name: "deviceContent", type: "enum", title: "Content-Type", options: getCtype(), defaultValue: "application/x-www-form-urlencoded", required: true, displayDuringSetup: true)
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true, required: false
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def installed() {
    initialize()
}

def updated() {
    initialize()
    
    if (logEnable) {
        log.info "Enabling Debug Logging for 30 minutes" 
        runIn(1800,logsOff)
    } else {
        unschedule(logsoff)
    }
}

def initialize() {
    state.version = version()
}

def deviceNotification(message) {
    def encodedMessage = java.net.URLEncoder.encode(message)
    
	def localDevicePort = (devicePort==null) ? "80" : devicePort
    def uriDest = "${deviceIP}:${localDevicePort}"
 	def path = devicePath //+ encodedMessage
    logDebug("Sending to: [${uriDest}${path}]")

    // Define the initial postBody keys and values for all messages
    def postBody = [
        message: "${message}",
        Sourcename: "${sourceName}"
    ]  // this will get converted to a JSON

    logDebug("Sending Body: [${postBody}]")

	def params = [
      uri:  uriDest,
      contentType: deviceContent,
      path: path,
      body: postBody,  
      //timeout: 15
	]
	asynchttpPost("myPostResponse", params)
}

def myPostResponse(response,data){
	if(response.status != 200) {
		log.error "Received HTTP error ${response.status}."
        if(response.hasError()) {
            log.warn(response.getErrorMessage())
        }
	}
    else {
        log.info "Response Received; [${response.json}]"
    }
}

private logDebug(msg) {
    if (logEnable == true) {
        if (msg instanceof List && msg.size() > 0) {
            msg = msg.join(", ");
        }
        log.debug "$msg"
    }
}

def getCtype() {
    def cType = []
    cType = ["application/x-www-form-urlencoded","application/json"]
}
