package life.qbic.samplesize.view;

import java.util.List;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import jline.internal.Log;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.samplesize.control.MathHelpers;
import life.qbic.samplesize.control.VMConnection;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;

public class RNASeqEstimationView extends ARNASeqPrepView {

  public RNASeqEstimationView(VMConnection v, SliderFactory deGenes, SliderFactory fdr,
      SliderFactory minFC, SliderFactory avgReads, SliderFactory dispersion, String title,
      String infoText, String link) {
    super(deGenes, fdr, minFC, avgReads, dispersion, title, infoText, link);

    button = new Button("Compute Power");
    button.setEnabled(false);
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
          try {
            v.sampleSizeWithData(nextSampleCode, m, m1, f, getTestDataName(), getDataMode());
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
            v.sampleSize(nextSampleCode, m, m1, phi0, f, lambda0);
            Styles.notification("New power estimations started",
                "Please allow a few minutes for the sample size estimation to finish. You can come back later or refresh the project to update the current status.",
                NotificationType.SUCCESS);
          } catch (Exception e) {
            Styles.notification("Estimation could not be run",
                "Something went wrong. Please try again or contact us if the problem persists.",
                NotificationType.ERROR);
            Log.error(e.getMessage());
          }
        }
      }
    });
    pilotData.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        button.setEnabled(inputsReady());
      }
    });

    testData.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        button.setEnabled(inputsReady());
      }
    });
    
    parameterSource.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        button.setEnabled(inputsReady());
      }
    });
  }

  @Override
  public Map<String, String> getProps() {
    Map<String, String> res = super.getProps();
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
      xmlProps.add(new Property("base_dataset", getTestDataName(), PropertyType.Property));
    }
    return xmlProps;
  }

}
