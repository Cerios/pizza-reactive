package nl.cerios.reactive.pizza;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class FetchJokeServiceJava {

  private static final Logger LOG = LoggerFactory.getLogger(FetchJokeServiceJava.class);
  private static final String API_URL = "http://api.icndb.com/jokes/random?limitTo=[nerdy,explicit]"; // Chuck Norris jokes

  public static String fetchJoke() {
    LOG.debug("fetch joke");
    try {
      final HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
      try {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
          throw new RuntimeException("Error reading from InputStream", e);
        }
      } finally {
        connection.disconnect();
        LOG.debug("fetched joke");
      }
    } catch (MalformedURLException e) {
      throw new RuntimeException("Error in URL", e);
    } catch (IOException e) {
      throw new RuntimeException("Error connecting to " + API_URL, e);
    }
  }
}
