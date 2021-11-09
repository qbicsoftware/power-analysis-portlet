package life.qbic.samplesize.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import jline.internal.Log;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.samplesize.control.MathHelpers;
import life.qbic.samplesize.control.VMConnection;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;

/**
 * 
 * @author afriedrich
 *
 */
public class RNASeqCheckView extends ARNASeqPrepView {

  private static final Logger logger = LogManager.getLogger(RNASeqCheckView.class);

  private ComboBox factors;
  private Map<String, Map<String, Set<String>>> sampleSizesOfFactorLevels;
  private Map<String, Set<String>> selectedLevels = new HashMap<>();

  public RNASeqCheckView(VMConnection v, SliderFactory deGenes, SliderFactory fdr,
      SliderFactory minFC, SliderFactory avgReads, SliderFactory dispersion, String title,
      String infoText, String link) {
    super(v, deGenes, fdr, minFC, avgReads, dispersion, title, infoText, link);

    fdrSlider.setVisible(false);

    factors = new ComboBox("Study Factor");

    factors.setNullSelectionAllowed(false);
    factors.setImmediate(true);
    addComponent(factors);

    button = new Button("Estimation based on existing samples.");
    button.setEnabled(false);

    initValueChangeListeners();
  }

  protected void checkInputsReady() {
    String factor = (String) factors.getValue();
    if (factor != null) {
      Map<String, Set<String>> levels = sampleSizesOfFactorLevels.get(factor);
      if (selectedLevels.size() == 2) {
        button.setEnabled(inputsReady());
      } else if (levels.size() < 2) {
        Styles.notification("Can't perform Power Estimation",
            "Two study groups are needed for power estimation. " + factor + " contains "
                + levels.size() + " levels.",
            NotificationType.DEFAULT);
      } else if (levels.size() > 2) {
        selectLevelsPopup(factor, levels);
      } else {
        boolean replicates = true;
        for (Set<String> levelMembers : levels.values()) {
          if (levelMembers.size() < 2) {
            replicates &= false;
          }
        }
        if (replicates) {
          selectedLevels = levels;
        } else {
          selectLevelsPopup(factor, levels);
        }
      }
    } else {
      button.setEnabled(false);
    }
  }

  @Override
  public void execute() {
    // TODO Auto-generated method stub
    int m1 = (int) (100.0 * percDEGenesSlider.getValue());
    int m = 10000;
    int gcd = MathHelpers.getGCD(m1, m);
    m = m / gcd;
    m1 = m1 / gcd;

    double avgReads = avgReadCountSlider.getValue();
    // dispersion
    double phi0 = dispersionSlider.getValue();

    int sampleSize = getMinSampleSizeOfFactor();

    if (useTestData()) {
      try {
        vmConnection.powerWithData(nextSampleCode, m, m1, sampleSize, getTestDataName(),
            getDataMode());
        Styles.notification("New power estimations started",
            "It may take a while for power estimations on real data to finish. You can come back later or refresh the project at the top from time to time to update the current status.",
            NotificationType.SUCCESS);
      } catch (Exception e) {
        Styles.notification("Estimation could not be run",
            "Something went wrong. Please try again or contact us if the problem persists.",
            NotificationType.ERROR);
        Log.error(e.getMessage());
      }

    } else {
      try {
        vmConnection.power(nextSampleCode, m, m1, sampleSize, phi0, avgReads);
        Styles.notification("New power estimations started",
            "Please allow a few minutes for the power estimation to finish. You can come back later or refresh the project to update the current status.",
            NotificationType.SUCCESS);
      } catch (Exception e) {
        Styles.notification("Estimation could not be run",
            "Something went wrong. Please try again or contact us if the problem persists.",
            NotificationType.ERROR);
        Log.error(e.getMessage());
      }
    }
  }

  protected int getMinSampleSizeOfFactor() {
    int n = Integer.MAX_VALUE;
    for (Set<String> group : selectedLevels.values()) {
      n = Math.min(n, group.size());
    }
    return n;
  }

