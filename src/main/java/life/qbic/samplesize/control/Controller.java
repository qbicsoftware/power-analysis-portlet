package life.qbic.samplesize.control;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Link;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table.Align;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.samplesize.view.APowerView;
import life.qbic.samplesize.view.MicroarrayCheckView;
import life.qbic.samplesize.view.MicroarrayEstimationView;
import life.qbic.samplesize.view.RNASeqCheckView;
import life.qbic.samplesize.view.RNASeqEstimationView;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.study.Qexperiment;

public class Controller {

  private static final Logger logger = LogManager.getLogger(Controller.class);

  private final String microArrayPowerInformation =
      "Predicts false discovery rate (FDR) based on fold change of interest and sample size per group. Based on the "
          + "R package OCplus by Pawitan et al. (2013)";

  private final String microArrayExperimentalDesignInformation =
      "Predicts the power of an experimental design based on its sample size. False discovery rate (FDR) is shown as "
          + "a function of the detectable log fold change as well as the ratio of non-differentially expressed genes in "
          + "the data. Based on the R package OCplus by Pawitan et al. (2013)";

  private final String RNAPowerInformation = "";

  private final String RNAExperimentalDesignInformation = "Estimates statistical power for a known experimental design. "
      + "Creates a power matrix showing statistical power as a function of false discovery rate and minimum detectable "
      + "fold change between groups. Users can select maximum false discovery rate, the estimated ratio of differentially expressed "
      + "genes.";
  
  private final String ocPlus =
      "https://www.bioconductor.org/packages/release/bioc/html/OCplus.html";

  private final String rnaseqsamplesize =
      "http://www.bioconductor.org/packages/release/bioc/html/RnaSeqSampleSize.html";

  private OpenBisClient openbis;
  private VerticalLayout mainLayout;
  final String POWER_SAMPLE_TYPE = "Q_POWER_ESTIMATION_RUN";

  private String newSampleCode;

  protected String projectID;

  protected String space;

  protected String project;

  protected String infoExpID;

  private Table runTable;

  private HashMap<String, Resource> resourcesForSamples;

  private StudyXMLParser studyXMLParser;

  private JAXBElement<Qexperiment> expDesign;

  public Controller(OpenBisClient openbis, VerticalLayout layout, ConfigurationManager config,
      String user) {
    this.openbis = openbis;
    this.mainLayout = layout;
    init(config, user);
  }

  private void init(ConfigurationManager config, String user) {
    final Set<String> spaces = new HashSet<>(openbis.getUserSpaces(user));
    final List<Project> projects = openbis.listProjects();
    List<String> projectIDs = new ArrayList<>();
    for (Project p : projects) {
      String space = p.getSpace().getCode();
      if (spaces.contains(space)) {
        projectIDs.add(p.getIdentifier().getIdentifier());
      }
    }

    VMConnection vm = new VMConnection(
        config.getSampleSizeVMUser() + "@" + config.getEpitopeAndSampleSizeVMHost(),
        config.getRNASeqSampleSizeContainerName(), ".", user);
    RController R = new RController(config.getRServeHost(), config.getRServePort(),
        config.getRServeUser(), config.getRServePassword());

    initView(vm, R, projectIDs);
  }

