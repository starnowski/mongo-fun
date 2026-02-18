package com.github.starnowski.mongo.fun.mongodb.container.changestream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResumeTokenCasTest {

  @Test
  public void shouldSetInitialValueIfNull() {
    // GIVEN
    ResumeTokenCas resumeTokenCas = new ResumeTokenCas();
    ResumeTokenInfo newValue = new ResumeTokenInfo("token1", 10L);

    // WHEN
    resumeTokenCas.compareAndSet(newValue);

    // THEN
    Assertions.assertEquals(newValue, resumeTokenCas.get());
  }

  @Test
  public void shouldUpdateValueIfCounterIsHigher() {
    // GIVEN
    ResumeTokenCas resumeTokenCas = new ResumeTokenCas();
    ResumeTokenInfo initialValue = new ResumeTokenInfo("token1", 10L);
    resumeTokenCas.compareAndSet(initialValue);
    ResumeTokenInfo newValue = new ResumeTokenInfo("token2", 20L);

    // WHEN
    resumeTokenCas.compareAndSet(newValue);

    // THEN
    Assertions.assertEquals(newValue, resumeTokenCas.get());
  }

  @Test
  public void shouldNotUpdateValueIfCounterIsLower() {
    // GIVEN
    ResumeTokenCas resumeTokenCas = new ResumeTokenCas();
    ResumeTokenInfo initialValue = new ResumeTokenInfo("token1", 10L);
    resumeTokenCas.compareAndSet(initialValue);
    ResumeTokenInfo newValue = new ResumeTokenInfo("token2", 5L);

    // WHEN
    resumeTokenCas.compareAndSet(newValue);

    // THEN
    Assertions.assertEquals(initialValue, resumeTokenCas.get());
  }

  @Test
  public void shouldNotUpdateValueIfCounterIsEqual() {
    // GIVEN
    ResumeTokenCas resumeTokenCas = new ResumeTokenCas();
    ResumeTokenInfo initialValue = new ResumeTokenInfo("token1", 10L);
    resumeTokenCas.compareAndSet(initialValue);
    ResumeTokenInfo newValue = new ResumeTokenInfo("token2", 10L);

    // WHEN
    resumeTokenCas.compareAndSet(newValue);

    // THEN
    Assertions.assertEquals(initialValue, resumeTokenCas.get());
  }

  @Test
  public void shouldDoNothingIfNewValueIsNull() {
    // GIVEN
    ResumeTokenCas resumeTokenCas = new ResumeTokenCas();
    ResumeTokenInfo initialValue = new ResumeTokenInfo("token1", 10L);
    resumeTokenCas.compareAndSet(initialValue);

    // WHEN
    resumeTokenCas.compareAndSet(null);

    // THEN
    Assertions.assertEquals(initialValue, resumeTokenCas.get());
  }
}
