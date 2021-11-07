package life.qbic.samplesize.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import life.qbic.xml.properties.Property;

public abstract class APowerView extends VerticalLayout {

  protected String projectID;
  protected String nextSampleCode;
  protected Button button;

  public APowerView(String title, String infoText, String link) {
    setMargin(true);
    setSpacing(true);

    addInfoPanel(title, infoText, link);
  }

  private void addInfoPanel(String title, String infoText, String url) {
    Panel infoPanel = new Panel(title);
    infoPanel.setWidth(500, Unit.PIXELS);

    final VerticalLayout contentLayout = new VerticalLayout();
    contentLayout.setWidth(500, Unit.PIXELS);
    contentLayout.setSpacing(false);
    contentLayout.addComponent(new Label(infoText, ContentMode.HTML));

    infoPanel.setContent(contentLayout);
    addComponent(infoPanel);

    Link link = new Link("More information", new ExternalResource(url));
    link.setTargetName("_blank");

    addComponent(link);
  }

  public void setProjectContext(String projectID, String nextSampleCode) {
    this.projectID = projectID;
    setNextSampleCode(nextSampleCode);
  }
  
  public void setNextSampleCode(String nextSampleCode) {
    this.nextSampleCode = nextSampleCode;
  }

  public Button getButton() {
    return button;
  }

  public String getProject() {
    String project = projectID.split("/")[2];
    return project;
  }

  public Map<String, String> getProps() {
    return new HashMap<>();
  }

  public List<Property> getCurrentProperties() {
    return new ArrayList<Property>();
  }

}
