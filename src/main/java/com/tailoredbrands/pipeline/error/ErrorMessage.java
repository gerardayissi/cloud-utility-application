package com.tailoredbrands.pipeline.error;


public class ErrorMessage {
  private String rawMessage;

  private String errorCode;

  private String errMsg;

  private String stacktrace;

  private String shortPatternId;

  private String fullPatternId;

  private String jobId;

  public String getRawMessage() {
    return rawMessage;
  }

  public void setRawMessage(String rawMessage) {
    this.rawMessage = rawMessage;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrMsg() {
    return errMsg;
  }

  public void setErrMsg(String errMsg) {
    this.errMsg = errMsg;
  }

  public String getStacktrace() {
    return stacktrace;
  }

  public void setStacktrace(String stacktrace) {
    this.stacktrace = stacktrace;
  }

  public String getShortPatternId() {
    return shortPatternId;
  }

  public void setShortPatternId(String shortPatternId) {
    this.shortPatternId = shortPatternId;
  }

  public String getFullPatternId() {
    return fullPatternId;
  }

  public void setFullPatternId(String fullPatternId) {
    this.fullPatternId = fullPatternId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ErrorMessage that = (ErrorMessage) o;

    if (!rawMessage.equals(that.rawMessage)) return false;
    if (!errorCode.equals(that.errorCode)) return false;
    if (!errMsg.equals(that.errMsg)) return false;
    if (!stacktrace.equals(that.stacktrace)) return false;
    if (!shortPatternId.equals(that.shortPatternId)) return false;
    if (!fullPatternId.equals(that.fullPatternId)) return false;
    return jobId.equals(that.jobId);
  }

  @Override
  public int hashCode() {
    int result = rawMessage.hashCode();
    result = 31 * result + errorCode.hashCode();
    result = 31 * result + errMsg.hashCode();
    result = 31 * result + stacktrace.hashCode();
    result = 31 * result + shortPatternId.hashCode();
    result = 31 * result + fullPatternId.hashCode();
    result = 31 * result + jobId.hashCode();
    return result;
  }
}
