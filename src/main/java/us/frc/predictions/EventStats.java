package us.frc.predictions;

import java.util.Arrays;

public class EventStats {
  public static final String EVENT = "2017vagle";
  public static final String[] HOUSTON = new String[] {
  "2017tur",
  "2017roe",
  "2017new",
  "2017hop",
  "2017gal",
  "2017carv"
  };
  
  public static final String[] STL = new String[] {
    "2017dar",
    "2017cars",
    "2017arc",
    "2017cur",
    "2017dal",
    "2017tes"
  };
  
  public static void main(String[] pArgs) {
//    
    Utils.getStatsForTeamsAttendingMulti(true, true, STL);
    
//    Utils.getStatsForSpecificEvents(true, true, STL);
    
//    Utils.getEventsForTeams(Utils.getTeamsForEvent("2017dar"), "2017dar");
//    System.out.println(Utils.getAllEventsForSingleTeam(1885));
    
//    System.out.println(Arrays.toString(Utils.getQualScheduleForEvent("2017dar")));
  }

  
}
