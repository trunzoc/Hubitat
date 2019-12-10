/**
*  AutoRemote API Device
*
*
*  Copyright 2019 Craig Trunzo
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
* This driver uses sections of code derived from the original Pushover Driver that @ogiewon and I worked on. Thanks for you contributions Dan.
*
*	12/10/19 	- Initial Release
*
*
*/

def version() {"v1.0.0"}

preferences {
	input("personalkey", "text", title: "AutoRemote Personal Key: <small><a href='https://joaoapps.com/autoremote/personal/' target='_blank'>[AutoRemote docs here]</a></small>", required: true, description: "")
	input("sender", "text", title: "Sender (optional)", required: false, description: "Optional text to send as the 'Sender' for this device.")
    input("logEnable", "bool", title: "Enable Debug Logging?:", required: true)
}

metadata {
  	definition (name: "AutoRemote Device", namespace: "trunzoc", author: "Craig Trunzo", importUrl: "") {
    	capability "Actuator"
        
        command "sendMessage", ["Text*"]
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

def sendMessage(message) {
    def params = [
        uri: "https://autoremotejoaomgcd.appspot.com/sendmessage?key=${personalkey}&sender=${sender}&message=${message}",
    ]
	
	if(logEnable) log.debug "Text params: ${params}"
  	
    //if ((personalkey =~ /[A-Za-z0-9]{30}/)) {
    	asynchttpPost('myPostResponse', params)
  	//}
  	//else {
    //	log.error "Personal key '${personalkey}' is not properly formatted!"
    //}
}

def myPostResponse(response,data){
	if(response.status != 200) {
		log.error "Received HTTP error ${response.status}. Check your keys!"
	}
    else {
    	if(logEnable) log.debug "Message Received by AutoRemote Server"
    }
}
