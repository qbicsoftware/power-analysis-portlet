package life.qbic.samplesize.model;

import java.util.HashMap;
import java.util.Map;

public class Constants {

  public final static double LI_DISPERSION = 0.5;
  public final static double LI_READCOUNT = 1.0;
  public final static Map<String, String> TCGA_DATASETS = new HashMap<String, String>() {
    {
      put("TCGA_BLCA (Bladder Urothelial Carcinoma)", "TCGA_BLCA");
      put("TCGA_BRCA (Breast invasive carcinoma)", "TCGA_BRCA");
      put("TCGA_CESC (Cervical squamous cell carcinoma and endocervical adenocarcinoma)", "TCGA_CESC");
      put("TCGA_COAD (Colon adenocarcinoma)", "TCGA_COAD");
      put("TCGA_HNSC (Head and Neck squamous cell carcinoma)", "TCGA_HNSC");
      put("TCGA_KIRC (Kidney renal clear cell carcinoma)", "TCGA_KIRC");
      put("TCGA_LGG (Brain Lower Grade Glioma)", "TCGA_LGG");
      put("TCGA_LUAD (Lung adenocarcinoma)", "TCGA_LUAD");
      put("TCGA_LUSC (Lung squamous cell carcinoma)", "TCGA_LUSC");
      put("TCGA_PRAD (Prostate adenocarcinoma)", "TCGA_PRAD");
      put("TCGA_READ (Rectum adenocarcinoma)", "TCGA_READ");
      put("TCGA_THCA (Thyroid carcinoma)", "TCGA_THCA");
      put("TCGA_UCEC (Uterine Corpus Endometrial Carcinoma)", "TCGA_UCEC");
    }
  };
  public final static Map<String, Double> dispersionEstimates = new HashMap<String, Double>() {
    {
      put("edgeR Manual: Human", 0.4 * 0.4);// BioCV from edgeR is rooted dispersion
      put("edgeR Manual: Model organism", 0.1 * 0.1);
      put("edgeR Manual: Techn. replicates", 0.01 * 0.01);
      put("Li et al. (conservative estimate)", 0.5);
    }
  };
  // private final static double HART_DISPERSION = 2.0;
  // private final static double HART_READCOUNT = 10.0;
  public static final Map<String, Double> avgReadEstimates = new HashMap<String, Double>() {
    {
      put("Li et al. (conservative estimate)", 1.0);
      put("Hart et al.", 5.0);
    }
  };
}
