// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private UserService userService = UserServiceFactory.getUserService();
  final private int MAX_LIMIT_COMMENTS = 50;
  final private int MAX_CHAR_PER_COMMENT = 280;
  final private String ERR_MSG = 
      String.format("Comment limit must be a non-negative integer, and do not exceed %d.", this.MAX_LIMIT_COMMENTS);
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int limit;
    try {
      limit = Integer.parseInt(request.getParameter("limit"));
    } catch (NumberFormatException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERR_MSG);
      return;
    }
    if (limit<0){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERR_MSG);
      return;
    }
    if (limit > this.MAX_LIMIT_COMMENTS) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERR_MSG);
      return;
    }
    
    Query q = new Query("Comment").addSort("likeCount", SortDirection.DESCENDING);
    PreparedQuery pq = this.datastore.prepare(q);

    List<Comment> comments = pq.asList(FetchOptions.Builder.withLimit(limit))
      .stream()
      .map(
          entity -> new Comment(
              (String) entity.getProperty("content"), 
              (long) entity.getKey().getId(),
              (long) entity.getProperty("likeCount"),
              (String) entity.getProperty("email"),
              (float) entity.getProperty("sentimentScore"))
          )
      .collect(Collectors.toList());
    
    String json = new Gson().toJson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    if (!userService.isUserLoggedIn()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Please log in before commenting.");
      return;
    }
    String userEmail = userService.getCurrentUser().getEmail();
    
    String newComment = request.getParameter("newComment");
    if (newComment == null || newComment.isEmpty()){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Comment must be a non-empty string.");
      return;
    }

    if (newComment.length() > this.MAX_CHAR_PER_COMMENT){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
        String.format("Comment must have fewer than %d characters.", this.MAX_CHAR_PER_COMMENT));
      return;
    }
    long timestamp = System.currentTimeMillis();
    float sentimentScore = getSentimentScore(newComment);
    
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("content", newComment);
    commentEntity.setProperty("timestamp", timestamp);
    commentEntity.setProperty("likeCount", 0);
    commentEntity.setProperty("email", userEmail);
    commentEntity.setProperty("sentimentScore", sentimentScore);
    this.datastore.put(commentEntity);
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query q = new Query("Comment").setKeysOnly();
    PreparedQuery pq = this.datastore.prepare(q);
    for (Entity entity : pq.asIterable()) {
      this.datastore.delete(entity.getKey());
    }
  }
  
  private float getSentimentScore(String msg) throws IOException {
    // Use Sentiment Analysis API
    Document doc =
      Document.newBuilder().setContent(msg).setType(Document.Type.PLAIN_TEXT).build();
    LanguageServiceClient languageService = LanguageServiceClient.create();
    Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
    float score = sentiment.getScore();
    languageService.close();
    // The score is a float number between -1 and 1.
    // The greater the score is, the more positive the msg is.
    return score;
  }
}
