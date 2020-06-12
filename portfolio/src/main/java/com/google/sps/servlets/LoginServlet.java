package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.UserLoginData;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
  private UserService userService = UserServiceFactory.getUserService();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json;");
    boolean isLoggedIn = userService.isUserLoggedIn();
    String urlToRedirect = "/";
    String url = isLoggedIn ? 
      userService.createLogoutURL(urlToRedirect) : userService.createLoginURL(urlToRedirect);
    UserLoginData userLoginData = new UserLoginData(isLoggedIn, url);
    response.getWriter().println(new Gson().toJson(userLoginData));
  }
}
