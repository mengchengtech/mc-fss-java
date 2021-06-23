package com.mctech.fss.client;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
public class MCFssClient {
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final MCFssClientConfig config;
  private final URI defaultEndpoint;
  private final URI publicEndPoint;

  private final CloseableHttpClient httpClient;

  @SneakyThrows
  public MCFssClient(MCFssClientConfig config) {
    this.config = config;
    this.publicEndPoint = new URI(config.getPublicEndPoint());
    this.defaultEndpoint = config.isInternal()
        ? new URI(config.getPrivateEndPoint())
        : this.publicEndPoint;
    this.httpClient = HttpClients.createDefault();
  }

  public RequestResult get(String key) {
    SignDataOption option = new SignDataOption();
    option.setMethod(FssOperation.GET);
    option.setKey(key);
    SignedData signedData = this.generateSignedData(option);
    return this.sendRequest(signedData, new HttpGet());
  }

  /**
   * @param key         文件存到服务器上的key
   * @param fileName    文件原始名称，下载时使用。可为null
   * @param is          要上传的文件内容流。
   * @param metadata    文件附加的meta信息。可为null
   * @param contentType 文件的content-type，下载的时候会用到。可为null
   * @param length      上传的内容长度。可为null
   * @return
   */
  @SneakyThrows
  public RequestResult put(String key, String fileName, InputStream is,
                           Map<String, String> metadata, String contentType, Long length) {
    Map<String, String> fssMetadata = new HashMap<>();
    if (metadata != null) {
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        String fssKey = HttpConsts.FSS_META_HEADER_PREFIX + entry.getKey();
        fssMetadata.put(fssKey, entry.getValue());
      }
    }

    String rawName = fileName != null ?
        new File(fileName).getName() : new File(key).getName();

    AbstractHttpEntity entity;
    if (length != null) {
      if (length <= 0) {
        throw new IllegalArgumentException("length必须为大于0的整数");
      }
      entity = new InputStreamEntity(is, length);
    } else {
      entity = new InputStreamEntity(is);
    }
    entity.setContentType(contentType);

    SignDataOption option = new SignDataOption();
    option.setMethod(FssOperation.PUT);
    option.setKey(key);
    option.setContentType(contentType);
    option.setMetadata(fssMetadata);

