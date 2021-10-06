package life.qbic.samplesize.control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import life.qbic.samplesize.model.EstimationMode;

public class VMConnection {

  private String vm;
  private String container;
  private String path;
  private String user;

  public VMConnection(String vm, String container, String path, String user) {
    this.vm = vm;
    this.container = container;
    this.path = path;
    this.user = user;
  }

  private ArrayList<String> getMatrixCommandBase(String sampleCode) {
    ArrayList<String> res =
        new ArrayList<>(Arrays.asList("ssh", vm, "singularity", "run", container, user, sampleCode));
    return res;
  }

  /**
   * mode <- args[1] # data, tcga or none m <- as.numeric(args[2]) # number of genes m1 <-
   * as.numeric(args[3]) # expected number of DE genes n <- as.numeric(args[4]) # sample size #rho
   * <- as.numeric(args[3]) # minimum detectable log fold change f <- as.numeric(args[5]) # FDR
   * 
   * if(mode=="none") { phi0 <- as.numeric(args[6]) # dispersion #lambda0 <- as.numeric(args[6]) #
   * avg. read count/gene result_file <- args[7] } if(mode=="tcga") { tcga <- args[6]
   * 
   */
  private ArrayList<String> getPowerMatrixCommand(int genes, int deGenes, int sampleSize,
      double avgReads, double dispersion) {
    ArrayList<String> res =
        new ArrayList<>(Arrays.asList(Integer.toString(genes), Integer.toString(deGenes),
            Integer.toString(sampleSize), Double.toString(avgReads), Double.toString(dispersion)));
    return res;
  }

  private ArrayList<String> getPowerMatrixWithDataCommand(int genes, int deGenes, int sampleSize,
      String dataset) {
    ArrayList<String> res = new ArrayList<>(Arrays.asList(Integer.toString(genes),
        Integer.toString(deGenes), Integer.toString(sampleSize), dataset));
    return res;
  }

  // public void touchTest() {
  // ArrayList<String> cmd = new ArrayList<>(Arrays.asList("ssh", vm, "touch", "testfile.txt"));
  // ProcessBuilderWrapper pbd = null;
  // try {
  // System.out.println("sending: " + cmd);
  // pbd = new ProcessBuilderWrapper(cmd);
  // } catch (Exception e) {
  // e.printStackTrace();
  // }
  // if (pbd.getStatus() != 0) {
  // System.out.println("Command has terminated with status: " + pbd.getStatus());
  // System.out.println("Error: " + pbd.getErrors());
  // System.out.println("Last command sent: " + cmd);
  // }
  // }

  // mode <- as.numeric(args[1]) # mode: data, tcga, none
  // m <- as.numeric(args[2]) # number of genes
  // m1 <- as.numeric(args[3]) # expected number of DE genes
  // f <- as.numeric(args[4]) # FDR
  //
  // if(mode=="none") {
  // phi0 <- as.numeric(args[5]) # dispersion
  // lambda0 <- as.numeric(args[6]) # avg. read count/gene
  // result_file <- args[7]
  // }
  // if(mode=="tcga") {
  // tcga <- args[5]
  // result_file <- args[6]

  private ArrayList<String> getSampleSizeMatrixCommand(int genes, int deGenes, double fdr,
      double dispersion, double avgReadCount) {
    ArrayList<String> res =
        new ArrayList<>(Arrays.asList(Integer.toString(genes), Integer.toString(deGenes),
            Double.toString(fdr), Double.toString(dispersion), Double.toString(avgReadCount)));
    return res;
  }

  private ArrayList<String> getSampleSizeMatrixWithDataCommand(int genes, int deGenes, double fdr,
      String dataset) {
    ArrayList<String> res = new ArrayList<>(Arrays.asList(Integer.toString(genes),
        Integer.toString(deGenes), Double.toString(fdr), dataset));
    return res;
  }

  public void sampleSize(String sampleCode, int genes, int deGenes, double dispersion, double fdr,
      double avgReadsPerGene) {
    List<String> cmd = getMatrixCommandBase(sampleCode);
    cmd.add("samples");
    cmd.add("none");
    cmd.addAll(getSampleSizeMatrixCommand(genes, deGenes, fdr, dispersion, avgReadsPerGene));
    runCommand(cmd);
  }

  public void sampleSizeWithData(String sampleCode, int genes, int deGenes, double fdr, String dataset,
      EstimationMode mode) {
    List<String> cmd = getMatrixCommandBase(sampleCode);
    cmd.add("samples");
    cmd.add(mode.toString().toLowerCase());
    cmd.addAll(getSampleSizeMatrixWithDataCommand(genes, deGenes, fdr, dataset));
    runCommand(cmd);
  }

  /**
   * power analysis based on statistical measures of an existing dataset
   * 
   * @param project code of openBIS project
   * @param genes number of genes
   * @param deGenes number of differentially expressed genes
   * @param sampleSize the number of samples per study group
   * @param dataset name of the dataset object
   * @param mode either 'tcga' for a dataset from the cancer genome atlas, or 'data' for any other
   *        dataset
   */
  public void powerWithData(String project, int genes, int deGenes, int sampleSize, String dataset, EstimationMode mode) {
    List<String> cmd = getMatrixCommandBase(project);
    cmd.add("power");
    cmd.add(mode.toString().toLowerCase());
    cmd.addAll(getPowerMatrixWithDataCommand(genes, deGenes, sampleSize, dataset));
    runCommand(cmd);
  }

  private void runCommand(List<String> cmd) {
    ProcessBuilderWrapper pbd = null;
    try {
      System.out.println("sending: " + cmd);
      pbd = new ProcessBuilderWrapper(cmd, false);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (pbd.getStatus() != 0) {
      System.out.println("Command has terminated with status: " + pbd.getStatus());
      System.out.println("Error: " + pbd.getErrors());
      System.out.println("Last command sent: " + cmd);
    }
  }

  /**
   * power analysis without data
   * 
   * @param project code of openBIS project
   * @param genes number of genes
   * @param deGenes number of differentially expressed genes
   * @param sampleSize the number of samples per study group
   * @param dispersion dispersion of the negative binomial distribution a.k.a. squared biological
   *        coefficient of variance
   * @param avgReads average read count
   */
  public void power(String sampleCode, int genes, int deGenes, int sampleSize, double dispersion,
      double avgReads) {
    List<String> cmd = getMatrixCommandBase(sampleCode);
    cmd.add("power");
    cmd.add("none");
    cmd.addAll(getPowerMatrixCommand(genes, deGenes, sampleSize, dispersion, avgReads));
    runCommand(cmd);
  }
}
