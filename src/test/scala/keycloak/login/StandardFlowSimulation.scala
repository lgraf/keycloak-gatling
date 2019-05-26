package keycloak.login

import java.net.URLEncoder
import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class StandardFlowSimulation extends Simulation {
  val keycloakUrl = "http://localhost:10080/auth"

  val httpProtocol = http
    .baseUrl(keycloakUrl)
    .disableFollowRedirect

  val realm = "gatling"
  val clientId = "gatling-app"

  val redirectUrl = "http://gatling-client-app/sso/login"
  val logoutRedirect = "http://gatling-client-app/logout"

  object Keycloak {
    val loadLoginPage = exec(http("keycloak_get_login-page")
      .get(s"/realms/$realm/protocol/openid-connect/auth")
      .queryParam("client_id", clientId)
      .queryParam("redirect_uri", redirectUrl)
      .queryParam("state", UUID.randomUUID().toString())
      .queryParam("nonce", UUID.randomUUID().toString())
      .queryParam("response_type", "code")
      .queryParam("scope", "openid")
      .check(status.is(200))
      .check(css("#kc-form-login")
        .ofType[Node]
        .transform(n => {
          n.getAttribute("action")
        }).saveAs("auth_url"))
    )

    val authenticate = exec(http("keycloak_post_authentication")
      .post("${auth_url}")
      .formParam("username", "user")
      .formParam("password", "user")
      .check(status.is(302))
      .check(header("Location").transform(l => {
        // TODO: code is always last parameter?
        l.substring(l.indexOf("code=") + 5, l.length())
      }).saveAs("code"))
    )
  }

  object ClientApplication {
    val codeToToken = exec(http("client-application_post_code-to-token")
      .post(s"/realms/$realm/protocol/openid-connect/token")
      .formParam("grant_type", "authorization_code")
      .formParam("code", "${code}")
      .formParam("client_id", clientId)
      .formParam("redirect_uri", redirectUrl)
      .check(status.is(200))
      .check(jsonPath("$..access_token").exists)
    )

    val logout = exec(http("client-application_get_logout")
      .get(s"/realms/$realm/protocol/openid-connect/logout?redirect_uri=${URLEncoder.encode(logoutRedirect, "UTF-8")}")
      .check(status.is(302))
      .check(header("Location").is(logoutRedirect))
    )
  }

  val keycloakStandardFlow = scenario("keycloak-standard-flow")
    .exec(Keycloak.loadLoginPage)
    .pause(5)
    .exec(Keycloak.authenticate)
    .pause(1)
    .exec(ClientApplication.codeToToken)
    .pause(10)
    .exec(ClientApplication.logout)


  setUp(
    keycloakStandardFlow.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}