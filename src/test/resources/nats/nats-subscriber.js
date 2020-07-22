const NATS = require('nats')
const nc = NATS.connect('localhost:4222')

nc.subscribe('idaas-data', function (msg) {
  console.log('Received a message: ' + msg)

  // Parse the JSON message
  try {
    let jsonMsg = JSON.parse(msg)
    if (jsonMsg && jsonMsg.data) {
      let data = jsonMsg.data
      console.log('Received data: ' + JSON.stringify(data))
      if (data.resourceType && data.id) {
        console.log('Data contains a resource with type: '+data.resourceType+" and id: "+data.id)
      }
    }
  } catch (ex) {
    console.log('Message does not contain valid json: '+ex)
  } 
})
