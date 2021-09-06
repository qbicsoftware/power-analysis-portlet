package life.qbic.portal.portlet;

import java.util.Arrays;

//import org.apache.commons.math3.distribution.TDistribution;
//import org.apache.commons.math3.stat.inference.TTest;

public class RStuff {

//  static TTest ttester = new TTest();
//
//  private static double CDF(double[] x, int n1, int n2, double[] D, double[] p, double sigma) {
//
//    if (D.length == 0) {
//      return 0;
//    }
//    double ncp = -D[0] * Math.sqrt((n1 * n2) / (n1 + n2)) / sigma;
//    int df = n1 + n2 - 2;
//    double ret = p[0] * tDist(x, df, ncp) + CDF(x, n1, n2, Arrays.copyOfRange(D, 1, D.length),
//        Arrays.copyOfRange(p, 1, p.length), sigma);
//    return ret;
//  }
//
//  private static double paired_CDF(double[] x, int n, double[] D, double[] p, double sigma) {
//    if (D.length == 0) {
//      return 0;
//    }
//    double ncp = -D[0] * Math.sqrt(n) / sigma;
//    int df = n - 1;
//    double ret = p[0] * tDist(x, df, ncp) + paired_CDF(x, n, Arrays.copyOfRange(D, 1, D.length),
//        Arrays.copyOfRange(p, 1, p.length), sigma);
//    return ret;
//  }
//
//  private static double tDist(double[] x, int df, double ncp) {
//    TDistribution t = new TDistribution(df);// TODO ncp?
//    return t;
//  }
//
//  private static double[] scaleArray(double[] arr, double factor) {
//    double[] res = new double[arr.length];
//    for (int i = 0; i < arr.length; i++) {
//      res[i] = arr[i] * factor;
//    }
//    return res;
//  }
//
//  private static void makeNegativeArray(double[] arr) {
//    for (int i = 0; i < arr.length; i++) {
//      if (arr[i] > 0) {
//        arr[i] = -arr[i];
//      }
//    }
//  }
//
//  private static double FDR(double[] x, int n1, int n2, double pmix, double[] D0, double[] p0,
//      double[] D1, double[] p1, double sigma) {
//    makeNegativeArray(x);
//    double[] negX = scaleArray(x, -1);
//    return pmix * (CDF(x, n1, n2, D0, p0, sigma) + 1 - CDF(negX, n1, n2, D0, p0, sigma))
//        / (CDFmix(x, n1, n2, pmix, D0, p0, D1, p1, sigma) + 1
//            - CDFmix(negX, n1, n2, pmix, D0, p0, D1, p1, sigma));
//  }
//
//  private static double CDFmix(double[] x, int n1, int n2, double pmix, double[] d0, double[] p0,
//      double[] d1, double[] p1, double sigma) {
//    return pmix * CDF(x, n1, n2, d0, p0, sigma) + (1 - pmix) * CDF(x, n1, n2, d1, p1, sigma);
//  }
//
//  private static double lfdr(double[] x, int n1, int n2, double pmix, double[] D0, double[] p0,
//      double[] D1, double[] p1, double sigma) {
//    makeNegativeArray(x);
//    double[] negX = scaleArray(x, -1);
//    return pmix * (dmt(x, n1, n2, D0, p0, sigma) + dmt(negX, n1, n2, D0, p0, sigma))
//        / (dmtmix(x, n1, n2, pmix, D0, p0, D1, p1, sigma)
//            + dmtmix(negX, n1, n2, pmix, D0, p0, D1, p1, sigma));
//  }
//
//
//  private static double dmtmix(double[] x, int n1, int n2, double pmix, double[] D0, double[] p0,
//      double[] D1, double[] p1, double sigma) {
//    return pmix * dmt(x, n1, n2, D0, p0, sigma) + (1 - pmix) * dmt(x, n1, n2, D1, p1, sigma);
//  }
//
//  private static double dmt(double[] x, int n1, int n2, double[] D, double[] p, double sigma) {
//    if (D.length == 0) {
//      return 0;
//    }
//    int df = n1 + n2 - 2;
//    double ncp = -D[1] * Math.sqrt((n1 * n2) / (n1 + n2)) / sigma;
//    double ret = p[1] * tDist(x, df, ncp) + dmt(x, n1, n2, Arrays.copyOfRange(D, 1, D.length),
//        Arrays.copyOfRange(p, 1, p.length), sigma);
//    return ret;
//  }
}

