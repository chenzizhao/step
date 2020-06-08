package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/like")
public class LikeServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    String likeComment = request.getParameter("like");
    if (likeComment==null || likeComment.isEmpty()){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Comment must be a non-empty string.");
      return;
    }

    Filter eqContent = new FilterPredicate("content", FilterOperator.EQUAL, likeComment);
    Query query = new Query("Comment").setFilter(eqContent);
    PreparedQuery pq = this.datastore.prepare(query);
    Entity commentEntity;
    try{
      commentEntity = pq.asSingleEntity();
    } catch(PreparedQuery.TooManyResultsException e){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Comment is not unique.");
      return;
    }
    if (commentEntity==null){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Comment not found in the database");
      return;
    }

    // Update property of the existing comment entity
    commentEntity.setProperty("timestamp", (long)System.currentTimeMillis());
    commentEntity.setProperty("like", (long)commentEntity.getProperty("like")+(long)1);
    this.datastore.put(commentEntity);
  }
}
