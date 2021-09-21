'use strict';
/* eslint-disable no-console */

const express = require('express');
const xsenv = require('@sap/xsenv');
const passport = require('passport');
const JWTStrategy = require('@sap/xssec').JWTStrategy;

const app = express();
passport.use(new JWTStrategy(xsenv.getServices({uaa:{tag:'xsuaa'}}).uaa));

app.use(passport.initialize());
app.use(passport.authenticate('JWT', { session: false }));

app.use((req, res, next) => {
  console.log('Service instance id:', req.authInfo.getCloneServiceInstanceId());
  console.log('Caller tenant id:', req.authInfo.getIdentityZone());
  console.log('Token grant type:', req.authInfo.getGrantType());
  console.log('Calling app has name %s and id %s',
    req.authInfo.getAdditionalAuthAttribute('application_name'),
    req.authInfo.getAdditionalAuthAttribute('application_id')
  );
  next();
});

app.get('/products', (req, res) => {
  res.json([{ name: 'Beer' }]);
});

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`SBF framework demo: products service application listening on port ${port} !`);
});