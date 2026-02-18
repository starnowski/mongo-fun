package com.github.starnowski.mongo.fun.mongodb.container.changestream;

import java.util.concurrent.atomic.AtomicReference;

public class ResumeTokenCas {

  private final AtomicReference<ResumeTokenInfo> reference = new AtomicReference<>();

  public void compareAndSet(ResumeTokenInfo value) {
    if (value == null) {
      return;
    }
    reference.accumulateAndGet(
        value,
        (current, newValue) -> {
          if (current == null || newValue.counter() > current.counter()) {
            return newValue;
          }
          return current;
        });
  }

  public ResumeTokenInfo get() {
    return reference.get();
  }
}
