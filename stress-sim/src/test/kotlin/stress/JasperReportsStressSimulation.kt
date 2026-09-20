package stress

import io.gatling.javaapi.core.Choice
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.ScenarioBuilder
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.*
import io.gatling.javaapi.http.HttpProtocolBuilder
import java.time.Duration

class JasperReportsStressSimulation : Simulation() {

    private val baseUrl: String = System.getProperty("stress.base.url", "http://localhost:18080")
    private val maxP99Ms: Int = System.getProperty("stress.max.p99.ms", "3000").toInt()
    private val minConcurrentUsers: Int = System.getProperty("stress.target.vusers", "1000").toInt()
    private val rampDuration: Duration = Duration.ofSeconds(
        System.getProperty("stress.ramp.duration.seconds", "30").toLong()
    )
    private val holdDuration: Duration = Duration.ofSeconds(
        System.getProperty("stress.hold.duration.seconds", "120").toLong()
    )

    private val httpProtocol: HttpProtocolBuilder = http
        .baseUrl(baseUrl)
        .acceptHeader("*/*")
        .basicAuth("user", "pass")
        .userAgentHeader("gatling-stress-test")

    private val reportScenarios: ScenarioBuilder = scenario("ReportGeneration")
        .randomSwitch()
        .on(
            Choice.WithWeight(
                40.0,
                exec(
                    http("root emp-report.pdf")
                        .get("/report/emp-report.pdf")
                        .check(status().shouldBe(200))
                )
            ),
            Choice.WithWeight(
                30.0,
                exec(
                    http("inner-dir emp-report.pdf")
                        .get("/report/inner-dir/emp-report.pdf")
                        .queryParam("dep_id", "1")
                        .check(status().shouldBe(200))
                )
            ),
            Choice.WithWeight(
                20.0,
                exec(
                    http("sub-report report.pdf")
                        .get("/report/sub-report/report.pdf")
                        .queryParam("some_time", "14:02:56")
                        .check(status().shouldBe(200))
                )
            ),
            Choice.WithWeight(
                10.0,
                exec(
                    http("book book.pdf")
                        .get("/report/book/book.pdf")
                        .queryParam("some_date", "2025-01-23")
                        .queryParam("JR_force_compile", "true")
                        .check(status().shouldBe(200))
                )
            )
        )

    init {
        setUp(
            reportScenarios.injectOpen(
                rampUsers(minConcurrentUsers).during(rampDuration),
                nothingFor(holdDuration)
            )
        ).protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile3().lt(maxP99Ms),
                global().successfulRequests().percent().gte(99.0),
                forAll().successfulRequests().percent().gte(95.0)
            )
    }
}
