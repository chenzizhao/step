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
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  final private int MAX_LIMIT_COMMENTS = 50;
  final private int MAX_CHAR_PER_COMMENT = 280;
  final private String ERR_MSG = 
    String.format("Comment limit must be a non-negative integer, and do not exceed %d.", this.MAX_LIMIT_COMMENTS);

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int limit;
    try {
      limit = Integer.parseInt(request.getParameter("limit"));
    }
    catch(NumberFormatException e){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERR_MSG);
      return;
    }
    if (limit<0){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERR_MSG);
      return;
    }
    if (limit>this.MAX_LIMIT_COMMENTS){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERR_MSG);
      return;
    }
    Query query = new Query("Comment").addSort("like", SortDirection.DESCENDING);
    PreparedQuery pq = this.datastore.prepare(query);

    List<String> comments = pq.asList(FetchOptions.Builder.withLimit(limit))
      .stream()
      .map(entity->(String) entity.getProperty("content"))
      .collect(Collectors.toList());
    
    String json = new Gson().toJson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    String newComment = request.getParameter("new-comment");
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
    
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("content", newComment);
    commentEntity.setProperty("timestamp", timestamp);
    commentEntity.setProperty("like", 0);
    this.datastore.put(commentEntity);
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query q = new Query("Comment").setKeysOnly();
    PreparedQuery pq = this.datastore.prepare(q);
    for (Entity entity : pq.asIterable()){
      this.datastore.delete(entity.getKey());
    }
  }
}
