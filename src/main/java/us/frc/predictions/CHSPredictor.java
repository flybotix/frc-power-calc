package us.frc.predictions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class CHSPredictor {
  // TODO - JavaFX Config display
  private static final String[] PRIOR_EVENTS = new String[] {
    "2017vabla", "2017vahay", 
    "2017mdbet", 
    "2017mdowi", "2017vapor",
    "2017vagle", "2017mdedg"
  };

  // TODO - JavaFX Config display
  private static final String EVENT_TO_PREDICT = "2017chcmp";

  /*
   * NOTE NOTE NOTE NOTE NOTE NOTE NOTE
   * 
   * SCHEDULE.length must equal the # of teams attending the event,
   * as retrieved by TBA! If this isn't the case, then re-generate
   * a schedule for N teams.
   * 
   * Ideally we'd have pre-calculated schedules for 24 - 100 teams in
   * a file somewhere, but that would take about 10 hours to generate
   * using MatchMaker.exe and then another few hours to get into a 
   * parse-able format.
   * 
   * TODO - automate this
   * Run MatchMaker.exe -t 58 -r 12 -b -s -u 3 > Schedule.txt
   *  - Replace '58' with # of teams
   *  - Edit "Schedule.txt"
   *  -- Delete all occurances of ' 0'
   *  -- Add in quotes & commas (find/replace in Sublime Text makes this easy)
   *  -- Copy result here
   */
  private static final String[] SCHEDULE = new String[]{
    "51 39 38 5 50 37",
    "34 28 53 10 12 22",
    "6 19 35 40 26 47",
    "18 20 58 55 4 54",
    "46 15 13 32 57 45",
    "43 9 21 16 56 48",
    "31 30 36 25 42 24",
    "17 7 1 52 33 11",
    "49 27 29 14 3 44",
    " 23 41 55 5 8 2",
    "53 4 38 43 26 46",
    "37 57 15 22 34 9",
    "56 25 52 28 33 48",
    "20 10 7 17 21 49",
    "12 51 8 16 31 19",
    "35 47 50 58 30 44",
    "39 40 18 14 23 45",
    "13 11 41 3 1 42",
    "36 32 2 54 27 6",
    "24 20 4 29 19 51",
    "35 31 7 56 5 22",
    "49 38 30 55 12 28",
    "9 3 23 52 34 26",
    "48 36 13 39 10 47",
    "50 21 58 45 11 8",
    "27 15 17 54 43 40",
    "33 6 42 18 44 57",
    "1 32 16 24 53 29",
    "46 2 41 37 14 25",
    "30 39 3 19 8 20",
    "47 23 51 27 11 56",
    "15 42 22 38 58 33",
    "29 55 21 6 57 28",
    "46 24 5 17 18 10",
    "12 50 14 43 25 32",
    "44 35 34 54 31 13",
    "52 2 37 40 4 16",
    "48 41 45 7 26 53",
    "58 9 1 46 36 49",
    "20 14 27 28 22 11",
    "21 6 34 25 50 17",
    "23 54 32 42 16 55",
    "3 18 2 15 47 31",
    "33 5 12 26 4 29",
    "52 45 49 35 43 41",
    "13 53 19 36 56 37",
    "9 7 51 1 44 39",
    "8 48 24 57 40 38",
    "10 5 3 30 28 54",
    "11 49 26 16 25 18",
    "19 17 43 55 22 13",
    "21 47 52 12 37 53",
    "36 45 1 44 2 48",
    "41 8 39 4 27 33",
    "23 50 10 29 35 57",
    "56 24 40 42 34 58",
    "15 51 30 7 46 32",
    "6 38 20 14 31 9",
    "26 5 28 45 13 43",
    "12 54 47 8 23 33",
    "50 48 27 19 37 18",
    "11 17 36 41 22 57",
    "1 53 31 6 40 30",
    "49 9 4 32 35 24",
    "38 46 14 34 56 3",
    "25 20 39 7 15 55",
    "29 52 42 51 21 2",
    "44 16 27 10 58 41",
    "22 43 4 30 1 23",
    "35 3 46 48 6 12",
    "32 39 26 31 11 34",
    "21 18 45 38 29 36",
    "47 57 5 49 25 15",
    "14 53 42 9 17 8",
    "2 28 19 7 16 58",
    "13 24 52 20 54 50",
    "55 37 40 44 10 56",
    "33 45 30 34 48 51",
    "47 18 22 41 32 21",
    "53 3 57 2 58 39",
    "4 7 25 13 38 27",
    "26 42 37 1 55 20",
    "33 16 14 5 36 35",
    "50 51 56 10 49 54",
    "43 29 11 40 9 46",
    "8 44 6 15 52 19",
    "28 24 17 31 12 23",
    "5 34 16 27 1 47",
    "54 7 14 57 21 4",
    "22 36 40 3 55 50",
    "25 13 58 51 52 18",
    "45 19 12 42 10 9",
    "31 44 24 53 46 11",
    "26 30 56 17 39 29",
    "41 49 6 38 48 23",
    "8 15 32 28 37 35",
    "2 43 10 20 33 34",
    "27 55 31 9 52 53",
    "45 54 25 19 1 14",
    "11 58 48 4 6 5",
    "22 17 38 47 29 44",
    "56 41 12 50 2 15",
    "37 33 21 24 49 3",
    "23 46 16 30 13 20",
    "40 8 28 51 36 26",
    "18 42 35 39 43 7",
    "57 19 10 32 38 52",
    "58 29 54 37 41 31",
    "11 30 9 2 13 33",
    "48 26 55 24 14 15",
    "42 28 50 4 47 46",
    "32 40 44 21 20 5",
    "3 51 25 53 35 17",
    "34 23 43 36 8 7",
    "57 56 1 18 27 12",
    "16 22 49 39 45 6"
  };

  // TODO - JavaFX Config display
  private static final int NUM_SCHEDULES_TO_PREDICT = 10000;
  
  public static void main(String... args) {
    Map<Integer, TeamStat> allEventStats = new HashMap<>();
    
    for(String event : PRIOR_EVENTS) {
      System.out.println("Processing " + event);
      TBACalc c = new TBACalc(event, true);
      final Map<Integer, Double> opr = c.getForKey("opr");
      final Map<Integer, Double> dpr = c.getForKey("dpr");

      opr.keySet().parallelStream()
        .map(team -> new TeamStat(team, c, opr.get(team), dpr.get(team)))
        // So long as the events are in chronological order, the latest event
        // will override earlier events
        .forEach(stat -> {
          allEventStats.remove(stat.mTeam);
          allEventStats.put(stat.mTeam, stat); 
        });
    }

    System.out.println("Normalizing Stats from 0 to Max Value");
    normalize(allEventStats);
    
    // Print the normalized team stats to the terminal
    System.out.println(TeamStat.getHeader());
    allEventStats.keySet().stream()
      .sorted()
      .map(team -> allEventStats.get(team))
      .forEach(System.out::println);
    

    System.out.println("Retrieving teams for " + EVENT_TO_PREDICT);
    List<Integer> dcmpTeams = TBACalc.api.getEventTeams(EVENT_TO_PREDICT).stream()
      .map(team -> team.getTeamNumber())
      .collect(Collectors.toList());
    
    // Init the rank averages array that stores each sim's rank for a team
    Map<Integer, List<Integer>> rankAverages = new HashMap<>();
    for(Integer i : dcmpTeams) {
      rankAverages.put(i, new ArrayList<Integer>());
    }
    
    // Time to predict!
    for(int i = 0; i < NUM_SCHEDULES_TO_PREDICT; i++) {
//      System.out.println("Prediction schedule " + i);
      SchedulePredictor sp = new SchedulePredictor(dcmpTeams, allEventStats, SCHEDULE);
      // TODO - JavaFX Match display
//      for(SchedulePredictor.Match m : sp.getMatches()) {
//        System.out.println(m);
//      }
      
//      System.out.println("Ranking schedule " + i);
      // Predictions are complete - sort by ranking
      List<SchedulePredictor.Team> ranks = new ArrayList<>(sp.getTeams().values());
      Collections.sort(ranks, (a, b) -> {
        // This changes year-to-year
        if(Double.compare(a.rankpoints/a.numMatches, b.rankpoints/b.numMatches) == 0) {
          return Double.compare(a.matchpoints, b.matchpoints);
        } else {
          return Double.compare(a.rankpoints/a.numMatches, b.rankpoints/b.numMatches);
        }
      });
      Collections.reverse(ranks);
      System.out.println("Schedule " + i + " " + ranks);
      
      // Store the ranking prediction for each team for this simulation run
      int rank = 1;
      for(SchedulePredictor.Team t : ranks) {
        rankAverages.get(t.num).add(rank);
        rank++;
      };
    } // End simulation loop
    
    System.out.println("Finalizing averages");
    // Average all ranks for all teams
    // Probably a better way? Just hacked in with lots of maps for now.
    Map<Integer, Double> dcmpRanks = new HashMap<>();
    Map<Integer, Double> dcmpMin = new HashMap<>();
    Map<Integer, Double> dcmpMax = new HashMap<>();
    for(Integer team : rankAverages.keySet()) {
      List<Integer> rs = rankAverages.get(team);
      dcmpRanks.put(team, (double)rs.stream().reduce(Integer::sum).get() / ((double)NUM_SCHEDULES_TO_PREDICT));
      DescriptiveStatistics ds = new DescriptiveStatistics();
      rs.forEach(i -> ds.addValue(Double.valueOf(Integer.toString(i))));
      
      // Problem with 3-sigmas in 2017 - basically it says
      // "I have 99.7% confidence that all teams will rank from 1 to 58 at a 58-team event"
      // If MORE teams did fuel to make scoring more linear, then we would probably use higher sigmas
      // since teams would be less likely to TIE matches
      
      // See table at https://en.wikipedia.org/wiki/Standard_deviation for confidence intervals
      // 1.644854 = 90% confidence
      // 1.281552 = 80% confidence
      double sig = ds.getStandardDeviation() * 1.281552d;
      dcmpMin.put(team, Math.max(1d, dcmpRanks.get(team)-sig));
      dcmpMax.put(team, Math.min(dcmpTeams.size(), dcmpRanks.get(team)+sig));
    }
    
    System.out.println("TEAM\t10K-AVG\t80%CONF-LO\t80%CONF-HI");
    for(Integer t : dcmpRanks.keySet()) {
      System.out.println(t + "\t" + nf.format(dcmpRanks.get(t)) + "\t" + nf.format(dcmpMin.get(t)) + "\t" + nf.format(dcmpMax.get(t)));
    }
    
    System.out.println("# of rotor RP matches predicted: " + 
      nf.format((double)SchedulePredictor.NUM_ROTOR_RP/(double)NUM_SCHEDULES_TO_PREDICT));
    System.out.println("# of Ties predicted: " +
      nf.format((double)SchedulePredictor.NUM_TIES / (double)NUM_SCHEDULES_TO_PREDICT));
  }
  
  /**
   * Clamps, normalizes, and otherwise cleans up the stats so they make sense for 'added value'.
   */
  private static void normalize(Map<Integer, TeamStat> allEventStats) {
    for(Integer team : allEventStats.keySet()) {
      for(Breakdown2017 stat : TeamStat.CalculatedValues) {
        Double v = Math.min(stat.getStaticValue(), Math.max(0d, allEventStats.get(team).get(stat)));
        
        // Hard to tell exactly which teams score "1 point every other match" vs a team that gets lucky.
        // This is important for 2017, because it is the tie-breaker in a lot of matches.
        if(stat == Breakdown2017.autoFuelPoints || stat == Breakdown2017.teleopFuelPoints) {
       // 0.3333 OPR = 1 point each match.  Thus less than 1 point each match: probably didn't do fuel
          if(v < 0.3d) { 
            v = 0d;
          }
        }
        allEventStats.get(team).override(stat, v);
      }
      
    }
    
  }

  private static final DecimalFormat nf = new DecimalFormat("0.0");
}
