/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gcm.demo.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that adds a new message to all registered devices.
 * <p>
 * This servlet is used just by the browser (i.e., not device).
 */
@SuppressWarnings("serial")
public class OnlySaveAllMessagesServlet extends BaseServlet {  

  /**
   * Processes the request to add a new message.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
    String gcmSender = req.getParameter("gcmSender");
    String gcmMessage = req.getParameter("gcmMessage");
    if (null != gcmSender && null != gcmMessage && !gcmSender.isEmpty()
        && !gcmMessage.isEmpty()) {
      Datastore.saveAlert(gcmSender, gcmMessage);      
      String status = "Successfully saved the Alert";
      req.setAttribute(HomeServlet.ATTRIBUTE_STATUS, status.toString());
    } else {
      req.setAttribute(HomeServlet.ATTRIBUTE_STATUS, "The Message and sender cannot be empty.");
    }
    getServletContext().getRequestDispatcher("/alertSaver").forward(req, resp);
  }

}
