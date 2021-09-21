# Node.js examples

## Simple usage example

[The example](simple) uses SBSS service instance and for simplicity the deploy steps use HANA SBSS service instance.
Still the demo can work with any other SBSS service supported by SBF.

This example also demonstrates how to deploy a broker with MTA.

## Custom behaviour example

[The example](custom-hooks) shows how to use `hooks` to add custom behaviour to the SBF. Provision operation is implemented to work asynchronously.

## Service Authentication with OAuth - Technical user flow

[The example](technical-user-flow) uses the UAA's plan *broker* to create a service that uses technical user flow for authentication.

## Service Authentication with OAuth - Named user flow

[The example](named-user-flow) uses the UAA's plan *broker* to create a service that uses named user flow for authentication.