package us.frc.predictions;

import static us.frc.predictions.Breakdown2017.rotor1Auto;
import static us.frc.predictions.Breakdown2017.rotor2Auto;
import static us.frc.predictions.Breakdown2017.rotor2Engaged;
import static us.frc.predictions.Breakdown2017.rotor3Engaged;
import static us.frc.predictions.Breakdown2017.rotor4Engaged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class SchedulePredictor {
  // Teams & field elements get better as time goes on.  How much better?
  static double GEAR_MULTIPLIER = 1.15;
  static double FUEL_MULTIPLIER = 1.1;
  
  
  private final Map<Integer, Team> mTeams = new HashMap<>();
  private final List<Match> mMatches = new ArrayList<>();
  
  public SchedulePredictor(List<Integer> pTeams, Map<Integer, TeamStat> pStats, String[] pSchedule) {
    // Randomize known schedule by randomizing the team list and assigning teams to the index values
    // in the schedule
    List<Integer> random = new ArrayList<>(pTeams);
    Collections.shuffle(random);
    random.forEach(team -> mTeams.put(team, new Team(team, pStats.get(team))));
    
    // Assign schedule & calculate score
    for(int i = 0; i < pSchedule.length; i++) {  // 2 empty lines at end of some generated outputs
      if(pSchedule[i] != null && pSchedule[i].length() > 0) {
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
  }
  public static EnumSet<Breakdown2017> CappedOPR = EnumSet.of(
    rotor1Auto, rotor2Auto,
    rotor2Engaged, rotor3Engaged
  );
  
  /**
   * Clamps, normalizes, and otherwise cleans up the stats so they make sense for 'added value'.
   */
  static void normalize(Map<Integer, TeamStat> allEventStats) {
    for(Integer team : allEventStats.keySet()) {
      for(Breakdown2017 stat : TeamStat.CalculatedValues) {
        // Max is the static value / 3.  As more teams do a task, 
        // OPR of that task -> [Value] / 3
        // Only applicable to cooperative tasks that have non-linear values
        double mult = CappedOPR.contains(stat) ? 1d/3d : 1;
        Double v = Math.min(stat.getStaticValue() * mult, Math.max(0d, allEventStats.get(team).get(stat)));
        
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
      int r = (int)Math.round(red.calcScore());
      int b = (int)Math.round(blue.calcScore());
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
    
    public int getTotalScore() {
      return red.mScore + blue.mScore;
    }
    
    public int getWinningScore() {
      return red.mScore > blue.mScore ? red.mScore : blue.mScore;
    }
    
    public boolean didAutonGearMakeDifference() {
      boolean result = false;
      if(red.mScore > blue.mScore && red.mAutonGear) {
        result = red.mScore - Breakdown2017.rotor1Auto.getStaticValue() <= blue.mScore;
      } else if (blue.mScore > red.mScore && blue.mAutonGear) {
        result = blue.mScore - Breakdown2017.rotor1Auto.getStaticValue() <= red.mScore;
      }
      return result;
    }
    
    public boolean didFuelBreakTie() {
      boolean result = false;
      if(!red.mScore.equals(blue.mScore)) {
        // It only broke the tie if one scored fuel and the other didn't
        if(red.mFuelScore == 0 && red.mScore < blue.mScore && blue.mFuelScore >= 1 && blue.mScore - blue.mFuelScore == red.mScore) {
          result = true;
        } else if (blue.mFuelScore == 0 && blue.mScore < red.mScore && red.mFuelScore >= 1 && red.mScore - red.mFuelScore == blue.mScore) {
          result = true;
        }
      }
      return result;
    }
    
    public boolean did4RotorTrumpFuel() {
      boolean result = false;
      if(!red.mScore.equals(blue.mScore)) {
        if(red.mRotorRP > 0 && blue.mFuelScore > 0 && red.mScore > blue.mScore) {
          result = true;
        } else if (blue.mRotorRP > 0 && red.mFuelScore > 0 && blue.mScore > red.mScore) {
          result = true;
        }
      }
      return result;
    }
    
    public int getMargin() {
      return Math.abs(red.mScore - blue.mScore);
    }
    
    public String toString() {
      return num + ": RED" + red.toString() + "\tBLUE" + blue.toString();
    }
  }
  
  static int NUM_KPA_RP = 0;
  static int NUM_ROTOR_RP = 0;
  static int NUM_TIES = 0;
  
  private class Alliance {
    List<Team> teams = new ArrayList<>();
    private Integer mScore = 0;
    private Integer mRotorRP = 0;
    private Integer mFuelRP = 0;
    private boolean mAutonGear = false;
    int mFuelScore = 0;
    
    public Alliance(Integer... pTeams) {
      for(Integer t : pTeams) {
        teams.add(mTeams.get(t));
      }
    }
    
    public double calcScore() {
      // Free Gear
      double value = 40d;
      double fuel = 0d;

      
      for(Breakdown2017 stat : TeamStat.CalculatedValues) {
        switch(stat) {
        case autoMobilityPoints:
          int num = (int)(Math.round(teams.stream().map(t -> t.stats.get(stat)).reduce(Double::sum).get())/5d);
          value += num * stat.getStaticValue();
          break;
        
        // Individual linear values are already determined in OPR
        case autoFuelPoints: 
        case teleopFuelPoints: 
          // We assume we've (mostly) filtered out non-fuel teams here.
          // Seems to be inline with what 1262, 346, & 836 did in their last few matches
          fuel += teams.stream().map(t -> t.stats.get(stat)).reduce(Double::sum).get()*FUEL_MULTIPLIER;
          break;
          
        // Alliance-cooperative static scores
        case rotor1Auto: 
        case rotor2Auto:
          // Max probability or value of alliance
          // OPR-capped, so mult by 3
          double p_Arotor = teams.stream()
            .map(t -> t.stats.get(stat)*3 / stat.getStaticValue() * GEAR_MULTIPLIER) // divide OPR by max to get probability
            .reduce(Double::max).get(); // max engagement for the alliance, since it's individual effort
          mAutonGear |= !(Math.random() > p_Arotor);
          value += mAutonGear ? 0 : stat.getStaticValue();
          break;
          
        case rotor2Engaged:
        case rotor3Engaged:
          // Max probability or value of alliance
          // OPR-capped, so mult by 3
          double p_Trotor = teams.stream()
            .map(t -> t.stats.get(stat)*3 / stat.getStaticValue() * GEAR_MULTIPLIER) // divide OPR by max to get probability
            .reduce(Double::max).get(); // max engagement for the alliance, since it's individual effort
          value += Math.random() > p_Trotor ? 0 : stat.getStaticValue();
          break;
          
        case teleopTakeoffPoints:
          value += teams.stream()
            .map(t -> t.stats.get(stat) / stat.getStaticValue()) // OPR Value - calculate individual probability
            .map(p_takeoff -> Math.random() > p_takeoff ? 0 : stat.getStaticValue()) // Calculate value based upon yes/no
            .reduce(Double::sum).get(); // Max individual values
          break;
          

        // The parsing of TBA set these as the bonus values, not true/false.  So
        // the probability of achievement is based upon opr value, not true/false.
//        case rotorRankingPointAchieved:
        case rotor4Engaged:
          // If probability of 3 rotors is so significant between all three partners, there is a
          // very high likelihood they will also be able to hit 4 rotors
          // Shown in NC state & NE-Hartford
          double p_3 = teams.stream()
            .map(t -> t.stats.get(Breakdown2017.rotor3Engaged)*3 / stat.getStaticValue()) // divide OPR by max to get probability
            .reduce(Double::sum).get();
          Double p_4rotor = 1d;
          if(p_3 >= Breakdown2017.rotor3Engaged.getStaticValue()) {
            p_4rotor = teams.stream()
              .map(t -> t.stats.get(stat) / stat.getStaticValue()) // divide OPR by max to get probability
              .reduce(Double::sum).get() * 3; // max engagement for the alliance
          } else {
            p_4rotor = teams.stream()
             .map(t -> t.stats.get(stat) / stat.getStaticValue()) // divide OPR by max to get probability
             .reduce(Double::max).get() * GEAR_MULTIPLIER; // max engagement for the alliance
          }
          mRotorRP = Math.random() > p_4rotor ? 0 : 1;
          value += mRotorRP * stat.getStaticValue();
          
        case kPaRankingPointAchieved:
          Double p_kpaBonus = teams.stream()
            .map(t -> t.stats.get(stat) / stat.getStaticValue()*3*FUEL_MULTIPLIER)
            .reduce(Double::sum).get()*FUEL_MULTIPLIER;
          mFuelRP = Math.random() >= p_kpaBonus ? 0 : 1;
          break;
        default:
        }
      }
      
      if(mFuelRP > 0) {
        mFuelScore = (int)Math.max(40d,  mFuelScore);
      } else {
        if(fuel < 1d) {
          mFuelScore = Math.random() > fuel ? 1 : 0;
        } else {
          mFuelScore = (int)Math.floor(fuel);
        }
      }

      value += mFuelScore;
      
      mScore = (int)value;
      return mScore;
    }
    
    private void end() {
      String out = "";
      teams.forEach(team -> team.matchpoints += mScore);
      teams.forEach(team -> team.rankpoints += mRotorRP);
      teams.forEach(team -> team.rankpoints += mFuelRP);
      
      if(!out.equalsIgnoreCase("")) {
//        System.out.println(out + this.toString());
      } 
      
      NUM_KPA_RP += mFuelRP;
      NUM_ROTOR_RP += mRotorRP;
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
