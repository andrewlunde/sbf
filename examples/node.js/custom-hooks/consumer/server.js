'use strict';

const express = require('express');
const request = require('request');
const xsenv = require('@sap/xsenv');

const app = express();

const serviceName = process.env.CATEGORY_SERVICE_NAME || '';
const credentials = xsenv.getServices({ category: serviceName }).category;

const doGet = (req, res) => {
  const categoryId = req.params.categoryId;
  const url = credentials.url + (categoryId && `/${categoryId}` || '');
  const auth = {
    user: credentials.username,
    pass: credentials.password
  };

  request.get({ url, auth }, (error, response, body) => {
    if (error) {
      /* eslint-disable no-console */
      console.error('Error requesting categories service:', error);
      return res.status(500).send(error);
    }
    if (response.statusCode !== 200) {
      /* eslint-disable no-console */
      console.error(`Request to categories service failed: ${response.statusCode}, ${response.statusMessage}`);
      return res.status(response.statusCode).send(response.statusMessage);
    }

    res.json(JSON.parse(body));
  });
};

app.get('/categories', doGet);
app.get('/category/:categoryId', doGet);

const port = process.env.PORT || 3000;
app.listen(port, () => {
  /* eslint-disable no-console */
  console.log(`SBF framework demo: consumer application listening on port ${port} !`);
});