  private void initView(VMConnection vm, RController R, List<String> projectIDs) {

    TabSheet tabs = new TabSheet();

    SliderFactory sensitivity =
        new SliderFactory("Minimum Sensitivity", "power", 2, 0.1, 1.0, 0.8, "200px");
    SliderFactory deGenes =
        new SliderFactory("Percentage of DE genes", "p1", 1, 0.1, 50.0, 2.0, "200px");
    SliderFactory fdr = new SliderFactory("False Discovery Rate", "f", 2, 0.01, 0.2, 0.05, "200px");
    SliderFactory minFC =
        new SliderFactory("Min Detectable Fold Change", "rho", 1, 1.1, 10, 2.0, "200px");
    SliderFactory avgReads =
        new SliderFactory("Average Read Count", "lambda0", 1, 1, 100, 7.0, "200px");
    SliderFactory dispersion =
        new SliderFactory("Average dispersion", "phi0", 4, 0.0001, 20, 1, "200px");

    RNASeqCheckView rnaCheckView = new RNASeqCheckView(vm, deGenes, fdr, minFC, avgReads,
        dispersion, "RNASeq Power Estimation based on Experimental Design",
        RNAExperimentalDesignInformation, rnaseqsamplesize);
    RNASeqEstimationView rnaEstView = new RNASeqEstimationView(vm, deGenes, fdr, minFC, avgReads,
        dispersion, "RNASeq Power Estimation", RNAPowerInformation, rnaseqsamplesize);
    MicroarrayCheckView maCheckView = new MicroarrayCheckView(R, sensitivity,
        "Microarray Power Estimation based on Experimental Design",
        microArrayExperimentalDesignInformation, ocPlus);
    MicroarrayEstimationView maEstView = new MicroarrayEstimationView(R, deGenes,
        "Microarray Power Estimation", microArrayPowerInformation, ocPlus);


    tabs.addTab(rnaCheckView, "Exp. Design RNA-Seq");
    tabs.addTab(rnaEstView, "RNA-Seq");
    tabs.addTab(maEstView, "Microarrays");
    tabs.addTab(maCheckView, "Exp. Design Microarrays");



    tabs.getTab(rnaCheckView).setEnabled(false);
    tabs.getTab(maCheckView).setEnabled(false);

    initTabButtonListener(rnaCheckView);
    initTabButtonListener(rnaEstView);

    // initTabButtonListener(maEstView);
    // initTabButtonListener(maCheckView);

    ComboBox projectBox = new ComboBox("Select a project");
    projectBox.setNullSelectionAllowed(false);
    projectBox.setStyleName(Styles.boxTheme);
    projectBox.setFilteringMode(FilteringMode.CONTAINS);
    projectBox.setImmediate(true);
    projectBox.addItems(projectIDs);
    projectBox.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (projectBox.getValue() != null) {
          projectID = (String) projectBox.getValue();
          space = projectID.split("/")[1];
          project = projectID.split("/")[2];
          infoExpID = ExperimentCodeFunctions.getInfoExperimentID(space, project);
          List<Sample> allSamples = openbis.getSamplesOfProject(projectID);
          Map<String, List<Sample>> samplesByType = new HashMap<>();
          for (Sample s : allSamples) {
            String type = s.getType().getCode();
            if (samplesByType.containsKey(type)) {
              samplesByType.get(type).add(s);
            } else {
              List<Sample> list = new ArrayList<>(Arrays.asList(s));
              samplesByType.put(type, list);
            }
          }
          Map<String, List<Integer>> sampleSizesOfFactorLevels =
              getSampleSizesForFactors(infoExpID, samplesByType);

          List<Sample> powerSamples = samplesByType.get(POWER_SAMPLE_TYPE);
          if (powerSamples == null) {
            powerSamples = new ArrayList<>();
          }

          showExistingRuns(powerSamples);
          String newSampleCode = newPowerSampleCode(powerSamples);

          maEstView.setProjectContext(projectID, newSampleCode);
          rnaEstView.setProjectContext(projectID, newSampleCode);
          maCheckView.setProjectContext(projectID, newSampleCode);
          rnaCheckView.setProjectContext(projectID, newSampleCode);
          enableDesignBasedViews(!sampleSizesOfFactorLevels.isEmpty());
          maCheckView.setDesigns(sampleSizesOfFactorLevels);
          rnaCheckView.setDesigns(sampleSizesOfFactorLevels);
        }
      }

