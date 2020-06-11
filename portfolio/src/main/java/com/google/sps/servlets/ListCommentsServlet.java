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
import java.io.UnsupportedEncodingException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.api.client.http.HttpResponseException;
import com.google.cloud.translate.TranslateException;

/** Servlet that returns comments */
@WebServlet("/list-comments")
public class ListCommentsServlet extends HttpServlet {

  DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  Gson gson = new Gson();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("Comment");

    // Filter on name.
    query = filterQuery(request, query, "searchName", "username");

    // Select sort method.
    String sort = getParameter(request, "sort", "descending");
    if (sort.equals("ascending")){
      query.addSort("timestamp", SortDirection.ASCENDING);
    } else {
      query.addSort("timestamp", SortDirection.DESCENDING);
    }
    
    // Get max limit on comments. Invalid input will defult to 5.
    int limit;
    try {
      limit =  Integer.parseInt(getParameter(request, "limit", "5"));
    } catch (NumberFormatException e) {
      limit = 5;
    }
    List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(limit));

    // Populate list with comment kind of entities
    ArrayList<Comment> comments = new ArrayList<>();
    for (Entity entity : results) {
      long id = entity.getKey().getId();
      String username = (String) entity.getProperty("username");
      String email = (String) entity.getProperty("email");
      String text = (String) entity.getProperty("text");
      String image = (String) entity.getProperty("image");
      long timestamp = (long) entity.getProperty("timestamp");

      // Translate text
      text = translate(request, text);

      Comment comment = new Comment(id, username, email, text, image, timestamp);
      comments.add(comment);
    }

    response.setContentType("application/json;");
    response.setCharacterEncoding("UTF-8"); 
    response.getWriter().println(gson.toJson(comments));
  }

  /**
   * Get document value of param
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

  /**
   * Filter query based on given params
   * @param searchParam parameter that holds specified value to filter on
   * @param matchParam parameter to filter for specified value 
   * @return query filtered by searchParam that matches matchParam
   */
  private Query filterQuery(HttpServletRequest request, Query query, String searchParam, String matchParam)  {
    String searchValue = getParameter(request, searchParam, null);

    // No param to filter on.
    if (searchValue.equals("")){
      return query;
    }

    Filter filter = new FilterPredicate(matchParam, FilterOperator.EQUAL, searchValue);
    return query.setFilter(filter);
  }

  /**
   * Translates text
   * @param text comment to translate
   * @return translated text
   */
  private String translate(HttpServletRequest request, String text) {
    // Get the request language param with English default.
    String languageCode = getParameter(request, "language", "en");

    String translatedText;
    try {
      Translate translate = TranslateOptions.getDefaultInstance().getService();
      Translation translation = translate.translate(text, Translate.TranslateOption.targetLanguage(languageCode));
      translatedText = translation.getTranslatedText();
    } catch (TranslateException e) {
      return text;
    }
    return translatedText;
  }
}
