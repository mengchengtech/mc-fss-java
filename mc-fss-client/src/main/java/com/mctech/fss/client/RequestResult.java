package com.mctech.fss.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.mctech.fss.client.exception.MCTechFssRequestException;
import lombok.SneakyThrows;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestResult implements Closeable {
  private final CloseableHttpResponse response;
  private final Map<String, String> headers;

  private final int statusCode;

  /**
   * @return 返回结果状态码
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * 返回结果状态码
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * @return 以字符串方式获取返回的文本内容
   */
  public String getContent() throws IOException {
    HttpEntity entity = this.response.getEntity();
    StringBuilder builder = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
    }
    return builder.toString();
  }

  public JSON getJsonObject() throws IOException {
    HttpEntity entity = this.response.getEntity();
    InputStream in = entity.getContent();
    return JSON.parseObject(in, JSON.class);
  }

  public <T> T getObject(Class<T> cls) throws IOException {
    HttpEntity entity = this.response.getEntity();
    InputStream in = entity.getContent();
    return JSON.parseObject(in, cls);
  }

  public <T> List<T> getList(Class<T> cls) throws IOException {
    HttpEntity entity = this.response.getEntity();
    InputStream in = entity.getContent();
    JSONArray array = JSON.parseObject(in, JSONArray.class);
    return array.toJavaList(cls);
  }

  /**
   * @return 获取一个用于读返回结果的流
   */
  public InputStream openRead() throws IOException {
    HttpEntity entity = this.response.getEntity();
    return entity.getContent();
  }

  private String contentType = null;

  /**
   * @return 获取内容的ContentType
   */
  public String getContentType() {
    return this.contentType;
  }

  RequestResult(CloseableHttpResponse response) throws MCTechFssRequestException {
    this.response = response;
    HttpEntity entity = response.getEntity();
    if(entity != null) {
      Header h = response.getEntity().getContentType();
      if (h != null) {
        contentType = h.getValue();
      }
    }

    this.headers = new HashMap<>();
    for (Header header : response.getAllHeaders()) {
      this.headers.put(header.getName(), header.getValue());
    }

    this.statusCode = response.getStatusLine().getStatusCode();

    if (this.statusCode >= HttpStatus.SC_BAD_REQUEST) {
      FssClientError error = createError(response);
      String message = error.getMessage();
      throw new MCTechFssRequestException(message, error);
    }
  }

  public void close() throws IOException {
    this.response.close();
  }

  @SneakyThrows
  private static FssClientError createError(CloseableHttpResponse response) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document document = db.parse(response.getEntity().getContent());
    NodeList items = document.getDocumentElement().getChildNodes();

    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < items.getLength(); i++) {
      Node item = items.item(i);
      String name = item.getNodeName();
      String value = item.getTextContent();
      map.put(name, value);
    }

    return new FssClientError(map);
  }
}
