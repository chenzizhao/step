package com.google.sps.data;

public class Comment {
  public String content;
  public long id;
  public long likeCount;

  public Comment(String content, long id, long likeCount) {
    this.content = content;
    this.id = id;
    this.likeCount = likeCount;
  }
}