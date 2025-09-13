package com.github.starnowski.mongo.fun.mongodb.container.codec;

import org.bson.*;
import org.bson.codecs.*;

import java.math.BigInteger;

public class BigIntegerCodec implements Codec<BigInteger> {

    @Override
    public void encode(BsonWriter writer, BigInteger value, EncoderContext encoderContext) {
        writer.writeString(value.toString()); // store as string
    }

    @Override
    public BigInteger decode(BsonReader reader, DecoderContext decoderContext) {
        return new BigInteger(reader.readString());
    }

    @Override
    public Class<BigInteger> getEncoderClass() {
        return BigInteger.class;
    }
}
