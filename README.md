## keycloak-gatling


A basic [gatling](https://gatling.io) simulation to load test your [keycloak](https://www.keycloak.org/) installation.

Right now there is one Simulation to test the keycloak standard ([Authorization Code](https://tools.ietf.org/html/rfc6749#section-4.1))
flow login with an configurable amount of keycloak clients and users.

The duration of the simulation is fixed (default: 30 seconds) and executes a fixed number of parallel requests (default: 5).
Virtual user requests are generated randomly (client, user), based on fixed name pattern and a random number within a fixed range.
* client id - `app-client-NUM` (default range: 5)
* redirect url - `http://client-app-NUM/sso/login`
* logout url - `http://client-app-$randomClientId/logout`
* keycloak user - `user-NUM` (default range: 3)

There is no need for having actual client application(s), the simulation does not follow redirects and mimic the client application
by extracting the `authorization code` from the [Authorization Response](https://tools.ietf.org/html/rfc6749#section-4.1.2) `Location`
header and initiating the [Access Token Request](https://tools.ietf.org/html/rfc6749#section-4.1.3) by it self
(including a one second wait to simulate the client application redirect). With the result of **only** testing keycloak itself (not your protected applications).

Please note that this simulation will **not** simulate **your** actual production load and is in many ways to simplistic (static logout, no token refresh, no usage of roles, etc.).
It should mainly serve a starting ground where you can build on your actual (more complex) production simulation.

### Data Setup

There are shell scripts to generate keycloak test data for the above mentioned schema, by utilizing the
[Admin REST API](https://www.keycloak.org/docs/latest/server_development/index.html#admin-rest-api).
The keycloak server credentials can be configured in `keycloak-data-setup/login.sh` file.

`keycloak-data-setup/create-clients.sh NUM` (default: 3)  
`keycloak-data-setup/create-users.sh NUM` (default: 5)

Please note that this script will create users with an easy to guess usernames and fixed passwords, so **dont** use this with a public facing installation.

### Run the Simulation

`mvn gatling:test -DkeycloakUrl=http://localhost:10080/auth -DkeycloakRealm=gatling -Dduration=30 -DconcurrentUsers=5 -DkeycloakClients=3 -DkeycloakUsers=5`

The number of actual keycloak clients and users should match with your keycloak test data.
