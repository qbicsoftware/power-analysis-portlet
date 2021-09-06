package life.qbic.samplesize.view;

import java.util.List;
import java.util.Map;

import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Window;

import life.qbic.samplesize.control.RController;
import life.qbic.xml.properties.Property;

public abstract class AHeatMapPrepView extends APowerView {
  
  private RController R;
  protected ProgressBar spinner;
  
  public AHeatMapPrepView(RController R, String title, String infoText, String link) {
    super(title, infoText, link);
    this.R = R;
    spinner = new ProgressBar();
    spinner.setIndeterminate(true);
  }

  public void showHeatmap(String call, String caption, String resultVar) {
    Window graph = R.getGraph(call, 1200, 800);
    System.out.println(R.getReturnString(resultVar));
    graph.setCaption(caption);
    removeComponent(spinner);
    getUI().addWindow(graph);
  }
  
  @Override
  public Map<String, String> getProps() {
    Map<String,String> res = super.getProps();
    res.put("Q_TECHNOLOGY_TYPE", "DNA Microarray");
    return res;
  }
  
  @Override
  public List<Property> getCurrentProperties() {
    return super.getCurrentProperties();
  }

}
