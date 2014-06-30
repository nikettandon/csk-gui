package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javatools.datatypes.Pair;
import javatools.datatypes.Triple;
import util.AutoMap;
import util.DBConnector;
import util.IDHelper;
import util.IDHelper.IDSubjObj;
import util.IDHelper.wps;
import util.Util;
import util.Util.Timer;
import controller.InputToModel;
import controller.InputToView.Atom;
import controller.InputToView.AtomType;
import controller.InputToView.Decoration;
import controller.InputToView.Decoration.background;
import controller.InputToView.Decoration.boldface;
import controller.InputToView.Decoration.color;
import controller.InputToView.Decoration.font;
import controller.InputToView.Decoration.fontsize;
import controller.InputToView.ResultRow;

public class Logic {

private static Map<Integer, Integer> oppCsynsets;
private static int tripleID;
private static Triple<String, String, String> disambiXAY;
/** be colder than,<br> north pole;south pole, <br> winter;summer.. */
private static Map<String, String> rnormTriples;
private static String similarToX;
private static String similarToY;

/**
 * 
 * @param in e.g. car,bike
 * @param atoms <car,fast,bike>; <bike,efficient,bike>
 * @return 
 */
public String dbfetch(InputToModel in,List<Atom> atoms,List<ResultRow> rows, List<Atom> relatedWords){
  try{
    if(oppCsynsets == null) initOppCSynsets();
    if(rnormTriples == null) initRnormTriples();

    similarToX = similarNoun(in.x.get(0));
    similarToY = similarNoun(in.x.get(1));
    tripleID = 1;
    disambiXAY =
      new Triple<String, String, String>(in.x.get(0), "", in.x.get(1));
    Timer timer = new Timer();
    // Fetch matching csynsets
    System.out.println("Fetch matching csynsets ...");
    List<Integer> csynsetIDs = matchingCSynsets(in);
    // Cluster matched csynsets (zombies-> -1).
    System.out.println("Cluster matched csynsets (zombies-> -1) ...");
    Map<Integer, Integer> pairedCyn = pairCSynsets(csynsetIDs);
    // For each csynset, construct UIAtoms from synset member ctriples.
    System.out.println("For each csynset, construct UIAtoms of ctriples ...");
    // fillCSynsetAtomsUnordered(pairedCyn, atoms);
    findRelatedWords(in.x.get(0), in.x.get(1), similarToX,similarToY, relatedWords);
    fillCSynsetAtomsOrdered(pairedCyn, rows);
    timer.time("created GUI ");
  } catch (Exception e){
    e.printStackTrace();
  }

  return resultsDisambiguationSummary(disambiXAY, in);
}

private String similarNoun(String x){
  StringBuilder returnStr = new StringBuilder();
  String newLine = "<br>";
  List<String> ys = Autocompletion.y(x + ",", 10);
  for(String y: ys){
    int commaAt = y.indexOf(',');
    if(commaAt != -1){
      y = y.substring(commaAt + 1);
    }
    returnStr.append(y).append(newLine);
  }
  return returnStr.toString();
}


/**
 * Currently combining all similar nouns to both X and Y
 */

private void findRelatedWords(String X, String Y, String simX, String simY, List<Atom> relWords){
	String[] arrX = simX.split("<br>");
	String[] arrY = simY.split("<br>");
	List<String> relatedList = new ArrayList<String>();
	
	Decoration d;
	d = new Decoration(color.green, font.normal, boldface.bold, fontsize.normal,
			      background.white);
	for (String xs : arrX){
		if (Arrays.asList(arrY).contains(xs) && xs.trim().length()>0){
			relWords.add(new Atom(null, 0 , xs, "?x="+xs, xs, "", d));
			relatedList.add(xs);
		}
	}

	d = new Decoration(color.red, font.normal, boldface.normal, fontsize.normal,
		      background.white);
	for (String xs : arrX){
		if (!relatedList.contains(xs) && xs.trim().length()>0 && !xs.trim().equals(Y)){
			relWords.add(new Atom(null, 0 , xs, "?x="+xs, xs, "", d));
			relatedList.add(xs);
		}
	}

	d = new Decoration(color.black, font.normal, boldface.normal, fontsize.normal,
		      background.white);
	for (String ys : arrY){
		if (!relatedList.contains(ys) && ys.trim().length()>0 && !ys.trim().equals(X)){
			relWords.add(new Atom(null, 0 , ys, "?x="+ys, ys, "", d));
			relatedList.add(ys);
		}
	}	
}

/**
 * Build on Query: select x,y from __csynsetinput where rnorm='be hotter than' 
 *        order by freq desc limit 10
 * @throws SQLException
 */
private void initRnormTriples() throws SQLException{
  AutoMap<String, List<String>> tempRnormTriples = new AutoMap<>();
  int max = 10; // top10 ctriples for every rnorm.
  String sql = "select x,y,rnorm from __csynsetinput order by freq desc";
  ResultSet rs = DBConnector.getDB().query(sql);
  while (rs.next()){
    String k = rs.getString(3);
    String v = rs.getString(1) + "," + rs.getString(2);
    if(tempRnormTriples.containsKey(k) && tempRnormTriples.get(k).size() > max)
      continue;
    tempRnormTriples.addArrayValueNoRepeat(k, v);
  }

  String newLine = "<br>";
  rnormTriples = new HashMap<>();
  for(Entry<String, List<String>> e: tempRnormTriples.entrySet()){
    StringBuilder value = new StringBuilder();
    for(String v: e.getValue()){
      value.append(v).append(newLine);
    }
    rnormTriples.put(e.getKey(), value.toString());
  }

}

private String resultsDisambiguationSummary(
  Triple<String, String, String> disambiXAY2,InputToModel in){
  String formattedSummary = "<h4>";
  if(disambiXAY2 != null){
    try{
      IDSubjObj metaX = IDHelper.getTextMetaSlowly(disambiXAY2.first, "nounid");
      IDSubjObj metaY = IDHelper.getTextMetaSlowly(disambiXAY2.third, "nounid");
      formattedSummary +=
        "<h4>" + metaX.word + ":&nbsp;\t&nbsp;" + shortenGloss(metaX.gloss)
          + "\n<br>" + metaY.word + ":&nbsp;\t&nbsp;"
          + shortenGloss(metaY.gloss);

    } catch (Exception e){
      System.out.println("Exception fetching gloss for " + disambiXAY2.first
        + " or " + disambiXAY2.third);
    }
    formattedSummary += "</h4>";

  } else{
    formattedSummary += in.x.get(0) + "&nbsp;\t&nbsp;<br>" + in.x.get(1);
  }

  return formattedSummary;

}

private String shortenGloss(String gloss){
  String shortenedGloss = "";
  if(gloss == null) return shortenedGloss;
  // max 80 characters.
  if(gloss.length() <= 80)
    return gloss;
  else return gloss.substring(0, 80) + "...";
}

// man,woman --> fill important (high frequency/ size of csynset/
// if there is an oppose set.) vertical tabs
private void fillCSynsetAtomsOrdered(Map<Integer, Integer> pairedCyn,
  List<ResultRow> rows) throws SQLException{
  AutoMap<Pair<Integer, Integer>, Double> mUnsorted = new AutoMap<>();
  for(Entry<Integer, Integer> e: pairedCyn.entrySet()){
    double value = valuationOfCSynsetPair(e.getKey(), e.getValue());
    mUnsorted.put(new Pair<Integer, Integer>(e.getKey(), e.getValue()), value);
  }
  TreeMap<Pair<Integer, Integer>, Double> mSorted = mUnsorted.sortByValue();

  boolean isDisambiguated = false;
  // Foreach csynset; adj -> ctriples
  for(Entry<Pair<Integer, Integer>, Double> e: mSorted.entrySet()){
    List<DbTriple> triples = memberTriples(e.getKey().first);
    String repAdj = representativeAdj(triples);

    // Opinion about something "better than" .. manually classified.
    if(repAdj.equals("good") || repAdj.equals("bad")
      || repAdj.equals("good,bad") || repAdj.equals("bad,good"))
      repAdj = "OPINION";

    List<DbTriple> oppTriples = memberTriples(e.getKey().second);

    // Based on freq first put black (supp.) or red (opp.).
    int freqSuppTriples = 0;
    int freqOppTriples = 0;
    for(DbTriple t: triples)
      freqSuppTriples += t.freq;
    for(DbTriple t: oppTriples)
      freqOppTriples += t.freq;

    if(freqOppTriples > freqSuppTriples){
      for(DbTriple t: oppTriples)
        fillARow(color.red, repAdj, t, rows);
    }

    // Supporting triples
    for(DbTriple t: triples){
      if(!isDisambiguated){
        // disambiXAY = Disambiguation.disambiguateXAY(t.x, t.adj, t.y, true, 1);
        disambiXAY =
          Disambiguation.disambiguateXAY(disambiXAY.first, t.adj,
            disambiXAY.third, true, 1);
        if(disambiXAY == null)
          disambiXAY = new Triple<String, String, String>(t.x, t.adj, t.y);
        isDisambiguated = true;
      }
      fillARow(color.black, repAdj, t, rows);
    }

    if(freqSuppTriples >= freqOppTriples){
      // Counter triples
      for(DbTriple t: oppTriples)
        fillARow(color.red, repAdj, t, rows);
    }

  }
}

// importance of a synset pair based on :
// 1. sum(frequency) of members of csynset fwd
// 2. size of csynset fwd
// 3. stats (1,2) of oppose set.
private double valuationOfCSynsetPair(Integer fwd,Integer opp)
  throws SQLException{
  double v = 0.0;
  Pair<Integer, Integer> fwdStats = valuationOfCSynset(fwd);
  Pair<Integer, Integer> oppStats = valuationOfCSynset(opp);
  int supportCoefficient = 2;
  int freqCoefficient = 1;
  // Even one matters.
  int oppFreqCoefficient = 2;
  int oppSupportCoefficient = 2;
  v =
    (fwdStats.first > 1 ? fwdStats.first * supportCoefficient : fwdStats.first)
      + fwdStats.second * freqCoefficient + oppStats.first
      * oppSupportCoefficient + oppStats.second * oppFreqCoefficient;

  return v;
}

private Pair<Integer, Integer> valuationOfCSynset(Integer fwd)
  throws SQLException{
  int sumFwdFreq = 0;
  int numFwdMembers = 0;
  if(fwd != -1){
    for(int m: synsetMembers(fwd)){
      // Fetch freq. of each synset member
      String sql = "select freq from __csynsetinput " + " where observid=" + m;
      ResultSet rs = DBConnector.q(sql);
      while (rs.next())
        // Add freq of a member
        sumFwdFreq += rs.getInt(1);
      numFwdMembers++;
    }
  }

  return new Pair<Integer, Integer>(numFwdMembers, sumFwdFreq);

}

/*private void fillCSynsetAtomsUnordered(Map<Integer, Integer> pairedCyn,
  List<Atom> atoms) throws SQLException{
  // Foreach csynset; adj -> ctriples
  for(Entry<Integer, Integer> e: pairedCyn.entrySet()){
    List<DbTriple> triples = memberTriples(e.getKey());
    String repAdj = representativeAdj(triples);

    // Opinion about something "better than" .. manually classified.
    if(repAdj.equals("good") || repAdj.equals("bad")
      || repAdj.equals("good,bad") || repAdj.equals("bad,good"))
      repAdj = "OPINION";

    // Supporting triples
    for(DbTriple t: triples)
      fillAnAtom(color.black, repAdj, t, atoms);
    // Counter triples
    for(DbTriple t: memberTriples(e.getValue()))
      fillAnAtom(color.red, repAdj, t, atoms);
  }
}*/

private void fillARow(color c,String a,DbTriple t,List<ResultRow> rows){
  a = a.replaceAll(" ", "_"); // Vertical tab labels do not accept spaces.
  Decoration d =
    new Decoration(c, font.normal, boldface.normal, fontsize.normal,
      background.white);
  String similarXY = rnormTriples.get(t.rnorm);
  // red color denotes opp. triple

  List<Atom> rowElems = new ArrayList<>();
  rowElems.add(new Atom(AtomType.content, ++tripleID, t.x, t.x + "," + t.y,
    "freq: " + t.freq + "<BR>" + similarToX, a, d));
  rowElems.add(new Atom(AtomType.content, tripleID, t.r, t.x + "," + t.y,
    "freq: " + t.freq + "<BR><BR>" + similarXY, a, d));
  rowElems.add(new Atom(AtomType.content, tripleID, t.y, t.x + "," + t.y,
    "freq: " + t.freq + "<BR>" + similarToY, a, d));

  rows.add(new ResultRow(rowElems, a));
}

private String representativeAdj(List<DbTriple> triples){
  String a = "";
  Set<String> adjs = new HashSet<>();
  for(DbTriple t: triples)
    if(t.adj.length() > 0) adjs.add(t.adj);
  int maxRepAdj = 2;
  for(String adj: adjs){
    if(--maxRepAdj < 0) break;
    a += (maxRepAdj != 1 ? "," : "") + adj;
  }
  if(adjs.size() == 0){
    a = triples.get(0).rnorm;
  }
  return a;
}

private List<DbTriple> memberTriples(int csynset) throws SQLException{
  List<DbTriple> triples = new ArrayList<>();
  if(csynset < 0) return triples;
  for(int m: synsetMembers(csynset)){
    // Fetch each synset member @formatter:off
    String sql =
      "select x,r,y,a,freq,rnorm from __csynsetinput " + " where observid=" + m;
    ResultSet rs = DBConnector.q(sql);
    while (rs.next())
      // Add Triple object @formatter:on
      triples.add(new DbTriple(rs.getString(1), rs.getString(2), rs
        .getString(3), rs.getString(4), rs.getInt(5), rs.getString(6)));
  }
  return triples;
}

private static class DbTriple {
public String x;
public String r;
public String y;
public String adj;
public int freq;
public String rnorm;

public DbTriple(String x,String r,String y,String adj,int freq,String rnorm) {
  super();
  this.x = x;
  this.r = r;
  this.y = y;
  this.adj = adj;
  this.freq = freq;
  this.rnorm = rnorm;
}
}

private List<Integer> synsetMembers(int csynsetID) throws SQLException{
  List<Integer> members = new ArrayList<>();
  // Fetch from Db @formatter:off
  String sql = "select observid " +
    "from __csynsets  where csynsetid="+csynsetID;
  ResultSet rs = DBConnector.q(sql);
  while(rs.next()) members.add(rs.getInt(1));
  // Return non empty members @formatter:on
  return members;
}

// Csyn with no opp. have as corresponding map value -1.
private Map<Integer, Integer> pairCSynsets(List<Integer> csynsetIDs){
  Map<Integer, Integer> paired = new HashMap<>();
  for(int c: csynsetIDs){
    int oc = oppCsynsets.containsKey(c) ? oppCsynsets.get(c) : -1;
    boolean oppConsidered = paired.containsKey(oc);
    if(oppConsidered) continue;
    boolean oppInMatching = oc >= 0 ? csynsetIDs.contains(oc) : false;
    if(!oppInMatching) oc = -1;
    paired.put(c, oc);
  }
  return paired;
}

private void initOppCSynsets() throws SQLException{
  oppCsynsets = new HashMap<Integer, Integer>();
  String sql = "select c1,c2 from __oppcsynsets";
  ResultSet rs = DBConnector.q(sql);
  while (rs.next())
    oppCsynsets.put(rs.getInt(1), rs.getInt(2));
}

/** Get similar csynsets. 
 * TODO currently in.x is matched against x and in.y against y. 
 * Later match against xnorm and ynorm
 */
private List<Integer> matchingCSynsets(InputToModel in) throws SQLException{
  List<Integer> csynsetID = new ArrayList<>();
  // Fetch matching csynsets @formatter:off
  String sql = "select distinct(csynsetid) " +
    "from __csynsets as t1, __csynsetinput as t2 where " +
    "((x='"+in.x.get(0)+"' and y='"+in.x.get(1)+"')" + " or " + 
    "(y='"+in.x.get(0)+"' and x='"+in.x.get(1)+"')) " +
    "and t1.observid = t2.observid limit "+in.maxDbResults;
  System.out.print("Querying "+sql+" ...");
  ResultSet rs = DBConnector.q(sql);
  System.out.println("  [done]");
  while(rs.next()) csynsetID.add(rs.getInt(1));
  // Return list of csynsets.
  
  return csynsetID;
}

}

