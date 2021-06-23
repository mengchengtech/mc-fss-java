package com.mctech.fss.client;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.DateUtils;

import java.util.Date;
import java.util.Map;

@Getter
public class SignatureOption {
  /**
   * 获取或设置设置REST调用签名中的method信息
   */
  private final FssOperation method;
  private final String contentType;

  //public String ContentMd5 { get; private set; }

  /**
   * 发出请求的客户端时间
   */
  private Date date = null;

  /**
   * 发出请求的客户端时间
   */
  private Long expires = null;
  private Long absoluteExpires = null;

  private String process;

  private Map<String, String> response;

  private Map<String, String> metadata;

  public SignatureOption(FssOperation method) {
    this(method, null);
  }

  @SneakyThrows
  public SignatureOption(FssOperation method, String contentType) {
    this.method = method;
    this.contentType = contentType;
    //this.contentMd5 = "";
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public void setExpires(Long expires) {
    this.expires = expires;
    this.absoluteExpires = new Date().getTime() / 1000 + expires;
  }

  /**
   * 获取或设置REST调用中的content-type头
   *
   * @return
   */
  public String getContentType() {
    if (StringUtils.isBlank(contentType)) {
      return "";
    }
    return contentType;
  }

  Long getAbsoluteExpires() {
    return this.absoluteExpires;
  }

  public String getFormatedDate() {
    return DateUtils.formatDate(new Date());
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public void setProcess(String process) {
    this.process = process;
  }

  public void setResponse(Map<String, String> response) {
    this.response = response;
  }
}
