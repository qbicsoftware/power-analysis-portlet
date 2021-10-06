package life.qbic.portal.portlet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import life.qbic.openbis.openbisclient.IOpenBisClient;

public class Statistics {

  private IOpenBisClient openbis;
  private final Set<String> TECH_TYPES =
      new HashSet<String>(Arrays.asList("Q_NGS_SINGLE_SAMPLE_RUN", "Q_MS_RUN", "Q_MICROARRAY_RUN",
          "Q_MHC_LIGAND_EXTRACT", "Q_BMI_GENERIC_IMAGING_RUN"));
  private final Set<String> BLACKLIST =
      new HashSet<String>(Arrays.asList("2500_HUMAN_REFERENCE_GENOMES", "A4B_TESTING", "BIH_DEMO",
          "BLA_BLUP", "CFMB_TESTS", "CHICKEN_FARM", "CONFERENCE_DEMO", "DAMIEN_MSC", "DEFAULT",
          "DELETE_THIS_TEST", "EDDA_EXPERIMENTAL_DESIGN", "FRANCESCA_TEST", "ICGC-DATA",
          "IPSPINE_TESTING", "IVAC_ALL", "IVAC_ALL_1", "IVAC_ALL_DKTK", "IVAC_AML_KIKLI",
          "IVAC_BRAINTUMOR_MUE", "IVAC_CEGAT", "IVAC_EWING", "IVAC_HEPA_VAC",
          "IVAC_INDIVIDUAL_LIVER", "IVAC_INFORM_DKTK_KIKLI", "IVAC_LUCA", "IVAC_MACA", "IVAC_OVCA",
          "IVAC_PANC", "IVAC_PANC_KIKLI", "IVAC_RCC", "IVAC_RCC_KIKLI", "IVAC_SARC",
          "IVAC_SFB685_C9_PC", "IVAC_TEST_SPACE", "KIM-HELLMUTH_COVID-19", "LAST_TEST",
          "LUISANDLUIS", "LUIS_KUHN_TEST_SPACE", "PCT_TESTS", "PUBLIC_PROJECTS",
          "QBIC_ADMINISTRATION", "QBIC_BENCHMARK_DATASETS", "QBIC_CONSULTANCY",
          "QBIC_DECLINED_PROJECTS", "QBIC_N", "QBIC_TEACHING", "QBIC_WN", "SANDBOX_HSFS",
          "SANDBOX_IMGAG", "SANDBOX_MPC", "SANDBOX_MPI", "SANDBOX_NMI", "SANDBOX_RKI",
          "SANDBOX_ZMBP", "SIMON_TEST_ETL", "STAHL_TEST_SPACE", "TEACHING_DMQB_PROJECT",
          "TEST-AND-TEST", "TEST28", "TEST3", "TEST_DROPBOXES", "TEST_PLS_DELETE",
          "TEST_SAMPLE_TRACKING", "TEST_SPACE", "TEST_STEFAN", "TEST_STEFAN2", "TEST_STEFFEN_L"));

  public Statistics(IOpenBisClient openbis) {
    super();
    this.openbis = openbis;
    printProjectsAndSamplesWithTypes();
  }

