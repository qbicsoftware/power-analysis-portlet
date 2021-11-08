package life.qbic.samplesize.view;

import java.util.List;
import java.util.Map;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
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
    super(v, deGenes, fdr, minFC, avgReads, dispersion, title, infoText, link);

    button = new Button("Compute Power");
    addComponent(button);

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
  public void execute() {
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
        vmConnection.sampleSizeWithData(nextSampleCode, m, m1, f, getTestDataName(), getDataMode());
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
        vmConnection.sampleSize(nextSampleCode, m, m1, phi0, f, lambda0);
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

  @Override
  public Map<String, String> getMetadata() {
    Map<String, String> res = super.getMetadata();
    res.put("Q_SECONDARY_NAME", "Sample Size Estimation");
    return res;
  }

  @Override
  public List<Property> getCurrentParameters() {
    List<Property> xmlProps = super.getCurrentParameters();
    xmlProps.add(new Property("diff_expr_genes",
        Double.toString(percDEGenesSlider.getValue()) + '%', PropertyType.Property));
    xmlProps.add(
        new Property("maximum_fdr", Double.toString(fdrSlider.getValue()), PropertyType.Property));
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
