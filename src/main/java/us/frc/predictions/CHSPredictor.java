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


  // TODO - JavaFX Config display
  private static final int NUM_SCHEDULES_TO_PREDICT = 10000;
  
  public static void main(String... args) {
    Map<Integer, TeamStat> allEventStats = new HashMap<>();
    
    String[] schedule = ScheduleGenerator.getScheduleForTeams(12, 58);
    
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
    
    double allsimscores = 0;
    double allsimwinscores = 0;
    double countFuelTieBreakers = 0;
    
    // Time to predict!
    for(int i = 0; i < NUM_SCHEDULES_TO_PREDICT; i++) {
//      System.out.println("Prediction schedule " + i);
      SchedulePredictor sp = new SchedulePredictor(dcmpTeams, allEventStats, schedule);
      // TODO - JavaFX Match display
      for(SchedulePredictor.Match m : sp.getMatches()) {
        allsimscores += m.getTotalScore();
        allsimwinscores += m.getWinningScore();
        if(m.getMargin() < 5 && m.getMargin() > 0) {
          countFuelTieBreakers++;
        }
        
        if(NUM_SCHEDULES_TO_PREDICT < 5) {
          System.out.println(m);
        }
      }
      
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
    
    System.out.println("TEAM\t10K-AVG\t80%-LO\t80%-HI");
    for(Integer t : dcmpRanks.keySet()) {
      System.out.println(t + "\t" + nf.format(dcmpRanks.get(t)) + "\t" + nf.format(dcmpMin.get(t)) + "\t" + nf.format(dcmpMax.get(t)));
    }
    
    System.out.println("Includes gear multiplier of " + SchedulePredictor.GEAR_MULTIPLIER + " due to better pegs @ DCMP");
    System.out.println("# of rotor RP matches predicted: " + 
      nf.format((double)SchedulePredictor.NUM_ROTOR_RP/(double)NUM_SCHEDULES_TO_PREDICT));
    System.out.println("# of fuel RP matches predicted: " +
      nf.format((double)SchedulePredictor.NUM_KPA_RP / (double)NUM_SCHEDULES_TO_PREDICT));
    System.out.println("# of Ties predicted: " +
      nf.format((double)SchedulePredictor.NUM_TIES / (double)NUM_SCHEDULES_TO_PREDICT));
    System.out.println("Average Alliance Score: " + nf.format(allsimscores/(double)schedule.length/(double)NUM_SCHEDULES_TO_PREDICT/2d));
    System.out.println("Average Winning Score: " + nf.format(allsimwinscores/(double)schedule.length/(double)NUM_SCHEDULES_TO_PREDICT));
    double avgtiebreak = countFuelTieBreakers/(double)NUM_SCHEDULES_TO_PREDICT;
    System.out.println("# of Matches Where Fuel broke the Tie: " + 
      nf.format(avgtiebreak) + " (" + nf.format(avgtiebreak/(double)schedule.length*100d) + "%)");
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
          if(v < 0.33d) { 
            v = 0d;
          }
        }
        allEventStats.get(team).override(stat, v);
      }
      
    }
    
  }

  private static final DecimalFormat nf = new DecimalFormat("0.0");
}
