package us.frc.predictions;

public enum Breakdown2017 {
  teleopPoints, rotor1Auto, autoPoints, rotor1Engaged, foulCount, 
  touchpadFar, foulPoints, techFoulCount, totalPoints, tba_rpEarned, autoRotorPoints, 
  adjustPoints, rotor2Auto, rotor4Engaged, teleopRotorPoints, autoFuelHigh, 
  teleopFuelHigh, teleopTakeoffPoints, kPaRankingPointAchieved, autoFuelLow, 
  teleopFuelLow, rotorBonusPoints, autoMobilityPoints, rotor3Engaged, autoFuelPoints, 
  teleopFuelPoints, touchpadMiddle, touchpadNear, rotorRankingPointAchieved, 
  kPaBonusPoints, rotor2Engaged,
  
  // Custom
  aGearCount,
  tGearCount,
  gearCount;
  
  public String toString() {
    return name();
  }
  
  public Double getStaticValue() {
    switch(this) {
    case autoMobilityPoints:
      return 5d;
      
    case rotor1Auto: 
    case rotor2Auto:
      return 1d;
      
     case autoRotorPoints:
       return 20d;
      
    case rotor1Engaged:
    case rotor2Engaged:
    case rotor3Engaged:
    case rotor4Engaged:
      return 1d;
      
    case teleopTakeoffPoints:
      return 50d;
      
    case rotorRankingPointAchieved:
      return 100d;
      
    case kPaRankingPointAchieved:
      return 20d;
      
    default: 
      return 10000d;
    }
  }
  
  public Double map(String pValue) {
    switch(this) {
    case rotor1Auto:
    case rotor2Auto:
    case rotor1Engaged:
    case rotor2Engaged:
    case rotor3Engaged:
    case rotor4Engaged:
    case rotorRankingPointAchieved:
    case kPaRankingPointAchieved:
      return Boolean.parseBoolean(pValue) ? getStaticValue() : 0d;
    case touchpadFar:
    case touchpadMiddle:
    case touchpadNear:
      return pValue.equalsIgnoreCase("ReadyForTakeoff") ? 50d : 0d;
    default:
      try {
        return Double.parseDouble(pValue);
      } catch (NumberFormatException nfe) {
        System.err.println("Unable to parse " + this + " - " + pValue);
        return 0d;
      }
    }
  }
}