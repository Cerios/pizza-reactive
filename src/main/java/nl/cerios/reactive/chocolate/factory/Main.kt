package nl.cerios.reactive.chocolate.factory

import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.eventbus.Message
import nl.cerios.reactive.chocolate.factory.verticle.Chocolatifier
import nl.cerios.reactive.chocolate.factory.verticle.HttpEventServer
import nl.cerios.reactive.chocolate.factory.verticle.LetterStamper
import nl.cerios.reactive.chocolate.factory.verticle.Packager
import nl.cerios.reactive.chocolate.factory.verticle.Painter
import nl.cerios.reactive.chocolate.factory.verticle.PeanutPaceLogger
import nl.cerios.reactive.chocolate.factory.verticle.PeanutPaceMonitor
import nl.cerios.reactive.chocolate.factory.verticle.PeanutPooper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rx.Observable
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.roundToLong
import kotlin.random.Random

fun main() = Main.run()

object Main {

  internal val log = LoggerFactory.getLogger(javaClass)
  private const val abortAfterMillis = 120_000L // 2 minutes

  fun run() {
    val vertxOptions = VertxOptions()
    if (log.isDebugEnabled) {
      vertxOptions.blockedThreadCheckInterval = abortAfterMillis
    }

    val vertx = Vertx.vertx(vertxOptions)

    val deploymentOptions = DeploymentOptions()
        .setConfig(JsonObject()
            .put("${PeanutPooper::class.java.name}.minIntervalMillis", 100L)
            .put("${PeanutPooper::class.java.name}.maxIntervalMillis", 10_000L)
            .put("${Chocolatifier::class.java.name}.processingMillis", 2_000L)
            .put("${Painter::class.java.name}.processingMillis", 1_000L)
            .put("${LetterStamper::class.java.name}.processingMillis", 500L)
            .put("${Packager::class.java.name}.numMnMs", 25)
            .put("${Packager::class.java.name}.collectAfterMillis", 60_000L)
            .put("${HttpEventServer::class.java.name}.port", 8080)
        )
    CompositeFuture
        .all(
            CompositeFuture.all(
                deployVerticle(vertx, deploymentOptions, PeanutPooper::class.java.name),
                deployVerticle(vertx, deploymentOptions, Chocolatifier::class.java.name),
                deployVerticle(vertx, deploymentOptions, Painter::class.java.name),
                deployVerticle(vertx, deploymentOptions, LetterStamper::class.java.name),
                deployVerticle(vertx, deploymentOptions, Packager::class.java.name)
            ),
            CompositeFuture.all(
                deployVerticle(vertx, deploymentOptions, PeanutPaceMonitor::class.java.name),
                deployVerticle(vertx, deploymentOptions, PeanutPaceLogger::class.java.name),
                deployVerticle(vertx, deploymentOptions, HttpEventServer::class.java.name)
            )
        )
        .setHandler { result ->
          if (result.succeeded()) {
            log.info("We have hyperdrive, captain.")
            vertx.eventBus().publish("peanut.pace.set", JsonObject().put("value", 0.10))
          } else {
            log.error("Error", result.cause())
          }
        }

//    vertx.setTimer(abortAfterMillis) {
//      vertx.close()
//      log.info("And... it's gone!")
//      System.exit(0)
//    }
  }

  private fun deployVerticle(vertx: Vertx, deploymentOptions: DeploymentOptions, verticleName: String): Future<Void> {
    val result = Promise.promise<Void>()
    vertx.deployVerticle(verticleName, deploymentOptions) { deployResult ->
      if (deployResult.succeeded()) {
        result.complete()
      } else {
        result.fail(deployResult.cause())
      }
    }
    return result.future()
  }
}

fun <T : Enum<T>> pickEnum(enumValues: Array<T>) = enumValues[Random.nextInt(enumValues.size)]

fun Long.timesHalfToTwo() = (this * (0.5 + 1.5 * Random.nextDouble())).roundToLong()

fun <T : Message<JsonObject>> Observable<T>.delayAndNotifyConsumption(vertx: Vertx, anticipateMillis: Long, averageProcessingMillis: Long = 0): Observable<out T> =
    delay(anticipateMillis, MILLISECONDS)
        .throttleFirst(averageProcessingMillis, MILLISECONDS)
        .doOnNext { message -> vertx.eventBus().publish(message.address().replace("produced", "consumed"), message.body()) }
        .delay(1_000L, MILLISECONDS) // ramp-up
        .delay { item ->
          val delayObservable = Observable.create<T> { emitter -> emitter.onNext(item) }
          if (averageProcessingMillis > 0) {
            delayObservable.delay(averageProcessingMillis/*.timesHalfToTwo()*/, MILLISECONDS)
          } else {
            delayObservable
          }
        }
        .delay(1_000L, MILLISECONDS) // ramp-down

fun <T> Observable<T>.logIt(log: Logger, label: String = ""): Observable<out T> =
    doOnNext {
      val prefix = if (label.isNotEmpty()) "$label: " else ""
      val value = when (it) {
        is Message<*> -> "[${it.address()}] = ${it.body()}"
        else -> "$it"
      }
      log.debug("$prefix$value")
    }
