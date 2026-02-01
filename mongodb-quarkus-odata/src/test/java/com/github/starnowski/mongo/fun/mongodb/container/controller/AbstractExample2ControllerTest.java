package com.github.starnowski.mongo.fun.mongodb.container.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;

public abstract class AbstractExample2ControllerTest {

  protected JavaTimeModule javaTimeModule;
  protected EasyRandom generator;
  protected ObjectMapper mapper;

  @PostConstruct
  public void init() {
    javaTimeModule = new JavaTimeModule();

    // Custom serializer for OffsetDateTime
    javaTimeModule.addSerializer(
        OffsetDateTime.class,
        new com.fasterxml.jackson.databind.JsonSerializer<OffsetDateTime>() {
          @Override
          public void serialize(
              OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            // https://www.mongodb.com/docs/manual/reference/method/Date/
            /*
            Internally, Mongod Date objects are stored as a signed 64-bit integer representing the number of milliseconds since the Unix epoch (Jan 1, 1970).
            No microseconds or nanoseconds
            */
            gen.writeString(
                value
                    .truncatedTo(ChronoUnit.MILLIS)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
          }
        });
    //        javaTimeModule.disable();
    // https://www.mongodb.com/docs/manual/reference/method/Date/
    EasyRandomParameters parameters =
        new EasyRandomParameters()
            .randomize(
                InputStream.class,
                new Randomizer<InputStream>() {
                  private final Random random = new Random();

                  @Override
                  public InputStream getRandomValue() {
                    byte[] bytes = new byte[16 + random.nextInt(64)]; // random size 16–80 bytes
                    random.nextBytes(bytes);
                    return new ByteArrayInputStream(bytes);
                  }
                })
            .randomize(
                Object.class,
                new Randomizer<Object>() {

                  private final Random random = new Random();

                  @Override
                  public Object getRandomValue() {
                    return random.nextInt();
                  }
                });
    generator = new EasyRandom(parameters);
    mapper = new ObjectMapper();
    mapper.registerModule(javaTimeModule);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }
}
