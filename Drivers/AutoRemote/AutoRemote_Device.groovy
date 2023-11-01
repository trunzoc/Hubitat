/**
*  AutoRemote API Device
*
*
*  Copyright 2019-2023 Craig Trunzo
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
* This driver uses sections of code derived from the original Join Driver that @stephack wrote. Thanks for you contributions.
*
*	12/10/2019 	- Initial Release
*	04/29/2020	- Added message prefix
*			- Added URLEncode
*   07/06/2021 - Added Notification Capabilities
*
*/

def version() {"v1.0.20231027"}

preferences {
	input("personalkey", "text", title: "AutoRemote Personal Key: <small><a href='https://joaoapps.com/autoremote/personal/' target='_blank'>[AutoRemote docs here]</a></small>", required: true, description: "")
	input("sender", "text", title: "Sender (optional)", required: false, description: "Optional text to send as the 'Sender' for this device.")
	input("messageprefix", "text", title: "Message Prefix (optional)", required: false, description: "Adds '{Message Prefix}=:=' before the text to be send.")
    input("logEnable", "bool", title: "Enable Debug Logging?:", required: true)
}

metadata {
  	definition (name: "AutoRemote Device", namespace: "trunzoc", author: "Craig Trunzo", importUrl: "https://raw.githubusercontent.com/trunzoc/Hubitat/master/Drivers/AutoRemote/AutoRemote_Device.groovy") {
     	capability "Notification"
   	    capability "Actuator"
        
        attribute "LastMessageDate", "string"
  	}
}

def installed() {
 	initialize()
}

def updated() {
 	initialize()   
}

def initialize() {
    state.version = version()
}

def deviceNotification(message) {
    def finalMessage = "${message}"
    
    if(logEnable) log.debug "messageprefix: " + "${messageprefix}"
    if(logEnable) log.debug "message: " + "${message}"
    
    def messagePref = "${messageprefix}"
    
    if(messagePref?.trim()){
        finalMessage = "${messageprefix}" + "=:=" + "${message}"
    }
    if(logEnable) log.debug "final message: " + finalMessage
 
	def encodedMessage = java.net.URLEncoder.encode(finalMessage)
    
    if(logEnable) log.debug "urlencoded message: " + "${encodedMessage}"

    def params = [
        uri: "https://autoremotejoaomgcd.appspot.com/sendmessage?key=${personalkey}&sender=${sender}&message=" + encodedMessage,
    ]
	
	if(logEnable) log.debug "Text params: ${params}"
  	
    asynchttpPost('myPostResponse', params)
}

def myPostResponse(response,data){
	if(response.status != 200) {
        state.LastResult = "failed"
		log.error "Received HTTP error ${response.status}. Check your keys!"
        if(response.hasError()) {
            log.warn(response.getErrorMessage())
        }
	}
    else {
        state.LastResult = "success"
    	if(logEnable) log.debug "Message Received by AutoRemote Server"
        def dateNow = new Date()
		dateNow = dateNow.format("yyyy-MM-dd HH:mm:ss") 
        sendEvent(name: "LastMessageDate", value: dateNow)
    }
}
