package com.github.starnowski.mongo.fun.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

public class Post {
    @JsonProperty("_id")
    @BsonIgnore
    private String id;

    @BsonId
    @JsonIgnore
    private ObjectId oid;

    private String text;

    private Date date;

    private String email;

    private List<Comment> comments;

    public Post withText(String text) {
        this.text = text;
        return this;
    }

    public Post withDate(Date date) {
        this.date = date;
        return this;
    }

    public Post withEmail(String email) {
        this.email = email;
        return this;
    }

    public Post withComments(List<Comment> comments) {
        this.comments = comments;
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

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
}
