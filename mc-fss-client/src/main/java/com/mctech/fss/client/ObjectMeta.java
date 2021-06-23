package com.mctech.fss.client;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ObjectMeta {
  private Map<String, String> meta;
  private Map<String, String> headers;
  private int statusCode;
}
