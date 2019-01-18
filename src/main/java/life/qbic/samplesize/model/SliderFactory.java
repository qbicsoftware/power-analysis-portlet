package life.qbic.samplesize.model;

import com.vaadin.ui.Slider;

import life.qbic.samplesize.components.SliderWithLabel;

public class SliderFactory {

  private String name;
  private String symbol;
  private int resolution;
  private double min;
  private double max;
  private double start;
  private String width;

  public SliderFactory(String name, String symbol, int resolution, double min, double max, double start,
      String width) {
    this.name = name;
    this.symbol = symbol;
    this.resolution = resolution;
    this.min = min;
    this.max = max;
    this.start = start;
    this.width = width;
  }

  public SliderWithLabel getSliderWithLabel() {
    Slider s = new Slider(name);
    s.setResolution(resolution);
    s.setMin(min);
    s.setMax(max);
    s.setValue(start);
    s.setWidth(width);
    return new SliderWithLabel(s, symbol);
  }
}
