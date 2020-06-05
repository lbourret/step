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

import java.util.*;
import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/** Servlet that returns comments */
@WebServlet("/list-comments")
public class ListCommentsServlet extends HttpServlet {

  DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  Gson gson = new Gson();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    System.out.println("start list");
    // Get max limit on comments.
    int limit = getLimit(request);

    List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(limit));
    ArrayList<Comment> comments = new ArrayList<>();

    // Populate list with comment kind of entities
    for (Entity entity : results) {
      long id = entity.getKey().getId();
      String title = (String) entity.getProperty("title");
      long timestamp = (long) entity.getProperty("timestamp");

      Comment comment = new Comment(id, title, timestamp);
      comments.add(comment);
    }
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
        System.out.println("end list");
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  /** Returns the choice entered by the player, or -1 if the choice was invalid. */
  private int getLimit(HttpServletRequest request) {
    // Get the input from the form.
    String limitString = request.getParameter("limit");

    // Convert the input to an int.
    int limit;
    try {
      limit = Integer.parseInt(limitString);
    } catch (NumberFormatException e) {
      System.err.println("Could not convert to int: " + limitString);
      return 0;
    }
    return limit;
  }
  
}
