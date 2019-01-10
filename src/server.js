const express = require('express');
const {incrementCount, getCount} = require('./count');

const app = express();
const PORT = 9233;

app.use(function(req, res, next) {
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    res.header("Access-Control-Allow-Credentials", "true");
    res.header("Access-Control-Allow-Origin", "http://localhost:3030");
    res.header("AMP-Access-Control-Allow-Source-Origin", "http://localhost:3030");
    res.header("access-control-allow-methods", "POST, GET, OPTIONS");
    res.header("access-control-expose-headers", "AMP-Access-Control-Allow-Source-Origin");
    next();
});

app.get('/oauth', (req, res) => {
    incrementCount();
    const views = getCount() <= 4;
    console.log(getCount(), views);
    
    res.setHeader('Content-Type', 'application/json');
    res.send(JSON.stringify({
        "granted": true,
        "grantReason": "SUBSCRIBER",
        "data" : {
            "loggedIn": false,
            "subscriptionType": "premium",
            "showSwg": views
          }
      }));
})

app.post('/pingback', (req, res) => {
    console.log(req.headers.cookie);
    res.send('{}');
})

app.get('/login', (req, res) => {
    res.send('{}');
})

app.get('/subscribe', (req, res) => {
    res.send('{}');
})

app.get('/_healthcheck', (req, res) => {
    res.send('healthy');
})

app.get('/', (req, res) => {
    res.send('server is up');
})

app.listen(PORT, () => {
    console.log('server is running on port:', PORT);
});