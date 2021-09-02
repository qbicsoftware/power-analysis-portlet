package life.qbic.samplesize.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
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
  private Map<String, List<Integer>> factorToLevelSize;
  private ComboBox factors;

  public MicroarrayCheckView(RController R, SliderFactory sensitivity) {
    super(R);
    prepareRCode(R);

    sensitivitySlider = sensitivity.getSliderWithLabel();
    addComponent(sensitivitySlider);

    button = new Button("Show Power Estimation based on existing Samples");
    button.setEnabled(false);

    factors = new ComboBox("Study Factor");
    factors.setNullSelectionAllowed(false);
    factors.setImmediate(true);
    addComponent(factors);
    factors.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        String factor = (String) factors.getValue();
        if (factor != null) {
          List<Integer> groups = factorToLevelSize.get(factor);
          if (groups.size() != 2) {
            Styles.notification("Can't perform Power Estimation",
                "Two study groups are needed for power estimation. " + factor + " contains "
                    + groups.size() + " levels.",
                NotificationType.DEFAULT);
            button.setEnabled(false);
          } else {
            button.setEnabled(true);
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
        List<Integer> groups = factorToLevelSize.get(factors.getValue());

        addComponent(new Label("Compute FDR for sensitivity of " + sensitivitySlider.getValue()
            + " and groups of size " + groups.get(0) + " and " + groups.get(1)));

        addComponent(spinner);

        String dataVar = "power_map";
        String computeMatrix =
            dataVar + " <- ma_create_check_data(n1=" + groups.get(0) + ", n2=" + groups.get(1)
                + ", " + sensitivitySlider.getSymbol() + "=" + sensitivitySlider.getValue() + ")";
        String call = "standard_heatmap(" + dataVar
            + ", xlab = 'Detectable Log Fold Change', ylab = 'Ratio of Non-Diff. Expressed Genes')";

        R.backgroundEval(computeMatrix, new MicroHeatmapReadyRunnable(view, call,
            "False Discovery Rate (FDR) for sensitivity of " + sensitivitySlider.getValue()));
      }
    });
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
    int n1 = factorToLevelSize.get(factors.getValue()).get(0);
    int n2 = factorToLevelSize.get(factors.getValue()).get(1);
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
        "ma_create_check_data <- function (n1, n2, p0=c(0.9,0.95,0.99), D=c(1,2,3,4), paired = FALSE, power) {"
            + "mat <- NULL; p0_size <- length(p0); for (d in D) {"
            + "ma <- ma_check(n1 = n1, n2 = n2, p0 = p0, D = d);"
            + "row <- max(which(ma[,2*p0_size+3] >= power));"
            + "mat <- c(mat, as.numeric(ma[row,2:(p0_size+1)])) };"
            + "mat <- matrix(mat, ncol = length(D));" + "colnames(mat) <- D;"
            + "row.names(mat) <- p0;" + "return(mat) }");
  }

  public void setDesigns(Map<String, List<Integer>> sampleSizesOfFactorLevels) {
    this.factorToLevelSize = sampleSizesOfFactorLevels;
    factors.removeAllItems();
    factors.addItems(sampleSizesOfFactorLevels.keySet());
  }

}
