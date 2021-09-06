package life.qbic.samplesize.view;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Property;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

import life.qbic.samplesize.components.ParameterEstimationComponent;
import life.qbic.samplesize.components.SliderWithLabel;
import life.qbic.samplesize.model.Constants;
import life.qbic.samplesize.model.SliderFactory;

public abstract class ARNASeqPrepView extends AContainerPrepView {

  protected VerticalLayout optionBox;
  protected OptionGroup parameterSource;
  protected ParameterEstimationComponent estimFromLit;
  protected Table pilotData;
  protected ListSelect testData;
  protected SliderWithLabel sensitivitySlider, percDEGenesSlider, fdrSlider, minFoldChangeSlider,
      avgReadCountSlider, dispersionSlider;

  public ARNASeqPrepView(SliderFactory deGenes, SliderFactory fdr, SliderFactory minFC,
      SliderFactory avgReads, SliderFactory dispersion, String title, String infoText, String link) {
    super(title, infoText, link);
    
    percDEGenesSlider = deGenes.getSliderWithLabel();
    minFoldChangeSlider = minFC.getSliderWithLabel();
    fdrSlider = fdr.getSliderWithLabel();
    avgReadCountSlider = avgReads.getSliderWithLabel();
    dispersionSlider = dispersion.getSliderWithLabel();

    optionBox = new VerticalLayout();
    optionBox.setMargin(true);
    optionBox.setSpacing(true);

    parameterSource = new OptionGroup("Source of Read Count and Dispersion");
    parameterSource.addItems(Arrays.asList("Use pilot data (recommended)",
        "Use estimates from literature", "Compute using TCGA data", "Select them myself"));
    parameterSource.select("Select them myself");
    optionBox.addComponent(parameterSource);
    addComponent(optionBox);

    estimFromLit = new ParameterEstimationComponent();
    estimFromLit.setVisible(false);
    optionBox.addComponent(estimFromLit);
    OptionGroup avgReadSelector = estimFromLit.getAvgReadSelector();
    avgReadSelector.addValueChangeListener((Property.ValueChangeListener) event -> {
      if (avgReadSelector.getValue() != null) {
        String val = (String) avgReadSelector.getValue();
        avgReadCountSlider.setValue(Constants.avgReadEstimates.get(val));
      }
    });

    OptionGroup dispersionSelector = estimFromLit.getDispersionSelector();
    dispersionSelector.addValueChangeListener((Property.ValueChangeListener) event -> {
      if (dispersionSelector.getValue() != null) {
        String val = (String) dispersionSelector.getValue();
        dispersionSlider.setValue(Constants.dispersionEstimates.get(val));
      }
    });

    parameterSource.addValueChangeListener((Property.ValueChangeListener) event -> {
      String val = (String) parameterSource.getValue();
      hideComplexInputs();
      if (!val.equals("Select them myself")) {
        enableCountAndDispersionInput(false);
      }
      switch (val) {
        case "Use pilot data (recommended)":
          pilotData.setVisible(true);
          if (pilotData.isEmpty()) {
            System.out.println("no pilot data found");
          }
          break;
        case "Compute using TCGA data":
          testData.setVisible(true);
          break;
        case "Use estimates from literature":
          estimFromLit.setVisible(true);
          break;
        default:
          enableCountAndDispersionInput(true);
          break;
      }
    });

    pilotData = new Table("Available Pilot Study Data");
    pilotData.setPageLength(Math.min(10, pilotData.size() + 1));

    testData = new ListSelect("Available Test Datasets");
    testData.addItems(Constants.TCGA_DATASETS.keySet());
    testData.setVisible(false);
    addComponent(testData);

    HorizontalLayout sliderBar = new HorizontalLayout();
    sliderBar.setMargin(true);
    sliderBar.setSpacing(true);

    sliderBar.addComponent(percDEGenesSlider);
    // sliderBar.addComponent(new SliderWithLabel(numPrognGenes));
    sliderBar.addComponent(fdrSlider);
    // sliderBar.addComponent(minFoldChangeSlider);
    sliderBar.addComponent(avgReadCountSlider);
    sliderBar.addComponent(dispersionSlider);
    addComponent(sliderBar);
  }

  protected boolean useTestData() {
    String val = (String) parameterSource.getValue();
    return val.equals("Compute using TCGA data");
  }

  protected String getTestDataSet() {
    return Constants.TCGA_DATASETS.get(testData.getValue());
  }

  private void hideComplexInputs() {
    pilotData.setVisible(false);
    testData.setVisible(false);
    testData.select(testData.getNullSelectionItemId());
    estimFromLit.setVisible(false);
    estimFromLit.resetSelections();
  }

  private void enableCountAndDispersionInput(boolean enable) {
    avgReadCountSlider.setEnabled(enable);
    dispersionSlider.setEnabled(enable);
  }
  
  @Override
  public Map<String, String> getProps() {
    Map<String,String> res = super.getProps();
    res.put("Q_TECHNOLOGY_TYPE", "RNA-Sequencing");
    return res;
  }

}
