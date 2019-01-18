package life.qbic.samplesize.model;

import java.util.Map;

public class PowerAnalysisRun {

  private String datasetCode;
  private String projectID;
  private Map<String, String> parameters;

  public PowerAnalysisRun(String datasetCode, String projectID, Map<String, String> parameters) {
    super();
    this.datasetCode = datasetCode;
    this.projectID = projectID;
    this.parameters = parameters;
  }

  public String getDatasetCode() {
    return datasetCode;
  }

  public void setDatasetCode(String datasetCode) {
    this.datasetCode = datasetCode;
  }

  public String getProjectID() {
    return projectID;
  }

  public void setProjectID(String projectID) {
    this.projectID = projectID;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }



}
