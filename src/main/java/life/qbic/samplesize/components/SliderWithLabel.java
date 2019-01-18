package life.qbic.samplesize.components;

import com.vaadin.data.Property;
import com.vaadin.ui.Label;
import com.vaadin.ui.Slider;
import com.vaadin.ui.VerticalLayout;

public class SliderWithLabel extends VerticalLayout {

  private Label label;
  private String symbol;
  private Slider slider;

  public SliderWithLabel(Slider slider, String symbol) {
    this.slider = slider;
    this.symbol = symbol;
    addComponent(slider);
    label = new Label();
    String val = Double.toString(slider.getValue());
    label.setValue(val);
    addComponent(label);

    slider.addValueChangeListener((Property.ValueChangeListener) event -> {
      label.setValue(String.valueOf(slider.getValue()));
    });
  }

  public String getSymbol() {
    return symbol;
  }

  public Double getValue() {
    return slider.getValue();
  }
  
  @Override
  public void setEnabled(boolean enable) {
    slider.setEnabled(enable);
  }

  /**
   * sets value for slider. checks if input is in expected range and respecively uses min or max
   * value if it is not.
   * 
   * @param val
   */
  public void setValue(double val) {
    if (val < slider.getMin()) {
      slider.setValue(slider.getMin());
      return;
    }
    if (val > slider.getMax()) {
      slider.setValue(slider.getMax());
      return;
    }
    slider.setValue(val);
  }
}
