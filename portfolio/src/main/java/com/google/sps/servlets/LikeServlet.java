package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.EntityNotFoundException;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ConcurrentModificationException;
import com.google.appengine.api.datastore.Transaction;

/* 
REQUEST: a POST query string like this "/like?likeID=1234567890123456"
Behavior: update "like" counter in the database
*/

@WebServlet("/like")
public class LikeServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    long commentId;
    try {
      commentId = Long.parseLong(request.getParameter("commentId"));
    } catch (NumberFormatException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid comment ID.");
      return;
    }
    Key likeKey = KeyFactory.createKey("Comment", commentId);
    // Wrap retrieving and storing entity in one transaction
    // to avoid counting collision. 
    int retries = 3;
    while (true) {
      Transaction txn = datastore.beginTransaction();
      try {
        buildTransaction(datastore, likeKey, txn);
      } catch (EntityNotFoundException e) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Comment not found.");
        break;
      }
      try {
        txn.commit();
        break;
      } catch (ConcurrentModificationException e) {
        if (retries == 0) {
          response.sendError(HttpServletResponse.SC_CONFLICT, "Database busy.");
        }
        --retries;
      } finally {
        if (txn.isActive()) {
          txn.rollback();
        }
      }
    } // end of while loop
  } // end of doPost

  private void buildTransaction(DatastoreService datastore, Key key, Transaction txn) 
    throws EntityNotFoundException{
    Entity commentEntity;
    try {
      commentEntity = datastore.get(key);        
    } catch (EntityNotFoundException e) {
      throw e;
    }
    long count = (long) commentEntity.getProperty("likeCount");
    ++count;
    commentEntity.setProperty("likeCount", count);
    datastore.put(txn, commentEntity);
  }
}
