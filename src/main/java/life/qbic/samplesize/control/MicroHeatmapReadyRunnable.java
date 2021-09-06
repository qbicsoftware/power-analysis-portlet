package life.qbic.samplesize.control;

import life.qbic.samplesize.view.AHeatMapPrepView;

public class MicroHeatmapReadyRunnable implements Runnable {

  private AHeatMapPrepView view;
  private String call;
  private String caption;
  private String resultVar;
  
  public MicroHeatmapReadyRunnable(AHeatMapPrepView view, String call, String caption, String resultVariable) {
    this.view = view;
    this.call = call;
    this.caption = caption;
    this.resultVar = resultVariable;
  }

  @Override
  public void run() {
    view.showHeatmap(call, caption, resultVar);
  }

}
