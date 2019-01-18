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
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Image;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
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
import life.qbic.xml.manager.XMLParser;
import life.qbic.xml.properties.Property;
import life.qbic.xml.study.Qexperiment;

public class Controller {

  private static final Logger logger = LogManager.getLogger(Controller.class);

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

          Map<String, List<Integer>> sampleSizesOfFactorLevels =
              getSampleSizesForFactors(infoExpID);

          List<Sample> samples = openbis.getSamplesOfProjectBySearchService(projectID);

          showExistingRuns(samples);
          String newSampleCode = newPowerSampleCode(samples);

          maEstView.setProjectContext(projectID, newSampleCode);
          rnaEstView.setProjectContext(projectID, newSampleCode);
          maCheckView.setProjectContext(projectID, newSampleCode);
          rnaCheckView.setProjectContext(projectID, newSampleCode);
          maCheckView.setDesigns(sampleSizesOfFactorLevels);
          rnaCheckView.setDesigns(sampleSizesOfFactorLevels);
        }
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

  protected void showExistingRuns(List<Sample> samples) {
    List<String> relevantSampleIDs = new ArrayList<>();

    runTable.removeAllItems();
    for (Sample s : samples) {
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

    Image image = new Image("Matrix", resource);
    subContent.addComponent(image);
    // PopupView popup = new PopupView("Pop it up", image);
    // subContent.addComponent(popup);

    // if (dssPath.endsWith(".png")) {
    // Image image = new Image("DSS image", resource);
    // mainLayout.addComponent(image);
    // } else if (dssPath.endsWith(".pdf")) {
    // Window window = new Window();
    // window.setWidth("90%");
    // window.setHeight("90%");
    // BrowserFrame e = new BrowserFrame("PDF File", resource);
    // e.setWidth("100%");
    // e.setHeight("100%");
    // window.setContent(e);
    // window.center();
    // window.setModal(true);
    // UI.getCurrent().addWindow(window);
    // }

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
    String prefix = samples.get(0).getCode().split("-")[0];
    for (Sample s : samples) {
      if (s.getSampleTypeCode().equals(POWER_SAMPLE_TYPE)) {
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
    XMLParser xmlParser = new XMLParser();
    String qProperties = "";
    try {
      qProperties = xmlParser.toString(xmlParser.createXMLFromProperties(xmlProps));
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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

    props.put("Q_PROPERTIES", qProperties);
    params.put("properties", props);

    openbis.ingest("DSS1", "register-samp", params);
    return newSampleCode;
  }

  private Map<String, List<Integer>> getSampleSizesForFactors(String infoExp) {
    List<Experiment> exps = openbis.getExperimentById2(infoExp);
    Map<String, List<Integer>> res = new HashMap<>();
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
        res = parser.getSampleSizesOfFactorLevels(expDesign);
      } catch (JAXBException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return res;
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
      System.out.println(ds);
      System.out.println(ds.getSampleIdentifierOrNull());
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
