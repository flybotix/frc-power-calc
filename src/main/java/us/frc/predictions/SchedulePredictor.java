package us.frc.predictions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class SchedulePredictor {
  private final Map<Integer, Team> mTeams = new HashMap<>();
  private final List<Match> mMatches = new ArrayList<>();
  
  public SchedulePredictor(List<Integer> pTeams, Map<Integer, TeamStat> pStats, String[] pSchedule) {
    // Randomize known schedule by randomizing the team list and assigning teams to the index values
    // in the schedule
    List<Integer> random = new ArrayList<>(pTeams);
    Collections.shuffle(random);
    random.forEach(team -> mTeams.put(team, new Team(team, pStats.get(team))));
    
    // Assign schedule & calculate score
    for(int i = 0; i < pSchedule.length; i++) {
      StringTokenizer st = new StringTokenizer(pSchedule[i], " ");
      Alliance red = new Alliance(
        random.get(Integer.parseInt(st.nextToken())-1), 
        random.get(Integer.parseInt(st.nextToken())-1), 
        random.get(Integer.parseInt(st.nextToken())-1));
      Alliance blue = new Alliance(
        random.get(Integer.parseInt(st.nextToken())-1), 
        random.get(Integer.parseInt(st.nextToken())-1), 
        random.get(Integer.parseInt(st.nextToken())-1));
      Match match = new Match(i+1, red, blue);
      match.calcScore();
      mMatches.add(match);
    }
  }
  
  public List<Match> getMatches() {
    Collections.sort(mMatches, (a, b)-> a.num.compareTo(b.num));
    return mMatches;
  }
  
  public Map<Integer, Team> getTeams() {
    return mTeams;
  }
  
  public class Match {
    Integer num;
    Alliance red;
    Alliance blue;
    
    public Match(Integer _num, Alliance _red, Alliance _blue) {
      num = _num;
      red = _red;
      blue = _blue;
    }
    
    public void calcScore() {
      Integer r = red.calcScore();
      Integer b = blue.calcScore();
      if(r > b) {
        red.win();
        blue.lose();
      } else if (b > r) {
        blue.win();
        red.lose();
      } else {
        NUM_TIES++;
        red.tie();
        blue.tie();
      }
    }
    
    public String toString() {
      return num + ": RED" + red.toString() + "\tBLUE" + blue.toString();
    }
  }
  static int NUM_ROTOR_RP = 0;
  static int NUM_TIES = 0;
  
  private class Alliance {
    List<Team> teams = new ArrayList<>();
    private Integer mScore = 0;
    private Integer mRotorRP = 0;
    private Integer mFuelRP = 0;
    
    public Alliance(Integer... pTeams) {
      for(Integer t : pTeams) {
        teams.add(mTeams.get(t));
      }
    }
    
    public Integer calcScore() {
      // Auton move & Free Gear
      double value = 55d;
      
      for(Breakdown2017 stat : TeamStat.CalculatedValues) {
        switch(stat) {
        
        // Individual linear values are already determined in OPR
        case autoFuelPoints: 
        case teleopFuelPoints: 
          value += teams.stream().map(t -> t.stats.get(stat)).reduce(Double::sum).get();
          break;
          
        // Alliance-cooperative static scores
        case rotor1Auto: 
        case rotor2Auto:
        case rotor2Engaged:
        case rotor3Engaged:
          // Max probability or value of alliance
          double p_rotor = teams.stream()
            .map(t -> t.stats.get(stat) / (stat.getStaticValue()/3)) // divide OPR by max to get probability
            .reduce(Double::max).get(); // max engagement for the alliance
          value += Math.random() > p_rotor ? 0 : stat.getStaticValue();
          break;
          
        case teleopTakeoffPoints:
          value += teams.stream()
            .map(t -> t.stats.get(stat) / stat.getStaticValue()) // OPR Value - calculate individual probability
            .map(p_takeoff -> Math.random() > p_takeoff ? 0 : stat.getStaticValue()) // Calculate value based upon yes/no
            .reduce(Double::sum).get(); // Sum individual values
          break;
          

        // The parsing of TBA set these as the bonus values, not true/false.  So
        // the probability of achievement is based upon opr value, not true/false.
//        case rotorRankingPointAchieved:
        case rotor4Engaged:
          // For this we SUM the probabilities to account for each team's leftover contributions
          // Ok, it really is to slightly bias towards 4-rotor bonuses that will be more prevelant
          Double p_4rotor = teams.stream()
            .map(t -> t.stats.get(stat) / stat.getStaticValue()) // divide OPR by max to get probability
            .reduce(Double::sum).get(); // max engagement for the alliance
          mRotorRP = Math.random() > p_4rotor ? 0 : 1;
          value += mRotorRP * stat.getStaticValue();
          
        case kPaRankingPointAchieved:
          Double kpa = teams.stream().map(t -> t.stats.get(stat)).reduce(Double::sum).get();
          mFuelRP = kpa > 40d ? 1 : 0;
          break;
        default:
        }
      }

      mScore = (int)value;
      return mScore;
    }
    
    private void end() {
      String out = "";
      teams.forEach(team -> team.matchpoints += mScore);
      if(mRotorRP > 0){
        out += "ROTOR RP ";
        NUM_ROTOR_RP++;
      }
      teams.forEach(team -> team.rankpoints += mRotorRP);
      if(mFuelRP > 0) out += "FUEL RP ";
      teams.forEach(team -> team.rankpoints += mFuelRP);
      if(!out.equalsIgnoreCase("")) {
//        System.out.println(out + this.toString());
      } 
      teams.forEach(team -> team.numMatches++);
    }
    
    
    public void win() {
      teams.forEach(team -> team.rankpoints += 2);
      end();
    }
    
    public void tie() {
      end();
      teams.forEach(team -> team.rankpoints += 1);
    }
    
    public void lose() {
      end();
    }
    
    public String toString() {
      String result = "[";
      for(Team t : teams) {
        result += t.num;
        result += " ";
      }
      return result + "] (" + mScore + ")";
    }
  }
  
  public class Team {
    public final Integer num;
    final TeamStat stats;
    public double rankpoints = 0;
    public double matchpoints = 0;
    public double numMatches = 0;
    
    public String toString() {
      return num + " " + rankpoints + " " + matchpoints;
    }
    
    Team(Integer pNum, TeamStat stat) {
      num = pNum;
      stats = stat;
    }
  }
}
