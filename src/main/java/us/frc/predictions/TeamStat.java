package us.frc.predictions;

import static us.frc.predictions.Breakdown2017.autoFuelPoints;
import static us.frc.predictions.Breakdown2017.kPaRankingPointAchieved;
import static us.frc.predictions.Breakdown2017.rotor1Auto;
import static us.frc.predictions.Breakdown2017.rotor2Auto;
import static us.frc.predictions.Breakdown2017.rotor2Engaged;
import static us.frc.predictions.Breakdown2017.rotor3Engaged;
import static us.frc.predictions.Breakdown2017.rotor4Engaged;
import static us.frc.predictions.Breakdown2017.rotorRankingPointAchieved;
import static us.frc.predictions.Breakdown2017.teleopFuelPoints;
import static us.frc.predictions.Breakdown2017.teleopTakeoffPoints;

import java.text.DecimalFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class TeamStat {
  public final Integer mTeam;
  public final Double OPR, DPR, CCWM;
  private final Map<Breakdown2017, Double> mStats = new HashMap<>();
  private final Map<Breakdown2017, Double> mAntiStats = new HashMap<>();
  

  public static EnumSet<Breakdown2017> CalculatedValues = EnumSet.of(
    rotor1Auto, rotor2Auto,
    autoFuelPoints, teleopFuelPoints, 
    rotor2Engaged, rotor3Engaged, rotor4Engaged,
    rotorRankingPointAchieved, kPaRankingPointAchieved,
    teleopTakeoffPoints
  );
  
  public TeamStat (Integer pTeam, TBACalc pCalc, Double pOpr, Double pDpr) {
    mTeam = pTeam;
    for(Breakdown2017 stat : CalculatedValues) {
      mStats.put(stat, pCalc.getForKey(stat.name()).get(pTeam));
      mAntiStats.put(stat, pCalc.getForKey(stat.name(), true).get(pTeam));
    }
    OPR = pOpr;
    DPR = pDpr;
    CCWM = pOpr - pDpr;
  }
  
  private static DecimalFormat nf = new DecimalFormat("0.0");
  
  /**
   * @return String that represents the headers of row outputs
   */
  public static String getHeader() {
    return "TEAM\tOPR\tCCWM\tVALUE\tAUTOR1\tROTOR2\tROTOR3\tROTOR4\tCLIMB\tAFUEL\tTFUEL\tKPABONUS\t4ROTORB";
  }
  private static char SEP = '\t';
  public String toString(){ 
    StringBuilder sb = new StringBuilder();
    sb.append(mTeam)
      .append(SEP).append(nf.format(OPR))
      .append(SEP).append(nf.format(CCWM))
      .append(SEP).append(nf.format(getAddedValue()))
      .append(SEP).append(nf.format(get(rotor1Auto)))
      .append(SEP).append(nf.format(get(rotor2Engaged)))
      .append(SEP).append(nf.format(get(rotor3Engaged)))
      .append(SEP).append(nf.format(get(rotor4Engaged)))
      .append(SEP).append(nf.format(get(teleopTakeoffPoints)))
      .append(SEP).append(nf.format(get(autoFuelPoints)))
      .append(SEP).append(nf.format(get(teleopFuelPoints)))
      .append(SEP).append(nf.format(get(kPaRankingPointAchieved)))
      .append(SEP).append(nf.format(get(rotorRankingPointAchieved)))
    ;
    return sb.toString();
  }
  
  /**
   * @return opponent's stat breakdowns, similar to DPR
   */
  public Double getAntiValue() {
    Double result = 0d;
    for(Breakdown2017 stat : CalculatedValues) {
      result += getAnti(stat);
    }
    return result;
  }
  
  /**
   * @return this team's value, as calculated by the component breakdowns
   */
  public Double getAddedValue() {
    Double result = 0d;
    for(Breakdown2017 stat : CalculatedValues) {
      result += get(stat);
    }
    return result;
  }
  
  /**
   * @return gets the added value, but also adds "free" points that pretty much every bot can do
   */
  public Double getMatchValue() {
    Double result = getAddedValue();
    result += get(Breakdown2017.rotor1Engaged); // assume they can place a free gear...
    result += 5d; // assume they move in auton...
    return result;
  }
  
  /**
   * Get a stat's value
   */
  public double get(Breakdown2017 pStat) {
    return mStats.get(pStat);
  }
  
  /**
   * Use with caution - i.e. use this to normalize/clamp, but 
   * don't use this as a regular 'set' method.
   */
  public void override(Breakdown2017 pStat, Double pValue) {
    mStats.put(pStat, pValue);
  }
  
  /**
   * Return the opponents' stat
   */
  public double getAnti(Breakdown2017 pStat) {
    return mAntiStats.get(pStat);
  }
}
