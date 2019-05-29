package keycloak.login

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class StandardFlowSimulation extends Simulation {
  val concurrentUserCount = default("concurrentUsers", 5)
  val simulationDuration = default("duration", 30) // seconds

  val keycloakUrl = default("keycloakUrl", "http://localhost:10080/auth")
  val realm = default("keycloakRealm", "gatling")

  val keycloakUserCount = default("keycloakUsers", 5)
  val keycloakClientCount = default("keycloakClients", 3)

  val httpProtocol = http
    .baseUrl(keycloakUrl)
    .disableFollowRedirect


  val feeder = Iterator.continually({
    val randomUserId = ThreadLocalRandom.current().nextInt(keycloakUserCount - 1)
    val randomClientId = ThreadLocalRandom.current().nextInt(keycloakClientCount - 1)
    Map(
      "userName" -> s"user-$randomUserId",
      "clientId" -> s"client-app-$randomClientId",
      "redirectUrl" -> s"http://client-app-$randomClientId/sso/login",
      "logoutUrl" -> s"http://client-app-$randomClientId/logout"
    )
  })


  object Keycloak {
    val loadLoginPage = exec(http("keycloak_get_login-page")
      .get(s"/realms/$realm/protocol/openid-connect/auth")
      .queryParam("client_id", "${clientId}")
      .queryParam("redirect_uri", "${redirectUrl}")
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
      .formParam("username", "${userName}")
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
      .formParam("client_id", "${clientId}")
      .formParam("redirect_uri", "${redirectUrl}")
      .check(status.is(200))
      .check(jsonPath("$..access_token").exists)
    )

    val logout = exec(http("client-application_get_logout")
      .get("/realms/" + realm + "/protocol/openid-connect/logout?redirect_uri=${logoutUrl}")
      .check(status.is(302))
      .check(header("Location").is("${logoutUrl}"))
    )
  }

  val keycloakStandardFlow = scenario("keycloak-standard-flow")
    .feed(feeder)
    .exec(Keycloak.loadLoginPage)
    .pause(5)
    .exec(Keycloak.authenticate)
    .pause(1)
    .exec(ClientApplication.codeToToken)
    .pause(10)
    .exec(ClientApplication.logout)


  setUp(
    keycloakStandardFlow.inject(constantConcurrentUsers(concurrentUserCount) during (simulationDuration))
  ).protocols(httpProtocol)


  def default[T](option: String, defaultValue: T): T = {
    if (System.getProperty(option) == null)
      return defaultValue

    (defaultValue match {
      case t: String => System.getProperty(option)
      case t: Int => System.getProperty(option).toInt
      case t@_ => throw new IllegalArgumentException("unsupported type")
    }).asInstanceOf[T]
  }
}