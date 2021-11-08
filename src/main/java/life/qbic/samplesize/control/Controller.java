package life.qbic.samplesize.control;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
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
import com.vaadin.ui.HorizontalLayout;
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
import jline.internal.Log;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.samplesize.model.RNACountData;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.samplesize.view.APowerView;
import life.qbic.samplesize.view.ARNASeqPrepView;
import life.qbic.samplesize.view.MicroarrayCheckView;
import life.qbic.samplesize.view.MicroarrayEstimationView;
import life.qbic.samplesize.view.RNASeqCheckView;
import life.qbic.samplesize.view.RNASeqEstimationView;
import life.qbic.xml.manager.StudyXMLParser;
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.study.Qexperiment;

public class Controller {

  private static final Logger logger = LogManager.getLogger(Controller.class);

  private final String microArrayPowerInformation =
      "Predicts false discovery rate (FDR) based on fold change of interest and sample size per group. Based on the "
          + "R package <b>OCplus</b> by Pawitan et al. (2013)";

  private final String microArrayExperimentalDesignInformation =
      "Predicts the power of an experimental design based on its sample size. False discovery rate (FDR) is shown as "
          + "a function of the detectable log fold change as well as the ratio of non-differentially expressed genes in "
          + "the data. Based on the R package <b>OCplus</b> by Pawitan et al. (2013)";

  private final String RNAPowerInformation =
      "Estimates the necessary sample size for an experimental design. "
          + "Creates a matrix showing needed sample size to reach different power (sensitivity) levels in order to detect "
          + "for detecting differential expression of different log fold changes. Needs maximum tolerated false discovery "
          + "rate (FDR), as well as estimated percentage of differentiall expressed genes as input. Dispersion and "
          + "the average read count can be set or estimated using values from literature, data from The Cancer Genome Atlas "
          + "(TCGA) or pilot data, if it is available. Based on the R package <b>RnaSeqSampleSize</b> by Zhao et al. (2018)";

  private final String RNAExperimentalDesignInformation =
      "Estimates statistical power for a known experimental design. "
          + "Creates a power matrix showing statistical power as a function of false discovery rate and minimum detectable "
          + "fold change between groups. Needs the estimated ratio of differentially expressed genes as input. Dispersion and "
          + "the average read count can be set or estimated using values from literature, data from The Cancer Genome Atlas "
          + "(TCGA) or pilot data, if it is available. Based on the R package <b>RnaSeqSampleSize</b> by Zhao et al. (2018)";

  private final String ocPlus = "https://doi.org/doi:10.18129/B9.bioc.OCplus";

  private final String rnaseqsamplesize = "https://doi.org/doi:10.18129/B9.bioc.RnaSeqSampleSize";

  private OpenBisClient openbis;
  private VerticalLayout mainLayout;
  final String POWER_SAMPLE_TYPE = "Q_POWER_ESTIMATION_RUN";
  final List<String> RNASEQ_COUNT_SAMPLE_TYPES =
      Arrays.asList("Q_WF_NGS_RNA_EXPRESSION_ANALYSIS_RUN", "Q_WF_NGS_MAPPING_RUN");

  private String nextSampleCode;

  protected String projectID;

  protected String space;

  protected String project;

  protected String infoExpID;

  private Table runTable;

  private TabSheet tabs;

  private Map<String, Resource> resourcesForSamples;

  private final StudyXMLParser studyXMLParser = new StudyXMLParser();
  private final XMLParser xmlParser = new XMLParser();

  private JAXBElement<Qexperiment> expDesign;

  private MicroarrayEstimationView maEstView;

  private RNASeqEstimationView rnaEstView;

  private MicroarrayCheckView maCheckView;

  private RNASeqCheckView rnaCheckView;

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

  /**
   * Finds the the matching strings in the list, e.g. sample codes in file names
   * 
   * @param list The list of strings to check
   * @param substring The regular expression to use
   * @return List of matching Strings
   */
  static List<String> getMatchingStrings(List<String> list, String substring) {
    List<String> res = new ArrayList<String>();
    for (String s : list) {
      if (s.contains(substring)) {
        res.add(s);
      }
    }
    return res;
  }

