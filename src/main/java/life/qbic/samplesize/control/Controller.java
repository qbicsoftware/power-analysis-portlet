package life.qbic.samplesize.control;

import java.io.Serializable;
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
import com.vaadin.ui.Image;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table.Align;

import ch.systemsx.cisd.openbis.dss.client.api.v1.DataSet;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.EntityRegistrationDetails;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import ch.systemsx.cisd.openbis.plugin.query.shared.api.v1.dto.QueryTableModel;
import life.qbic.datamodel.identifiers.ExperimentCodeFunctions;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.Styles;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.samplesize.model.RNACountData;
import life.qbic.samplesize.model.SampleTypesInfo;
import life.qbic.samplesize.model.SliderFactory;
import life.qbic.samplesize.view.APowerView;
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

  private OpenBisClient openbis;
  private VerticalLayout mainLayout;
  final String POWER_SAMPLE_TYPE = "Q_POWER_ESTIMATION_RUN";
  final String RNASEQ_COUNT_SAMPLE_TYPE = "Q_WF_NGS_RNA_EXPRESSION_ANALYSIS_RUN";

  private String newSampleCode;

  protected String projectID;

  protected String space;

  protected String project;

  protected String infoExpID;

  private Table runTable;

  private HashMap<String, Resource> resourcesForSamples;

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
      String space = p.getSpaceCode();
      if (spaces.contains(space)) {
        projectIDs.add(p.getIdentifier());
      }
    }
    // new Statistics(openbis, projectIDs);

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

    RNASeqCheckView rnaCheckView =
        new RNASeqCheckView(vm, deGenes, fdr, minFC, avgReads, dispersion);
    RNASeqEstimationView rnaEstView =
        new RNASeqEstimationView(vm, deGenes, fdr, minFC, avgReads, dispersion);
    MicroarrayCheckView maCheckView = new MicroarrayCheckView(R, sensitivity);
    MicroarrayEstimationView maEstView = new MicroarrayEstimationView(R, deGenes);
    tabs.addTab(rnaCheckView, "Exp. Design RNA-Seq");
    tabs.addTab(rnaEstView, "RNA-Seq");
    tabs.addTab(maEstView, "Microarrays");
    tabs.addTab(maCheckView, "Exp. Design Microarrays");

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

          // List<String> fileNames = getFilenamesOfProject(projectID);
          //
          // getFactors(infoExpID);
          // mapExperimentalProperties(projectID, fileNames);


          // List<Sample> samples = openbis.getSamplesOfProjectBySearchService(projectID);
          List<Sample> samples =
              openbis.getSamplesWithParentsAndChildrenOfProjectBySearchService(projectID);
          System.out.println(samples.size() + " samples");
          SampleTypesInfo sampleInfo = sortBySampleTypes(samples);
          System.out.println("got infos");
          Map<String, Set<Sample>> samplesByType = sampleInfo.getSamplesByType();
          System.out.println("by type");
          Map<String, Map<String, Set<String>>> sampleSizesOfFactorLevels =
              getSampleSizesForFactors(infoExpID, sampleInfo.getCodesByType());
          System.out.println("factor stuff");
          showExistingRuns(samplesByType.get(POWER_SAMPLE_TYPE));
          System.out.println("existing runs");
          List<RNACountData> pilotData =
              preparePilotDataInfo(samplesByType.get(RNASEQ_COUNT_SAMPLE_TYPE));
          System.out.println("pilotdata");
          String newSampleCode = newPowerSampleCode(samples);

          maEstView.setProjectContext(projectID, newSampleCode);
          rnaEstView.setProjectContext(projectID, newSampleCode, pilotData);
          maCheckView.setProjectContext(projectID, newSampleCode);
          rnaCheckView.setProjectContext(projectID, newSampleCode, pilotData);
          maCheckView.setDesigns(sampleSizesOfFactorLevels);
          rnaCheckView.setDesigns(sampleSizesOfFactorLevels);
        }
      }

      private SampleTypesInfo sortBySampleTypes(List<Sample> samples) {
        Map<String, Set<Sample>> sampleMap = new HashMap<>();
        Map<String, Set<String>> codeMap = new HashMap<>();
        for (Sample s : samples) {
          String type = s.getSampleTypeCode();
          String code = s.getCode();
          if (sampleMap.containsKey(type)) {
            sampleMap.get(type).add(s);
            codeMap.get(type).add(code);
          } else {
            Set<Sample> sampleList = new HashSet<>(Arrays.asList(s));
            Set<String> codeList = new HashSet<>(Arrays.asList(code));
            sampleMap.put(type, sampleList);
            codeMap.put(type, codeList);
          }
        }
        return new SampleTypesInfo(sampleMap, codeMap);
      }
    });
    runTable = new Table("Existing Calculations");
    runTable.setVisible(false);
    runTable.setStyleName(Styles.tableTheme);
    runTable.addContainerProperty("Technology", String.class, "");
    runTable.addContainerProperty("Result Type", String.class, "");
    runTable.addContainerProperty("Download", Button.class, null);
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

  private List<RNACountData> preparePilotDataInfo(Set<Sample> set) {
    List<RNACountData> res = new ArrayList<>();
    if (set != null) {
      for (Sample s : set) {
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

  protected void showExistingRuns(Set<Sample> set) {
    if (set == null)
      return;
    List<String> relevantSampleIDs = new ArrayList<>();

    runTable.removeAllItems();
    for (Sample s : set) {
      if (s.getSampleTypeCode().equals(POWER_SAMPLE_TYPE)) {
        relevantSampleIDs.add(s.getIdentifier());

        Map<String, String> props = s.getProperties();
        String tech = props.get("Q_TECHNOLOGY_TYPE");
        String type = props.get("Q_SECONDARY_NAME");
        String xml = props.get("Q_PROPERTIES");
        Button download = new Button();
        // download.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
        download.setIcon(FontAwesome.DOWNLOAD);
        // download.setWidth(30, Unit.PIXELS);
        // download.setHeight(40, Unit.PIXELS);
        Button showResult = new Button();
        // showResult.setWidth(30, Unit.PIXELS);
        // showResult.setHeight(40, Unit.PIXELS);
        showResult.setIcon(FontAwesome.PICTURE_O);

        List<Object> row = new ArrayList<Object>();
        showResult.addClickListener(new Button.ClickListener() {

          @Override
          public void buttonClick(ClickEvent event) {
            preparePopUpView(xml, resourcesForSamples.get(s.getIdentifier()));
          }
        });

        row.add(tech);
        row.add(type);
        row.add(download);
        row.add(showResult);

        EntityRegistrationDetails regDetails = s.getRegistrationDetails();
        String user = regDetails.getModifierFirstName() + " " + regDetails.getModifierLastName();
        Date regDate = regDetails.getRegistrationDate();

        row.add(user);
        row.add(regDate);

        runTable.addItem(row.toArray(new Object[row.size()]), s);

        runTable.setVisible(true);
      }
    }
    int runs = runTable.size();
    runTable.setPageLength(runs);
    if (runs > 0) {
      mapSampleIDsToMatrixImages(relevantSampleIDs);
    }
  }

  private void preparePopUpView(String xml, Resource resource) {
    Window subWindow = new Window("Results");
    subWindow.setWidth("600");
    subWindow.setHeight("800");
    VerticalLayout subContent = new VerticalLayout();
    subContent.setSpacing(true);
    subContent.setMargin(true);
    subWindow.setContent(subContent);

    Image image = new Image("", resource);
    subContent.addComponent(image);

    Table params = new Table("Parameters");
    params.setStyleName(Styles.tableTheme);
    params.addContainerProperty("Name", String.class, "");
    params.addContainerProperty("Value", String.class, "");
    params.setVisible(false);
    XMLParser xmlParser = new XMLParser();
    List<Property> props = new ArrayList<>();
    try {
      props = xmlParser.getPropertiesFromXML(xml);
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
    for (Sample s : samples) {
      if (s.getSampleTypeCode().equals(POWER_SAMPLE_TYPE)) {
        String suffix = s.getCode().split("-")[1];
        int num = Integer.parseInt(suffix);
        latest = Math.max(latest, num);
      }
    }
    String suffix = Integer.toString(latest + 1);
    newSampleCode = project + "-" + suffix;
    return newSampleCode;
  }

  private void incrementPowerSampleCode() {
    String[] splt = newSampleCode.split("-");
    String prefix = splt[0];
    int num = Integer.parseInt(splt[1]);
    newSampleCode = prefix + '-' + Integer.toString(num + 1);
  }

  private String createStatisticsSample(List<Property> xmlProps, Map<String, String> props) {
    XMLParser xmlParser = new XMLParser();
    String qProperties = "";
    try {
      qProperties = xmlParser.toString(xmlParser.createXMLFromProperties(xmlProps));
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // (make sure space, project, experiment and parent sample(s) exist, if applicable!)
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("code", newSampleCode);
    params.put("type", POWER_SAMPLE_TYPE); // sample type - e.g. Q_BMI...

    params.put("parents",
        new ArrayList<String>(Arrays.asList('/' + space + '/' + project + "000"))); // these need to
                                                                                    // be
                                                                                    // identifiers
                                                                                    // of the parent
                                                                                    // samples
                                                                                    // (including
                                                                                    // the space)!
                                                                                    // E.g.
                                                                                    // /MULTISCALE_HCC/QMSHSENTITY-1

    params.put("project", project);
    params.put("space", space);
    params.put("experiment", project + "_INFO"); // e.g. the Q_BMI... experiment - the code is
                                                 // usually built differently than in this example!

    props.put("Q_PROPERTIES", qProperties); // xml properties, not important
    params.put("properties", props);

    openbis.ingest("DSS1", "register-samp", params);
    return newSampleCode;
  }

  /**
   * finds sample sizes of top tier sample type of a factor
   * 
   * @param infoExp
   * @param codesByType
   * @return
   */
  private Map<String, Map<String, Set<String>>> getSampleSizesForFactors(String infoExp,
      Map<String, Set<String>> codesByType) {

    List<SampleType> factorSampleTypes =
        new ArrayList<SampleType>(Arrays.asList(SampleType.Q_BIOLOGICAL_ENTITY,
            SampleType.Q_BIOLOGICAL_SAMPLE, SampleType.Q_TEST_SAMPLE));

    Map<String, Map<String, Set<String>>> factors = getExperimentalDesign(infoExp);
    Map<String, Map<String, Set<String>>> slimFactors = new HashMap<>();
    for (SampleType tier : factorSampleTypes) {
      Set<String> currentCodes = codesByType.get(tier.toString());
      for (String label : factors.keySet()) {
        // if label is found, factor was filled in on higher tier of the experimental design
        if (!slimFactors.containsKey(label)) {
          Map<String, Set<String>> levels = factors.get(label);
          Map<String, Set<String>> slimLevels = new HashMap<>();
          for (String level : levels.keySet()) {
            Set<String> codesOnFactorLevel = new HashSet<>();
            for (String code : levels.get(level)) {
              if (currentCodes.contains(code)) {
                codesOnFactorLevel.add(code);
              }
            }
            // empty if factor starts on subsequent hierarchy tier (e.g. not a patient factor)
            if (!codesOnFactorLevel.isEmpty()) {
              slimLevels.put(level, codesOnFactorLevel);
            }
          }
          if (!slimLevels.isEmpty()) {
            slimFactors.put(label, slimLevels);
          }
        }
      }
    }
    return slimFactors;
  }

  private Map<String, Map<String, Set<String>>> getExperimentalDesign(String infoExp) {
    List<Experiment> exps = openbis.getExperimentById2(infoExp);
    if (exps.isEmpty()) {
      logger.error("could not find info experiment" + infoExp);
    } else {
      Experiment designExperiment = exps.get(0);
      StudyXMLParser parser = new StudyXMLParser();
      JAXBElement<Qexperiment> expDesign;
      try {
        expDesign =
            parser.parseXMLString(designExperiment.getProperties().get("Q_EXPERIMENTAL_SETUP"));
        logger.debug("setting exp design: " + expDesign);
        return parser.getSamplesPerLevelForFactors(expDesign);
      } catch (

      JAXBException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return null;
  }


  public void mapSampleIDsToMatrixImages(List<String> sampleIDs) {
    resourcesForSamples = new HashMap<>();
    List<DataSet> datasets = openbis.listDataSetsForSamples(sampleIDs);
    Map<String, String> dataSetCodeToSampleID = new HashMap<>();
    List<String> dsCodes = new ArrayList<String>();
    Map<String, List<String>> params = new HashMap<String, List<String>>();

    for (DataSet ds : datasets) {
      String dsCode = ds.getCode();
      dsCodes.add(dsCode);
      dataSetCodeToSampleID.put(dsCode, ds.getSampleIdentifierOrNull());
    }
    params.put("codes", dsCodes);
    QueryTableModel res = openbis.queryFileInformation(params);

    for (Serializable[] ss : res.getRows()) {
      String dsCode = (String) ss[0];
      // String fileName = (String) ss[2];
      String dssPath = (String) ss[1];
      try {
        URL url = openbis.getDataStoreDownloadURLLessGeneric(dsCode, dssPath);
        Resource resource = new ExternalResource(url);

        resourcesForSamples.put(dataSetCodeToSampleID.get(dsCode), resource);
      } catch (MalformedURLException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
