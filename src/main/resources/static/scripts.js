import uitoolkit from './@zoom/videosdk-ui-toolkit/index.js'

const urlParams = new URLSearchParams(window.location.search);
console.log("passcode: " + atob(urlParams.get("passcode")));
var sessionContainer = document.getElementById('sessionContainer')
//var authEndpoint = 'http://localhost:4000'
var authEndpoint = 'http://localhost:8080/jwt'
var config = {
    videoSDKJWT: '',
    sessionName: atob(urlParams.get("sessionName")),
    userName: 'Lars',
    sessionPasscode: atob(urlParams.get("passcode")),
    features: ['video', 'audio', 'settings', 'users', 'chat', 'share']
};
var role = 1

window.getVideoSDKJWT = getVideoSDKJWT

function getVideoSDKJWT() {

    fetch(authEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sessionName:  config.sessionName,
            role: role,
        })
    }).then((response) => {
        return response.json()
    }).then((data) => {
        if(data.signature) {
            console.log(data.signature)
            config.videoSDKJWT = data.signature
            joinSession()
        } else {
            console.log(data)
        }
    }).catch((error) => {
        console.log(error)
    })
}

function joinSession() {
    uitoolkit.joinSession(sessionContainer, config)

    uitoolkit.onSessionClosed(sessionClosed)
}

var sessionClosed = (() => {
    console.log('session closed')
    uitoolkit.closeSession(sessionContainer)

    document.getElementById('join-flow').style.display = 'block'
})
