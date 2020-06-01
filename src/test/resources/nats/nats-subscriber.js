const NATS = require('nats')
const nc = NATS.connect('localhost:4222')

nc.subscribe('test', function (msg) {
  console.log('Received a message: ' + msg)
})
