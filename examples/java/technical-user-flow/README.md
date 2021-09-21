# Service Authentication with OAuth - Technical user flow

## Deploy the reuse service

The service expects a JWT token from the application and validates it using Security API.
The token contains information about the caller.

The service logs some of these properties.
It also logs the additional authorization attributes passed by the calling application.
These attributes can contain arbitrary data.
See [Products.java](reuse_service/service/src/main/java/service/Products.java).

### Build the service using maven and install dependencies for the broker

```sh
cd reuse_service/service
mvn clean install
cd ../broker
npm install
cd ..
```

### Perform the steps in the [prerequisites section](/examples/prerequisites.md)

### Substitute placeholders to avoid collisions

In order to avoid collisions in naming, several placeholders in application files should be substituted with your own names. Substitute `[c/d/i-user]` with your user ID or other string that will not result in collisions with host names.
Change `[cfdomain]` with the CF domain e.g. `cfapps.sap.hana.ondemand.com`.
Make sure to substitute placeholders in `reuse_service/manifest.yml`, `reuse_service/xs-security.json` and `reuse_service/service/src/main/resources/application.properties`

### Create UAA service of plan broker

As a first step we create an UAA service instance of plan broker to bind to our service and service broker. The service name is provided to service and service broker applications via environment variables.

```sh
cf create-service xsuaa broker products-uaa -c xs-security.json
```

### Push reuse service

```sh
cf push
```

### Register the service broker in Cloud Foundry

Before executing the next command you need to substitute the placeholders in it.
This command registers new service broker with space scope at the provided URL.

```sh
cf create-service-broker products-demo-service-broker-[c/d/i-user] [user] [plain-text-password] https://products-service-broker-[c/d/i-user].[cfdomain] --space-scoped
```

## Consume the newly created Products service

To demonstrate the usage of products service there is a small consumer application prepared in _consumer_ directory.

The application requests a JWT token from UAA using **client credentials** flow and then it uses this token to call the reuse service. See [consumer/src/consumer/Consumer.java](consumer/src/consumer/Consumer.java).

The consuming application can add arbitrary data in the JWT token.
In this example the application adds `application_name` and `application_id` properties.

Build the consumer application:
```sh
cd ../consumer
mvn clean install
```

Open `consumer/manifest.yml` and substitute `[c/d/i-user]` with your user ID or other string that will not result in collisions with host names.

### Create service instance of type products-service

```sh
cf create-service products-service-[c/d/i-user] default products-service-instance -c parameters.json
```

### Deploy the consumer application

```sh
cf push
```

### Call the application

Get the consumer application URL using CF cli, like:

```sh
cf app products-service-consumer
```

Get products by appending the `/products` to the URL and request it via browser for example.

Check the service logs
```sh
cf logs products-service --recent
```
You should see the data extracted from the token, e.g.
```
2017-07-13T17:54:16.82+0300 [APP/PROC/WEB/0] OUT Service instance id: 167395a5-de69-422f-bfe0-1ea553ad7e30
2017-07-13T17:54:16.82+0300 [APP/PROC/WEB/0] OUT Caller tenant id: cc-sap
2017-07-13T17:54:16.82+0300 [APP/PROC/WEB/0] OUT Token grant type: client_credentials
2017-07-13T17:54:16.82+0300 [APP/PROC/WEB/0] OUT Calling app has name products-service-consumer and id f407548d-0854-435b-8d26-d91a95ad4c64
```

## Cleanup

When you no longer need this example, you can delete its artifacts from Cloud Foundry:
```sh
cf delete -f -r products-service-consumer
cf delete-service -f products-service-instance
cf delete-service-broker -f products-demo-service-broker-[c/d/i-user]
cf delete -f -r products-service-broker
cf delete -f -r products-service
cf delete-service -f products-uaa
cf delete-service broker-audit -f
```
