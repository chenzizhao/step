package com.google.sps.data;

public class Comment {
  public String content;
  public long id;
  public long likeCount;
  public String email;
  public float sentimentScore;

  public Comment(String content, long id, long likeCount, String email, float sentimentScore) {
    this.content = content;
    this.id = id;
    this.likeCount = likeCount;
    this.email = email;
    this.sentimentScore = sentimentScore;
  }
}