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

package com.google.sps;

import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.sps.servlets.SpendingAnalyticsServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public final class SpendingAnalyticsServletTest {
  private static final HashMap<String, Double> EXPECTED_STORE_ANALYTICS = new HashMap<>();
  private static final HashMap<String, Double> EXPECTED_CATEGORY_ANALYTICS = new HashMap<>();

  // Local Datastore
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()).setEnvIsLoggedIn(true);

  @Mock private SpendingAnalyticsServlet servlet;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private DatastoreService datastore;
  private StringWriter stringWriter;
  private PrintWriter writer;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    helper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();

    servlet = new SpendingAnalyticsServlet(datastore);

    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    EXPECTED_STORE_ANALYTICS.put("walmart", 26.12);
    EXPECTED_STORE_ANALYTICS.put("contoso", 14.51);
    EXPECTED_STORE_ANALYTICS.put("main street restaurant", 29.01);

    EXPECTED_CATEGORY_ANALYTICS.put("candy", 26.12);
    EXPECTED_CATEGORY_ANALYTICS.put("drink", 26.12);
    EXPECTED_CATEGORY_ANALYTICS.put("cappuccino", 14.51);
    EXPECTED_CATEGORY_ANALYTICS.put("food", 43.52);
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void doGetWithReceiptsInDatastore() throws IOException {
    // Receipts in datastore:
    // Walmart: $26.12, Contoso: $14.51, Main Street Restaurant: $29.01
    TestUtils.addTestReceipts(datastore);

    servlet.doGet(request, response);
    writer.flush();

    HashMap<String, Double> storeAnalytics = TestUtils.parseStoreAnalytics(stringWriter.toString());
    HashMap<String, Double> categoryAnalytics =
        TestUtils.parseCategoryAnalytics(stringWriter.toString());

    Assert.assertTrue(EXPECTED_STORE_ANALYTICS.equals(storeAnalytics));
    Assert.assertTrue(EXPECTED_CATEGORY_ANALYTICS.equals(categoryAnalytics));
  }

  @Test
  public void doGetWithNoReceiptsInDatastore() throws IOException {
    servlet.doGet(request, response);
    writer.flush();

    // Make sure empty HashMaps are returned.
    HashMap<String, Double> storeAnalytics = TestUtils.parseStoreAnalytics(stringWriter.toString());
    HashMap<String, Double> categoryAnalytics =
        TestUtils.parseCategoryAnalytics(stringWriter.toString());

    Assert.assertTrue(storeAnalytics.isEmpty());
    Assert.assertTrue(categoryAnalytics.isEmpty());
  }
}