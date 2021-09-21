'use strict';

const catalog = require('./catalog');

exports.getAll = () => catalog;

exports.get = categoryId => catalog.find(product => product.id === categoryId);
