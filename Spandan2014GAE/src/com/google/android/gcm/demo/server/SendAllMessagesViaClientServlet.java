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

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import com.asb.gae.spandan.DESedeEncryption;
import com.asb.gae.spandan.SomeClass;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that adds a new message to all registered devices.
 * <p>
 * This servlet is used just by the browser (i.e., not device).
 */
@SuppressWarnings("serial")
public class SendAllMessagesViaClientServlet extends BaseServlet {
  

  /**
   * Processes the request to add a new message.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
    String gcmSender = req.getParameter("sender");
    String gcmMessage = req.getParameter("message");
    String gcmSecurity = req.getParameter("someText");
    if (null != gcmSender && null != gcmMessage && !gcmSender.isEmpty()
        && !gcmMessage.isEmpty() && null != gcmSecurity
        && !gcmSecurity.isEmpty()) {
      DESedeEncryption deSedeEncryption = null;
      try {
        deSedeEncryption = new DESedeEncryption();
        String text = deSedeEncryption.decrypt(gcmSecurity);
        if (text.equals(SomeClass.SOME_STRING)) {

          Datastore.saveAlert(gcmSender, gcmMessage);
          List<String> devices = Datastore.getDevices();
          String status;
          if (devices.isEmpty()) {
            status = "Message ignored as there is no device registered!";
          } else {
            Queue queue = QueueFactory.getQueue("gcm");
            // NOTE: check below is for demonstration purposes; a real
            // application
            // could always send a multicast, even for just one recipient
            if (devices.size() == 1) {
              // send a single message using plain post
              String device = devices.get(0);
              queue.add(withUrl("/send")
                  .param(SendMessageServlet.PARAMETER_DEVICE, device)
                  .param(SendMessageServlet.PARAMETER_MESSAGE, gcmMessage)
                  .param(SendMessageServlet.PARAMETER_SENDER, gcmSender));
              status = "Single message queued for registration id " + device;
            } else {
              // send a multicast message using JSON
              // must split in chunks of 1000 devices (GCM limit)
              int total = devices.size();
              List<String> partialDevices = new ArrayList<String>(total);
              int counter = 0;
              int tasks = 0;
              for (String device : devices) {
                counter++;
                partialDevices.add(device);
                int partialSize = partialDevices.size();
                if (partialSize == Datastore.MULTICAST_SIZE || counter == total) {
                  String multicastKey = Datastore
                      .createMulticast(partialDevices);
                  logger.fine("Queuing " + partialSize
                      + " devices on multicast " + multicastKey);
                  TaskOptions taskOptions = TaskOptions.Builder
                      .withUrl("/send")
                      .param(SendMessageServlet.PARAMETER_MULTICAST,
                          multicastKey)
                      .param(SendMessageServlet.PARAMETER_MESSAGE, gcmMessage)
                      .param(SendMessageServlet.PARAMETER_SENDER, gcmSender)
                      .method(Method.POST);
                  queue.add(taskOptions);
                  partialDevices.clear();
                  tasks++;
                }
              }
              status = "Queued tasks to send " + tasks
                  + " multicast messages to " + total + " devices";
            }
          }

          req.setAttribute(HomeServlet.ATTRIBUTE_STATUS, status.toString());
        }
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    } else {
      req.setAttribute(HomeServlet.ATTRIBUTE_STATUS,
          "The Message and sender cannot be empty.");
    }
    
    resp.getWriter().print("Success!");

  }

}
