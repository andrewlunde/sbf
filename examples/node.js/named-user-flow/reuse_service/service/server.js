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

app.get('/products', (req, res) => {
  console.log('Email is: ', req.authInfo.getEmail());
  console.log('Identity zone: ', req.authInfo.getIdentityZone());
  console.log('Clone instance id is: ', req.authInfo.getCloneServiceInstanceId());

  res.json([{ name: 'Beer' }]);
});

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`SBF framework demo: products service application listening on port ${port} !`);
});
