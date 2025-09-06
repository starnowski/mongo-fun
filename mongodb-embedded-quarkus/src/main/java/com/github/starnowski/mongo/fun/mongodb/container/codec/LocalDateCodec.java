package com.github.starnowski.mongo.fun.mongodb.container.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class LocalDateCodec implements Codec<LocalDate> {

    private final ZoneId zoneId;

    public LocalDateCodec() {
        this.zoneId = ZoneId.of("UTC"); // or your preferred zone
    }

    public LocalDateCodec(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public void encode(BsonWriter writer, LocalDate value, org.bson.codecs.EncoderContext encoderContext) {
        if (value != null) {
            Instant instant = value.atStartOfDay(zoneId).toInstant();
            writer.writeDateTime(instant.toEpochMilli());
        } else {
            writer.writeNull();
        }
    }

    @Override
    public LocalDate decode(BsonReader reader, org.bson.codecs.DecoderContext decoderContext) {
        if (reader.getCurrentBsonType() == org.bson.BsonType.NULL) {
            reader.readNull();
            return null;
        }
        long millis = reader.readDateTime();
        return Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate();
    }

    @Override
    public Class<LocalDate> getEncoderClass() {
        return LocalDate.class;
    }
}
