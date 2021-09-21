# Service Broker Framework - simple usage example

## Install applications dependencies

```bash
cd service
mvn clean install
cd ../broker
npm install
cd ../consumer
mvn clean install
cd ..
```

### Perform the steps in the [prerequisites section](/examples/prerequisites.md)

## Substitute placeholders to avoid collisions

In order to avoid collisions in naming several placeholders in application files should be substituted with your own names. Open `manifest.yml` and substitute `[c/d/i-user]` with your user ID or other string that will not result in collisions with host names.
Change `[cfdomain]` with the CF domain e.g. `cfapps.sap.hana.ondemand.com`.
Add the tenant ID of the broker in the `[tenant-id]` placeholder. How to find the tenant ID is explained [here](/README.md#providing-the-tenant-id).

## Deploy Products service application

### Create HANA SBSS service

As a first step we create SBSS service instance to bind to our service and service broker. The service name is provided to service and service broker applications via environment variables. By default SBF can search for SBSS service instance even if the service name is not provided via env var, but for clarity (and probably a best practice) we provide the name explicitly in `manifest.yml`.

```bash
cf create-service hana sbss products-sbss
```

**Note:** Since [v4 of `@sap/sbf`](/migration.md#version-3--version-4) brokers that use SBSS on HANA must add `@sap/hdbext` as a [dependency](/examples/java/simple/broker/package.json).

### Push Products service application

```bash
cf push products-service
```

## Deploy the service broker application

### Change service catalog GUID's

The service catalog need to use unique GUID's for service name, service id and plan id's. To ensure uniqueness, 2 options are available

1. Use the `SBF_CATALOG_SUFFIX` environment variable to provide ID suffix that SBF will use.
2. Open `broker/catalog.json` and change the service name and GUID's used in service and plan `id` with unique values.

In our example we use option 1, where you have already substituted the placeholder `[c/d/i-user]` for `SBF_CATALOG_SUFFIX` in the manifest.yml.

### Update manifest.yml with actual service url

Get the URL of the deployed product service, open manifest.yml and update the plan's URL in `SBF_SERVICE_CONFIG` environment variable.

### Push service broker
```bash
cf push products-service-broker
```

## Register the service broker in Cloud Foundry

Before executing the next command you need to substitute the placeholders URL in the command.
This command registers new service broker with space scope at the provided URL.

```bash
cf create-service-broker products-demo-service-broker-[c/d/i-user] [user] [plain-text-password] https://products-service-broker-[c/d/i-user].[cfdomain] --space-scoped
```

## Consume the newly created Products service

To demonstrate the usage of products service there is a small consumer application prepared in `consumer` directory.
In the next steps you will deploy the application and call it.

### Create service instance of type products-service

```bash
cf create-service products-service-[c/d/i-user] default products-service-instance
```

The service is bound to the consumer application in the manifest.yml.
In case you use different service instance name, update `PRODUCTS_SERVICE_NAME` in `manifest.yml` accordingly.

### Deploy the consumer application

```bash
cf push products-service-consumer
```

### Call the application

Get the consumer application URL using CF cli, like:

```bash
cf app products-service-consumer
```

Get products by appending the `/products` to the URL and request it via browser for example.

## Cleanup

```
cf delete products-service-consumer -f -r
cf delete-service -f products-service-instance
cf delete-service-broker products-demo-service-broker-[c/d/i-user] -f
cf delete products-service-broker -f -r
cf delete products-service -f -r
cf delete-service products-sbss -f
cf delete-service broker-audit -f
```
