package us.frc.predictions;

import java.util.Collection;

public class Average {
  private double mBaseVal = 0d;
  private double mCount = 0d;
  
  public void add(double pVal) {
    mBaseVal += pVal;
    mCount ++;
  }
  
  public double getAverage() {
    return mBaseVal / mCount;
  }
  
  public static double of(Double... doubles) {
    Average a = new Average();
    for(double d : doubles) {
      a.add(d);
    }
    return a.getAverage();
  }
  
  public static double of(double... doubles) {
    Average a = new Average();
    for(double d : doubles) {
      a.add(d);
    }
    return a.getAverage();
  }
  
  public static double of(Collection<Double> doubles) {
    return of(doubles.toArray(new Double[0]));
  }
}
