package life.qbic.samplesize.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.vaadin.data.Property;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import life.qbic.portal.Styles;
import life.qbic.samplesize.components.ParameterEstimationComponent;
import life.qbic.samplesize.components.SliderWithLabel;
import life.qbic.samplesize.control.VMConnection;
import life.qbic.samplesize.model.Constants;
import life.qbic.samplesize.model.EstimationMode;
import life.qbic.samplesize.model.RNACountData;
import life.qbic.samplesize.model.SliderFactory;

public abstract class ARNASeqPrepView extends AContainerPrepView {

  protected VerticalLayout optionBox;
  protected OptionGroup parameterSource;
  protected ParameterEstimationComponent estimFromLit;
  protected Table pilotData;
  protected ListSelect testData;
  protected SliderWithLabel sensitivitySlider, percDEGenesSlider, fdrSlider, minFoldChangeSlider,
      avgReadCountSlider, dispersionSlider;
  protected VMConnection vmConnection;

  public ARNASeqPrepView(VMConnection v, SliderFactory deGenes, SliderFactory fdr,
      SliderFactory minFC, SliderFactory avgReads, SliderFactory dispersion, String title,
      String infoText, String link) {
    super(title, infoText, link);

    vmConnection = v;

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
          pilotData.setSelectable(true);
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
    pilotData.setStyleName(Styles.tableTheme);
    pilotData.addContainerProperty("Run Code", String.class, "");
    pilotData.addContainerProperty("Run Information", String.class, "");
    pilotData.setVisible(false);

    optionBox.addComponent(pilotData);

    testData = new ListSelect("Available Test Datasets");
    testData.addItems(Constants.TCGA_DATASETS.keySet());
    testData.setNullSelectionAllowed(false);
    testData.setVisible(false);
    optionBox.addComponent(testData);

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
    EstimationMode mode = getDataMode();
    return mode.equals(EstimationMode.Data) || mode.equals(EstimationMode.TCGA);
  }

  protected EstimationMode getDataMode() {
    String val = (String) parameterSource.getValue();
    switch (val) {
      case "Compute using TCGA data":
        return EstimationMode.TCGA;
      case "Use pilot data (recommended)":
        return EstimationMode.Data;
      default:
        return EstimationMode.None;
    }
  }

  protected boolean inputsReady() {
    if (nextSampleCode == null || nextSampleCode.isEmpty()) {
      return false;
    }
    EstimationMode mode = getDataMode();
    if (mode.equals(EstimationMode.Data) || mode.equals(EstimationMode.TCGA)) {
      return getTestDataName() != null;
    }
    return true;
  }

  protected String getTestDataName() {
    if (getDataMode().equals(EstimationMode.TCGA)) {
      return Constants.TCGA_DATASETS.get(testData.getValue());
    }
    if (getDataMode().equals(EstimationMode.Data)) {
      RNACountData data = (RNACountData) pilotData.getValue();
      if (data != null) {
        return data.getSampleCode();
      }
    }
    return null;
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

  public void setProjectContext(String projectID, String newSampleCode,
      List<RNACountData> pilotData) {
    super.setProjectContext(projectID, newSampleCode);
    setPilotData(pilotData);
  }

  public void setPilotData(List<RNACountData> datasets) {
    pilotData.removeAllItems();
    for (RNACountData d : datasets) {
      List<Object> row = new ArrayList<Object>();
      row.add(d.getSampleCode());
      row.add(d.getInformation());
      pilotData.addItem(row.toArray(new Object[row.size()]), d);
    }
    pilotData.setPageLength(Math.min(10, pilotData.size() + 1));
  }

  @Override
  public Map<String, String> getMetadata() {
    Map<String, String> res = super.getMetadata();
    res.put("Q_TECHNOLOGY_TYPE", "RNA-Sequencing");
    return res;
  }

  public abstract void execute();

}
