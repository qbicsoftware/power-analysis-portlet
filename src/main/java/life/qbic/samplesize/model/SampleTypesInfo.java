package life.qbic.samplesize.model;

import java.util.Map;
import java.util.Set;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;

public class SampleTypesInfo {

  private Map<String, Set<Sample>> samplesByType;
  private Map<String, Set<String>> codesByType;



  public SampleTypesInfo(Map<String, Set<Sample>> sampleMap,
      Map<String, Set<String>> codeMap) {
    super();
    this.samplesByType = sampleMap;
    this.codesByType = codeMap;
  }

  public Map<String, Set<Sample>> getSamplesByType() {
    return samplesByType;
  }

  public Map<String, Set<String>> getCodesByType() {
    return codesByType;
  }


}