    SignedData signedData = this.generateSignedData(option);
    HttpPut httpPut = new HttpPut();
    httpPut.setEntity(entity);
    httpPut.setHeader(HttpConsts.CONTENT_DISPOSITION,
        "attachment;filename=" + URLEncoder.encode(rawName, "UTF-8"));
    return this.sendRequest(signedData, httpPut);
  }

  public void delete(String key) {
    SignDataOption option = new SignDataOption();
    option.setKey(key);
    option.setMethod(FssOperation.DELETE);
    SignedData signedData = this.generateSignedData(option);
    HttpDelete delete = new HttpDelete();
    delete.setHeader(HttpHeaders.CONTENT_LENGTH, "0");
    this.sendRequest(signedData, delete);
  }

  public void copy(String toKey, String fromKey) {
    SignDataOption option = new SignDataOption();
    option.setKey(toKey);
    option.setMethod(FssOperation.PUT);
    Map<String, String> meta = new HashMap<>();
    meta.put(HttpConsts.FSS_COPY_FILE_HEADER, fromKey);
    option.setMetadata(meta);
    SignedData signedData = this.generateSignedData(option);
    this.sendRequest(signedData, new HttpPut());
  }

  public String generateObjectUrl(String key) {
    String resource = this.getResource(key);
    URIBuilder builder = createUriBuilder(resource, false);
    return builder.toString();
  }

  private URIBuilder createUriBuilder(String resource, boolean usePublic) {
    URI endPoint = usePublic ? this.publicEndPoint : this.defaultEndpoint;
    URIBuilder builder = new URIBuilder(endPoint);
    String basePath = builder.getPath();
    String absolutePath;
    if (basePath.endsWith("/")) {
      if (resource.startsWith("/")) {
        absolutePath = basePath.substring(0, basePath.length() - 1) + resource;
      } else {
        absolutePath = basePath + resource;
      }
    } else {
      if (resource.startsWith("/")) {
        absolutePath = basePath + resource;
      } else {
        absolutePath = basePath + "/" + resource;
      }
    }
    builder.setPath(absolutePath);
    return builder;
  }

  /**
   * @param {String} key
   * @param {any}    option
   */
  public String getSignatureUrl(String key, SignatureOption option) {
    String resource = this.getResource(key);
    SignedResource sign = this.signatureResource(resource, option);

    // 默认为给外部使用，所以指定用外网地址
    URIBuilder builder = createUriBuilder(resource, true);
    builder.addParameter(HttpConsts.ACCESS_KEY_ID, this.config.getAccessKeyId());
    builder.addParameter(HttpConsts.EXPIRES, Long.toString(sign.getExpires()));
    builder.addParameter(HttpConsts.SIGNATURE, sign.getSignature());

    for (Map.Entry<String, String> entry : sign.getSubResource().entrySet()) {
      builder.addParameter(entry.getKey(), entry.getValue());
    }

    return builder.toString();
  }

  /**
   * @param {String} key
   */
  public ObjectMeta head(String key) {
    SignDataOption option = new SignDataOption();
    option.setKey(key);
    option.setMethod(FssOperation.HEAD);
    SignedData signedData = this.generateSignedData(option);
    RequestResult result = this.sendRequest(signedData, new HttpHead());

    Map<String, String> meta = new HashMap<>();
    for (Map.Entry<String, String> entry : result.getHeaders().entrySet()) {
      if (entry.getKey().startsWith(HttpConsts.FSS_META_HEADER_PREFIX)) {
        meta.put(entry.getKey().substring(11), entry.getValue());
      }
    }

    ObjectMeta om = new ObjectMeta();
    om.setMeta(meta);
    om.setHeaders(result.getHeaders());
    om.setStatusCode(result.getStatusCode());
    return om;
  }

  /**
   * @param {String} key
   */
  public Map<String, String> getObjectMeta(String key) {
    SignDataOption option = new SignDataOption();
    option.setKey(key);
    option.setMethod(FssOperation.HEAD);
    SignedData signedData = this.generateSignedData(option);
    RequestResult result = this.sendRequest(signedData, new HttpHead());
    return result.getHeaders();
  }

  @SneakyThrows
  private RequestResult sendRequest(SignedData data, HttpRequestBase request) {
    request.setURI(data.getTargetUrl());
    request.setHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/json, application/xml"));
    request.setHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"));


    for (Map.Entry<String, String> entry : data.getHeaders().entrySet()) {
      request.setHeader(entry.getKey(), entry.getValue());
    }

    CloseableHttpResponse response = this.httpClient.execute(request);
    return new RequestResult(response);
  }

  /**
   * 使用header传递签名方式生成签名数据
   *
   * @param {SignDataOption} option
   */
  @SneakyThrows
  private SignedData generateSignedData(SignDataOption option) {
    FssOperation method = option.getMethod();
    String key = option.getKey();
    String resource = this.getResource(key);

    Map<String, String> headers = new HashMap<>();
    if (option.getMetadata() != null) {
      for (Map.Entry<String, String> entry : option.getMetadata().entrySet()) {
        // 全部转换为小写
        String lowerName = entry.getKey().toLowerCase();
        headers.put(lowerName, entry.getValue());
      }
    }

    SignatureOption opts = new SignatureOption(method, option.getContentType());
    if (opts.getExpires() == null) {
      opts.setDate(new Date());
    }
    opts.setMetadata(option.getMetadata());
    SignedResource sign = this.signatureResource(resource, opts);
    headers.put(HttpHeaders.AUTHORIZATION,
        String.format("FSS %s:%s", this.config.getAccessKeyId(), sign.getSignature()));

    URIBuilder builder = createUriBuilder(resource, false);

    for (Map.Entry<String, String> entry : sign.getSubResource().entrySet()) {
      builder.addParameter(entry.getKey(), entry.getValue());
    }


    // 拼成服务端需要的地址
    URI targetUrl = builder.build();
    headers.put(HttpHeaders.ACCEPT, "application/xml,*/*");
    headers.put(HttpHeaders.DATE, opts.getFormatedDate());
    SignedData data = new SignedData();
    data.setTargetUrl(targetUrl);
    data.setMethod(option.getMethod());
    if (opts.getExpires() == null) {
      data.setHeaders(headers);
    }
    data.setResource(resource);
    return data;
  }

  private String getResource(String key) {
    return "/" + this.config.getBucketName() + "/" + key;
  }

  @SneakyThrows
  private SignedResource signatureResource(String resource, SignatureOption option) {
    Map<String, String> subResource = SignUtility.buildSubResource(option);
    String canonicalString = SignUtility.buildCanonicalString(resource, option, subResource);

    byte[] key = this.config.getAccessKeySecret().getBytes(DEFAULT_CHARSET);
    SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(signingKey);
    byte[] data = canonicalString.getBytes(DEFAULT_CHARSET);
    byte[] signedData = mac.doFinal(data);
    String signature = Base64.encodeBase64String(signedData);
    SignedResource signedResource = new SignedResource();
    signedResource.setSignature(signature);
    signedResource.setSubResource(subResource);
    signedResource.setExpires(option.getAbsoluteExpires());
    return signedResource;
  }
}