      private void enableDesignBasedViews(boolean hasDesign) {
        tabs.getTab(maCheckView).setEnabled(hasDesign);
        tabs.getTab(rnaCheckView).setEnabled(hasDesign);
      }
    });
    runTable = new Table("Existing Calculations");
    runTable.setVisible(false);
    runTable.setStyleName(Styles.tableTheme);
    runTable.addContainerProperty("Technology", String.class, "");
    runTable.addContainerProperty("Result Type", String.class, "");
    runTable.addContainerProperty("Download", Link.class, null);
    runTable.setColumnAlignment("Download", Align.CENTER);
    runTable.addContainerProperty("Show", Button.class, null);
    runTable.setColumnWidth("Show", 60);
    runTable.setColumnAlignment("Show", Align.CENTER);
    runTable.addContainerProperty("Created by", String.class, "");
    runTable.addContainerProperty("Date", Date.class, null);

    mainLayout.addComponent(projectBox);

    mainLayout.addComponent(runTable);
    mainLayout.addComponent(tabs);
  }

  protected void showExistingRuns(List<Sample> powerSamples) {
    mapSampleIDsToMatrixImages(powerSamples);

    runTable.removeAllItems();

    int runs = powerSamples.size();

    for (Sample s : powerSamples) {
      Map<String, String> props = s.getProperties();
      String tech = props.get("Q_TECHNOLOGY_TYPE");
      String type = props.get("Q_SECONDARY_NAME");
      // Button download = new Button();
      // download.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
      // download.setWidth(30, Unit.PIXELS);
      // download.setHeight(40, Unit.PIXELS);
      Button showResult = new Button();
      // showResult.setWidth(30, Unit.PIXELS);
      // showResult.setHeight(40, Unit.PIXELS);
      showResult.setIcon(FontAwesome.PICTURE_O);

      List<Object> row = new ArrayList<Object>();

      Resource resource = resourcesForSamples.get(s.getIdentifier().getIdentifier());


      Link download = new Link(null, resource);
      download.setTargetName("_blank");
      download.setIcon(FontAwesome.DOWNLOAD);

      if (resource != null) {
        showResult.setEnabled(true);
        download.setEnabled(true);
        showResult.addClickListener(new Button.ClickListener() {

          @Override
          public void buttonClick(ClickEvent event) {
            preparePopUpView(s.getCode(), resource);
          }
        });
      } else {
        showResult.setEnabled(false);
        showResult.setIcon(FontAwesome.HOURGLASS);
        download.setEnabled(false);
        download.setIcon(FontAwesome.HOURGLASS);
      }

      row.add(tech);
      row.add(type);
      row.add(download);
      row.add(showResult);

      String user = s.getModifier().getFirstName() + " " + s.getModifier().getLastName();
      Date regDate = s.getRegistrationDate();

      row.add(user);
      row.add(regDate);

      runTable.addItem(row.toArray(new Object[row.size()]), s);

    }
    runTable.setVisible(runs > 0);

    runTable.setPageLength(runs);
  }

  private void preparePopUpView(String sampleCode, Resource resource) {
    Window subWindow = new Window("Results");
    subWindow.setWidth("800");
    subWindow.setHeight("1000");
    VerticalLayout subContent = new VerticalLayout();
    subContent.setSpacing(true);
    subContent.setMargin(true);
    subWindow.setContent(subContent);

    // Image image = new Image("Matrix", resource);
    // subContent.addComponent(image);


    // PopupView popup = new PopupView("Pop it up", image);
    // subContent.addComponent(popup);

    String MIMEType = resource.getMIMEType();

    if (MIMEType.endsWith("png")) {

      subContent.addComponent(new Embedded("Matrix", resource));

      // Image image = new Image("DSS image", resource);
      // mainLayout.addComponent(image);

      Table params = new Table("Parameters");
      params.setStyleName(Styles.tableTheme);
      params.addContainerProperty("Name", String.class, "");
      params.addContainerProperty("Value", String.class, "");
      params.setVisible(false);

      List<Property> props =
          studyXMLParser.getFactorsAndPropertiesForSampleCode(expDesign, sampleCode);

      for (Property p : props) {
        List<Object> row = new ArrayList<Object>();
        row.add(p.getLabel());
        String val = p.getValue();
        if (p.hasUnit()) {
          val = val + ' ' + p.getUnit();
        }
        row.add(val);
        params.addItem(row.toArray(new Object[row.size()]), p);
        params.setVisible(true);
      }
      params.setPageLength(props.size());
      subContent.addComponent(params);

      // Center it in the browser window
      subWindow.center();
      // Open it in the UI
      UI.getCurrent().addWindow(subWindow);

    } else if (MIMEType.endsWith("pdf")) {
      Window window = new Window();
      window.setWidth("90%");
      window.setHeight("90%");
      BrowserFrame e = new BrowserFrame("PDF File", resource);
      e.setWidth("100%");
      e.setHeight("100%");
      window.setContent(e);
      window.center();
      window.setModal(true);
      UI.getCurrent().addWindow(window);
    }

  }

  private void initTabButtonListener(APowerView powerView) {
    ClickListener listener = new Button.ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        createStatisticsSample(powerView.getCurrentProperties(), powerView.getProps());
        incrementPowerSampleCode();
      }
    };
    powerView.getButton().addClickListener(listener);
  }

  private String newPowerSampleCode(List<Sample> samples) {
    int latest = 0;

    String prefix = project + "000";
    for (Sample s : samples) {
      if (s.getType().getCode().equals(POWER_SAMPLE_TYPE)) {
        String suffix = s.getCode().split("-")[1];
        int num = Integer.parseInt(suffix);
        latest = Math.max(latest, num);
      }
    }
    String suffix = Integer.toString(latest + 1);
    newSampleCode = prefix + "-" + suffix;
    return newSampleCode;
  }

  private void incrementPowerSampleCode() {
    String[] splt = newSampleCode.split("-");
    String prefix = splt[0];
    int num = Integer.parseInt(splt[1]);
    newSampleCode = prefix + '-' + Integer.toString(num + 1);
  }

  private String createStatisticsSample(List<Property> xmlProps, Map<String, String> props) {
    // String qProperties = "";
    // try {
    // qProperties = xmlParser.toString(xmlParser.createXMLFromProperties(xmlProps));
    // } catch (JAXBException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // openbis.ingest(dss, serviceName, params);
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("code", newSampleCode);
    params.put("type", POWER_SAMPLE_TYPE);

    params.put("parents",
        new ArrayList<String>(Arrays.asList('/' + space + '/' + project + "000")));

    params.put("project", project);
    params.put("space", space);
    params.put("experiment", project + "_INFO");

    StringBuilder result = new StringBuilder();
    result.append("Input Files: ");

    // props.put("Q_PROPERTIES", qProperties);
    // params.put("properties", props);

    openbis.ingest("DSS1", "register-samp", params);
    return newSampleCode;
  }

  private Map<String, List<Integer>> getSampleSizesForFactors(String infoExp,
      Map<String, List<Sample>> samplesByType) {
    Experiment exp = openbis.getExperimentById(infoExp);
    Map<String, List<Integer>> res = new HashMap<>();
    if (exp == null) {
      logger.error("could not find info experiment" + infoExp);
    } else {
      studyXMLParser = new StudyXMLParser();
      try {
        expDesign = studyXMLParser.parseXMLString(exp.getProperties().get("Q_EXPERIMENTAL_SETUP"));
        logger.debug("setting exp design: " + expDesign);

        Map<String, Map<String, Set<String>>> sizeMap =
            studyXMLParser.getSamplesPerLevelForFactors(expDesign);
        for (String factor : sizeMap.keySet()) {
          List<Integer> groupSizes = new ArrayList<>();
          Map<String, Set<String>> levels = sizeMap.get(factor);
          Set<String> factorSamples = getFactorSampleType(samplesByType, levels);
          for (Set<String> sampleCodes : levels.values()) {
            int size = 0;
            for (String code : sampleCodes) {
              if (factorSamples.contains(code)) {
                size++;
              }
            }
            groupSizes.add(size);
          }
          res.put(factor, groupSizes);
        }
      } catch (JAXBException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return res;
  }

  private Set<String> getFactorSampleType(Map<String, List<Sample>> samplesByType,
      Map<String, Set<String>> levels) {
    List<String> sampleCodes = new ArrayList<>();
    for (Set<String> codes : levels.values()) {
      sampleCodes.addAll(codes);
    }
    Set<String> res = new HashSet<>();
    List<SampleType> sampleTypes = new ArrayList<>(Arrays.asList(SampleType.Q_BIOLOGICAL_ENTITY,
        SampleType.Q_BIOLOGICAL_SAMPLE, SampleType.Q_TEST_SAMPLE));
    for (SampleType type : sampleTypes) {
      try {
        for (Sample s : samplesByType.get(type.toString())) {
          if (sampleCodes.contains(s.getCode())) {
            res.add(s.getCode());
          }
        }
        if (res.size() > 0) {
          return res;
        }
      } catch (NullPointerException e) {
        logger.info(type + " not found.");
      }
    }
    return res;
  }

  public void mapSampleIDsToMatrixImages(List<Sample> powerSamples) {
    resourcesForSamples = new HashMap<>();

    List<String> sampleIds = new ArrayList<>();
    if (!powerSamples.isEmpty()) {
      for (Sample s : powerSamples) {
        sampleIds.add(s.getIdentifier().getIdentifier());
      }
    } else {
      return;
    }
    List<DataSet> datasets = openbis.listDataSetsForSamples(sampleIds);

    Map<String, String> dataSetCodeToSampleID = new HashMap<>();
    List<String> dsCodes = new ArrayList<String>();

    for (DataSet ds : datasets) {
      String dsCode = ds.getCode();
      dsCodes.add(dsCode);
      System.out.println(ds);
      String sampleID = ds.getSample().getIdentifier().getIdentifier();
      System.out.println(sampleID);
      dataSetCodeToSampleID.put(dsCode, sampleID);

      List<DataSetFile> files = openbis.getFilesOfDataSetWithID(dsCode);
      String dssPath = files.get(0).getPath();
      for (DataSetFile file : files) {
        if (file.getPath().length() > dssPath.length()) {
          dssPath = file.getPath();
        }
      }
      try {
        URL url = openbis.getDataStoreDownloadURLLessGeneric(dsCode, dssPath);
        url = new URL(url.toString().replace(":444", ""));
        System.out.println(url);
        Resource resource = new ExternalResource(url);
        // Image image = new Image("Image from file", resource);
        // mainLayout.addComponent(image);
        resourcesForSamples.put(dataSetCodeToSampleID.get(dsCode), resource);
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
