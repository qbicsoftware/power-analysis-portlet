package life.qbic.samplesize.model;

public class RNACountData {
  
  private String sampleCode;
  private String information;
  
  public RNACountData(String sampleCode, String information) {
    this.sampleCode = sampleCode;
    this.information = information;
  }

  public String getSampleCode() {
    return sampleCode;
  }

  public String getInformation() {
    return information;
  }

}
