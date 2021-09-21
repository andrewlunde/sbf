'use strict';

const express = require('express');
const passport = require('passport');
const BasicStrategy = require('passport-http').BasicStrategy;
const xsenv = require('@sap/xsenv');
const categories = require('./categories');
const auth = require('./auth');

const sbssCredentials = xsenv.cfServiceCredentials(process.env.SBSS_SERVICE_NAME);
const checkUser = auth(sbssCredentials);

passport.use(new BasicStrategy((username, password, done) => {
  checkUser(username, password, (err, user) => {
    if (err) { return done(err); }
    if (!user) { return done(null, false); }

    return done(null, user);
  });
}));

const app = express();

app.use(passport.authenticate('basic', { session: false }));

app.get('/categories', (req, res) => res.json(categories.getAll()));
app.get('/categories/:categoryId', (req, res) => {
  const category = categories.get(parseInt(req.params.categoryId));
  if (!category) { return res.status(404).end(); }

  res.json(category);
});

const port = process.env.PORT || 5000;
app.listen(port, () => {
  /* eslint-disable no-console */
  console.log(`SBF framework demo: categories service application listening on port ${port} !`);
});
