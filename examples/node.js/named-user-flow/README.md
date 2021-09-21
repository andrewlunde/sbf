# Service Authentication with OAuth - Named user flow

## Deploy reuse service

The service expects a valid JWT token from the application and validates it using Security API(@sap/xssec). See [reuse_service/service/server.js](reuse_service/service/server.js).

### Install applications dependencies

```bash
cd reuse_service/service
npm install
cd ../broker
npm install
cd ..
```

### Perform the steps in the [prerequisites section](/examples/prerequisites.md)

### Substitute placeholders to avoid collisions

In order to avoid collisions in naming, several placeholders in application files should be substituted with your own names. Open `reuse_service/manifest.yml` and substitute `[c/d/i-user]` with your user ID or other string that will not result in collisions with host names. Also substitute `[cfdomain]` with the CF domain e.g. `cfapps.sap.hana.ondemand.com`. Do the same for `reuse_service/xs-security.json`.

### Create UAA service of plan broker

As a first step we create a UAA service instance of plan *broker* to bind to our service and service broker. The service name is provided to both the service and the service broker applications via environment variables.

```bash
cf create-service xsuaa broker named-uaa -c xs-security.json
```

### Push reuse service

```bash
cf push
```

### Register the service broker in Cloud Foundry

Substitute the placeholders in the command below before executing it.
This command registers new service broker with space scope at the provided URL.

```bash
cf create-service-broker named-demo-service-broker-[c/d/i-user] [user] [plain-text-password] https://named-service-broker-[c/d/i-user].[cfdomain] --space-scoped
```

## Consume the newly created Products service

To demonstrate the usage of the products service, there is a small consumer application prepared in the `consumer` directory.

Before calling the service, the application exchanges the JWT token it has received from Application router with another JWT token that is more restricted. The latter token is used by the application when calling the service. See [consumer/backend/server.js](consumer/backend/server.js)

In the next steps you will deploy the application.

```bash
cd ../consumer/backend
npm install
```

Open `consumer/manifest.yml` and substitute `[c/d/i-user]` with your user ID or other string that will not result in collisions with host names. Change `[cfdomain]` with the CF domain e.g. `cfapps.sap.hana.ondemand.com`. Do the same for `consumer/backend/parameters.json` and `consumer/approuter/xs-security.json`.

### Create service instance of type named-service

```bash
cf create-service named-service-[c/d/i-user] default named-service-instance -c parameters.json
```

### Configure Approuter

In order to authenticate to the consuming application, you can use Application router.

```sh
cd ../approuter
npm install
```

### Create UAA with plan application

You'll need another UAA of plan *application* to authenticate with. Open the *approuter/xs-security.json* and substitute `[identityzone_id]` with whatever identity zone id you are willing to use.

```sh
cf create-service xsuaa application named-uaa-default -c xs-security.json
```

### Deploy the consumer application and approuter

```bash
cd ..
cf push
```

### Call the Approuter

Get the consumer application's URL using CF cli, like:

```bash
cf app named-approuter
```

Get the products by appending `/consume` to the URL and request it via browser for example.

### Check results

```sh
cf logs named-service --recent
```

You should see in the logs entries like:

```
Email is:  [some-email@example.com]
Identity zone:  [some-identity-zone]
Clone instance id is:  [some-guid]
```

## Cleanup

```
cf delete named-service-consumer -f -r
cf delete-service named-service-instance -f
cf delete-service-broker named-demo-service-broker-[c/d/i-user] -f
cf delete named-service-broker -f -r
cf delete named-service -f -r
cf delete-service named-uaa -f
cf delete named-approuter -f -r
cf delete-service named-uaa-default -f
cf delete-service broker-audit -f
```
