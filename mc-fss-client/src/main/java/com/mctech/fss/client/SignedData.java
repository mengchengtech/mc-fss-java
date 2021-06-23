package com.mctech.fss.client;

import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.Map;

@Getter
@Setter
public class SignedData {
  private URI targetUrl;
  private FssOperation method;
  private String resource;
  private Map<String, String> headers;
}
