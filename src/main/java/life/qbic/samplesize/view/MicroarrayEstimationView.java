package life.qbic.samplesize.view;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

import life.qbic.samplesize.components.SliderWithLabel;
import life.qbic.samplesize.control.MicroHeatmapReadyRunnable;
import life.qbic.samplesize.control.RController;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;

public class MicroarrayEstimationView extends AHeatMapPrepView {

  SliderWithLabel deGenesSlider;
  public MicroarrayEstimationView(RController R, SliderFactory deGenes, String title, String infoText, String link) {
    super(R, title, infoText, link);
    prepareRCode(R);

    deGenesSlider = deGenes.getSliderWithLabel();

    addComponent(deGenesSlider);
    button = new Button("Show Power Estimation");
    addComponent(button);

    // draw the heatmap
    MicroarrayEstimationView view = this;
    button.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(',');
        DecimalFormat df = new DecimalFormat("#.###", otherSymbols);
        df.setRoundingMode(RoundingMode.HALF_UP);
        String p0 = df.format(1 - deGenesSlider.getValue() / 100);

        addComponent(spinner);

        String dataVar = "size_map";
        String computeMatrix = dataVar + " <- ma_samplesize_data(p0=" + p0 + ")";
        String call = "standard_heatmap(" + dataVar
            + ", xlab = 'Detectable Log Fold Change', ylab = 'Samples per Group', cexCol = 2, cexRow = 2)";
        R.backgroundEval(computeMatrix, new MicroHeatmapReadyRunnable(view, call,
            "False discovery/false negative rate if " + deGenesSlider.getValue() + "% genes are DE",
            dataVar));
      }
    });
  }

  @Override
  public Map<String, String> getMetadata() {
    Map<String, String> res = super.getMetadata();
    res.put("Q_SECONDARY_NAME", "Sample Size Estimation");
    return res;
  }

  @Override
  public List<Property> getCurrentParameters() {
    List<Property> xmlProps = super.getCurrentParameters();
    xmlProps.add(new Property("diff_expr_genes", Double.toString(deGenesSlider.getValue()) + '%',
        PropertyType.Property));
    return xmlProps;
  }

  private void prepareRCode(RController R) {
    R.eval("library(OCplus)");
    R.eval("ma_samplesize <- function (n, p0, D, paired = FALSE) {"
        + "samplesize(n = n, p0 = p0, D=D, plot = FALSE, paired = paired, crit.style = c('top percentage'), crit = 1-p0)}");
    R.eval(
        "ma_samplesize_data <- function (n = seq(5, 50, by = 5), p0, D=c(1,1.3,1.5,1.7,2,3), paired = FALSE) {"
            + "mat <- NULL;" + "for (d in D) { "
            + "mat <- c(mat, ma_samplesize(n = n, p0 = p0, D = d)[,1])};"
            + "mat <- matrix(mat, ncol = length(D));" + "colnames(mat) <- D;"
            + "row.names(mat) <- n;" + "return(mat) }");
  }
}
