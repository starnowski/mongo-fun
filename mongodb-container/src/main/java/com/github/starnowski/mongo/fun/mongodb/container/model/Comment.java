package com.github.starnowski.mongo.fun.mongodb.container.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.Date;

import static com.github.starnowski.mongo.fun.mongodb.container.repositories.DaoProperties.COMMENTS_POSTS_ID_COLLUMN_NAME;

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

    @JsonProperty(COMMENTS_POSTS_ID_COLLUMN_NAME)
    @BsonIgnore
    private String postId;

    @BsonProperty(COMMENTS_POSTS_ID_COLLUMN_NAME)
    @JsonIgnore
    private ObjectId postObjectId;

    public Comment withText(String text) {
        this.text = text;
        return this;
    }

    public Comment withDate(Date date) {
        this.date = date;
        return this;
    }

    public Comment withEmail(String email) {
        this.email = email;
        return this;
    }

    public Comment withPostId(String postId) {
        this.setPostId(postId);
        return this;
    }

    public Comment withPostObjectId(ObjectId postObjectId) {
        this.setPostObjectId(postObjectId);
        return this;
    }

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
