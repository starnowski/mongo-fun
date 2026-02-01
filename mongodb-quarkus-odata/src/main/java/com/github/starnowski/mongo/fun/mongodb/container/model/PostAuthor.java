package com.github.starnowski.mongo.fun.mongodb.container.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class PostAuthor {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("count")
  @BsonProperty("count")
  private int numPosts;

  public PostAuthor() {}

  public PostAuthor(String id, int numPosts) {
    this.id = id;
    this.numPosts = numPosts;
  }

  public int getNumPosts() {
    return numPosts;
  }

  public void setNumPosts(int numPosts) {
    this.numPosts = numPosts;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PostAuthor that = (PostAuthor) o;
    return numPosts == that.numPosts && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, numPosts);
  }

  @Override
  public String toString() {
    return "PostAuthor{" + "id='" + id + '\'' + ", numPosts=" + numPosts + '}';
  }
}
