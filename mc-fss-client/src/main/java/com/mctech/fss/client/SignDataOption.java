package com.mctech.fss.client;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SignDataOption {
  private FssOperation method;
  private String key;
  private String contentType;
  private Map<String, String> metadata;
}
