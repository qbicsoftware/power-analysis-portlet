package life.qbic.samplesize.control;

import java.math.BigInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MathHelpers {
  
  private static final Logger logger = LogManager.getLogger(MathHelpers.class);
  
  public static int getGCD(int n, int m) {
    return BigInteger.valueOf(m).gcd(BigInteger.valueOf(n)).intValue();
  }

}
