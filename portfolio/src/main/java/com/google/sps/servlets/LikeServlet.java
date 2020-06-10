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
Behavior: update "like" attribute in the database
*/

@WebServlet("/like")
public class LikeServlet extends HttpServlet {
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {    
    long commentId;
    try {
      commentId = Long.parseLong(request.getParameter("commentId"));
    } catch (NumberFormatException e){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid comment ID.");
      return;
    }
    Key likeKey = KeyFactory.createKey("Comment", commentId);

    int retries = 3;
    while (true) {
      Transaction txn = datastore.beginTransaction();
      Entity commentEntity;
      try {
        commentEntity = datastore.get(likeKey);        
      } catch (EntityNotFoundException e) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Comment does not exist");
        break;
      }
      long count = (long) commentEntity.getProperty("like");
      ++count;
      try {
        Thread.sleep(5000); // test 
        System.out.println("sleep ended");
      } catch (InterruptedException e) {
        e.printStackTrace();
        break; // 5000 for test only, related to Thread.sleep
      }
      commentEntity.setProperty("like", count);
      datastore.put(txn, commentEntity);
      try {
        txn.commit();
        System.out.println("transaction success"); // DEBUG
        break;
      } catch (ConcurrentModificationException e) {
        if (retries == 0) {
          System.out.println("concurrent modification exception"); // DEBUG
          // throw e;
          response.sendError(HttpServletResponse.SC_CONFLICT, "Database busy");
        }
        // Allow retry to occur
        System.out.println("reties"); // DEBUG
        --retries;
      } finally {
        if (txn.isActive()) {
          txn.rollback();
        }
      }
    } // end of while loop
  } // end of doPost
}
