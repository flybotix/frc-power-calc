package us.frc.predictions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vegetarianbaconite.blueapi.beans.Event;

public class Utils {
  
  public static Map<Integer, TeamStat> getLiveStatsForTeamsAttending(boolean pPrint, String...pEvents) {
    // don't roll up with live data - only historical
    return getStatsForSpecificEvents(false, pPrint, pEvents);
  }
  
  public static Map<Integer, TeamStat> getStatsForSpecificEvents(boolean pRollUp, boolean pPrint, String... pEvents) {    
    List<Integer> attendingTeams = new ArrayList<>();
  for(String event : pEvents) {
    attendingTeams.addAll(Utils.getTeamsForEvent(event));
  }
  Map<Integer, TeamStat> allEventStats = Utils.generateTeamStats(new LinkedList<>(Arrays.asList(pEvents)));
  SchedulePredictor.normalize(allEventStats, pRollUp); 
  if(pPrint) {
    print(attendingTeams, allEventStats);
  }
  return allEventStats;
    
  }
  
  public static void print(List<Integer> pTeams, Map<Integer, TeamStat> pStats) {

    // Print the team stats to the terminal
    System.out.println(TeamStat.getHeader());
    pStats.keySet().stream()
      .filter(team -> pTeams.contains(team))
      .sorted()
      .map(team -> pStats.get(team))
      .forEach(System.out::println);
  }
  
  public static Map<Integer, TeamStat> getStatsForTeamsAttendingMulti(boolean pRollupRotors, boolean pPrint, String... pEvents) {
    
    List<Integer> attendingTeams = new ArrayList<>();
    for(String event : pEvents) {
      attendingTeams.addAll(Utils.getTeamsForEvent(event));
    }
    Map<Integer, TeamStat> results = new HashMap<>();
    
    List<Integer> teamsToRecalculate  = new ArrayList<>();
    attendingTeams.stream().forEach(team -> {
      TeamStat ts = Utils.retrieveLatestStatsFromCache(team, "2017");
      if(ts == null) {
        teamsToRecalculate.add(team);
      } else {
        results.put(ts.mTeam, ts);
      }
    });
    
    if(teamsToRecalculate.isEmpty() == false) {
      LinkedList<String> priorEvents = Utils.getEventsForTeams(teamsToRecalculate, pEvents);
      System.out.println("Found " + priorEvents + " prior events to process as the teams' \"latest\" events.");
      Map<Integer, TeamStat> allEventStats = Utils.generateTeamStats(priorEvents);
      // tmp hack to see about lvls of improvement
      if(allEventStats.containsKey(1885)) {
        allEventStats.get(1885).mod(Breakdown2017.rotor4Engaged, 2d);
      }
  
      List<Integer> toRemove = new ArrayList<>();
      for(Integer team : allEventStats.keySet()) {
        if(!attendingTeams.contains(team)) {
          toRemove.add(team);
        }
      }
      for(Integer team : toRemove) {
        allEventStats.remove(team);
      }
      
      System.out.println("Normalizing Stats from 0 to Max Value");
      SchedulePredictor.normalize(allEventStats, pRollupRotors);
      
      writeTeamStatsToCache(allEventStats);
      
      results.putAll(allEventStats);
    }
    
    if(pPrint) {
      print(attendingTeams, results);
    }
    
    
    
    return results;
    
  }
  
  public static Map<Integer, TeamStat> getStatsForTeamsAttending(String pEvent, boolean pRollupRotors, boolean pPrint) {
    return getStatsForTeamsAttendingMulti(pRollupRotors, pPrint, pEvent);
  }
  
  public static Map<Integer, TeamStat> generateTeamStats(LinkedList<String> pPriorEvents) {

    Map<Integer, TeamStat> results = new HashMap<>();
    
    // Don't make this parallel - we made a big assumption that the incoming list is in the proper order
    pPriorEvents.stream().forEach(event -> {
      results.putAll(retrieveStatsForEvent(event));
    });
    return results;
  }
  
  private static final Map<String, Map<Integer, TeamStat>> CACHE = new HashMap<>();
  
  // For events like einstein and micmp which are just playoff tournaments
  private static final String[] sINVALID_EVENTS = new String[]{
    "2017micmp",
    "2017ctss" // Week 0 scrimmage
  };
  
