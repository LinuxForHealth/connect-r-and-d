const NATS = require('nats')
const nc = NATS.connect('localhost:4222')

nc.subscribe('idaas-data', function (msg) {
  console.log('Received a message: ' + msg)
})
