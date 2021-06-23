package com.mctech.fss.client.exception;

import com.mctech.fss.client.FssClientError;
import lombok.Getter;

@Getter
public class MCTechFssRequestException extends MCTechException {

  private final FssClientError error;

  public MCTechFssRequestException(String message, FssClientError error) {
    super(message);

    this.error = error;
  }
}
