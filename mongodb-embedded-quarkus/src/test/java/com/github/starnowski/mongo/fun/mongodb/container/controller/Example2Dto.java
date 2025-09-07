package com.github.starnowski.mongo.fun.mongodb.container.controller;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Example2Dto {
    private String plainString;
    private byte[] base64Data;
    private InputStream fileUpload; // usually not serialized directly
    private LocalDate birthDate;
    private OffsetDateTime timestamp;
    private String password;
    private UUID uuidProp;
    private BigDecimal genericNumber;
    private Float floatValue;
    private Double doubleValue;
    private BigInteger genericInteger;
    private Integer smallInteger;
    private Long bigInteger;
    private Boolean isActive;
    private List<String> tags;
    private Map<String, Object> metadata;

    public UUID getUuidProp() {
        return uuidProp;
    }

    public void setUuidProp(UUID uuidProp) {
        this.uuidProp = uuidProp;
    }

    public String getPlainString() {
        return plainString;
    }

    public void setPlainString(String plainString) {
        this.plainString = plainString;
    }

    public byte[] getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(byte[] base64Data) {
        this.base64Data = base64Data;
    }

    public InputStream getFileUpload() {
        return fileUpload;
    }

    public void setFileUpload(InputStream fileUpload) {
        this.fileUpload = fileUpload;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getGenericNumber() {
        return genericNumber;
    }

    public void setGenericNumber(BigDecimal genericNumber) {
        this.genericNumber = genericNumber;
    }

    public Float getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(Float floatValue) {
        this.floatValue = floatValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public BigInteger getGenericInteger() {
        return genericInteger;
    }

    public void setGenericInteger(BigInteger genericInteger) {
        this.genericInteger = genericInteger;
    }

    public Integer getSmallInteger() {
        return smallInteger;
    }

    public void setSmallInteger(Integer smallInteger) {
        this.smallInteger = smallInteger;
    }

    public Long getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(Long bigInteger) {
        this.bigInteger = bigInteger;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
// getters and setters ...
}
