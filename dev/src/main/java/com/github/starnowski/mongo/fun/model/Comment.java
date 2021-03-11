package com.github.starnowski.mongo.fun.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import java.util.Date;

public class Comment {

    @JsonProperty("_id")
    @BsonIgnore
    private String id;

    @BsonId
    @JsonIgnore
    private ObjectId oid;

    private String text;

    private Date date;

    private String email;

    @JsonProperty("post_id")
    @BsonIgnore
    private String postId;

    @BsonId
    @JsonIgnore
    private ObjectId postObjectId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.oid = new ObjectId(id);
    }

    public ObjectId getOid() {
        return oid;
    }

    public void setOid(ObjectId oid) {
        this.oid = oid;
        this.id = oid.toHexString();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ObjectId getPostObjectId() {
        return postObjectId;
    }

    public void setPostObjectId(ObjectId postObjectId) {
        this.postObjectId = postObjectId;
        this.postId = postObjectId.toHexString();
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
        this.postObjectId = new ObjectId(postId);
    }
}
