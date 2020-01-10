package gatling

import scala.concurrent.duration._

import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.LoggerFactory
import io.gatling.core.Predef._
import io.gatling.core.structure.{ ChainBuilder, ScenarioBuilder }
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import io.gatling.commons.validation._

class LoadTest extends Simulation {

  val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
  // Log all HTTP requests
  //context.getLogger("io.gatling.http").setLevel(Level.valueOf("TRACE"))
  // Log failed HTTP requests
  //context.getLogger("io.gatling.http").setLevel(Level.valueOf("DEBUG"))
  
  var domain: String = "feature-aae-731.35.228.195.195.nip.io"
  var realm: String = "activiti"
  var authenticated = new CountDownLatch(1);

  val httpConf = http.disableFollowRedirect
  
  val ssoUrl = "http://identity.${domain}/auth/realms/${realm}/protocol/openid-connect/token"
  val rbUrl = "http://gateway.${domain}/rb"
  val queryUrl = "http://gateway.${domain}/query"
  var access_token: String = ""
  val sessionHeaders = Map(
    "Authorization" -> "Bearer ${access_token}",
    "Content-Type" -> "application/json")

  val httpProtocol: HttpProtocolBuilder = http
  
  val authenticate = scenario("Get access_token")
    .exec(session => session
      .set("domain", domain)
      .set("realm", realm))
    .exec(http("Login")
      .post(ssoUrl)
      .header("Content-Type", "application/x-www-form-urlencoded")
      .formParam("client_id", "activiti")
      .formParam("grant_type", "password")
      .formParam("username", "hruser")
      .formParam("password", "password")
      .check(status.is(200))
      .check(jsonPath("$.access_token").exists.saveAs("access_token")))
    .exec(session => {
      access_token = session("access_token").as[String]
      println("============ Access Token ============")
      println(access_token)
      authenticated.countDown()
      session
    })

  object Scenarios {
    val healthCheck: ChainBuilder = 
      exec(session => session
        .set("domain", domain))
      .exec(http("Rb is Up")
        .get(rbUrl + "/actuator/health")
        .header("Accept", "*/*")
        .check(status.is(200)))
      .exec(http("Query is Up")
        .get(queryUrl + "/actuator/health")
        .header("Accept", "*/*")
        .check(status.is(200)))
        
    val slaCheck: ChainBuilder = 
      exec(session => session
        .set("domain", domain)
        .set("access_token", access_token)
      )
      .exec(http("Start Process Instance")
        .post(rbUrl + "/v1/process-instances")
        .headers(sessionHeaders)
        .body(StringBody("""{
            "processDefinitionKey": "SimpleProcess",
            "payloadType":"StartProcessPayload",
            "variables": {
              "movieToRank" : "The Lord of The Rings"
            }
          }"""))
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("id")))
        .pause(500 milli)
        .exec(http("Query Process Instance")
          .get(queryUrl + "/v1/process-instances/${id}")
          .headers(sessionHeaders)
          .check(status.is(200))
          .check(jsonPath("$.id"))
          .check(jsonPath("$.status")))
          .exitHereIfFailed
  }

  val awaitAuthenticated = exec(session => {
      authenticated.await(2, TimeUnit.SECONDS)
      session
  }).exitHereIfFailed 

  val healthCheck: ScenarioBuilder = scenario("Health Check").exec(Scenarios.healthCheck)
  val slaCheck: ScenarioBuilder = scenario("Rb -> Query Sync SLA").exec(awaitAuthenticated).exec(Scenarios.slaCheck)

  setUp(authenticate.inject(constantUsersPerSec(1) during (1 seconds)),
        healthCheck.inject(constantUsersPerSec(1) during (1 seconds)),
        slaCheck.inject(incrementConcurrentUsers(5)
          .times(5)
          .eachLevelLasting(10 seconds)
          .separatedByRampsLasting(10 seconds)
          .startingFrom(15))
        .protocols(httpConf))
        .assertions(global.successfulRequests.percent.is(100))
}