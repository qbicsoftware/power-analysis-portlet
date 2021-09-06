package life.qbic.samplesize.view;

import java.util.List;
import java.util.Map;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

import life.qbic.samplesize.control.MathHelpers;
import life.qbic.samplesize.control.VMConnection;
import life.qbic.samplesize.model.EstimationMode;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;

public class RNASeqEstimationView extends ARNASeqPrepView {

  public RNASeqEstimationView(VMConnection v, SliderFactory deGenes, SliderFactory fdr,
      SliderFactory minFC, SliderFactory avgReads, SliderFactory dispersion) {
    super(deGenes, fdr, minFC, avgReads, dispersion);

    button = new Button("Compute Power Curve");
    addComponent(button);
    button.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {

        int m1 = (int) (100.0 * percDEGenesSlider.getValue());
        int m = 10000;
        int gcd = MathHelpers.getGCD(m1, m);
        m = m / gcd;
        m1 = m1 / gcd;

        double f = fdrSlider.getValue();
        double phi0 = dispersionSlider.getValue();
        // double rho = minFoldChangeSlider.getValue();
        double lambda0 = avgReadCountSlider.getValue();

        if (useTestData()) {
          v.sampleSizeWithData(newSampleCode, m, m1, f, getTestDataSet(), EstimationMode.TCGA);
        } else {
          v.sampleSize(newSampleCode, m, m1, phi0, f, lambda0);
        }
      }
    });
  }
  
  @Override
  public Map<String, String> getProps() {
    Map<String,String> res = super.getProps();
    res.put("Q_SECONDARY_NAME", "Sample Size Estimation");
    return res;
  }

  @Override
  public List<Property> getCurrentProperties() {
    List<Property> xmlProps = super.getCurrentProperties();
    xmlProps.add(
        new Property("maximum_fdr", Double.toString(fdrSlider.getValue()), PropertyType.Property));
    xmlProps.add(new Property("diff_expr_genes",
        Double.toString(percDEGenesSlider.getValue()) + '%', PropertyType.Property));

    if (!useTestData()) {
      xmlProps.add(new Property("dispersion", Double.toString(dispersionSlider.getValue()),
          PropertyType.Property));
      xmlProps.add(new Property("avg_read_count", Double.toString(avgReadCountSlider.getValue()),
          PropertyType.Property));
    } else {
      xmlProps.add(new Property("base_dataset", getTestDataSet(), PropertyType.Property));
    }
    return xmlProps;
  }

}
