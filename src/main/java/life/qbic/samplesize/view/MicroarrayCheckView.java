package life.qbic.samplesize.view;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.samplesize.components.SliderWithLabel;
import life.qbic.samplesize.control.MicroHeatmapReadyRunnable;
import life.qbic.samplesize.control.RController;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;

public class MicroarrayCheckView extends AHeatMapPrepView {

  private SliderWithLabel sensitivitySlider;
  // private OptionGroup controlOption =
  // new OptionGroup("Control for...", Arrays.asList("Sensitivity", "False discovery rate (FDR)"));
  private ComboBox factors;
  private Map<String, Map<String, Set<String>>> sampleSizesOfFactorLevels;
  private Map<String, Set<String>> selectedLevels;

  public MicroarrayCheckView(RController R, SliderFactory sensitivity, String title, String infoText, String link) {
    super(R, title, infoText, link);
    prepareRCode(R);

    factors = new ComboBox("Study Factor");
    factors.setNullSelectionAllowed(false);
    factors.setImmediate(true);
    addComponent(factors);

    sensitivitySlider = sensitivity.getSliderWithLabel();
    addComponent(sensitivitySlider);

    button = new Button("Show Power Estimation based on existing Samples");
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

    // draw the heatmap
    MicroarrayCheckView view = this;
    button.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        Iterator<Set<String>> groups = selectedLevels.values().iterator();
        int n1 = groups.next().size();
        int n2 = groups.next().size();

        addComponent(new Label("Compute FDR for sensitivity of " + sensitivitySlider.getValue()
            + " and groups of size " + n1 + " and " + n2));

        addComponent(spinner);

        String dataVar = "power_map";
        String computeMatrix = dataVar + " <- ma_create_check_data(n1=" + n1 + ", n2=" + n2 + ", "
            + sensitivitySlider.getSymbol() + "=" + sensitivitySlider.getValue() + ")";
        String call = "standard_heatmap(" + dataVar
            + ", xlab = 'Detectable Log Fold Change', ylab = 'Ratio of Non-Diff. Expressed Genes', cexRow = 2, cexCol = 2)";

        R.backgroundEval(computeMatrix,
            new MicroHeatmapReadyRunnable(view, call,
                "False Discovery Rate (FDR) for sensitivity of " + sensitivitySlider.getValue(),
                dataVar));
      }
    });
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
    Iterator<Set<String>> groups = selectedLevels.values().iterator();
    int n1 = groups.next().size();
    int n2 = groups.next().size();
    xmlProps.add(new Property("samplesize_group1", Integer.toString(n1), PropertyType.Property));
    xmlProps.add(new Property("samplesize_group2", Integer.toString(n2), PropertyType.Property));
    xmlProps.add(new Property("minimum_sensitivity", Double.toString(sensitivitySlider.getValue()),
        PropertyType.Property));
    return xmlProps;
  }

  private void prepareRCode(RController R) {
    R.eval("library(OCplus)");
    R.eval("ma_check <- function (n1, n2, p0, D, paired = FALSE) {"
        + "TOC(n1 = n1, n2 = n2, p0 = p0, paired = paired, plot = FALSE, D=D) }");
    R.eval(
        "ma_create_check_data <- function (n1, n2, p0=c(0.9,0.95,0.97,0.99), D=c(1,1.3,1.5,1.7,2,3), paired = FALSE, power) {"
            + "mat <- NULL; p0_size <- length(p0); for (d in D) {"
            + "ma <- ma_check(n1 = n1, n2 = n2, p0 = p0, D = d);"
            + "row <- max(which(ma[,2*p0_size+3] >= power));"
            + "mat <- c(mat, as.numeric(ma[row,2:(p0_size+1)])) };"
            + "mat <- matrix(mat, ncol = length(D));" + "colnames(mat) <- D;"
            + "row.names(mat) <- p0;" + "return(mat) }");
  }
  
  public void setDesigns(Map<String, Map<String, Set<String>>> sampleSizesOfFactorLevels) {
    this.sampleSizesOfFactorLevels = sampleSizesOfFactorLevels;
    factors.removeAllItems();
    if (!sampleSizesOfFactorLevels.isEmpty()) {
      factors.addItems(sampleSizesOfFactorLevels.keySet());
    }
  }

}
