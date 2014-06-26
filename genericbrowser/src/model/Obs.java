package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import util.AutoMap;
import util.DBConnector;
import util.SimilarWords;
import util.Util;
import util.Util.Timer;

/**
 * Non comparative (i.e. absolute) triple 
 * @author ntandon
 *
 */
public class Obs {

String x,y,xhead;

public Obs(String x,String y) {
  super();
  this.x = x;
  this.y = y;
  this.xhead = headNoun(x);
}

public static String headNoun(String word){
  String[] xSplits = word.split(" ");
  return xSplits[xSplits.length - 1];
}

@Override public String toString(){

  return new StringBuilder().append(x).append('\t').append(y).toString();
}

/** Find similar Obs to car,fast,bike; includes itself in result.
 * @param inTb e.g. __csynsetinput
 * @param restrictToMFS typically 5
 * @param topK e.g. 5 returns top5 similar Obs in the order:
 * 1st: same x, same y, same a (diff. order)
 * 2nd: same x, similar y, same a (diff y)
 * 3rd: similar x, same y, same a (diff x)
 * 4th: similar x, similar y, same a (diff x,y)
 * 5th: same x, similar y, similar a (diff y,a)
 * 6th: similar x, same y, similar a (diff x,a)
 * 7th: similar x, similar y, similar a (diff x,y,a)
 * 8th: same x, diff y, same a
 * As soon as topK similar are obtained, returns.
 * @param maxCand 
 * @return car,fast,bike ; bike,fast,car; bus,slow,car etc. 
 * @throws Exception
 */
public List<Obs> similar(int maxCandObs,String inTb,int restrictToMFS,int topK)
  throws Exception{
  // Timer timer = new Timer();
  Set<Obs> similarObsCandi = new HashSet<>();

  // System.out.println("--> Fetching similar words... ");
  SimilarWords simUtil = SimilarWords.getInstance(restrictToMFS);
  Set<String> similarX = simUtil.getSimilar(xhead, true);
  if(similarX == null) return null;
  if(numSenses(x) != 1) similarX.add(xhead); // "unripe fruit" for
                                             // "ripe fruit"
  similarX.add(x);// Similar to itself

  Set<String> similarA = simUtil.getSimilar(y, false);
  if(similarA == null) return null;
  similarA.add(y);

  // System.out.println("--> Pruning similar words based on inTb ... ");

  // Score map.
  AutoMap<Obs, Double> m = new AutoMap<>();
  // System.out.println("--> Fetching similar Obs ... ");
  for(String arg1: Util.nullableIter(similarX)){
    List<Obs> ignoringA = fromDb(arg1, "", maxCandObs, inTb);
    for(Obs cob: ignoringA){
      if(similarA.contains(cob.y)){
        double sim = simScore(cob, similarX, similarA);
        similarObsCandi.add(cob);
        m.put(cob, sim);
      }
    }
  }
  // timer.time("[similar Obs computed");

  List<Obs> similarObs = new ArrayList<>();

  for(Entry<Obs, Double> e: m.sortByValue().entrySet()){
    if(topK-- > 0)
      similarObs.add(e.getKey());
    else break;
  }
  return similarObs;

}

private List<Obs> fromDb(String x1,String a1,int max,String tb)
  throws SQLException{
  boolean hasX1 = x1 != null && x1.length() > 0;
  boolean hasX2 = a1 != null && a1.length() > 0;
  if(!hasX1 && !hasX2) return null;

  // Both cannot be true.
  String predicate = hasX1 ? " xhead='" + x1 + "'" : " y='" + a1 + "'";
  List<Obs> o = new ArrayList<>();
  String sql =
    "select x,y,xhead from " + tb + " where " + predicate + " limit " + max;
  ResultSet rs = DBConnector.q(sql);
  while (rs.next()){
    o.add(new Obs(rs.getString(1), rs.getString(2)));
  }
  return o;
}

/**
 * top5 similar Obs in the order:
 * 1st: same x, same y, same a (diff. order)
 * 2nd: same x, similar y, same a (diff y)
 * 3rd: similar x, same y, same a (diff x)
 * 4th: similar x, similar y, same a (diff x,y)
 * 5th: same x, similar y, similar a (diff y,a)
 * 6th: similar x, same y, similar a (diff x,a)
 * 7th: similar x, similar y, similar a (diff x,y,a)
 * 8th: same x, diff y, same a
 * As soon as topK similar are obtained, returns.
 * @param cob
 * @param similarX
 * @param similarA
 * @param similarY
 * @return score based on the heuristics above. TODO: formulate these booleans to formalize rank?
 */
private double simScore(Obs cob,Set<String> similarX,Set<String> similarA){
  boolean sameX = false;
  boolean sameA = false;
  boolean kindofX = false;
  boolean kindofA = false;
  boolean sameXNorm = false;
  boolean kindofXNorm = false;

  if(cob.x.equals(this.x)) sameX = true;
  if(cob.y.equals(this.y)) sameA = true;
  if(similarX.contains(cob.x)) kindofX = true;
  if(similarA.contains(cob.y)) kindofA = true;

  if(cob.xhead.equals(xhead)) sameXNorm = true;
  if(similarX.contains(cob.xhead)) kindofXNorm = true;

  double sim = 0.0;
  if(sameX && sameA) return 1.0; // car,fast,bike/ car,fast,bike
  if(sameX && kindofA) return 0.99000; // car,fast,bike/ car,slow,bike
  if(kindofX && sameA) return 0.90000; // car,fast,bike/
                                       // motor,fast,bike
  if(kindofX && kindofA) return 0.79990; // car,fast,bike/
                                         // motor,slow,bike

  if(sameXNorm && sameA) return 0.50; // synthetic rubber, soft,
                                      // natural rubber /
  // heated rubber, soft, softened rubber
  if(sameXNorm && kindofA) return 0.49;
  if(sameXNorm && kindofA && kindofXNorm) return 0.47;

  return sim;
}

private static AutoMap<String, Integer> m_numsense;

public static int numSenses(String nounNoWpsNo_) throws SQLException{
  if(m_numsense == null){
    m_numsense = new AutoMap<>();
    ResultSet rs = DBConnector.q("select word,sensenum from nounid");
    while (rs.next())
      m_numsense.addNumericValueInt(rs.getString(1).replaceAll("_", " "), 1);
    // Util.writeFile("./data/disambiguate/num.nounsenses", m_numsense, false);
  }
  if(m_numsense.containsKey(nounNoWpsNo_)){
    return m_numsense.get(nounNoWpsNo_);
  } else return 0;
}

public static void main(String[] args) throws Exception{

  Timer timer = new Timer();
  String input = "";
  do{
    input = Util.readStringFromUser("\nx,y: ");
    if(input == null || input.length() == 0) input = "tiger,fast";
    timer.reset();
    String[] xyhead = input.split(",");
    Obs o = new Obs(xyhead[0], xyhead[1]);
    int i = 1;
    for(Obs o2: Util.nullableIter(o.similar(10000, "_hasprop", 4, 30))){
      System.out.println(i++ + ". " + o2);
    }
    timer.time();
  } while (input != null && input.length() > 0);

  DBConnector.closeConnections();
}

}
