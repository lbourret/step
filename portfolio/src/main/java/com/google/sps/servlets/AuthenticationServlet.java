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
import com.google.sps.data.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* Servlet to check user's authentication status- 
*/
@WebServlet("/auth")
public class AuthenticationServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Gson gson = new Gson();
    User user;
    response.setContentType("application/json");

    UserService userService = UserServiceFactory.getUserService();
    
    if (userService.isUserLoggedIn()) {
      String urlToRedirectToAfterUserLogsOut = "/contact.html";
      String logoutUrl = userService.createLogoutURL(urlToRedirectToAfterUserLogsOut);

      user = new User(logoutUrl, true);
    } else {
      String urlToRedirectToAfterUserLogsIn = "/contact.html";
      String loginUrl = userService.createLoginURL(urlToRedirectToAfterUserLogsIn);

      user = new User(loginUrl, false);
    }
    response.getWriter().println(gson.toJson(user));
  }
}
