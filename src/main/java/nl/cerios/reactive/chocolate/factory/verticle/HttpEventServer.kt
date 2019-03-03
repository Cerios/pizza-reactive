package nl.cerios.reactive.chocolate.factory.verticle

import io.vertx.core.Future
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.handler.StaticHandler
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler
import org.slf4j.LoggerFactory

class HttpEventServer : AbstractVerticle() {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun start(futureResult: Future<Void>) {
    val port = config().getInteger("${javaClass.name}.port")
    val router = Router.router(vertx)

    router.route()
        .handler(StaticHandler.create("www").setIndexPage("chocolateFactory.html"))

    router.route("/eventbus/*")
        .handler(SockJSHandler.create(vertx)
            .bridge(BridgeOptions()
                .addInboundPermitted(PermittedOptions().setAddress("peanut.pace.get"))
                .addInboundPermitted(PermittedOptions().setAddress("peanut.pace.set"))
                .addOutboundPermitted(PermittedOptions().setAddress("peanut"))
                .addOutboundPermitted(PermittedOptions().setAddress("peanut.pace.set"))))
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(port) { result ->
          if (result.succeeded()) {
            log.info("Server is listening on http://localhost:$port/")
            futureResult.complete()
          } else {
            futureResult.fail(result.cause())
          }
        }
  }
}