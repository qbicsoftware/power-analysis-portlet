package life.qbic.samplesize.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Button.ClickEvent;

import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.samplesize.control.MathHelpers;
import life.qbic.samplesize.control.VMConnection;
import life.qbic.samplesize.model.EstimationMode;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.xml.properties.Property;
import life.qbic.xml.properties.PropertyType;

public class RNASeqCheckView extends ARNASeqPrepView {

  private static final Logger logger = LogManager.getLogger(RNASeqCheckView.class);

  private Map<String, List<Integer>> factorToLevelSize;
  private ComboBox factors;

  public RNASeqCheckView(VMConnection v, SliderFactory deGenes, SliderFactory fdr,
      SliderFactory minFC, SliderFactory avgReads, SliderFactory dispersion) {
    super(deGenes, fdr, minFC, avgReads, dispersion);

    button = new Button("Estimation based on existing samples.");
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
        // double fc = minFoldChangeSlider.getValue();

        int sampleSize = getMinSampleSizeOfFactor();

        if (useTestData()) {
          v.powerWithData(getProject(), m, m1, sampleSize, f, getTestDataSet(),
              EstimationMode.TCGA);
        } else {
          v.power(getProject(), m, m1, sampleSize, phi0, f);
        }
      }
    });
  }

  protected int getMinSampleSizeOfFactor() {
    List<Integer> groups = factorToLevelSize.get(factors.getValue());
    return Math.min(groups.get(0), groups.get(1));
  }

  @Override
  public Map<String, String> getProps() {
    Map<String,String> res = super.getProps();
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

  public void setDesigns(Map<String, List<Integer>> sampleSizesOfFactorLevels) {
    this.factorToLevelSize = sampleSizesOfFactorLevels;
    factors.removeAllItems();
    factors.addItems(sampleSizesOfFactorLevels.keySet());
  }

}
