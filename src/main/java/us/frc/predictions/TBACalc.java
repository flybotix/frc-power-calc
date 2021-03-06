package us.frc.predictions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import com.vegetarianbaconite.blueapi.SynchronousBlueAPI;
import com.vegetarianbaconite.blueapi.beans.Alliance;
import com.vegetarianbaconite.blueapi.beans.Match;
import com.vegetarianbaconite.blueapi.beans.Stats;

public class TBACalc {
  private String eventKey;
  private Boolean qualsOnly;

  public static final SynchronousBlueAPI api = new SynchronousBlueAPI("DominicCanora", "PowerCalc", "1");
  private List<Match> eventMatches;

  private TreeSet<Integer> teams = new TreeSet<>();
  private Map<String, Integer> teamKeyPositionMap = new HashMap<>();
  private double[][] matrix, scores;
  private RealMatrix finalMatrix;
  private CholeskyDecomposition cholesky;
  public boolean isValid;
  public final String mEvent;
  
  public class Insight {
    double numMatches = 0;
    double num4thRotors = 0d;
    double num3rdRotors = 0d;
    double num2ndRotors = 0d;
    double numAuton1Rotors = 0d;
    double numAuton2Rotors = 0d;
    double num40kpa = 0d;
  }

  public TBACalc(String eventKey, Boolean qualsOnly) {
    mEvent = eventKey;
    eventMatches = api.getEventMatches(eventKey);
    if(eventMatches.isEmpty()){
      isValid = false;
      return;
    }
    try {
      for (Match m : eventMatches) {
        for (String t : m.getAlliances().getRed().getTeams()) {
          if (!teams.contains(Integer.parseInt(t.substring(3)))) {
            teams.add(Integer.parseInt(t.substring(3)));
          }
        }
  
        for (String t : m.getAlliances().getBlue().getTeams()) {
          if (!teams.contains(Integer.parseInt(t.substring(3)))) {
            teams.add(Integer.parseInt(t.substring(3)));
          }
        }
      }
  
      int i = 0;
      for (Integer t : teams) {
        teamKeyPositionMap.put("frc" + t, i);
        i++;
      }
  
      matrix = new double[teams.size()][teams.size()];
      scores = new double[teams.size()][1];
  
      this.eventKey = eventKey;
      cleanup();
      reInit(qualsOnly);
    } catch (Exception e) {
      System.err.println("Error when handling event: " + mEvent);
      e.printStackTrace();
      isValid = false;
    }

    isValid = true;
//    System.out.println("Finished event init");
    // TODO - put this in a logging output on a display so we can easily update it
    // year-to-year
//    System.out.println("Score Breakdown:");
//    System.out.println(Arrays.toString(eventMatches.iterator()
//      .next()
//      .getScoreBreakdown()
//      .getBlue()
//      .keySet()
//      .toArray(new String[0])));
//    System.exit(0);
  }

  public Map<Integer, Double> getForKey(String key, boolean anti) {
    synchronized (this) {
      cleanup();
      
      if(key.equalsIgnoreCase(Breakdown2017.teleopTakeoffPoints.name())) {
        return getClimbs();
      }

      Map<Integer, Double> returnedMap = new HashMap<>();
//      if(isValid == false) {
//        return returnedMap;
//      }

      for (Match m : eventMatches) {
        if (!m.getCompLevel().equals("qm") && qualsOnly) {
          continue;
        }
        Alliance blue = m.getAlliances().getBlue();
        Alliance red = m.getAlliances().getRed();
        try {
          for (String team : blue.getTeams()) {
            if (key.equalsIgnoreCase("opr")) {
              scores[teamKeyPositionMap.get(team)][0] += blue.getScore();
            } else if (key.equalsIgnoreCase("dpr")) {
              scores[teamKeyPositionMap.get(team)][0] += red.getScore();
            } else {
              if(anti) {
                // TODO - Here, we should use an interface that is custom-mapped to the enum each year
                scores[teamKeyPositionMap.get(team)][0] += Breakdown2017.valueOf(key).map(m.getScoreBreakdown().getRed().get(key));
              } else {
                scores[teamKeyPositionMap.get(team)][0] += Breakdown2017.valueOf(key).map(m.getScoreBreakdown().getBlue().get(key));
              }
            }
          }
          for (String team : red.getTeams()) {
            if (key.equalsIgnoreCase("opr"))
              scores[teamKeyPositionMap.get(team)][0] += red.getScore();
            else if (key.equalsIgnoreCase("dpr")) {
              scores[teamKeyPositionMap.get(team)][0] += blue.getScore();
            } else {
              if(anti) {
                scores[teamKeyPositionMap.get(team)][0] += Breakdown2017.valueOf(key).map(m.getScoreBreakdown().getBlue().get(key));
              } else {
                scores[teamKeyPositionMap.get(team)][0] += Breakdown2017.valueOf(key).map(m.getScoreBreakdown().getRed().get(key));
              }
            }
          }
        } catch (Exception e) {
          System.err.println("Error when calculating OPR " + key + " for event " + mEvent);
          // I mean .. what can we do with bad TBA data but toss it?
        }
      }

      RealMatrix scoreMatrix = MatrixUtils.createRealMatrix(scores);
      double[][] output = cholesky.getSolver().solve(scoreMatrix).getData();

      for (Integer team : teams) {
        returnedMap.put(team, output[teamKeyPositionMap.get("frc" + team)][0]);
      }

      return returnedMap;
    }
  }
  private static final String[] CLIMB_KEYS = new String[]{
    Breakdown2017.touchpadFar.name(), 
    Breakdown2017.touchpadMiddle.name(),
    Breakdown2017.touchpadNear.name()
  };
  
