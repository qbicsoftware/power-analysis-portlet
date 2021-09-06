package life.qbic.samplesize.components;

import com.vaadin.data.Property;
import com.vaadin.ui.Slider;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class SliderWithLabel extends VerticalLayout {

  // private Label label;
  private TextField input;
  private String symbol;
  private Slider slider;

  public SliderWithLabel(Slider slider, String symbol) {
    this.slider = slider;
    this.symbol = symbol;
    addComponent(slider);
    // label = new Label();
    input = new TextField();
    String val = Double.toString(slider.getValue());
    // label.setValue(val);
    input.setValue(val);
    // addComponent(label);
    addComponent(input);

    slider.addValueChangeListener((Property.ValueChangeListener) event -> {
      // label.setValue(String.valueOf(slider.getValue()));
      String newVal = String.valueOf(slider.getValue());
      if (!input.getValue().equals(newVal)) {
        input.setValue(newVal);
      }
    });

    input.addValueChangeListener((Property.ValueChangeListener) event -> {
      String value = input.getValue();
      try {
        Double num = Double.parseDouble(value);
        if (slider.getValue() != num) {
          setValue(num);
        }
      } catch (NumberFormatException e) {

      }
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
    input.setEnabled(enable);
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
