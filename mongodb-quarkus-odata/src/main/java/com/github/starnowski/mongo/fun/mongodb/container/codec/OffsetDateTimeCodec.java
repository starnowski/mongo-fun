package com.github.starnowski.mongo.fun.mongodb.container.codec;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;

public class OffsetDateTimeCodec implements Codec<OffsetDateTime> {

  @Override
  public void encode(
      BsonWriter writer, OffsetDateTime value, org.bson.codecs.EncoderContext encoderContext) {
    if (value != null) {
      // Convert OffsetDateTime → Instant → Date → store as BSON date
      writer.writeDateTime(value.toInstant().toEpochMilli());
    } else {
      writer.writeNull();
    }
  }

  @Override
  public OffsetDateTime decode(BsonReader reader, org.bson.codecs.DecoderContext decoderContext) {
    if (reader.getCurrentBsonType() == org.bson.BsonType.NULL) {
      reader.readNull();
      return null;
    }
    long millis = reader.readDateTime();
    // Convert BSON date → Instant → OffsetDateTime
    return OffsetDateTime.ofInstant(new Date(millis).toInstant(), ZoneOffset.UTC);
  }

  @Override
  public Class<OffsetDateTime> getEncoderClass() {
    return OffsetDateTime.class;
  }
}
