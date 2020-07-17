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

import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.StatusCode.Code;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.google.rpc.Status;
import com.google.sps.data.AnalysisResults;
import com.google.sps.servlets.ReceiptAnalysis;
import com.google.sps.servlets.ReceiptAnalysis.ReceiptAnalysisException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ImageAnnotatorClient.class, ReceiptAnalysis.class, URL.class})
public final class ReceiptAnalysisTest {
  private static final String EMPTY_BATCH_RESPONSE_WARNING =
      "Received empty batch image annotation response.";
  private static final String RESPONSE_ERROR_WARNING =
      "Received image annotation response with error.";
  private static final String EMPTY_TEXT_ANNOTATIONS_LIST_WARNING =
      "Received image annotation response without text annotations.";
  private static final String REQUEST_FAILED_WARNING = "Image annotation request failed.";

  private static final ByteString IMAGE_BYTES = ByteString.copyFromUtf8("byte string");
  private static final String RAW_TEXT = "raw text";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void serveImageTextUrlReturnsAnalysisResults()
      throws IOException, ReceiptAnalysisException {
    URL url = mock(URL.class);
    InputStream inputStream = new ByteArrayInputStream(IMAGE_BYTES.toByteArray());
    when(url.openStream()).thenReturn(inputStream);

    ImageAnnotatorClient client = mock(ImageAnnotatorClient.class);
    mockStatic(ImageAnnotatorClient.class);
    when(ImageAnnotatorClient.create()).thenReturn(client);

    EntityAnnotation annotation = EntityAnnotation.newBuilder().setDescription(RAW_TEXT).build();
    AnnotateImageResponse response =
        AnnotateImageResponse.newBuilder().addTextAnnotations(annotation).build();
    BatchAnnotateImagesResponse batchResponse =
        BatchAnnotateImagesResponse.newBuilder().addResponses(response).build();
    when(client.batchAnnotateImages(anyList())).thenReturn(batchResponse);

    Image image = Image.newBuilder().setContent(IMAGE_BYTES).build();
    Feature feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
    AnnotateImageRequest request =
        AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
    ImmutableList<AnnotateImageRequest> requests = ImmutableList.of(request);

    AnalysisResults results = ReceiptAnalysis.serveImageText(url);

    Assert.assertEquals(results.getRawText(), RAW_TEXT);
    verify(client).batchAnnotateImages(requests);
  }

  @Test
  public void serveImageTextThrowsIfEmptyBatchResponse()
      throws IOException, ReceiptAnalysisException {
    URL url = mock(URL.class);
    InputStream inputStream = new ByteArrayInputStream(IMAGE_BYTES.toByteArray());
    when(url.openStream()).thenReturn(inputStream);

    ImageAnnotatorClient client = mock(ImageAnnotatorClient.class);
    mockStatic(ImageAnnotatorClient.class);
    when(ImageAnnotatorClient.create()).thenReturn(client);

    BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder().build();
    when(client.batchAnnotateImages(Mockito.<AnnotateImageRequest>anyList()))
        .thenReturn(batchResponse);

    ReceiptAnalysisException exception = Assertions.assertThrows(
        ReceiptAnalysisException.class, () -> { ReceiptAnalysis.serveImageText(url); });

    Assert.assertEquals(EMPTY_BATCH_RESPONSE_WARNING, exception.getMessage());
  }

  @Test
  public void serveImageTextThrowsIfResponseHasError()
      throws IOException, ReceiptAnalysisException {
    URL url = mock(URL.class);
    InputStream inputStream = new ByteArrayInputStream(IMAGE_BYTES.toByteArray());
    when(url.openStream()).thenReturn(inputStream);

    ImageAnnotatorClient client = mock(ImageAnnotatorClient.class);
    mockStatic(ImageAnnotatorClient.class);
    when(ImageAnnotatorClient.create()).thenReturn(client);

    AnnotateImageResponse response =
        AnnotateImageResponse.newBuilder().setError(Status.getDefaultInstance()).build();
    BatchAnnotateImagesResponse batchResponse =
        BatchAnnotateImagesResponse.newBuilder().addResponses(response).build();
    when(client.batchAnnotateImages(Mockito.<AnnotateImageRequest>anyList()))
        .thenReturn(batchResponse);

    ReceiptAnalysisException exception = Assertions.assertThrows(
        ReceiptAnalysisException.class, () -> { ReceiptAnalysis.serveImageText(url); });

    Assert.assertEquals(RESPONSE_ERROR_WARNING, exception.getMessage());
  }

  @Test
  public void serveImageTextThrowsIfEmptyTextAnnotationsList()
      throws IOException, ReceiptAnalysisException {
    URL url = mock(URL.class);
    InputStream inputStream = new ByteArrayInputStream(IMAGE_BYTES.toByteArray());
    when(url.openStream()).thenReturn(inputStream);

    ImageAnnotatorClient client = mock(ImageAnnotatorClient.class);
    mockStatic(ImageAnnotatorClient.class);
    when(ImageAnnotatorClient.create()).thenReturn(client);

    AnnotateImageResponse response = AnnotateImageResponse.newBuilder().build();
    BatchAnnotateImagesResponse batchResponse =
        BatchAnnotateImagesResponse.newBuilder().addResponses(response).build();
    when(client.batchAnnotateImages(Mockito.<AnnotateImageRequest>anyList()))
        .thenReturn(batchResponse);

    ReceiptAnalysisException exception = Assertions.assertThrows(
        ReceiptAnalysisException.class, () -> { ReceiptAnalysis.serveImageText(url); });

    Assert.assertEquals(EMPTY_TEXT_ANNOTATIONS_LIST_WARNING, exception.getMessage());
  }

  @Test
  public void serveImageTextThrowsIfRequestFails() throws IOException, ReceiptAnalysisException {
    URL url = mock(URL.class);
    InputStream inputStream = new ByteArrayInputStream(IMAGE_BYTES.toByteArray());
    when(url.openStream()).thenReturn(inputStream);

    ImageAnnotatorClient client = mock(ImageAnnotatorClient.class);
    mockStatic(ImageAnnotatorClient.class);
    when(ImageAnnotatorClient.create()).thenReturn(client);

    StatusCode statusCode = GrpcStatusCode.of(io.grpc.Status.INTERNAL.getCode());
    ApiException clientException = new ApiException(null, statusCode, false);
    when(client.batchAnnotateImages(Mockito.<AnnotateImageRequest>anyList()))
        .thenThrow(clientException);

    ReceiptAnalysisException exception = Assertions.assertThrows(
        ReceiptAnalysisException.class, () -> { ReceiptAnalysis.serveImageText(url); });

    Assert.assertEquals(REQUEST_FAILED_WARNING, exception.getMessage());
    Assert.assertEquals(clientException, exception.getCause());
  }
}