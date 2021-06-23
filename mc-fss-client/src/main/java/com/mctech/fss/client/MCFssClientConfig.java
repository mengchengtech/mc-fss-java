package com.mctech.fss.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MCFssClientConfig {
  private boolean internal = false;
  private String bucketName;
  private String accessKeyId;
  private String accessKeySecret;
  private String publicEndPoint;
  private String privateEndPoint;
}