  protected void selectLevelsPopup(String factor, Map<String, Set<String>> levels) {
    Window subWindow = new Window("Level selection");
    subWindow.setWidth("300");
    subWindow.setHeight("300");
    VerticalLayout subContent = new VerticalLayout();
    subContent.setSpacing(true);
    subContent.setMargin(true);
    subWindow.setContent(subContent);
    Label info = new Label("Select two levels of factor " + factor
        + ". Each level needs at least one replicate (2 samples).");
    subContent.addComponent(info);
    OptionGroup levelGroup = new OptionGroup();
    levelGroup.setMultiSelect(true);

    for (String level : levels.keySet()) {
      int size = levels.get(level).size();
      Object item = level + " (" + size + ")";
      levelGroup.addItem(item);
      if (size < 2) {
        levelGroup.setItemEnabled(item, false);
      }
    }

    Button close = new Button("Ok");
    close.setEnabled(false);
    levelGroup.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (levelGroup.getValue() != null) {
          Set<String> values = (Set<String>) levelGroup.getValue();
          close.setEnabled(values.size() == 2);
          if (values.size() < 2) {
            for (Object displayName : levelGroup.getItemIds()) {
              String label = levelLabelToName((String) displayName);
              int levelSize = levels.get(label).size();
              if (levelSize > 1) {
                levelGroup.setItemEnabled(displayName, true);
              }
            }
          } else {
            for (Object id : levelGroup.getItemIds()) {
              if (!values.contains(id)) {
                levelGroup.setItemEnabled(id, false);
              }
            }
          }
        }
      }
    });

    close.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        selectedLevels = new HashMap<>();

        Set<String> values = (Set<String>) levelGroup.getValue();
        for (String displayName : values) {
          String label = levelLabelToName(displayName);
          selectedLevels.put(label, levels.get(label));
        }
        subWindow.close();
        button.setEnabled(true);
      }
    });

    subContent.addComponent(levelGroup);
    subContent.addComponent(close);
    // Center it in the browser window
    subWindow.center();
    // Open it in the UI
    UI.getCurrent().addWindow(subWindow);
  }

  private String levelLabelToName(String label) {
    return label.split(" \\(")[0];
  }

  @Override
  public Map<String, String> getMetadata() {
    Map<String, String> res = super.getMetadata();
    res.put("Q_SECONDARY_NAME", "Power Estimation");
    return res;
  }

  @Override
  public List<Property> getCurrentParameters() {
    List<Property> xmlProps = super.getCurrentParameters();
    xmlProps.add(new Property("diff_expr_genes",
        Double.toString(percDEGenesSlider.getValue()) + '%', PropertyType.Property));
    xmlProps.add(new Property("samplesize", Double.toString(getMinSampleSizeOfFactor()),
        PropertyType.Property));

    if (!useTestData()) {
      xmlProps.add(new Property("dispersion", Double.toString(dispersionSlider.getValue()),
          PropertyType.Property));
      xmlProps.add(new Property("avg_read_count", Double.toString(avgReadCountSlider.getValue()),
          PropertyType.Property));
    } else {
      xmlProps.add(new Property("base_dataset", getTestDataName(), PropertyType.Property));
    }
    return xmlProps;
  }

  public void setDesigns(Map<String, Map<String, Set<String>>> sampleSizesOfFactorLevels) {
    this.sampleSizesOfFactorLevels = sampleSizesOfFactorLevels;
    factors.removeAllItems();
    if (!sampleSizesOfFactorLevels.isEmpty()) {
      factors.addItems(sampleSizesOfFactorLevels.keySet());
    }
  }

  private void initValueChangeListeners() {

    factors.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        checkInputsReady();
      }
    });

    addComponent(button);

    pilotData.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        checkInputsReady();
      }
    });

    testData.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        checkInputsReady();
      }
    });

    parameterSource.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        checkInputsReady();
      }
    });
  }
}
