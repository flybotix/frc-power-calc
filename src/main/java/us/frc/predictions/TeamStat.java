package us.frc.predictions;

import static us.frc.predictions.Breakdown2017.autoFuelPoints;
import static us.frc.predictions.Breakdown2017.autoMobilityPoints;
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
import java.util.StringTokenizer;

public class TeamStat {
  public final Integer mTeam;
  public final Double OPR, DPR, CCWM;
  public final String mLocation;
  public final String EVENT;
  private final Map<Breakdown2017, Double> mStats = new HashMap<>();
  private final Map<Breakdown2017, Double> mAntiStats = new HashMap<>();
  

  public static EnumSet<Breakdown2017> CalculatedValues = EnumSet.of(
    rotor1Auto, rotor2Auto,
    autoMobilityPoints, //autoRotorPoints,
    autoFuelPoints, teleopFuelPoints, 
    rotor2Engaged, rotor3Engaged, rotor4Engaged,
    rotorRankingPointAchieved, kPaRankingPointAchieved,
    teleopTakeoffPoints
  );
  
  public TeamStat (Integer pTeam, TBACalc pCalc, Double pOpr, Double pDpr) {
    mTeam = pTeam;
    for(Breakdown2017 stat : CalculatedValues) {
      mStats.put(stat, pCalc.getForKey(stat.name()).get(pTeam)/stat.getStaticValue());
      mAntiStats.put(stat, pCalc.getForKey(stat.name(), true).get(pTeam)/stat.getStaticValue());
    }
    String loc = TBACalc.api.getTeam(mTeam).getLocation();
    int firstComma = loc.indexOf(",");
    
    loc = loc
      .substring(firstComma, loc.indexOf(",", firstComma+1))
      .replaceAll(", ", "")
      .replaceAll("[0-9]", "");
    loc = loc
        .substring(0, Math.min(loc.length(), 8))
        .trim();
    mLocation = loc;
    OPR = pOpr;
    DPR = pDpr;
    CCWM = pOpr - pDpr;
    EVENT = pCalc.mEvent;
  }
  
  private TeamStat(Integer pTeam, Map<Breakdown2017, Double> pStats, Double pOpr, Double pCCWM, String pLoc, String pEvent) {
    mTeam = pTeam;
    mStats.putAll(pStats);
    OPR = pOpr;
    CCWM = pCCWM;
    DPR = OPR - CCWM;
    mLocation = pLoc;
    EVENT = pEvent;
  }
  
  private static DecimalFormat nf = new DecimalFormat("0.00");
  
  /**
   * @return String that represents the headers of row outputs
   */
  public static String getHeader() {
    return "TEAM\tOPR\tCCWM"
      + "\tVALUE\tAUTOR1\tAMOVE\tROTOR2\tROTOR3\tROTOR4"
      + "\tTOTGRS"
      + "\tCLIMB\tAFUEL\tTFUEL\tKPABONUS\tLOC\tEVENT"
      ;
  }
  
  public double getNumGears() {
    double autogears = get(rotor1Auto);
    double teleopgears = get(rotor2Engaged) *2d + get(rotor3Engaged) * 4d + get(rotor4Engaged) * 6d - autogears;
    double numgears = autogears + teleopgears;
    return numgears;
  }
  
  public boolean contains(Breakdown2017 pStat) {
    return mStats.containsKey(pStat);
  }
  
  private static String SEP = "\t";
  public String toString(){ 
    
    StringBuilder sb = new StringBuilder();
    sb.append(mTeam)
      .append(SEP).append(nf.format(OPR))
      .append(SEP).append(nf.format(CCWM))
      .append(SEP).append(nf.format(getAddedValue()))
      .append(SEP).append(nf.format(get(rotor1Auto)))
      .append(SEP).append(nf.format(get(autoMobilityPoints)))
      .append(SEP).append(nf.format(get(rotor2Engaged)))
      .append(SEP).append(nf.format(get(rotor3Engaged)))
      .append(SEP).append(nf.format(get(rotor4Engaged)))
      .append(SEP).append(nf.format(getNumGears()))
      .append(SEP).append(nf.format(get(teleopTakeoffPoints)))
      .append(SEP).append(nf.format(get(autoFuelPoints)))
      .append(SEP).append(nf.format(get(teleopFuelPoints)))
      .append(SEP).append(nf.format(get(kPaRankingPointAchieved)))
      .append(SEP).append(mLocation)
      .append(SEP).append(EVENT)
    ;
    return sb.toString();
  }
  
  public static TeamStat fromString(String pLine) {
    Map<Breakdown2017, Double> values = new HashMap<>();
    StringTokenizer st = new StringTokenizer(pLine, SEP);
    Integer team = Integer.parseInt(st.nextToken());
    Double opr = Double.parseDouble(st.nextToken());
    Double ccwm = Double.parseDouble(st.nextToken());
    Double addedValue = Double.parseDouble(st.nextToken());
    values.put(rotor1Auto, Double.parseDouble(st.nextToken()));
    values.put(autoMobilityPoints, Double.parseDouble(st.nextToken()));
    values.put(rotor2Engaged, Double.parseDouble(st.nextToken()));
    values.put(rotor3Engaged, Double.parseDouble(st.nextToken()));
    values.put(rotor4Engaged, Double.parseDouble(st.nextToken()));
    Double numGears = Double.parseDouble(st.nextToken());
    values.put(teleopTakeoffPoints, Double.parseDouble(st.nextToken()));
    values.put(autoFuelPoints, Double.parseDouble(st.nextToken()));
    values.put(teleopFuelPoints, Double.parseDouble(st.nextToken()));
    values.put(kPaRankingPointAchieved, Double.parseDouble(st.nextToken()));
    String loc = st.nextToken();
    String event = st.nextToken();

    return new TeamStat(team, values, opr, ccwm, loc, event);
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
    Double result = 13.3d;
//    for(Breakdown2017 stat : CalculatedValues) {
//      result += get(stat);
//    }
    result += get(rotor1Auto) * 20d;
    result += get(autoMobilityPoints) * 5d;
    result += get(rotor2Engaged) * 40d;
    result += get(rotor3Engaged) * 40d;
    result += get(rotor4Engaged) * 40d;
    result += get(teleopTakeoffPoints) * 50d;
    result += get(autoFuelPoints);
    result += get(teleopFuelPoints);
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
    // WARNING - this is effectively casting a Double to a double (a.k.a. auto-boxing).
    // if there is a null pointer exception on this line then it means the map doesn't
    // contain a value for pStat, which is likely due to an improper mapping or logic somewhere.
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
  
  public void mod(Breakdown2017 pStat, Double pMult) {
    mStats.put(pStat, mStats.get(pStat) * pMult);
  }
}
