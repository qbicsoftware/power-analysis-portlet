package life.qbic.samplesize.components;

import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;

import life.qbic.samplesize.model.Constants;

public class ParameterEstimationComponent extends HorizontalLayout {
  
  private OptionGroup avgReadSelector;
  private OptionGroup dispersionSelector;

  public ParameterEstimationComponent() {
    avgReadSelector = new OptionGroup("Average Read Count");
    avgReadSelector.addItems(Constants.avgReadEstimates.keySet());
    addComponent(avgReadSelector);

    dispersionSelector = new OptionGroup("Dispersion");
    dispersionSelector.addItems(Constants.dispersionEstimates.keySet());
    addComponent(dispersionSelector);
  }
  
  public void resetSelections() {
    dispersionSelector.select(dispersionSelector.getNullSelectionItemId());
    avgReadSelector.select(avgReadSelector.getNullSelectionItemId());
  }
  
  public OptionGroup getDispersionSelector() {
    return dispersionSelector;
   }
  
  public OptionGroup getAvgReadSelector() {
   return avgReadSelector; 
  }
}
