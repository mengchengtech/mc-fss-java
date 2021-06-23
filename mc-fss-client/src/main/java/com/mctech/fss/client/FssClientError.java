package com.mctech.fss.client;

import java.util.Map;

public class FssClientError {
  private static final String PROP_CODE = "Code";
  private static final String PROP_MESSAGE = "Message";
  private static final String PROP_STRING_TO_SIGN_BYTES = "StringToSignBytes";
  private static final String PROP_SIGNATURE_PROVIDED = "SignatureProvided";
  private static final String PROP_STRING_TO_SIGN = "StringToSign";
  private static final String PROP_ACCESS_KEY_ID = "AccessKeyId";

  private final Map<String, String> map;

  public FssClientError(Map<String, String> map) {
    this.map = map;
  }

  public String getCode() {
    return this.map.get(PROP_CODE);
  }

  public String getMessage()  {
    return this.map.get(PROP_MESSAGE);
  }

  public  String getStringToSignBytes()  {
    return this.map.get(PROP_STRING_TO_SIGN_BYTES);
  }

  public  String getSignatureProvided()  {
    return this.map.get(PROP_SIGNATURE_PROVIDED);
  }

  public String getStringToSign()  {
    return this.map.get(PROP_STRING_TO_SIGN);
  }

  public String getAccessKeyId()  {
    return this.map.get(PROP_ACCESS_KEY_ID);
  }

  public String getProperty(String name) {
    return this.map.get(name);
  }
}
