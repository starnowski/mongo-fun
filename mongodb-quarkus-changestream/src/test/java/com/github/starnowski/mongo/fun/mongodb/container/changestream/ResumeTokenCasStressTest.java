//package com.github.starnowski.mongo.fun.mongodb.container.changestream;
//
//import org.openjdk.jcstress.annotations.*;
//import org.openjdk.jcstress.infra.results.J_Result;
//
//@JCStressTest
//@Outcome(id = "20", expect = Expect.ACCEPTABLE, desc = "Highest counter wins")
//@State
//public class ResumeTokenCasStressTest {
//
//  private final ResumeTokenCas cas = new ResumeTokenCas();
//
//  @Actor
//  public void actor1() {
//    cas.compareAndSet(new ResumeTokenInfo("t1", 10L));
//  }
//
//  @Actor
//  public void actor2() {
//    cas.compareAndSet(new ResumeTokenInfo("t2", 20L));
//  }
//
//  @Actor
//  public void actor3() {
//    cas.compareAndSet(new ResumeTokenInfo("t3", 5L));
//  }
//
//  @Arbiter
//  public void arbiter(J_Result r) {
//    ResumeTokenInfo info = cas.get();
//    r.r1 = info != null ? info.counter() : -1L;
//  }
//}
