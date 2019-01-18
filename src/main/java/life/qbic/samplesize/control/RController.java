package life.qbic.samplesize.control;

import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import fi.vtt.RVaadin.RContainer;

public class RController {

  private RContainer R;

  public RController(String host, String port, String user, String pwd) {
    if (host != null) {
      remoteLogin(host, Integer.parseInt(port), user, pwd);
    } else {
      this.R = new RContainer();
    }
    R.eval("library(heatmap3)");
    R.eval("standard_heatmap <- function (mat, main='', xlab, ylab) {"
        + "heatmap3(mat, Colv = NA, Rowv = NA, xlab = xlab, ylab = ylab, "
        + "scale = 'n', col = matlab::jet.colors(1000), cexCol = 1, "
        + "cexRow = 1, lasCol = 1, lasRow = 1, main = main)}");
  }

  private void remoteLogin(String host, int port, String user, String pwd) {
    this.R = new RContainer(host, port);
    R.login(user, pwd);
  }

  public void eval(String call) {
    // R.eval(call);
    try {
      R.tryEval(call);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void backgroundEval(String code, MicroHeatmapReadyRunnable ready) {
    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {
        R.eval(code);
        UI.getCurrent().setPollInterval(-1);
        UI.getCurrent().access(ready);
      }
    });
    t.start();
    UI.getCurrent().setPollInterval(100);
  }

  public Window getGraph(String RPlotCall, int width, int height) {
    return R.getGraph(RPlotCall, width, height);
  }

}