  private Map<Integer, Double> getClimbs() {
    Map<Integer, Average> avgs = new HashMap<>();
    for (Match m : eventMatches) {
      if (!m.getCompLevel().equals("qm") && qualsOnly) {
        continue;
      }
      Alliance blue = m.getAlliances().getBlue();
//      System.out.println(m.getMatchNumber() + "\tBLUE\t" + Arrays.toString(blue.getTeams()));
      for(int t = 0; t < blue.getTeams().length; t++) {
        Integer team = Integer.parseInt(blue.getTeams()[t].replace("frc", ""));
//        System.out.println(team + "\t" + m.getScoreBreakdown().getBlue().get(CLIMB_KEYS[t]));
        Average a = avgs.get(team);
        if(a == null) { a = new Average(); avgs.put(team, a); }
        String key = CLIMB_KEYS[t];
        Breakdown2017 stat = Breakdown2017.valueOf(key);
        a.add(stat.map(m.getScoreBreakdown().getBlue().get(key)));
      }
      
      
      Alliance red = m.getAlliances().getRed();
//      System.out.println(m.getMatchNumber() + "\tRED\t" + Arrays.toString(red.getTeams()));
      for(int t = 0; t < red.getTeams().length; t++) {
        Integer team = Integer.parseInt(red.getTeams()[t].replace("frc", ""));
//        System.out.println(team + "\t" + m.getScoreBreakdown().getRed().get(CLIMB_KEYS[CLIMB_KEYS.length - 1 - t]));
        Average a = avgs.get(team);
        if(a == null) { a = new Average(); avgs.put(team, a); }
        String key = CLIMB_KEYS[CLIMB_KEYS.length - 1 - t]; // reverse for red due to mirrored field
        Breakdown2017 stat = Breakdown2017.valueOf(key);
        a.add(stat.map(m.getScoreBreakdown().getBlue().get(key)));
      }
    }
    
    Map<Integer, Double> result = new HashMap<>();
    for(Integer team : avgs.keySet()) {
      result.put(team, avgs.get(team).getAverage());
    }
    return result;
  }

  public Map<Integer, Double> getForKey(String key) {
    return getForKey(key, false);
  }

  public Map<Integer, Double> getForSupplier(StatProvider sp) {
    synchronized (this) {
      cleanup();

      Map<Integer, Double> returnedMap = new HashMap<>();

      for (Match m : eventMatches) {
        if (!m.getCompLevel().equals("qm") && qualsOnly)
          continue;

        for (String team : m.getAlliances().getBlue().getTeams())
          scores[teamKeyPositionMap.get(team)][0] += sp.get(m.getScoreBreakdown().getBlue());
        for (String team : m.getAlliances().getRed().getTeams())
          scores[teamKeyPositionMap.get(team)][0] += sp.get(m.getScoreBreakdown().getRed());
      }

      RealMatrix scoreMatrix = MatrixUtils.createRealMatrix(scores);
      double[][] output = cholesky.getSolver().solve(scoreMatrix).getData();

      for (Integer team : teams) {
        returnedMap.put(team, output[teamKeyPositionMap.get("frc" + team)][0]);
      }

      return returnedMap;
    }
  }

  private void cleanup() {
    scores = new double[teams.size()][1];
  }

  public void reInit(boolean qualsOnly) {
    this.qualsOnly = qualsOnly;
    synchronized (this) {
      for (Integer t : teams) {
        for (Match m : api.getTeamEventMatches(t, eventKey)) {
          if (!m.getCompLevel().equals("qm") && qualsOnly)
            continue;

          Alliance a = m.getAlliances().getBlue().contains("frc" + t) ? m.getAlliances().getBlue()
            : m.getAlliances().getRed();
          for (String allianceMember : a.getTeams()) {
            matrix[teamKeyPositionMap.get("frc" + t)][teamKeyPositionMap.get(allianceMember)] += 1;
          }
        }
      }

      finalMatrix = MatrixUtils.createRealMatrix(matrix);
      cholesky = new CholeskyDecomposition(finalMatrix);
    }
  }

  public interface StatProvider {
    double get(Map<String, String> scoreBreakdown);
  }
}
