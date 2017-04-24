package us.frc.predictions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vegetarianbaconite.blueapi.beans.Match;

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
//    "2017cars",
//    "2017arc",
//    "2017cur",
//    "2017dal",
//    "2017tes"
  };
  
  public static void main(String[] pArgs) {
    
//    Utils.getStatsForTeamsAttendingMulti(true, true, STL);
    
//    Utils.getStatsForSpecificEvents(true, true, EVENT);
    
//    Utils.getEventsForTeams(Utils.getTeamsForEvent("2017dar"), "2017dar");
//    System.out.println(Utils.getAllEventsForSingleTeam(1885));

    List<Match> matches = TBACalc.api.getEventMatches(EVENT).stream()
      .filter(m -> m.getCompLevel().equals("qm"))
      .collect(Collectors.toList());
    String[] schedule = new String[matches.size()];
    for(Match m : matches) {
      int index = m.getMatchNumber() - 1;
      String red = Arrays.toString(m.getAlliances().getRed().getTeams());
      String blue = Arrays.toString(m.getAlliances().getBlue().getTeams());
      schedule[index] = (red + " " + blue)
        .replaceAll("frc", "")
        .replaceAll(",", "")
        .replaceAll("\\[", "")
        .replaceAll("\\]", "")
//        .replaceAll("  ", " ")
        .trim();
    }
    
    for(int i = 0; i < schedule.length; i++) {
      System.out.println(i+1 + "\t" + schedule[i]);
    }
  }
}
