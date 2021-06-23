package com.mctech.fss;

import com.alibaba.fastjson.JSON;
import com.mctech.fss.client.*;
import lombok.SneakyThrows;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FssSample {
  @SneakyThrows
  public static void main(String[] args) {
    MCFssClientConfig config = new MCFssClientConfig();
    Properties p = new Properties();
    InputStream s = FssSample.class.getResourceAsStream("/config.properties");
    p.load(s);
    config.setBucketName(p.getProperty("bucketName"));
    config.setPrivateEndPoint(p.getProperty("privateEndPoint"));
    config.setPublicEndPoint(p.getProperty("publicEndPoint"));
    config.setAccessKeyId(p.getProperty("accessKeyId"));
    config.setAccessKeySecret(p.getProperty("accessKeySecret"));

    String key = "java-client/de." + new Date().getTime() + ".html";
    System.out.println(key);
    MCFssClient client = new MCFssClient(config);
    {
      Map<String, String> meta = new HashMap<>();
      meta.put("x-fss-meta-module", "mod");
      FileInputStream is = new FileInputStream("d:\\de.html");
      client.put(key, null,
          is,
          meta,
          "text/html", null
      );
    }

    {
      ObjectMeta meta = client.head(key);
      System.out.println(JSON.toJSONString(meta));
    }

    {
      Map<String, String> om = client.getObjectMeta(key);
      System.out.println(om);
    }

    RequestResult result = client.get(key);
    System.out.println(result.getContent());

    {
      SignatureOption opts = new SignatureOption(FssOperation.GET);
      opts.setExpires(15 * 1000L);
      String accessUrl = client.getSignatureUrl(key, opts);

      System.out.println(accessUrl);
      System.out.println("===================");
    }

    {
      SignatureOption opts = new SignatureOption(FssOperation.GET);
      Map<String, String> response = new HashMap<>();
      response.put("content-disposition", "nnnnnnnnnnnnnn.jpg");
      opts.setResponse(response);
      opts.setProcess("video/snapshot,t_7000,f_jpg,w_800,h_600,m_fast");
      opts.setExpires(15 * 1000L);
      String accessUrl = client.getSignatureUrl("demo" + Math.random()+".jpg", opts);
      System.out.println((accessUrl));
    }

    {
      String objectUrl = client.generateObjectUrl(key);
      System.out.println(objectUrl);
    }

    {
      String toKey = key + "." + Math.random() + ".copy.to";
      client.copy(toKey, key);

      client.delete(key);
    }
  }
}
