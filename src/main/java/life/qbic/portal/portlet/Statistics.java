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

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Sample;
import life.qbic.openbis.openbisclient.IOpenBisClient;

public class Statistics {

  private IOpenBisClient openbis;
  private List<String> projectIDs;
  private final Set<String> TECH_TYPES =
      new HashSet<String>(Arrays.asList("Q_NGS_SINGLE_SAMPLE_RUN", "Q_MS_RUN", "Q_MICROARRAY_RUN",
          "Q_MHC_LIGAND_EXTRACT", "Q_BMI_GENERIC_IMAGING_RUN"));
  private final Set<String> BLACKLIST =
      new HashSet<String>(Arrays.asList("EDDA_EXPERIMENTAL_DESIGN", "TEST_DROPBOXES",
          "DELETE_THIS_TEST", "ECKH", "CHICKEN_FARM", "TEACHING_DMQB_PROJECT", "CREATE_SPACE_DEMO",
          "BLA_BLUP", "IMMIGENE_TEST", "LUIS_KUHN_TEST_SPACE", "CONFERENCE_DEMO", "TEST-AND-TEST",
          "SIMON_TEST_ETL", "QBIC_BENCHMARK_DATASETS", "DEFAULT", "TEST28", "QBIC_ADMINISTRATION",
          "DELETE_PLS", "IMGAG_ORPHANS", "DAMIEN_MSC", "IVAC_TEST_SPACE", "QBIC_TEACHING",
          "LUISANDLUIS", "STAHL_TEST_SPACE", "TEST_PLS_DELETE"));

  public Statistics(IOpenBisClient openbis, List<String> projectIDs) {
    super();
    this.openbis = openbis;
    this.projectIDs = projectIDs;
    printProjectsAndSamplesWithTypes();
  }

  private void printProjectsAndSamplesWithTypes() {
//    int numProjects = 0;
//    Map<String, Integer> projectsPerType = new HashMap<String, Integer>();
//    Map<String, Integer> samplesPerType = new HashMap<String, Integer>();
//    int numSamples = 0;
//
//    for (String type : TECH_TYPES) {
//      projectsPerType.put(type, 0);
//      samplesPerType.put(type, 0);
//    }

    // int i = 0;
    // for (String p : projectIDs) {
    // i++;
    // if (i % 50 == 0) {
    // System.out.println(i + " of " + projectIDs.size());
    // }
    // String space = p.split("/")[1];
    // if (!BLACKLIST.contains(space)) {
    // List<Sample> samples = openbis.getSamplesOfProjectBySearchService(p);
    // numSamples += samples.size();
    // Set<String> foundTypes = new HashSet<String>();
    // for (Sample s : samples) {
    // String type = s.getSampleTypeCode();
    // if (TECH_TYPES.contains(type)) {
    // foundTypes.add(type);
    // samplesPerType.put(type, samplesPerType.get(type) + 1);
    // }
    // }
    // if (!foundTypes.isEmpty()) {
    // numProjects++;
    // }
    // for (String type : foundTypes) {
    // projectsPerType.put(type, projectsPerType.get(type) + 1);
    // }
    // }
    // }
    // System.out.println(numSamples + " samples.");
    // System.out.println(numProjects + " non-empty projects.");
    // System.out.println(projectsPerType);
    // System.out.println(samplesPerType);

    Map<Integer, Set<String>> yearToProjects = new HashMap<>();
    Map<String, Set<String>> analytesInProject = new HashMap<>();

    // for (Sample s : openbis.listSamplesForProjects(projectIDs)) {
    // String space = s.getSpaceCode();
    int i = 0;
    List<String> spaces = openbis.listSpaces();
    for (String space : spaces) {
      i++;
      if (i % 50 == 0) {
        System.out.println(i + " of " + (spaces.size() - BLACKLIST.size()));
      }
      if (!BLACKLIST.contains(space)) {
        for (Sample s : openbis.getSamplesofSpace(space)) {
          String project = s.getExperimentIdentifierOrNull().split("/")[2];
          String projectID = "/" + space + "/" + project;
          String type = s.getSampleTypeCode();
          if (type.equals("Q_TEST_SAMPLE")) {
            String analyte = s.getProperties().get("Q_SAMPLE_TYPE");
            if (analytesInProject.containsKey(projectID)) {
              analytesInProject.get(projectID).add(analyte);
            } else {
              analytesInProject.put(projectID, new HashSet<>(Arrays.asList(analyte)));
            }
          }
          Date regDate = s.getRegistrationDetails().getRegistrationDate();
          int year = Integer.parseInt(new SimpleDateFormat("yyyy").format(regDate));
          if (yearToProjects.containsKey(year)) {
            yearToProjects.get(year).add(projectID);
          } else {
            yearToProjects.put(year, new HashSet<>(Arrays.asList(projectID)));
          }
        }
      }
    }

    List<Integer> years = new ArrayList<>(yearToProjects.keySet());
    Collections.sort(years);

    for (int year : years) {
      System.out.println(year);
      List<String> projects = new ArrayList<>(yearToProjects.get(year));
      Collections.sort(projects);
      for (String project : projects) {
        System.out.println(project + "\t" + analytesInProject.get(project));
      }
      System.out.println("---");
    }

    //
    // Map<String, Integer> samplesPerMonth = new HashMap<>();
    // for (Sample s : openbis.listSamplesForProjects(projectIDs)) {
    // String space = s.getSpaceCode();
    // if (!BLACKLIST.contains(space)) {
    // Date regDate = s.getRegistrationDetails().getRegistrationDate();
    // String date = new SimpleDateFormat("yyyy-MM-dd").format(regDate);
    // if (samplesPerMonth.containsKey(date)) {
    // samplesPerMonth.put(date, samplesPerMonth.get(date) + 1);
    // } else {
    // samplesPerMonth.put(date, 1);
    // }
    // }
    // }
    // System.out.println(samplesPerMonth);
  }

}
