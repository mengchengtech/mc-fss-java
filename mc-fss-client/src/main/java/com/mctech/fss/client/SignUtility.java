package com.mctech.fss.client;

import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class SignUtility {
  public static Map<String, String> buildSubResource(SignatureOption option) {
    Map<String, String> subResource = new HashMap<>();
    if (StringUtils.isNotBlank(option.getProcess())) {
      subResource.put(HttpConsts.FSS_PROCESS, option.getProcess());
    }

    if (option.getResponse() != null) {
      for (Map.Entry<String, String> entry : option.getResponse().entrySet()) {
        subResource.put("response-" + entry.getKey().toLowerCase(), entry.getValue());
      }
    }
    return subResource;
  }

  public static String buildCanonicalString(
      String resource, SignatureOption option, Map<String, String> subResource) {
    List<String> itemsToSign = new ArrayList<>();
    itemsToSign.add(option.getMethod().name());
    itemsToSign.add(""); // MD5
    itemsToSign.add(option.getContentType());

    if (option.getAbsoluteExpires() != null) {
      itemsToSign.add(option.getAbsoluteExpires().toString());
    } else {
      itemsToSign.add(option.getFormatedDate());
    }

    if (option.getMetadata() != null) {
      Map<String, String> headers = option.getMetadata();
      List<String> keys = headers.keySet().stream()
          .filter(key -> key.toLowerCase().startsWith(HttpConsts.FSS_PREFIX))
          .sorted()
          .collect(Collectors.toList());
      for (String key : keys) {
        itemsToSign.add(key + ":" + headers.get(key));
      }
    }

    // Add canonical resource
    String canonicalizedResource = buildCanonicalizedResource(resource, subResource);
    itemsToSign.add(canonicalizedResource);

    return StringUtils.join(itemsToSign, "\n");
  }

  @SneakyThrows
  private static String buildCanonicalizedResource(String canonicalizedResource, Map<String, String> subResource) {
    URIBuilder urlBuilder = new URIBuilder(canonicalizedResource);
    List<NameValuePair> params = urlBuilder.getQueryParams();
    if (!params.isEmpty()) {
      params.sort(Comparator.comparing(NameValuePair::getName));
      urlBuilder.removeQuery().addParameters(params);
    }
    return urlBuilder.toString();
  }
}
