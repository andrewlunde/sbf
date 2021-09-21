'use strict';

const Broker = require('@sap/sbf');

const CREATE_SERVICE_MILLIS = 20 * 1000;
let provisionData = {};

const broker = new Broker({
  autoCredentials: true,
  hooks: {
    onProvision: (params, callback) => {
      provisionData[params['instance_id']] = false;

      // Delay the response with async function to simulate resource creation (like database schemas etc.)
      setTimeout(() => {
        provisionData[params['instance_id']] = true;
      }, CREATE_SERVICE_MILLIS);

      // Because of { async: true } provision operation will be asynchronous
      callback(null, { async: true });
    },
    onDeprovision: (params, callback) => {
      // Free any resources created during provision of the service instance
      callback(null, {});
    },
    onLastOperation: (params, callback) => {
      let state = 'in progress';
      if (provisionData[params['instance_id']]) {
        state = 'succeeded';
      }
      callback(null, { state });
    }
  }
});
broker.start();
