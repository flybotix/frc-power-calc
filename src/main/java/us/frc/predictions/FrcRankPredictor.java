package us.frc.predictions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.vegetarianbaconite.blueapi.beans.Match;

public class FrcRankPredictor {
//   TODO - Config display
//  private static final String EVENT_TO_PREDICT = "2017mrcmp";
//  private static final String EVENT_TO_PREDICT = "2017gacmp";
//  private static final String EVENT_TO_PREDICT = "2017chcmp";
//  private static final String EVENT_TO_PREDICT = "2017iscmp";
//  private static final String EVENT_TO_PREDICT = "2017nccmp";
//  private static final String EVENT_TO_PREDICT = "2017necmp";
//  private static final String EVENT_TO_PREDICT = "2017pncmp";
//  private static final String EVENT_TO_PREDICT = "2017incmp";
//  private static final String EVENT_TO_PREDICT = "2017micmp";
//  private static final String EVENT_TO_PREDICT = "2017oncmp";
  private static final String EVENT_TO_PREDICT = "2017dar";


  // TODO - Config display
  private static final double NUM_SCHEDULES_TO_PREDICT = 10000;
  private static final boolean RANDOM_SCHEDULES = false;
  
  public static void main(String... args) {
    
    for(String e : EventStats.STL) {
    
      Map<Integer, TeamStat> allEventStats = Utils.getStatsForTeamsAttending(e, true, RANDOM_SCHEDULES);
      System.out.println("Retrieved " + allEventStats.size() + " team stats for " + e);
  
      String[] schedule;
      if(RANDOM_SCHEDULES) {
        schedule = ScheduleGenerator.getScheduleForTeams(allEventStats.size(), 10);
        System.out.println("Finished retrieving randomized schedule with " + schedule.length + " matches for " + allEventStats.size() + " teams");
      } else {
        schedule = Utils.getQualScheduleForEvent(e);
        System.out.println("Finished retrieving schedule with " + schedule.length + " matches for " + e);
      }
      
      // Init the rank averages array that stores each sim's rank for a team
      Map<Integer, DescriptiveStatistics> rpAverages = new HashMap<>();
      Map<Integer, DescriptiveStatistics> rankAverages = new HashMap<>();
      for(Integer i : allEventStats.keySet()) {
        rpAverages.put(i, new DescriptiveStatistics());
        rankAverages.put(i, new DescriptiveStatistics());
      }
      
      double allsimscores = 0;
      double allsimwinscores = 0;
      double countFuelTieBreakers = 0;
      double fourRotorTrump = 0;
      double autonRotorTieCount = 0;
      double rotorRP = 0;
      double fuelRP = 0;
      double numTies = 0;
      
      // Time to predict!
      System.out.println("Performing " + NUM_SCHEDULES_TO_PREDICT + " simulations...");
      for(int i = 0; i < NUM_SCHEDULES_TO_PREDICT; i++) {
  //      System.out.println("Prediction schedule " + i);
        // Randomize known schedule by randomizing the team list and assigning teams to the index values
        // in the schedule
        List<Integer> random = new ArrayList<>(allEventStats.keySet());
        if(RANDOM_SCHEDULES) {
          Collections.shuffle(random);
        }
        SchedulePredictor sp = new SchedulePredictor(random, allEventStats, schedule, RANDOM_SCHEDULES);
        sp.calcScores();
        // TODO - JavaFX Match display
        for(SchedulePredictor.Match m : sp.getMatches()) {
          allsimscores += m.getTotalScore();
          allsimwinscores += m.getWinningScore();
          if(m.didFuelBreakTie()) {
            countFuelTieBreakers++;
          } else if(m.did4RotorTrumpFuel()) {
            fourRotorTrump++;
          } else if (m.didAutonGearMakeDifference()) {
            autonRotorTieCount++;
          }
          
          // lol, "macro"
          if(NUM_SCHEDULES_TO_PREDICT < 5) {
            System.out.println(m);
          }
        }
        
        rotorRP += sp.NUM_ROTOR_RP;
        fuelRP += sp.NUM_KPA_RP;
        numTies += sp.NUM_TIES;
        
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
  //      System.out.println("Schedule " + i + " " + ranks);
        
        // Store the ranking prediction for each team for this simulation run
        double rank = 1d;
        for(SchedulePredictor.Team t : ranks) {
          rpAverages.get(t.num).addValue(t.rankpoints);
          rankAverages.get(t.num).addValue(rank);
          rank++;
        };
      } // End simulation loop
      
      System.out.println("Finalizing averages");
      List<RankStat> ranks = new ArrayList<>();
      
      for(Integer team : rpAverages.keySet()) {
        DescriptiveStatistics ds = rankAverages.get(team);
        RankStat stat = new RankStat();
        
        stat.mTeam = team;
        stat.mRankAvg = ds.getMean();
        stat.mAvgRP = rpAverages.get(team).getMean();
        
        // Problem with 3-sigmas in 2017 - basically it says
        // "I have 99.7% confidence that all teams will rank from 1 to 58 at a 58-team event"
        // If MORE teams did fuel to make scoring more linear, then we would probably use higher sigmas
        // since teams would be less likely to TIE matches
        
        // See table at https://en.wikipedia.org/wiki/Standard_deviation for confidence intervals
        // 1.644854 = 90% confidence
        // 1.281552 = 80% confidence
        // 1 = 68.8% confidence
        double sig = ds.getStandardDeviation() * 1.281552;
        stat.mRankLow = Math.max(1d, stat.mRankAvg-sig);
        stat.mRankHigh = Math.min(allEventStats.size(), stat.mRankAvg+sig);
        ranks.add(stat);
      }
      
      System.out.println("RANK\tTEAM\tAvgRP\t100K-AVG\t80%-LO\t80%-HI");
      ranks = ranks.stream()
        .sorted((a, b) -> Double.compare(a.mAvgRP, b.mAvgRP) * -1)
        .collect(Collectors.toList());
      for(int i = 0; i < ranks.size(); i++) {
        System.out.println((i+1) + "\t" + ranks.get(i));
      }
      
      outputPct("# of rotor RP matches predicted: ",rotorRP, schedule.length);
      outputPct("# of fuel RP matches predicted: ", fuelRP, schedule.length);
      outputPct("# of Ties predicted: ", (double)numTies, schedule.length);
      outputPct("# of Matches Where Fuel broke the Tie: ", countFuelTieBreakers, schedule.length);
      outputPct("# of Matches where 4 rotors won over fuel: ", fourRotorTrump, schedule.length);
      outputPct("# of Matches where Auton Gear Made the difference: ", autonRotorTieCount, schedule.length);
      
      System.out.println("Average Alliance Score: " + nf.format(allsimscores/(double)schedule.length/NUM_SCHEDULES_TO_PREDICT/2d));
      System.out.println("Average Winning Score: " + nf.format(allsimwinscores/(double)schedule.length/NUM_SCHEDULES_TO_PREDICT));
    }

  }
  
  private static void outputPct(String message, double totalValue, int schedulelength) {
    double avgpct = totalValue/NUM_SCHEDULES_TO_PREDICT;
    System.out.println(message + 
      nf.format(avgpct) + " (" + nf.format(avgpct/(double)schedulelength*100d) + "%)");
  }
  
  private static class RankStat {
    private int mTeam;
    private double mAvgRP;
    private double mRankAvg;
    private double mRankHigh;
    private double mRankLow;
    
    public String toString() {
      return mTeam  + "\t" + nf.format(mAvgRP) + "\t" + nf.format(mRankAvg) + "\t" + nf.format(mRankLow)  + "\t" + nf.format(mRankHigh) ;
    }
  }

  static final DecimalFormat nf = new DecimalFormat("0.0");
}
