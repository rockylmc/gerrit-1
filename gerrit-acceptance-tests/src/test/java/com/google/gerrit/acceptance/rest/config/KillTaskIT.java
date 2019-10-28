// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.acceptance.rest.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.server.config.ListTasks.TaskInfo;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpStatus;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class KillTaskIT extends AbstractDaemonTest {

  @Test
  public void killTask() throws IOException {
    RestResponse r = adminSession.get("/config/server/tasks/");
    List<TaskInfo> result = newGson().fromJson(r.getReader(),
        new TypeToken<List<TaskInfo>>() {}.getType());
    r.consume();
    int taskCount = result.size();
    assertTrue(taskCount > 0);

    r = adminSession.delete("/config/server/tasks/" + result.get(0).id);
    assertEquals(HttpStatus.SC_NO_CONTENT, r.getStatusCode());
    r.consume();

    r = adminSession.get("/config/server/tasks/");
    result = newGson().fromJson(r.getReader(),
        new TypeToken<List<TaskInfo>>() {}.getType());
    r.consume();
    assertEquals(taskCount - 1, result.size());
  }

  @Test
  public void killTask_NotFound() throws IOException {
    RestResponse r = adminSession.get("/config/server/tasks/");
    List<TaskInfo> result = newGson().fromJson(r.getReader(),
        new TypeToken<List<TaskInfo>>() {}.getType());
    r.consume();
    assertTrue(result.size() > 0);

    r = userSession.delete("/config/server/tasks/" + result.get(0).id);
    assertEquals(HttpStatus.SC_NOT_FOUND, r.getStatusCode());
  }
}