  private void printProjectsAndSamplesWithTypes() {
    int numProjects = 0;
    Map<String, Integer> projectsPerType = new HashMap<String, Integer>();
    Map<String, Integer> samplesPerType = new HashMap<String, Integer>();
    int numSamples = 0;

    for (String type : TECH_TYPES) {
      projectsPerType.put(type, 0);
      samplesPerType.put(type, 0);
    }

    int i = 0;
    List<Project> projects = openbis.listProjects();
    for (Project p : projects) {
      i++;
      if (i % 50 == 0) {
        System.out.println(i + " of " + projects.size());
      }
      
      String space = p.getSpace().getCode();
      if (!BLACKLIST.contains(space)) {
        List<Sample> samples = openbis.getSamplesOfProject(p.getIdentifier().getIdentifier());
        numSamples += samples.size();
        Set<String> foundTypes = new HashSet<String>();
        for (Sample s : samples) {
          String type = s.getType().getCode();
          if (TECH_TYPES.contains(type)) {
            foundTypes.add(type);
            samplesPerType.put(type, samplesPerType.get(type) + 1);
          }
        }
        if (!foundTypes.isEmpty()) {
          numProjects++;
        }
        for (String type : foundTypes) {
          projectsPerType.put(type, projectsPerType.get(type) + 1);
        }
      }
    }
    System.out.println(numSamples + " samples.");
    System.out.println(numProjects + " non-empty projects.");
    System.out.println(projectsPerType);
    System.out.println(samplesPerType);

  //228837 samples.
  //651 non-empty projects.
  //{Q_MS_RUN=364, Q_MICROARRAY_RUN=18, Q_MHC_LIGAND_EXTRACT=17, Q_NGS_SINGLE_SAMPLE_RUN=279, Q_BMI_GENERIC_IMAGING_RUN=2}
  //{Q_MS_RUN=19482, Q_MICROARRAY_RUN=701, Q_MHC_LIGAND_EXTRACT=1739, Q_NGS_SINGLE_SAMPLE_RUN=19010, Q_BMI_GENERIC_IMAGING_RUN=172}


    Map<Integer, Set<String>> yearToProjects = new HashMap<>();
    Map<String, Set<String>> analytesInProject = new HashMap<>();

    // for (Sample s : openbis.listSamplesForProjects(projectIDs)) {
    // String space = s.getSpaceCode();
    // i = 0;
    // List<String> spaces = openbis.listSpaces();
    // for (String space : spaces) {
    // i++;
    // if (i % 50 == 0) {
    // System.out.println(i + " of " + (spaces.size() - BLACKLIST.size()));
    // }
    // if (!BLACKLIST.contains(space)) {
    // for (Sample s : openbis.getSamplesofSpace(space)) {
    // String project = "";
    // try {
    // project = s.getExperiment().getIdentifier().getIdentifier().split("/")[2];
    //
    // } catch (Exception e) {
    // System.out.println(s.getCode();
    // }
    // String projectID = "/" + space + "/" + project;
    // String type = s.getType().getCode();
    // if (type.equals("Q_TEST_SAMPLE")) {
    // String analyte = s.getProperties().get("Q_SAMPLE_TYPE");
    // if (analytesInProject.containsKey(projectID)) {
    // analytesInProject.get(projectID).add(analyte);
    // } else {
    // analytesInProject.put(projectID, new HashSet<>(Arrays.asList(analyte)));
    // }
    // }
    // Date regDate = s.getRegistrationDate();
    // int year = Integer.parseInt(new SimpleDateFormat("yyyy").format(regDate));
    // if (yearToProjects.containsKey(year)) {
    // yearToProjects.get(year).add(projectID);
    // } else {
    // yearToProjects.put(year, new HashSet<>(Arrays.asList(projectID)));
    // }
    // }
    // }
    // }
    //
    // List<Integer> years = new ArrayList<>(yearToProjects.keySet());
    // Collections.sort(years);
    //
    // for (int year : years) {
    // System.out.println(year);
    // List<String> projects = new ArrayList<>(yearToProjects.get(year));
    // Collections.sort(projects);
    // for (String project : projects) {
    // System.out.println(project + "\t" + analytesInProject.get(project));
    // }
    // System.out.println("---");
    // }
    //
    // //
    // // Map<String, Integer> samplesPerMonth = new HashMap<>();
    // // for (Sample s : openbis.listSamplesForProjects(projectIDs)) {
    // // String space = s.getSpaceCode();
    // // if (!BLACKLIST.contains(space)) {
    // // Date regDate = s.getRegistrationDetails().getRegistrationDate();
    // // String date = new SimpleDateFormat("yyyy-MM-dd").format(regDate);
    // // if (samplesPerMonth.containsKey(date)) {
    // // samplesPerMonth.put(date, samplesPerMonth.get(date) + 1);
    // // } else {
    // // samplesPerMonth.put(date, 1);
    // // }
    // // }
    // // }
    // // System.out.println(samplesPerMonth);
  }

}