  private void initView(VMConnection vm, RController R, List<String> projectIDs) {

    tabs = new TabSheet();

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

    rnaCheckView = new RNASeqCheckView(vm, deGenes, fdr, minFC, avgReads, dispersion,
        "RNA-Seq Power Estimation based on Experimental Design", RNAExperimentalDesignInformation,
        rnaseqsamplesize);
    rnaEstView = new RNASeqEstimationView(vm, deGenes, fdr, minFC, avgReads, dispersion,
        "RNA-Seq Sample Size Estimation", RNAPowerInformation, rnaseqsamplesize);
    maCheckView = new MicroarrayCheckView(R, sensitivity,
        "Microarray Power Estimation based on Experimental Design",
        microArrayExperimentalDesignInformation, ocPlus);
    maEstView = new MicroarrayEstimationView(R, deGenes, "Microarray Sample Size Estimation",
        microArrayPowerInformation, ocPlus);

    tabs.addTab(rnaCheckView, "RNA-Seq Power");
    tabs.addTab(rnaEstView, "RNA-Seq Sample Size");

    tabs.getTab(rnaCheckView).setEnabled(false);

    initTabButtonListener(rnaCheckView);
    initTabButtonListener(rnaEstView);

    tabs.getTab(maCheckView).setEnabled(false);
    tabs.addTab(maEstView, "Microarray Sample Size");
    tabs.addTab(maCheckView, "Microarray Power");

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
        loadProject(projectBox);
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
    runTable.addContainerProperty("Parameters", Button.class, null);
    runTable.setColumnAlignment("Parameters", Align.CENTER);
    runTable.addContainerProperty("Started at", Date.class, null);

    HorizontalLayout projectBoxLayout = new HorizontalLayout();

    Button refreshProject = new Button();
    refreshProject.setIcon(FontAwesome.REFRESH);

    refreshProject.addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        loadProject(projectBox);
      }
    });

    mainLayout.addComponent(projectBox);
    mainLayout.addComponent(refreshProject);

    mainLayout.addComponent(projectBoxLayout);

    mainLayout.addComponent(runTable);
    mainLayout.addComponent(tabs);
  }

  protected void loadProject(ComboBox projectBox) {
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
      Map<String, Map<String, Set<String>>> sampleSizesOfFactorLevels =
          getSampleSizesForFactors(infoExpID, samplesByType);

      List<RNACountData> pilotData = new ArrayList<>();
      for (String type : RNASEQ_COUNT_SAMPLE_TYPES) {
        pilotData.addAll(preparePilotDataInfo(samplesByType.get(type)));
      }

      List<Sample> powerSamples = samplesByType.get(POWER_SAMPLE_TYPE);
      if (powerSamples == null) {
        powerSamples = new ArrayList<>();
      }

      showExistingRuns(powerSamples);
      setCurrentPowerSampleCode(powerSamples);

      maEstView.setProjectContext(projectID, nextSampleCode);
      maCheckView.setProjectContext(projectID, nextSampleCode);

      rnaEstView.setProjectContext(projectID, nextSampleCode, pilotData);
      rnaCheckView.setProjectContext(projectID, nextSampleCode, pilotData);

      enableDesignBasedViews(!sampleSizesOfFactorLevels.isEmpty());

      maCheckView.setDesigns(sampleSizesOfFactorLevels);
      rnaCheckView.setDesigns(sampleSizesOfFactorLevels);
    }
  }

  private void enableDesignBasedViews(boolean hasDesign) {
    tabs.getTab(maCheckView).setEnabled(hasDesign);
    tabs.getTab(rnaCheckView).setEnabled(hasDesign);
  }

  private List<RNACountData> preparePilotDataInfo(List<Sample> list) {
    List<RNACountData> res = new ArrayList<>();
    if (list != null) {
      for (Sample s : list) {
        String secName = s.getProperties().get("Q_SECONDARY_NAME");
        if (secName == null || secName.isEmpty()) {
          List<String> parentInfo =
              aggregateParentInfo(new HashSet<>(s.getParents()), new ArrayList<>());
          res.add(new RNACountData(s.getCode(), String.join(", ", parentInfo)));
        } else {
          res.add(new RNACountData(s.getCode(), secName));
        }
      }
    }
    return res;
  }

  private List<String> aggregateParentInfo(Set<Sample> parents, List<String> res) {
    while (res.isEmpty() && !parents.isEmpty()) {
      Set<Sample> newParents = new HashSet<>();
      for (Sample s : parents) {
        newParents.addAll(s.getParents());
        res.add(s.getProperties().get("Q_SECONDARY_NAME"));
      }
      return aggregateParentInfo(newParents, res);
    }
    return res;
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

      Date regDate = s.getRegistrationDate();
      Button paramButton = new Button();
      paramButton.setIcon(FontAwesome.CLIPBOARD);
      paramButton.addClickListener(new Button.ClickListener() {

        @Override
        public void buttonClick(ClickEvent event) {

          String xml = s.getProperties().get("Q_PROPERTIES");

          List<Property> props = new ArrayList<>();
          try {
            props = xmlParser.getPropertiesFromXML(xml);
            prepareParamPopup(props);
          } catch (JAXBException e) {
            Log.warn("Properties can not be displayed for sample" + s.getCode());
            Log.warn(e.getCause().toString());
          }
        }

      });
      row.add(paramButton);
      // row.add(user);
      row.add(regDate);

      runTable.addItem(row.toArray(new Object[row.size()]), s);

    }
    runTable.sort(new String[] {"Started at"}, new boolean[] {false});

    runTable.setVisible(runs > 0);

    runTable.setPageLength(runs);
  }


  private void addNewRunToTable(String newSampleCode, Map<String, String> props,
      List<Property> xmlProps) {

    List<Object> row = new ArrayList<Object>();

    String tech = props.get("Q_TECHNOLOGY_TYPE");
    String type = props.get("Q_SECONDARY_NAME");
    Button showResult = new Button();
    showResult.setIcon(FontAwesome.HOURGLASS);
    showResult.setEnabled(false);

    Link download = new Link(null, null);
    download.setIcon(FontAwesome.HOURGLASS);
    download.setEnabled(false);
    row.add(tech);
    row.add(type);
    row.add(download);
    row.add(showResult);

    Date regDate = Date.from(Instant.now());
    Button paramButton = new Button();
    paramButton.setIcon(FontAwesome.CLIPBOARD);
    paramButton.addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        prepareParamPopup(xmlProps);
      }
    });
    row.add(paramButton);
    row.add(regDate);

    runTable.addItem(row.toArray(new Object[row.size()]), newSampleCode);

    runTable.sort(new String[] {"Started at"}, new boolean[] {false});

    runTable.setPageLength(runTable.size());
    runTable.setVisible(runTable.size() > 0);
  }

  private void prepareParamPopup(List<Property> props) {
    Window subWindow = new Window("Parameters");
    subWindow.setWidth("260");
    subWindow.setHeight("300");
    VerticalLayout subContent = new VerticalLayout();
    subContent.setSpacing(true);
    subContent.setMargin(true);
    subWindow.setContent(subContent);

    Table paramTable = new Table("");
    paramTable.setStyleName(Styles.tableTheme);
    paramTable.addContainerProperty("Name", String.class, "");
    paramTable.addContainerProperty("Value", String.class, "");
    paramTable.setVisible(false);

    for (Property p : props) {
      List<Object> row = new ArrayList<Object>();
      row.add(p.getLabel());
      String val = p.getValue();
      if (p.hasUnit()) {
        val = val + ' ' + p.getUnit();
      }
      row.add(val);
      paramTable.addItem(row.toArray(new Object[row.size()]), p);
      paramTable.setVisible(true);
    }
    paramTable.setPageLength(props.size());
    subContent.addComponent(paramTable);

    // Center it in the browser window
    subWindow.center();
    // Open it in the UI
    UI.getCurrent().addWindow(subWindow);
  }

  private void preparePopUpView(String sampleCode, Resource resource) {
    Window subWindow = new Window("Results");
    subWindow.setWidth("800");
    subWindow.setHeight("1000");
    VerticalLayout subContent = new VerticalLayout();
    subContent.setSpacing(true);
    subContent.setMargin(true);
    subWindow.setContent(subContent);

    String MIMEType = resource.getMIMEType();

    if (MIMEType.endsWith("png")) {

      subContent.addComponent(new Embedded("Matrix", resource));

      // Image image = new Image("DSS image", resource);
      // mainLayout.addComponent(image);

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

  private void initTabButtonListener(ARNASeqPrepView powerView) {
    ClickListener listener = new Button.ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        incrementPowerSampleCode();
        createStatisticsSample(powerView.getCurrentParameters(), powerView.getMetadata());
        powerView.setNextSampleCode(nextSampleCode);
        powerView.execute();
      }
    };
    powerView.getButton().addClickListener(listener);
  }

  private void setCurrentPowerSampleCode(List<Sample> samples) {
    // 0 if there is no sample, yet
    int latest = 0;
    String prefix = project + "000";
    for (Sample s : samples) {
      if (s.getType().getCode().equals(POWER_SAMPLE_TYPE)) {
        String suffix = s.getCode().split("-")[1];
        int num = Integer.parseInt(suffix);
        latest = Math.max(latest, num);
      }
    }
    String suffix = Integer.toString(latest);
    nextSampleCode = prefix + "-" + suffix;
  }

  private void incrementPowerSampleCode() {
    String[] splt = nextSampleCode.split("-");
    String prefix = splt[0];
    int num = Integer.parseInt(splt[1]);
    nextSampleCode = prefix + '-' + Integer.toString(num + 1);
  }

  private void createStatisticsSample(List<Property> xmlProps, Map<String, String> props) {
    String qProperties = "";
    try {
      qProperties = xmlParser.toString(xmlParser.createXMLFromProperties(xmlProps));
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("code", nextSampleCode);
    params.put("type", POWER_SAMPLE_TYPE);

    params.put("parents",
        new ArrayList<String>(Arrays.asList('/' + space + '/' + project + "000")));

    params.put("project", project);
    params.put("space", space);
    params.put("experiment", project + "_INFO");
    props.put("Q_PROPERTIES", qProperties); // xml properties
    params.put("properties", props);

    logger.info("creating new estimation sample: " + params);
    openbis.ingest("DSS1", "register-samp", params);

    addNewRunToTable(nextSampleCode, props, xmlProps);
  }

  private Map<String, Map<String, Set<String>>> getSampleSizesForFactors(String infoExp,
      Map<String, List<Sample>> samplesByType) {
    Experiment exp = openbis.getExperimentById(infoExp);
    Map<String, Map<String, Set<String>>> res = new HashMap<>();
    if (exp == null) {
      logger.error("could not find info experiment" + infoExp);
    } else {
      try {
        expDesign = studyXMLParser.parseXMLString(exp.getProperties().get("Q_EXPERIMENTAL_SETUP"));
        logger.debug("setting exp design: " + expDesign);

        Map<String, Map<String, Set<String>>> sizeMap =
            studyXMLParser.getSamplesPerLevelForFactors(expDesign);
        for (String factor : sizeMap.keySet()) {
          Map<String, Set<String>> groups = new HashMap<>();
          Map<String, Set<String>> levels = sizeMap.get(factor);
          Set<String> factorSamples = getFactorSampleType(samplesByType, levels);
          for (String levelName : levels.keySet()) {
            Set<String> instances = new HashSet<>();
            for (String code : levels.get(levelName)) {
              if (factorSamples.contains(code)) {
                instances.add(code);
              }
            }
            groups.put(levelName, instances);
          }
          res.put(factor, groups);
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
      String sampleID = ds.getSample().getIdentifier().getIdentifier();
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
        Resource resource = new ExternalResource(url);

        resourcesForSamples.put(dataSetCodeToSampleID.get(dsCode), resource);
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
