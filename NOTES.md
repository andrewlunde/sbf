```
https://www.npmjs.com/package/@sap/sbf#usage

C02XN22LJGH6:sbf i830671$ npx hash-broker-password -b
Plaintext password:
9JLsZmCbXvoDWT7OzjwQkQKpcCy2r3vX
Hashed credentials:
sha256:7CsxG+FFghizWHI9FoAyA6dG7vM30GtTGmkgJrK5ftA=:kVTWKmiAmG7IhVy/1tGRLkJBiQulaXVbTuVSbN4XcGM=

cf create-service-broker my-broker-name broker-user <plain-broker-password> <broker-url> --space-scoped

cf create-service-broker theta-sb broker-user 9JLsZmCbXvoDWT7OzjwQkQKpcCy2r3vX https://theta-broker.cfapps.eu10.hana.ondemand.com --space-scoped

cf cs my-service my-plan THETA_SBF
cf bind-service theta-trustee THETA_SBF
cf update-service THETA_SBF -t xsbss
cf restage theta-trustee
cf jq theta-trustee | jq '.VCAP_SERVICES | ."my-service"'

cf create-service-broker my-broker-name broker-user broker-password <broker-url>/<unique-suffix> --space-scoped

cf t -o theta -s qa
cf create-service-broker theta-qa broker-user 9JLsZmCbXvoDWT7OzjwQkQKpcCy2r3vX https://theta-broker.cfapps.eu10.hana.ondemand.com/qa --space-scoped

cf cs my-service-qa my-plan da-service
cf create-service-key da-service da-key

cf service-key da-service da-key

cf delete-service-key da-service da-key
cf delete-service da-service

# in another org/space

cf t -o theta-suba -s dev
cf create-service-broker theta-dev broker-user 9JLsZmCbXvoDWT7OzjwQkQKpcCy2r3vX https://theta-broker.cfapps.eu10.hana.ondemand.com/dev --space-scoped


cf cs my-service-dev my-plan da-service
cf create-service-key da-service da-key

cf service-key da-service da-key

# in another org/space from different global account?

cf t -o A-Team_robot -s dev
cf create-service-broker theta-robot broker-user 9JLsZmCbXvoDWT7OzjwQkQKpcCy2r3vX https://theta-broker.cfapps.eu10.hana.ondemand.com/robot --space-scoped

cf service-brokers

cf cs my-service-robot my-plan da-service
cf create-service-key da-service da-key

cf service-key da-service da-key


cf delete-service-key da-service da-key
cf delete-service da-service
```
