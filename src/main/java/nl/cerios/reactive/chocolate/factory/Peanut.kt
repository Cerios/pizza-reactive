package nl.cerios.reactive.chocolate.factory

import io.vertx.core.json.JsonObject
import java.security.SecureRandom

internal class Peanut {

  private val quality = SecureRandom().nextFloat()

  fun toJson(): JsonObject {
    return JsonObject()
        .put("quality", quality)
  }
}