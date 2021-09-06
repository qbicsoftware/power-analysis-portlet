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
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.samplesize.control.MathHelpers;
import life.qbic.samplesize.control.VMConnection;
import life.qbic.samplesize.model.EstimationMode;
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
  private Map<String, Set<String>> selectedLevels;
  
  public RNASeqCheckView(VMConnection v, SliderFactory deGenes, SliderFactory fdr,
      SliderFactory minFC, SliderFactory avgReads, SliderFactory dispersion, String title,
      String infoText, String link) {
    super(deGenes, fdr, minFC, avgReads, dispersion, title, infoText, link);
    
    fdrSlider.setVisible(false);

    factors = new ComboBox("Study Factor");
    
    factors.setNullSelectionAllowed(false);
    factors.setImmediate(true);
    addComponent(factors);
    
    button = new Button("Estimation based on existing samples.");
    button.setEnabled(false);

    factors.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        String factor = (String) factors.getValue();
        if (factor != null) {
          Map<String, Set<String>> levels = sampleSizesOfFactorLevels.get(factor);
          switch (levels.size()) {
            case 0:
            case 1:
              Styles.notification("Can't perform Power Estimation",
                  "Two study groups are needed for power estimation. " + factor + " contains "
                      + levels.size() + " levels.",
                  NotificationType.DEFAULT);
              break;
            case 3:
              selectLevelsPopup(factor, levels);
              break;
            default:
              selectedLevels = levels;
              button.setEnabled(true);
              break;
          }
        } else {
          button.setEnabled(false);
        }
      }
    });

    addComponent(button);
    button.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {

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
          v.powerWithData(newSampleCode, m, m1, sampleSize, getTestDataSet(),
              EstimationMode.TCGA);
        } else {
          v.power(newSampleCode, m, m1, sampleSize, phi0, avgReads);
        }
      }
    });
  }

  protected int getMinSampleSizeOfFactor() {
    int n = Integer.MAX_VALUE;
    for(Set<String> group : selectedLevels.values()) {
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
    OptionGroup levelGroup = new OptionGroup("Select two levels of factor " + factor);
    levelGroup.setMultiSelect(true);

    for (String level : levels.keySet()) {
      levelGroup.addItem(level);
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
            for (Object id : levelGroup.getItemIds()) {
              levelGroup.setItemEnabled(id, true);
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
        for (String val : values) {
          selectedLevels.put(val, levels.get(val));
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
//old
    //  List<Integer> levels = factorToLevelSize.get(factors.getValue());
  //  int res = Integer.MAX_VALUE;
  //  for (int groupSize : levels) {
   //   res = Math.min(res, groupSize);
   // }
   // return res;
  }

  @Override
  public Map<String, String> getProps() {
    Map<String, String> res = super.getProps();
    res.put("Q_SECONDARY_NAME", "Power Estimation");
    return res;
  }

  @Override
  public List<Property> getCurrentProperties() {
    List<Property> xmlProps = super.getCurrentProperties();
    xmlProps.add(
        new Property("maximum_fdr", Double.toString(fdrSlider.getValue()), PropertyType.Property));
    xmlProps.add(new Property("diff_expr_genes",
        Double.toString(percDEGenesSlider.getValue()) + '%', PropertyType.Property));
    xmlProps.add(new Property("samplesize", Double.toString(getMinSampleSizeOfFactor()),
        PropertyType.Property));

    if (!useTestData()) {
      xmlProps.add(new Property("dispersion", Double.toString(dispersionSlider.getValue()),
          PropertyType.Property));
    } else {
      xmlProps.add(new Property("base_dataset", getTestDataSet(), PropertyType.Property));
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

}
