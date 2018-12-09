package nl.cerios.reactive.pizza.step2

import com.mongodb.client.MongoClient
import nl.cerios.reactive.pizza.step1.StorageService.getMongoClient
import org.slf4j.LoggerFactory
import kotlin.random.Random

object StorageServiceFlaky {

  private val log = LoggerFactory.getLogger(javaClass)

  fun getMongoClientFlaky(): MongoClient {
    return when (Random.nextInt(3)) {
      0 -> {
        log.debug("==> throw an exception")
        throw RuntimeException("EXCEPTION")
      }
      1 -> {
        log.debug("==> do not respond")
        Thread.sleep(Long.MAX_VALUE)
        throw RuntimeException("TIMEOUT")
      }
      else -> {
        log.debug("==> invoke service")
        getMongoClient()
      }
    }
  }
}