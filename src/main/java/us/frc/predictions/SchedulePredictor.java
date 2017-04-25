package us.frc.predictions;

import static us.frc.predictions.Breakdown2017.rotor1Auto;
import static us.frc.predictions.Breakdown2017.rotor2Auto;
import static us.frc.predictions.Breakdown2017.autoRotorPoints;
import static us.frc.predictions.Breakdown2017.rotor2Engaged;
import static us.frc.predictions.Breakdown2017.rotor3Engaged;
import static us.frc.predictions.Breakdown2017.rotor4Engaged;
import static us.frc.predictions.Breakdown2017.teleopTakeoffPoints;
import static us.frc.predictions.Breakdown2017.autoMobilityPoints;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class SchedulePredictor {
  private final Map<Integer, Team> mTeams = new HashMap<>();
  private final List<Match> mMatches = new ArrayList<>();
  public static final double GEAR_STD_DEV = 1.0d;
  public static final double FUEL_STD_DEV = 10d;
  public static final double MIN_GEAR_4ROTOR_THRESHOLD = 9d;

  static final DecimalFormat nf = new DecimalFormat("0");
  static final DecimalFormat pf = new DecimalFormat("0.0%");
  
  public static void main(String[] pArgs) {
    for(String event : EventStats.STL) {
      String[] schedule = Utils.getQualScheduleForEvent(event);
      List<Integer> teams = Utils.getTeamsForEvent(event);
      Map<Integer, TeamStat> stats = Utils.getStatsForTeamsAttending(event, true, false);
      SchedulePredictor sp = new SchedulePredictor(teams, stats, schedule, false);
      
      Map<Integer, DescriptiveStatistics> redscores = new HashMap<>();
      Map<Integer, DescriptiveStatistics> p_RedAutonRotor = new HashMap<>();
      Map<Integer, DescriptiveStatistics> p_Red4Rotors = new HashMap<>();
      Map<Integer, DescriptiveStatistics> p_Red40kpa = new HashMap<>();
      Map<Integer, DescriptiveStatistics> P_RedWin = new HashMap<>();
      
      Map<Integer, DescriptiveStatistics> bluescores = new HashMap<>();
      Map<Integer, DescriptiveStatistics> p_BlueAutonRotor = new HashMap<>();
      Map<Integer, DescriptiveStatistics> p_Blue4Rotors = new HashMap<>();
      Map<Integer, DescriptiveStatistics> p_Blue40kpa = new HashMap<>();
      Map<Integer, DescriptiveStatistics> P_BlueWin = new HashMap<>();
      
      Map<Integer, DescriptiveStatistics>[] redstats = new Map[] {
        p_RedAutonRotor, p_Red4Rotors, p_Red40kpa, P_RedWin
      };
      
      Map<Integer, DescriptiveStatistics>[] bluestats = new Map[] {
        p_BlueAutonRotor, p_Blue4Rotors, p_Blue40kpa, P_BlueWin
      };
      
      for(Match m : sp.mMatches) {
        redscores.put(m.num, new DescriptiveStatistics());
        p_RedAutonRotor.put(m.num, new DescriptiveStatistics());
        p_Red4Rotors.put(m.num, new DescriptiveStatistics());
        p_Red40kpa.put(m.num, new DescriptiveStatistics());
        P_RedWin.put(m.num, new DescriptiveStatistics()); 
  
        bluescores.put(m.num, new DescriptiveStatistics());
        p_BlueAutonRotor.put(m.num, new DescriptiveStatistics());
        p_Blue4Rotors.put(m.num, new DescriptiveStatistics());
        p_Blue40kpa.put(m.num, new DescriptiveStatistics());
        P_BlueWin.put(m.num, new DescriptiveStatistics());
      }
      
      for(int i = 0; i < 10000 ; i++) {
        sp.calcScores();
        for(Match m : sp.mMatches) {
          double p_redwin = 0.5d;
          if(m.red.mScore != m.blue.mScore) {
            p_redwin = m.red.mScore > m.blue.mScore ? 1d : 0d;
          }
          redscores.get(m.num).addValue(m.red.mScore);
          p_RedAutonRotor.get(m.num).addValue(m.red.mAuton2Gear ? 1d : 0d);
          p_Red4Rotors.get(m.num).addValue(m.red.mRotorRP);
          p_Red40kpa.get(m.num).addValue(m.red.mFuelRP);
          P_RedWin.get(m.num).addValue(p_redwin);
          
          bluescores.get(m.num).addValue(m.blue.mScore);
          p_BlueAutonRotor.get(m.num).addValue(m.blue.mAuton2Gear ? 1d : 0d);
          p_Blue4Rotors.get(m.num).addValue(m.blue.mRotorRP);
          p_Blue40kpa.get(m.num).addValue(m.blue.mFuelRP);
          P_BlueWin.get(m.num).addValue(1 - p_redwin);
        }
      }
      
      // output
      System.out.println("MATCH\t" +
        "RED1\tRED2\tRED3\tSCORE\tA_ROTOR\t4ROTOR\t40KPA\tWIN" + "\t" +
        "BLUE1\tBLUE2\tBLUE3\tSCORE\tA_ROTOR\t4ROTOR\t40KPA\tWIN");
      for(Match m : sp.mMatches) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.num);
        sb.append("\t");
        String red = Utils.convertToTabbedString(m.red.teams.stream().map(t->t.num).collect(Collectors.toList()));
        sb.append(red);
        sb.append("\t");
        sb.append(nf.format(redscores.get(m.num).getMean()));
        sb.append("\t");
        for(int i=0; i < redstats.length; i++) {
          sb.append(pf.format(redstats[i].get(m.num).getMean()));
          sb.append("\t");
        }
        String blue = Utils.convertToTabbedString(m.blue.teams.stream().map(t->t.num).collect(Collectors.toList()));
        sb.append(blue);
        sb.append("\t");
        sb.append(nf.format(bluescores.get(m.num).getMean()));
        sb.append("\t");
        for(int i=0; i < bluestats.length; i++) {
          sb.append(pf.format(bluestats[i].get(m.num).getMean()));
          sb.append("\t");
        }
        System.out.println(sb);
      }
    }
  }
  
  public SchedulePredictor(List<Integer> pTeams, Map<Integer, TeamStat> pStats, String[] pSchedule, boolean pRandomizedSchedule) {
    pTeams.forEach(team -> mTeams.put(team, new Team(team, pStats.get(team))));
    
    if(pRandomizedSchedule) {
      // Assign schedule & calculate score
      for(int i = 0; i < pSchedule.length; i++) {  // 2 empty lines at end of some generated outputs
        if(pSchedule[i] != null && pSchedule[i].length() > 0) {
          StringTokenizer st = new StringTokenizer(pSchedule[i], " ");
          Alliance red = new Alliance(
            pTeams.get(Integer.parseInt(st.nextToken())-1), 
            pTeams.get(Integer.parseInt(st.nextToken())-1), 
            pTeams.get(Integer.parseInt(st.nextToken())-1));
          Alliance blue = new Alliance(
            pTeams.get(Integer.parseInt(st.nextToken())-1), 
            pTeams.get(Integer.parseInt(st.nextToken())-1), 
            pTeams.get(Integer.parseInt(st.nextToken())-1));
          Match match = new Match(i+1, red, blue);
          match.calcScore();
          mMatches.add(match);
        }
      }
    } else {
      // Assign schedule & calculate score
      for(int i = 0; i < pSchedule.length; i++) {  // 2 empty lines at end of some generated outputs
        if(pSchedule[i] != null && pSchedule[i].length() > 0) {
          StringTokenizer st = new StringTokenizer(pSchedule[i], " ");
          Alliance red = new Alliance(
            Integer.parseInt(st.nextToken()), 
            Integer.parseInt(st.nextToken()), 
            Integer.parseInt(st.nextToken()));
          Alliance blue = new Alliance(
            Integer.parseInt(st.nextToken()), 
            Integer.parseInt(st.nextToken()), 
            Integer.parseInt(st.nextToken()));
          Match match = new Match(i+1, red, blue);
          match.calcScore();
          mMatches.add(match);
        }
      }
    }
  }
  
  public void calcScores() {
    mMatches.parallelStream().forEach(m -> m.calcScore());
  }
  
  public SchedulePredictor(List<Integer> pTeams, Map<Integer, TeamStat> pStats, String[] pSchedule) {
    this(pTeams, pStats, pSchedule, true);
  }
  
  public static EnumSet<Breakdown2017> CappedOPR = EnumSet.of(
    rotor1Auto, rotor2Auto, autoRotorPoints, autoMobilityPoints,
    rotor2Engaged, rotor3Engaged, rotor4Engaged, teleopTakeoffPoints
  );
  
  /**
   * Clamps, normalizes, and otherwise cleans up the stats so they make sense for 'added value'.
   */
  static void normalize(Map<Integer, TeamStat> allEventStats, boolean pRollup) {
    for(Integer team : allEventStats.keySet()) {
      TeamStat ts = allEventStats.get(team);
      
      // Floor of 0
      for(Breakdown2017 stat : TeamStat.CalculatedValues) {
        double v = ts.get(stat);
        v = Math.max(v, 0d);
        ts.override(stat, v);
      }
      
      // Deal with roll up of rotors
      if(pRollup) {
        String trun = Double.toString(rotor2Engaged.getStaticValue());
        trun = trun.substring(0, trun.indexOf(".") + 2);
        double staticvalue = Double.parseDouble(trun);
  
        // Roll up rotors 2 & 3
        double value2 = Math.max(0d, ts.get(rotor2Engaged));
        double extra2 = Math.max(0d, value2 - staticvalue);
        ts.override(rotor2Engaged, Math.min(staticvalue, value2));
        //Repeat for 3
        double value3 = Math.max(0d, ts.get(rotor3Engaged) + extra2);
        double extra3 = Math.max(0d, value3 - staticvalue);
        ts.override(rotor3Engaged, Math.min(staticvalue, value3));
        
        // Add extra3 for rotor 4
        double value4 = Math.max(0d, ts.get(rotor4Engaged) + extra3);
        ts.override(rotor4Engaged, value4);
      }

      // Ceiling of static value
      for(Breakdown2017 stat : TeamStat.CalculatedValues) {
        double v = ts.get(stat);
        if(CappedOPR.contains(stat)) {
          v = Math.min(v, 1d);
        } else {
          v = Math.min(v, stat.getStaticValue());
        }
        ts.override(stat, v);
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
      red.reset();
      blue.reset();
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
  
  public int NUM_KPA_RP = 0;
  public int NUM_ROTOR_RP = 0;
  public int NUM_TIES = 0;
  
  private class Alliance {
    List<Team> teams = new ArrayList<>();
    private Integer mScore = 0;
    private Integer mRotorRP = 0;
    private Integer mFuelRP = 0;
    private boolean mAutonGear = false;
    private boolean mAuton2Gear = false;
    int mFuelScore = 0;
    
    public Alliance(Integer... pTeams) {
      for(Integer t : pTeams) {
        teams.add(mTeams.get(t));
      }
    }
    
    public void reset() {
      mScore = 0;
      mRotorRP = 0;
      mFuelRP = 0;
      mAutonGear = false;
      mAuton2Gear = false;
      mFuelScore = 0;
      for(Team t : teams) {
        t.matchpoints = 0d;
        t.rankpoints = 0d;
      }
    }

    public double calcScore() {
      // Free Gear
      double value = 40d;
      double fuel = 0d;

      
      for(Breakdown2017 stat : TeamStat.CalculatedValues) {
        switch(stat) {
        case autoMobilityPoints:
          int num = (int)(Math.round(teams.stream().map(t -> t.stats.get(stat)).reduce(Double::sum).get()));
          value += num * autoMobilityPoints.getStaticValue();
          break;
        
        // Individual linear values are already determined in OPR
        case autoFuelPoints: 
        case teleopFuelPoints: 
          // We assume we've (mostly) filtered out non-fuel teams here.
          // Seems to be inline with what 1262, 346, & 836 did in their last few matches
          fuel += teams.stream().map(t -> t.stats.get(stat)).reduce(Double::sum).get();
          break;
          
        // Alliance-cooperative static scores
        case rotor1Auto: 
          double p_A1rotor = teams.stream()
            .map(t -> t.stats.get(stat)) // divide OPR by max to get probability
            .reduce(Double::max).get(); // max engagement for the alliance, since it's individual effort
          mAutonGear |= !(Math.random() > p_A1rotor);
          value += mAutonGear ? 0 : autoRotorPoints.getStaticValue();
        break;
        case rotor2Auto:
          // Max probability or value of alliance
          // OPR-capped, so mult by 3
          double p_A2rotor = teams.stream()
            .map(t -> t.stats.get(rotor1Auto)) // divide OPR by max to get probability
            .reduce(Double::sum).get(); // sum rotor 1 engagement for the alliance, since rotor 2 is combined
          mAuton2Gear |= !(Math.random()*teams.size() > p_A2rotor);
          value += mAuton2Gear ? 0 : autoRotorPoints.getStaticValue();
          break;
          
        case teleopTakeoffPoints:
          value += teams.stream()
            .map(t -> t.stats.get(stat)) // OPR Value - calculate individual probability
            .map(p_takeoff -> Math.random() > p_takeoff ? 0 : teleopTakeoffPoints.getStaticValue()) // Calculate value based upon yes/no
            .reduce(Double::sum).get(); // Max individual values
          break;
          
        case kPaRankingPointAchieved:
          Double p_kpaBonus = teams.stream()
            .map(t -> t.stats.get(stat))
            .reduce(Double::sum).get();
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
      
      double numGears = teams.stream()
        .map(team -> team.stats.getNumGears())
        // Randomly add +/- 1 standard deviation of gears for each team
        .map(gears -> gears + GEAR_STD_DEV * Math.random() * (Math.random() > 0.5d ? 1d : -1d))
        .map(sum -> Math.max(0d, sum))
        .reduce(Double::sum)
        .get();
      
      double g = 0d;
      if(numGears >= 2d) g += 40d;
      if(numGears >= 6d) {
        g += 40d;
      } else if (numGears >= 2d) {
        double p_3r = (6d - numGears) / 4d;
        g += (Math.random() > p_3r ? 0 : 1) * 40d;
      }
      if(numGears >= 12d) {
        g += 40d;
        mRotorRP = 1;
      } else if(numGears >= MIN_GEAR_4ROTOR_THRESHOLD) {
        double p_4r = (12d - numGears) / 6d;
        mRotorRP = Math.random() > p_4r ? 0 : 1;
//        if(mRotorRP > 0) {
//          System.out.println(teams + " got 4 rotors with " + numGears + " gears");
//        }
        g += mRotorRP * 40d;
      }
      value += g;

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
