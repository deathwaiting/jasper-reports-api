package stress

import io.gatling.javaapi.core.Choice
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.ScenarioBuilder
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.*
import io.gatling.javaapi.http.HttpProtocolBuilder
import java.time.Duration

class JasperReportsStressSimulation : Simulation() {

    private val baseUrl: String = System.getProperty("stress.base.url", "http://localhost:18080")
    private val maxP99Ms: Int = System.getProperty("stress.max.p99.ms", "3000").toInt()

    // Closed-workload ramp: start at `ramp.start` concurrent users and ramp linearly up to
    // `target` over `ramp.duration`. The load generator is aborted early (see below) if the
    // rolling success rate drops under `min.success.percent`.
    private val rampStartUsers: Int = System.getProperty("stress.ramp.start.vusers", "100").toInt()
    private val rampEndUsers: Int = System.getProperty("stress.target.vusers", "1000").toInt()
    private val rampDuration: Duration = Duration.ofSeconds(
        System.getProperty("stress.ramp.duration.seconds", "120").toLong()
    )
    private val minSuccessPercent: Double = System.getProperty("stress.min.success.percent", "99.0").toDouble()
    private val failureWindow: Duration = Duration.ofSeconds(
        System.getProperty("stress.fail.window.seconds", "30").toLong()
    )

    private val successRate = RollingSuccessRate(failureWindow.toSeconds().toInt())

    private val httpProtocol: HttpProtocolBuilder = http
        .baseUrl(baseUrl)
        .acceptHeader("*/*")
        .basicAuth("user", "pass")
        .userAgentHeader("gatling-stress-test")

    private val reportScenarios: ScenarioBuilder = scenario("ReportGeneration")
        .pause(Duration.ofSeconds(10)) //selecting parameters/navigating to report
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
                        .check(status().shouldBe(200))
                )
            )
        )
        // Record this iteration's outcome, then reset the session status so the next
        // iteration of the same virtual user starts clean.
        .exec { session ->
            successRate.record(!session.isFailed)
            if (session.isFailed) session else session.markAsSucceeded()
        }
        // Abort the whole load generator the moment the rolling success rate drops below the
        // threshold. Every virtual user evaluates this on each iteration; the first to see a
        // breach stops the run (no further users are injected, in-flight users are dropped).
        .exec(
            stopLoadGeneratorIf(
                "rolling success rate dropped below ${minSuccessPercent}%",
                { _: Session -> successRate.rate() < minSuccessPercent }
            )
        )

    init {
        setUp(
            reportScenarios.injectClosed(
                rampConcurrentUsers(rampStartUsers).to(rampEndUsers).during(rampDuration)
            )
        ).protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile3().lt(maxP99Ms),
                global().successfulRequests().percent().gte(99.0),
                forAll().successfulRequests().percent().gte(95.0)
            )
    }

    /**
     * Thread-safe rolling window of success/failure outcomes, bucketed by whole second.
     * [rate] returns the percentage of successful requests over the most recent
     * [windowSeconds] seconds (partial window early in the run).
     */
    private class RollingSuccessRate(private val windowSeconds: Int) {

        private val bucketEpochSecond = LongArray(windowSeconds)
        private val successes = LongArray(windowSeconds)
        private val totals = LongArray(windowSeconds)

        @Synchronized
        fun record(success: Boolean) {
            val now = System.currentTimeMillis() / 1000
            val idx = (now % windowSeconds).toInt()
            if (bucketEpochSecond[idx] != now) {
                bucketEpochSecond[idx] = now
                successes[idx] = 0L
                totals[idx] = 0L
            }
            totals[idx]++
            if (success) successes[idx]++
        }

        @Synchronized
        fun rate(): Double {
            val now = System.currentTimeMillis() / 1000
            var successTotal = 0L
            var requestTotal = 0L
            for (i in bucketEpochSecond.indices) {
                // Ignore buckets that have rolled out of the window.
                if (now - bucketEpochSecond[i] < windowSeconds) {
                    successTotal += successes[i]
                    requestTotal += totals[i]
                }
            }
            return if (requestTotal == 0L) 100.0 else successTotal.toDouble() * 100.0 / requestTotal
        }
    }
}