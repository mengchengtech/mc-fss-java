package com.mctech.fss.client;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SignedResource {
  private String signature;
  private Map<String, String> SubResource;

  private Long expires;

  private String formatDate;
}

