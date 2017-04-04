package us.frc.predictions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.vegetarianbaconite.blueapi.beans.Event;

public class FrcRankPredictor {
//   TODO - JavaFX Config display
//  private static final String EVENT_TO_PREDICT = "2017mrcmp";
//  private static final String EVENT_TO_PREDICT = "2017gacmp";
  private static final String EVENT_TO_PREDICT = "2017chcmp";
//  private static final String EVENT_TO_PREDICT = "2017iscmp";
//  private static final String EVENT_TO_PREDICT = "2017nccmp";
//  private static final String EVENT_TO_PREDICT = "2017necmp";
//  private static final String EVENT_TO_PREDICT = "2017pncmp";
//  private static final String EVENT_TO_PREDICT = "2017incmp";
//  private static final String EVENT_TO_PREDICT = "2017micmp";
//  private static final String EVENT_TO_PREDICT = "2017oncmp";


  // TODO - JavaFX Config display
  private static final double NUM_SCHEDULES_TO_PREDICT = 100000;
  
  public static void main(String... args) {

    System.out.println("Retrieving teams for " + EVENT_TO_PREDICT);
    List<Integer> attendingTeams = TBACalc.api.getEventTeams(EVENT_TO_PREDICT).stream()
      .map(team -> team.getTeamNumber())
      .collect(Collectors.toList());
    String[] schedule = ScheduleGenerator.getScheduleForTeams(attendingTeams.size(), 12);
    System.out.println("Finished retrieving randomized schedule with " + schedule.length + " matches");

    SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");
    Comparator<Event> eventSorter = (a, b) -> {
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
    Comparator<Event> reverseSorter = (a, b) -> eventSorter.compare(a, b) * -1;
    
    System.out.println("Retrieving events attended by teams");
    List<String> priorEvents = attendingTeams.stream()
      .map(team -> TBACalc.api.getTeamEvents(team, 2017))  // Team # to List<Event>
      .map(eventList -> // List<Event> to "Latest" event
        eventList.stream()
        // Filter future events signed up for, as well as target event in case it has happened
          .filter(e -> e.isPast() && !e.getKey().equalsIgnoreCase(EVENT_TO_PREDICT))
          .sorted(reverseSorter)
          .findFirst()
          .get()
        ) // List<Event> to "Latest" event
      .collect(Collectors.toSet()) // Stuff all results into a set to remove duplicates
      .stream() // Stream that result so we can chronologically sort and map to keys
      .sorted(eventSorter)
      .map(event -> event.getKey()) // Event --> Event Key
      .distinct()
      .collect(Collectors.toList())
      ;
    
    // If the event has already happened, then the list will contain our event to "predict"
    // This scenario happens when we're testing how close the prediction is to reality
    priorEvents.remove(EVENT_TO_PREDICT);
    System.out.println("Found " + priorEvents + " prior events to process as the teams' \"latest\" events.");

    Map<Integer, TeamStat> allEventStats = new HashMap<>();
    
    for(String event : priorEvents) {
      System.out.println("Processing " + event);
      TBACalc c = new TBACalc(event, true);
      if(c.isValid) {
        final Map<Integer, Double> opr = c.getForKey("opr");
        final Map<Integer, Double> dpr = c.getForKey("dpr");
  
        opr.keySet().parallelStream() // opr.keySet() contains a list of teams at the event
          .map(team -> new TeamStat(team, c, opr.get(team), dpr.get(team)))
          // So long as the events are in chronological order, the latest event
          // will override earlier events
          .forEach(stat -> {
            allEventStats.remove(stat.mTeam); // Theoretically shouldn't need...
            allEventStats.put(stat.mTeam, stat); 
          });
      }
    }
    
    System.out.println("Normalizing Stats from 0 to Max Value");
    SchedulePredictor.normalize(allEventStats);
    
    // Print the team stats to the terminal
    System.out.println(TeamStat.getHeader());
    allEventStats.keySet().stream()
      .filter(team -> attendingTeams.contains(team))
      .sorted()
      .map(team -> allEventStats.get(team))
      .forEach(System.out::println);
    
    // Init the rank averages array that stores each sim's rank for a team
    Map<Integer, List<Integer>> rankAverages = new HashMap<>();
    for(Integer i : attendingTeams) {
      rankAverages.put(i, new ArrayList<Integer>());
    }
    
    double allsimscores = 0;
    double allsimwinscores = 0;
    double countFuelTieBreakers = 0;
    double fourRotorTrump = 0;
    double autonRotorTieCount = 0;
    
    // Time to predict!
    System.out.println("Performing " + NUM_SCHEDULES_TO_PREDICT + " simulations...");
    for(int i = 0; i < NUM_SCHEDULES_TO_PREDICT; i++) {
//      System.out.println("Prediction schedule " + i);
      SchedulePredictor sp = new SchedulePredictor(attendingTeams, allEventStats, schedule);
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
      int rank = 1;
      for(SchedulePredictor.Team t : ranks) {
        rankAverages.get(t.num).add(rank);
        rank++;
      };
    } // End simulation loop
    
    System.out.println("Finalizing averages");
    List<RankStat> ranks = new ArrayList<>();
    
    for(Integer team : rankAverages.keySet()) {
      List<Integer> rs = rankAverages.get(team);
      RankStat stat = new RankStat();
      
      stat.mTeam = team;
      stat.mRankAvg = (double)rs.stream().reduce(Integer::sum).get() / NUM_SCHEDULES_TO_PREDICT;
      
      DescriptiveStatistics ds = new DescriptiveStatistics();
      rs.forEach(i -> ds.addValue(Double.valueOf(Integer.toString(i))));
      
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
      stat.mRankHigh = Math.min(attendingTeams.size(), stat.mRankAvg+sig);
      ranks.add(stat);
    }
    
    System.out.println("RANK\tTEAM\t100K-AVG\t80%-LO\t80%-HI");
    ranks = ranks.stream()
      .sorted((a, b) -> Double.compare(a.mRankAvg, b.mRankAvg))
      .collect(Collectors.toList());
    for(int i = 0; i < ranks.size(); i++) {
      System.out.println((i+1) + "\t" + ranks.get(i));
    }
    
    outputPct("# of rotor RP matches predicted: ",SchedulePredictor.NUM_ROTOR_RP, schedule.length);
    outputPct("# of fuel RP matches predicted: ", SchedulePredictor.NUM_KPA_RP, schedule.length);
    outputPct("# of Ties predicted: ", (double)SchedulePredictor.NUM_TIES, schedule.length);
    outputPct("# of Matches Where Fuel broke the Tie: ", countFuelTieBreakers, schedule.length);
    outputPct("# of Matches where 4 rotors won over fuel: ", fourRotorTrump, schedule.length);
    outputPct("# of Matches where Auton Gear Made the difference: ", autonRotorTieCount, schedule.length);
    
    System.out.println("Average Alliance Score: " + nf.format(allsimscores/(double)schedule.length/NUM_SCHEDULES_TO_PREDICT/2d));
    System.out.println("Average Winning Score: " + nf.format(allsimwinscores/(double)schedule.length/NUM_SCHEDULES_TO_PREDICT));

  }
  
  private static void outputPct(String message, double totalValue, int schedulelength) {
    double avgpct = totalValue/NUM_SCHEDULES_TO_PREDICT;
    System.out.println(message + 
      nf.format(avgpct) + " (" + nf.format(avgpct/(double)schedulelength*100d) + "%)");
  }
  
  private static class RankStat {
    private int mTeam;
    private double mRankAvg;
    private double mRankHigh;
    private double mRankLow;
    
    public String toString() {
      return mTeam  + "\t" + nf.format(mRankAvg) + "\t" + nf.format(mRankLow)  + "\t" + nf.format(mRankHigh);
    }
  }

  private static final DecimalFormat nf = new DecimalFormat("0.0");
}
