package com.github.starnowski.mongo.fun;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class TestHelper {

  public static void runAssertion(int maxSeconds, int delayInSeconds, Runnable runnable) {
    await()
        .atMost(maxSeconds, SECONDS)
        .pollInterval(delayInSeconds, SECONDS)
        .until(
            () -> {
              // THEN
              runnable.run();
              return true;
            });
  }
}
