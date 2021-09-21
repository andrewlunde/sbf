'use strict';
/* eslint-disable no-console */

const express = require('express');
const request = require('request');
const xsenv = require('@sap/xsenv');

const serviceName = process.env.PRODUCTS_SERVICE_NAME || '';
const serviceCredentials = xsenv.getServices({ products: serviceName }).products;
const uaaBrokerCreds = serviceCredentials.uaa;
const VCAP_APPLICATION = JSON.parse(process.env.VCAP_APPLICATION);

const app = express();

const requestAccessToken = (req, res, next) => {
  const uaaURL = uaaBrokerCreds.url;
  const clientId = uaaBrokerCreds.clientid;
  const clientSecret = uaaBrokerCreds.clientsecret;
  const tokenURL = `${uaaURL}/oauth/token`;

  console.log(`Requesting access token at ${tokenURL}`);

  let additionalAttributes = {
    // here the application can pass arbitrary data to the service
    'application_id': VCAP_APPLICATION.application_id,
    'application_name': VCAP_APPLICATION.application_name
  };
  request.post(tokenURL, {
    form: {
      'client_id': clientId,
      'client_secret': clientSecret,
      'grant_type': 'client_credentials',
      'response_type': 'token',
      'authorities': JSON.stringify({ 'az_attr': additionalAttributes })
    }
  }, (err, response, body) => {
    if (err) {
      console.error(err);
      return res.status(500).send('Problem during requsting access token');
    }
    if (response.statusCode !== 200) {
      console.error(`Request to UAA failed: ${response.statusCode}, ${body}`);
      return res.status(response.statusCode).send('Error from UAA service');
    }

    body = JSON.parse(body);
    req.accessToken = body.access_token;
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
      bearer: accessToken
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
app.get('/products', requestService);

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`SBF framework demo: consumer application listening on port ${port} !`);
});