  public static Map<Integer, TeamStat> retrieveStatsForEvent(String pEvent) {
    Map<Integer, TeamStat> result = CACHE.get(pEvent);
    if(result == null ) {
      result = new HashMap<>();
      System.out.println("Processing " + pEvent);
      TBACalc c = new TBACalc(pEvent, true);
      if(c.isValid) {
        final Map<Integer, Double> opr = c.getForKey("opr");
        final Map<Integer, Double> dpr = c.getForKey("dpr");
  
        List<TeamStat> stats = opr.keySet().stream() // opr.keySet() contains a list of teams at the event
          .map(team -> new TeamStat(team, c, opr.get(team), dpr.get(team)))
          // So long as the events are in chronological order, the latest event
          // will override earlier events
          .collect(Collectors.toList());
        for(TeamStat stat : stats) {
          result.put(stat.mTeam, stat);
        }
      }
      CACHE.put(pEvent, result);
    }
    return result;
  }

  private static final String TEAM_STAT_PATH = "data" + File.separator + "teamstats";
  private static final String TEAM_STAT_FILE_EXT = ".txt";
  private static File getFileForTeamAndYear(Integer pTeam, String pYear) {
    String dir = TEAM_STAT_PATH + File.separator + Integer.toString(pTeam);
    File f = new File(dir, pYear + TEAM_STAT_FILE_EXT);
    return f;
  }
  
  public static TeamStat retrieveLatestStatsFromCache(Integer pTeam, String pYear) {
    File f = getFileForTeamAndYear(pTeam, pYear);
    if(f.exists()) {
      try {
        String values = new String(Files.readAllBytes(f.toPath()));
        return TeamStat.fromString(values);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
  
  private static void writeTeamStatsToCache(Map<Integer, TeamStat> pStats) {
    for(Integer team : pStats.keySet()) {
      File f = getFileForTeamAndYear(team, "2017");
      f.getParentFile().mkdirs();
      try {
        Files.write(f.toPath(), pStats.get(team).toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static List<Integer> getTeamsForEvent(String pEvent) {
    System.out.println("Retrieving teams for " + pEvent);
    return TBACalc.api.getEventTeams(pEvent).stream()
      .map(team -> team.getTeamNumber())
      .collect(Collectors.toList());
  }
  
  public static LinkedList<String> getEventsForTeams(List<Integer> pTeams, String... pCurrentEvents) {
    List<String> eventsToFilter = new ArrayList<>();
    eventsToFilter.addAll(Arrays.asList(pCurrentEvents));
    eventsToFilter.addAll(Arrays.asList(sINVALID_EVENTS));
    System.out.println("Retrieving events attended by teams " + pTeams);
    System.out.println("Excluding events " + eventsToFilter);
//    List<String> search = Arrays.asList(pCurrentEvents);
    LinkedList<String> priorEvents = new LinkedList<>(pTeams.stream()
      .map(team -> TBACalc.api.getTeamEvents(team, 2017))  // Team # to List<Event>
      .map(rawEvents -> rawEvents.stream()
        .filter(re -> !eventsToFilter.contains(re.getKey()))
        .collect(Collectors.toList()))
      .map(eventList -> // List<Event> to "Latest" event
          eventList.stream() // Ahhhhh many-to-many non-distinct Ahhhhhh
          // Filter future events signed up for, as well as target event in case it has happened
//            .filter(e -> e.isPast())
            .sorted(Utils.reverseSorter)
            .findFirst()
            .get())   // List<Event> to "Latest" event
      .collect(Collectors.toList()) // Stuff all results into a set to remove duplicates
      .stream() // Stream that result so we can chronologically sort and map to keys
      .sorted(Utils.eventSorter)
      .map(event -> event.getKey()) // Event --> Event Key (String)
      .distinct()
      .collect(Collectors.toList()))
      ;
    // If the event has already happened, then the list will contain our event to "predict"
    // This scenario happens when we're testing how close the prediction is to reality
//    for(String e : pCurrentEvents) {
//      priorEvents.remove(e);
//    }
    return priorEvents;
  }
  
  public static List<String> getAllEventsForSingleTeam(Integer pTeam) {
    return TBACalc.api.getTeamEvents(pTeam, 2017).stream()
      .sorted(reverseSorter)
      .map(event -> event.getKey())
      .collect(Collectors.toList());
  }
  

  public static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
  public static final Comparator<Event> eventSorter = (a, b) -> {
    int result = 0;
    try {
      Date ad = df.parse(a.getDate());
      Date bd = df.parse(b.getDate());
      result = ad.compareTo(bd);
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    return result;
  };
  public static final Comparator<Event> reverseSorter = (a, b) -> eventSorter.compare(a, b) * -1;
}
