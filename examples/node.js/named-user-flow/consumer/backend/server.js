'use strict';
/* eslint-disable no-console */

const express = require('express');
const request = require('request');
const xsenv = require('@sap/xsenv');
const passport = require('passport');
const JWTStrategy = require('@sap/xssec').JWTStrategy;

const serviceName = process.env.PRODUCTS_SERVICE_NAME || '';
const serviceCredentials = xsenv.getServices({ products: serviceName }).products;
const uaaBrokerCreds = serviceCredentials.uaa;

const app = express();

passport.use(new JWTStrategy(xsenv.getServices({ uaa: { tag: 'xsuaa' } }).uaa));
app.use(passport.initialize());
app.use(passport.authenticate('JWT', { session: false }));

const requestAccessToken = (req, res, next) => {
  console.log('Requesting access token....');

  req.authInfo.requestTokenForClient(uaaBrokerCreds, null, (error, token) => {
    if (error) {
      console.error(error);
      return next(error);
    }
    req.accessToken = token;
    next();
  });
};

const requestService = (req, res) => {
  const serviceURL = serviceCredentials.url;
  const accessToken = req.accessToken;
  const url = `${serviceURL}/products`;

  console.log(`Requesting ${url}`);

  request.get(url, {
    auth: {
      'bearer': accessToken
    }
  }, (err, response, body) => {
    if (err) {
      console.error('Error requesting products service:', err);
      return res.status(500).send(err);
    }
    if (response.statusCode !== 200) {
      console.error(`Request to products service failed: ${response.statusCode}, ${body}`);
      return res.status(response.statusCode).send('Error from backend service');
    }

    res.json(JSON.parse(body));
  });
};

app.use(requestAccessToken);
app.get('/consume', requestService);

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`SBF framework demo: consumer application listening on port ${port} !`);
});