class Disambiguation {

public static void main(String[] args) throws Exception{

  String xay = "keyboard,fast,mouse";
  while (xay != null && xay.trim().length() > 0 && !xay.equalsIgnoreCase("q")){
    String[] xaySplit = xay.split(",");
    boolean backoff = false;
    if(xaySplit.length >= 4) backoff = Boolean.parseBoolean(xaySplit[3]);
    int maxK = backoff ? 2 : 1; // Print topK disambiguated sense triples.
    if(xaySplit.length >= 5) maxK = Integer.parseInt(xaySplit[4]);
    disambiguateXAY(xaySplit[0], xaySplit[1], xaySplit[2], backoff, maxK);
    System.out.println("\n======================\n");
    xay = Util.readStringFromUser("x,a,y");
  }
  DBConnector.closeConnections();
}

public static Triple<String, String, String> disambiguateXAY(String x,String a,
  String y,boolean backoff,int maxK) throws SQLException{
  Triple<String, String, String> answer = null;
  List<Triple<String, String, String>> allSenses = allSenses(x, a, y);
  AutoMap<String, Double> candSenseTriples = new AutoMap<>();
  for(Triple<String, String, String> t: allSenses){
    double xySim = simNNwps(t.first, t.third, backoff); // xwps,ywps
    double xaSim = simNAwps(t.first, t.second, backoff);// xwps,awps
    double yaSim = simNAwps(t.third, t.second, backoff);// ywps,ywps
    double xSenseScore = scoreSense(t.first); // xwps
    double aSenseScore = scoreSense(t.second); // awps
    double ySenseScore = scoreSense(t.third); // ywps
    double score =
      backoff ? scoreAggregateMult(xSenseScore, aSenseScore, ySenseScore,
        xySim, xaSim, yaSim) : scoreAggregateAdd(xSenseScore, aSenseScore,
        ySenseScore, xySim, xaSim, yaSim);
    candSenseTriples.put(t.first + "\t" + t.second + "\t" + t.third, score);
  }
  TreeMap<String, Double> topK = candSenseTriples.sortByValue();
  int atRank = 0;
  for(Entry<String, Double> e: topK.entrySet()){
    if(answer == null){
      // Form the winner.
      String[] winnerSenses = e.getKey().split("\t");
      answer =
        new Triple<String, String, String>(winnerSenses[0], winnerSenses[1],
          winnerSenses[2]);
    }
    if(++atRank > maxK) break;
    if(atRank > 1 & e.getValue() < 1e-2) // No point in having a distant second winner.
      continue;
    System.out.println(e.getKey() + "\t" + e.getValue());
  }

  return answer;
}

private static List<Triple<String, String, String>> allSenses(String x,
  String a,String y) throws SQLException{
  List<Triple<String, String, String>> allSenses = new ArrayList<>();
  // x , a , y -> replace space with underscore; a in NA sim table has _
  List<wps> xwps = IDHelper.getwps(x, true);
  List<wps> awps = IDHelper.getwps(a, false);
  List<wps> ywps = IDHelper.getwps(y, true);
  for(wps xsense: xwps){
    for(wps asense: awps){
      for(wps ysense: ywps){
        allSenses.add(new Triple<String, String, String>(xsense.toString(),
          asense.toString().replaceAll(" ", "_"), ysense.toString()));
      }
    }
  }
  return allSenses;
}

private static double scoreSense(String wps){
  /*double senseScore = 0.20; // For the unlikely 6+ senseNumbers.
  int senseNum = new wps(wps).s;
  // 1 -> 0.99; 2 -> 0.88; 3 -> 0.77, 4 -> 0.50, 5 -> 0.40, 6+ -> 0.20;
  if(senseNum == 1) senseScore = 0.99;
  if(senseNum == 2) senseScore = 0.88;
  if(senseNum == 3) senseScore = 0.77;
  if(senseNum == 4) senseScore = 0.50;
  if(senseNum == 5) senseScore = 0.40;
  // below this small score.
  return senseScore;*/
  double senseScore = 0.60; // For the unlikely 6+ senseNumbers.
  int senseNum = new wps(wps).s;
  // 1 -> 0.99; 2 -> 0.88; 3 -> 0.77, 4 -> 0.50, 5 -> 0.40, 6+ -> 0.20;
  if(senseNum == 1) senseScore = 0.99;
  if(senseNum == 2) senseScore = 0.90;
  if(senseNum == 3) senseScore = 0.85;
  if(senseNum == 4) senseScore = 0.80;
  if(senseNum == 5) senseScore = 0.75;
  // below this small score.
  return senseScore;
}

private static double simNNwps(String n1wps,String n2wps,boolean backoff)
  throws SQLException{
  double sim = 0.0;
  if(backoff) sim = 0.05;
  // ? double backoff = 0.05; // some noun pair similarities are pruned (0.1).
  // Path similarity scores are under-stated.

  int w1 = IDHelper.idMeta(new wps(n1wps)).wordID;
  int w2 = IDHelper.idMeta(new wps(n2wps)).wordID;
  if(w1 == w2) return 1.0;
  String sql =
    "select path from simnn where w1=" + (w2 > w1 ? w1 : w2) + " and w2="
      + (w2 > w1 ? w2 : w1);
  ResultSet rs = DBConnector.q(sql);
  while (rs.next())
    sim = rs.getDouble(1);

  return sim;
}

private static double simNAwps(String nwps,String awps,boolean backoff)
  throws SQLException{
  double sim = 0.0;
  if(backoff) sim = 0.05;
  // ? double backoff = 0.05; // some noun adj similarities are pruned (0.1).

  String sql =
    "select sim from __sensesim where xwps='" + nwps + "' and ywps='" + awps
      + "'";
  ResultSet rs = DBConnector.q(sql);
  while (rs.next())
    sim = rs.getDouble(1);

  return sim;
}

private static double scoreAggregateMult(double xSenseScore,double aSenseScore,
  double ySenseScore,double xySim,double xaSim,double yaSim){
  return xSenseScore * aSenseScore * ySenseScore * xySim * xaSim * yaSim;
}

private static double scoreAggregateAdd(double xSenseScore,double aSenseScore,
  double ySenseScore,double xySim,double xaSim,double yaSim){
  return (xSenseScore + aSenseScore + ySenseScore + xySim + xaSim + yaSim) / 6.0;
}

}
