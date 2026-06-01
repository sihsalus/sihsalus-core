package org.sihsalus.core.boot;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SihsalusBootstrapProbeServer implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(SihsalusBootstrapProbeServer.class);
  private static final int DEFAULT_PORT = 18080;

  private final HttpServer server;
  private final ExecutorService executor;
  private final Instant startedAt = Instant.now();
  private volatile boolean ready;
  private volatile Throwable startupFailure;

  private SihsalusBootstrapProbeServer(HttpServer server, ExecutorService executor) {
    this.server = server;
    this.executor = executor;
  }

  static SihsalusBootstrapProbeServer start() {
    int port = readPort("SIHSALUS_BOOTSTRAP_PROBE_PORT", DEFAULT_PORT);
    if (port <= 0) {
      return disabled();
    }

    try {
      InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
      HttpServer server = HttpServer.create(address, 0);
      ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "sihsalus-bootstrap-probe");
        thread.setDaemon(true);
        return thread;
      });

      SihsalusBootstrapProbeServer probeServer = new SihsalusBootstrapProbeServer(server, executor);
      server.createContext("/livez", probeServer::handleLiveness);
      server.createContext("/readyz", probeServer::handleReadiness);
      server.setExecutor(executor);
      server.start();
      log.info("Bootstrap probe server listening on 127.0.0.1:{}", port);
      return probeServer;
    } catch (IOException | RuntimeException ex) {
      log.warn("Bootstrap probe server could not start; continuing without it", ex);
      return disabled();
    }
  }

  void markReady() {
    ready = true;
  }

  void markFailed(Throwable failure) {
    startupFailure = failure;
  }

  @Override
  public void close() {
    if (server != null) {
      server.stop(0);
    }
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  private void handleLiveness(HttpExchange exchange) throws IOException {
    if (startupFailure == null) {
      writeJson(exchange, 200, "{\"status\":\"UP\",\"phase\":\"" + phase() + "\",\"startedAt\":\"" + startedAt + "\"}");
      return;
    }
    writeJson(exchange, 500, "{\"status\":\"DOWN\",\"phase\":\"failed\"}");
  }

  private void handleReadiness(HttpExchange exchange) throws IOException {
    if (ready) {
      writeJson(exchange, 200, "{\"status\":\"UP\",\"phase\":\"ready\"}");
      return;
    }
    writeJson(exchange, 503, "{\"status\":\"OUT_OF_SERVICE\",\"phase\":\"starting\"}");
  }

  private String phase() {
    return ready ? "ready" : "starting";
  }

  private static void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  private static SihsalusBootstrapProbeServer disabled() {
    return new SihsalusBootstrapProbeServer(null, null);
  }

  private static int readPort(String name, int defaultPort) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      return defaultPort;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      log.warn("Invalid {} value {}; using {}", name, value.toLowerCase(Locale.ROOT), defaultPort);
      return defaultPort;
    }
  }
}
