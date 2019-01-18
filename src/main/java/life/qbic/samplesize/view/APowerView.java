package life.qbic.samplesize.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;

import life.qbic.xml.properties.Property;

public abstract class APowerView extends VerticalLayout {

  protected String projectID;
  protected String newSampleCode;
  protected Button button;

  public APowerView() {
    setMargin(true);
    setSpacing(true);
  }

  public void setProjectContext(String projectID, String newSampleCode) {
    this.projectID = projectID;
    this.newSampleCode = newSampleCode;
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
