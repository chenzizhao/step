package com.google.sps.data;

public class UserLoginData {
  public boolean isLoggedIn;
  public String url;
  // If the user is logged in, the url leads to log out page,
  // and vice versa.

  public UserLoginData(boolean isLoggedIn, String url) {
    this.isLoggedIn = isLoggedIn;
    this.url = url;
  }
}