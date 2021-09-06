package life.qbic.samplesize.view;

import java.util.List;

public abstract class AContainerPrepView extends APowerView {
  private List<PowerAnalysisRun> runs;
  
  public AContainerPrepView(String title, String infoText, String link) {
    super(title, infoText, link);
  }
}
