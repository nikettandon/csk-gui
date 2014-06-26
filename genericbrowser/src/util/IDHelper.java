package util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import util.Util.Timer;

public class IDHelper {

private static AutoMap<Integer, String> adjwordidToWps; // 29873 -> lazy#a#1
private static AutoMap<Integer, String> nounwordidToWps; // 29873 -> human#n#1
private static AutoMap<Integer, List<String>> ssidToWps; // 304778653 ->
// lazy#a#1, slow#a#5
// ...
private static AutoMap<String, List<String>> nounwordToWps; // car -> car#n#1,
// car#n#2 ...
private static AutoMap<String, List<String>> adjwordToWps; // sweet -> sweet#a#1
// ...
private static HashMap<wps, IDSubjObj> wpsToID; // lazy#a#1 -> 304778653, 29873
private static HashMap<Integer, String> rawNounIDTorawNoun; // 901 -> apple
private static HashMap<Integer, String> rawAdjIDTorawAdj; // 901 -> deft
private static HashMap<Integer, Integer> nounidToSensenum;
private static HashMap<Integer, Integer> adjidToSensenum;

public static void mainToDumpIDs(String[] args) throws Exception{
  String glossQ = "select * from wordnet.wn_glosses";
  HashMap<Integer, String> allGlosses = new HashMap<Integer, String>(130000);
  ResultSet glossRS = DBConnector.getDB().query(glossQ);
  while (glossRS.next())
    allGlosses.put(glossRS.getInt(1), glossRS.getString(2).replaceAll("\t",
      "    "));
  System.out.println("Fetching glosses done. ");
  // ////////////////////////////////////////////////////////////
  /*
  wordid       | integer           |
  synsetid     | integer           |
  word         | character varying |
  pos          | character varying |
  sensenum     | integer           |
  wordpossense | character varying |
   */
  String nnIDQ = "select * from nounid_bk";
  ArrayList<String> outNN = new ArrayList<String>(150000);
  ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
  while (nnRS.next()){
    int wordID = nnRS.getInt(1);
    int synsetID = nnRS.getInt(2);
    String word = nnRS.getString(3);
    String pos = nnRS.getString(4);
    int senseNum = nnRS.getInt(5);
    String wordpossense = nnRS.getString(6);
    String gloss = allGlosses.get(synsetID);
    outNN.add(new StringBuilder(1000).append(wordID).append("\t").append(
      synsetID).append("\t").append(word).append("\t").append(pos).append("\t")
      .append(senseNum).append("\t").append(wordpossense).append("\t").append(
        gloss).toString());
  }
  String outFileNN = "./data/wnnoun.id2s";
  System.out.println("Writing nouns to " + outFileNN);
  Util.writeFileUserDefinedObject(outFileNN, outNN, false);
  String adjIDQ = "select * from adjid_bk";
  ArrayList<String> outADJ = new ArrayList<String>(50000);
  ResultSet adjRS = DBConnector.getDB().query(adjIDQ);
  while (adjRS.next()){
    int wordID = adjRS.getInt(1);
    int synsetID = adjRS.getInt(2);
    String word = adjRS.getString(3);
    String pos = adjRS.getString(4);
    int senseNum = adjRS.getInt(5);
    String wordpossense = adjRS.getString(6);
    String gloss = allGlosses.get(synsetID);
    outADJ.add(new StringBuilder(1000).append(wordID).append("\t").append(
      synsetID).append("\t").append(word).append("\t").append(pos).append("\t")
      .append(senseNum).append("\t").append(wordpossense).append("\t").append(
        gloss).toString());
  }
  String outFileADJ = "./data/wnadj.id2s";
  System.out.println("Writing adjectives to " + outFileADJ);
  Util.writeFileUserDefinedObject(outFileADJ, outADJ, false);
  DBConnector.getDB().close();
}

/** rawid 901 -> apple*/
public static String getRawWord(int rawID,boolean isNoun) throws SQLException{
  String returnW = "";
  if(!isNoun && rawAdjIDTorawAdj == null){
    rawAdjIDTorawAdj = new HashMap<Integer, String>();
    String sql = "select wordid, word from rawadjid";
    ResultSet rs = DBConnector.getDB().query(sql);
    while (rs.next())
      rawAdjIDTorawAdj.put(rs.getInt(1), rs.getString(2));
  }
  if(isNoun && rawNounIDTorawNoun == null){
    rawNounIDTorawNoun = new HashMap<Integer, String>();
    String sql = "select wordid, word from rawnounid";
    ResultSet rs = DBConnector.getDB().query(sql);
    while (rs.next())
      rawNounIDTorawNoun.put(rs.getInt(1), rs.getString(2));
  }
  returnW =
    isNoun ? rawNounIDTorawNoun.get(rawID) : rawAdjIDTorawAdj.get(rawID);
  return returnW;
}

/** 304778653 -> lazy#a#1, slow#a#5 ... */
public static List<wps> getwps(int synsetid) throws SQLException{
  List<wps> wpsList = new ArrayList<IDHelper.wps>();
  if(ssidToWps == null){
    System.out.println("\nInitializing synset_id To Wps ...");
    ssidToWps = new AutoMap<Integer, List<String>>();
    String adjIDQ = "select wordpossense, synsetid from adjid";
    ResultSet adjRS = DBConnector.getDB().query(adjIDQ);
    while (adjRS.next()){
      ssidToWps.addArrayValue(adjRS.getInt(2), adjRS.getString(1));
    }
    String nnIDQ = "select wordpossense, synsetid from nounid";
    ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
    while (nnRS.next()){
      ssidToWps.addArrayValue(nnRS.getInt(2), nnRS.getString(1));
    }
  }
  List<String> rawwps = ssidToWps.get(synsetid);
  for(String raw: Util.nullableIter(rawwps))
    wpsList.add(new wps(raw));
  return wpsList;
}

/** 304778653, slow -> slow#a#5 ... */
public static wps getwps(int synsetid,String rawword) throws SQLException{
  for(wps wpsCand: getwps(synsetid))
    if(wpsCand.w.equalsIgnoreCase(rawword)) return wpsCand;
  return null;
}

/** car -> car#n#1, car#n#2 ... , sweet -> sweet#a#1 ... 
 Since a word can be both adj and noun (e.g. red) thus the parameter isNoun is used 
 */
public static List<wps> getwps(String word,boolean isNoun) throws SQLException{
  List<wps> wpsList = new ArrayList<IDHelper.wps>();
  if(nounwordToWps == null || adjwordToWps == null){
    Timer timer = new Timer();
    System.out.print("\nInitializing word To Wps ...");
    adjwordToWps = new AutoMap<String, List<String>>();
    String adjIDQ = "select word,wordpossense from adjid";
    ResultSet adjRS = DBConnector.q(adjIDQ);
    while (adjRS.next()){
      adjwordToWps.addArrayValue(adjRS.getString(1), adjRS.getString(2));
    }
    String nnIDQ = "select word, wordpossense from nounid";
    nounwordToWps = new AutoMap<String, List<String>>();
    ResultSet nnRS = DBConnector.q(nnIDQ);
    while (nnRS.next()){
      nounwordToWps.addArrayValue(nnRS.getString(1), nnRS.getString(2));
    }
    timer.time();
  }
  if(isNoun && !nounwordToWps.containsKey(word.replaceAll(" ", "_"))){ return wpsList; } // if
                                                                                         // word
                                                                                         // absent.
  if(!isNoun && !adjwordToWps.containsKey(word)){ return wpsList; } // if word
                                                                    // absent.

  List<String> rawwps =
    isNoun ? nounwordToWps.get(word.replaceAll(" ", "_")) : adjwordToWps
      .get(word); // ex. of raw: "sports car#n#1" and
  // not "sports_car#n#1"
  for(String raw: rawwps)
    wpsList.add(new wps(raw));
  return wpsList;
}

/** 29873 -> lazy#a#1 ... */
public static wps getwps(int wordid,boolean iswordIDDummy,boolean isNoun)
  throws SQLException{
  if(nounwordidToWps == null || adjwordidToWps == null){
    System.out.println("\nInitializing wordid To Wps ...");
    adjwordidToWps = new AutoMap<Integer, String>();
    String adjIDQ = "select wordpossense, wordid from adjid";
    ResultSet adjRS = DBConnector.getDB().query(adjIDQ);
    while (adjRS.next()){
      adjwordidToWps.put(adjRS.getInt(2), adjRS.getString(1));
    }
    nounwordidToWps = new AutoMap<Integer, String>();
    String nnIDQ = "select wordpossense, wordid from nounid";
    ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
    while (nnRS.next()){
      nounwordidToWps.put(nnRS.getInt(2), nnRS.getString(1));
    }
  }
  String rawwps =
    isNoun ? nounwordidToWps.get(wordid) : adjwordidToWps.get(wordid);
  if(rawwps == null || !rawwps.contains("#")) return null;
  return new wps(rawwps);
}

public static int getsensenum(int wordid,boolean isNoun) throws SQLException{
  if(nounidToSensenum == null || adjidToSensenum == null){
    System.out.println("\nInitializing wordid To sensenum ...");
    adjidToSensenum = new AutoMap<Integer, Integer>();
    String adjIDQ = "select sensenum,wordid from adjid";
    ResultSet adjRS = DBConnector.getDB().query(adjIDQ);
    while (adjRS.next())
      adjidToSensenum.put(adjRS.getInt(2), adjRS.getInt(1));

    nounidToSensenum = new AutoMap<Integer, Integer>();
    String nnIDQ = "select sensenum, wordid from nounid";
    ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
    while (nnRS.next())
      nounidToSensenum.put(nnRS.getInt(2), nnRS.getInt(1));

  }
  if(wordid < 0) return 0;
  if(isNoun){
    if(!nounidToSensenum.containsKey(wordid))
      return 0;
    else return nounidToSensenum.get(wordid);
  } else{
    if(!adjidToSensenum.containsKey(wordid))
      return 0;
    else return adjidToSensenum.get(wordid);
  }
}

private static Map<String, Integer> numnounsenses;
private static Map<String, Integer> numadjsenses;

public static int countSenses(String wordNo_,boolean isNoun)
  throws SQLException{
  if(numnounsenses == null){
    numnounsenses = new HashMap<>();
    ResultSet rs = DBConnector.q("select word,n from __numsensenoun");
    while (rs.next())
      numnounsenses.put(rs.getString(1), rs.getInt(2));
  }
  if(numadjsenses == null){
    numadjsenses = new HashMap<>();
    ResultSet rs = DBConnector.q("select word,n from __numsenseadj");
    while (rs.next())
      numadjsenses.put(rs.getString(1), rs.getInt(2));
  }

  if(isNoun)
    return (numnounsenses.containsKey(wordNo_)) ? numnounsenses.get(wordNo_)
      : 0;
  else return (numadjsenses.containsKey(wordNo_)) ? numadjsenses.get(wordNo_)
    : 0;

}

/** car -> car#n#1, car#n#2 ... , sweet -> sweet#a#1 ... 
 Since a word can be both adj and noun (e.g. red) thus the parameter isNoun is used 
 */
/*
public static List<wps> getwps(String word,boolean isNoun) throws SQLException{
List<wps> wpsList = new ArrayList<IDHelper.wps>();
if(nounwordToWps == null || adjwordToWps == null){
 System.out.println("Initializing word To Wps ...");
 adjwordToWps = new AutoMap<String, List<String>>();
 String adjIDQ = "select word,wordpossense from adjid";
 ResultSet adjRS = DBConnector.getDB().query(adjIDQ);
 while (adjRS.next()){
   adjwordToWps.addArrayValue(adjRS.getString(1), adjRS.getString(2));
 }
 String nnIDQ = "select word, wordpossense from nounid";
 nounwordToWps = new AutoMap<String, List<String>>();
 ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
 while (nnRS.next()){
   nounwordToWps.addArrayValue(nnRS.getString(1), nnRS.getString(2));
 }
}
if(isNoun) word = word.replaceAll(" ", "_");
if(isNoun && !nounwordToWps.containsKey(word)){ return wpsList; } // if word absent.
if(!isNoun && !adjwordToWps.containsKey(word)){ return wpsList; } // if word absent.
// ex. of raw: "sports car#n#1" and not "sports_car#n#1":
// Note nounid contains no spaced words, so replace space with _
List<String> rawwps = isNoun ? nounwordToWps.get(word) : adjwordToWps.get(word);

for(String raw: Util.nullableIter(rawwps))
 wpsList.add(new wps(raw));
return wpsList;
}

*//** 29873 -> lazy#a#1 ... */
/*
public static wps getwps(int wordid,boolean iswordIDDummy,boolean isNoun) throws SQLException{
if(nounwordidToWps == null || adjwordidToWps == null){
System.out.println("Initializing wordid To Wps ...");
adjwordidToWps = new AutoMap<Integer, String>();
String adjIDQ = "select wordpossense, wordid from adjid";
ResultSet adjRS = DBConnector.getDB().query(adjIDQ);
while (adjRS.next()){
adjwordidToWps.put(adjRS.getInt(2), adjRS.getString(1));
}
nounwordidToWps = new AutoMap<Integer, String>();
String nnIDQ = "select wordpossense, wordid from nounid";
ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
while (nnRS.next()){
nounwordidToWps.put(nnRS.getInt(2), nnRS.getString(1));
}
}
if(nounwordidToWps == null || adjwordidToWps == null) return new wps("", 'a', 0);
String rawwps = isNoun ? nounwordidToWps.get(wordid) : adjwordidToWps.get(wordid);
if(rawwps == null || rawwps.length() == 0) return new wps("", 'a', 0);
return new wps(rawwps);
}
*/
public static void main(String[] args){
  try{
    System.out.println("adj  <red,3>: " + getwps("red", false));
    System.out.println("noun <red,4>: " + getwps("red", true));
    System.out.println("adj <savory#a#3>: " + getwps(21911, true, false));
    System.out.println("noun <sylvilagus_aquaticus#n#1>: "
      + getwps(21911, true, true));
  } catch (SQLException e){
    e.printStackTrace();
  }
}

/** lazy#a#1 -> 304778653, 29873 */
public static IDSubjObj idMeta(wps wps) throws SQLException{
  if(wpsToID == null){
    Timer timer = new Timer();
    System.out.print("\nInitializing wps To ID ...");
    wpsToID = new HashMap<util.IDHelper.wps, IDSubjObj>();
    String adjIDQ = "select wordpossense,synsetid, wordid from adjid";
    ResultSet adjRS = DBConnector.q(adjIDQ);
    while (adjRS.next()){
      wpsToID.put(new wps(adjRS.getString(1)), new IDSubjObj(adjRS.getInt(2),
        adjRS.getInt(3)));
    }
    String nnIDQ = "select wordpossense,synsetid, wordid from nounid";
    ResultSet nnRS = DBConnector.q(nnIDQ);
    while (nnRS.next()){
      wpsToID.put(new wps(nnRS.getString(1)), new IDSubjObj(nnRS.getInt(2),
        nnRS.getInt(3)));
    }
    timer.time();
  }
  if(wpsToID.containsKey(wps))
    return wpsToID.get(wps);
  else return new IDSubjObj(-1, -1);
}

public static IDSubjObj getTextMetaSlowly(int wordID,String tb)
  throws SQLException{
  String nnIDQ =
    "select word,wordpossense,gloss from " + tb + " where wordid=" + wordID;
  ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
  if(nnRS.next()){ return new IDSubjObj(nnRS.getString("wordpossense"), nnRS
    .getString("word"), nnRS.getString("gloss")); }
  return null;
}

public static IDSubjObj getTextMetaSlowly(String nwordpossense,String tb)
  throws SQLException{
  if(tb.startsWith("noun") && nwordpossense.contains(" "))
    nwordpossense = nwordpossense.replaceAll(" ", "_");
  String nnIDQ =
    "select word,gloss from " + tb + " where wordpossense='" + nwordpossense
      + "'";
  ResultSet nnRS = DBConnector.getDB().query(nnIDQ);
  if(nnRS.next()){ return new IDSubjObj(nwordpossense, nnRS.getString("word"),
    nnRS.getString("gloss")); }
  return null;
}

public static class wps {

static final String spaceStr = " ";
static final String _Str = "_";
public static final String sharpStr = "#";

public String w;
public POS p;
public int s;

public wps(String w,char p,int s) {
  constructwps(w, p, s);
}

private void constructwps(String w,char p,int s){
  this.w = w.toLowerCase();
  this.s = s;
  setTag(p);
  if(this.p.equals(POS.NOUN) && w.contains(spaceStr))
    this.w = this.w.replaceAll(spaceStr, _Str);
  else if(this.p.equals(POS.ADJECTIVE))
    this.w = this.w.replaceAll(_Str, spaceStr); // adjectives retain spaces.
}

/** @param wps bird#n#2 */
public wps(String wps) {
  // wps.split("#")[0], wps.split("#")[1].charAt(0),
  // Integer.parseInt(wps.split("#")[2])
  String[] splitted = wps.split(sharpStr);
  String w = splitted[0];
  char p = splitted[1].charAt(0);
  int s = Integer.parseInt(splitted[2]);
  constructwps(w, p, s);
}

@Override public boolean equals(Object obj){
  if(this == obj) return true;
  if(obj == null) return false;
  if(!(obj instanceof wps)) return false;
  final wps other = (wps) obj;
  if(!w.equals(other.w)) return false;
  if(!p.equals(other.p)) return false;
  if(s != other.s) return false;
  return true;
}

@Override public int hashCode(){
  final int PRIME = 31;
  int result = 1;
  result = PRIME * result + w.hashCode();
  result = PRIME * result + p.hashCode();
  return result;
}

private void setTag(char pos){
  switch (pos) {
  case 'a':
  case 's':
  p = POS.ADJECTIVE;
  break;
  case 'v':
  p = POS.VERB;
  break;
  case 'r':
  p = POS.ADVERB;
  break;
  case 'n':
  p = POS.NOUN;
  break;
  default:
  break;
  }
}

@Override public String toString(){
  return new StringBuilder(40).append(w).append('#').append(p.getTag()).append(
    '#').append(s).toString();
}
}
public static class IDSubjObj {

public String wordpossense;
public String word;
public String gloss;
public int synset;
public int wordID;

public IDSubjObj(int synset,int wordid) {
  this("", "", "", synset, wordid);
}

public IDSubjObj(String wordpossense,String word,String gloss) {
  this(wordpossense, word, gloss, -1, -1);
}

public IDSubjObj(String wordpossense,String word,String gloss,int synset,
  int wordid) {
  this.wordpossense = wordpossense;
  this.word = word;
  this.gloss = gloss;
  this.synset = synset;
  this.wordID = wordid;
}
}

public static class WNWords {
private static AutoMap<String, List<String>> wnWordsNo_;

/**
 * If the word is in wordnet returns list of pos it occurs with, else null
 * @param w e.g. potato
 * @return  a: adjective,  v: verb,  n: noun,  r: adverb
 * @throws SQLException during initialization of wordnet (words, pos) map
 */
public static List<String> inWN(String w) throws SQLException{
  if(wnWordsNo_ == null){
    wnWordsNo_ = new AutoMap<>();
    ResultSet rs =
      DBConnector.q("select lower(word), ss_type from wordnet.wn_synsets ");
    while (rs.next()){
      String pos = rs.getString(2).equals("s") ? "a" : rs.getString(2);
      wnWordsNo_.addArrayValueNoRepeat(rs.getString(1), pos);
    }
  }
  return wnWordsNo_.get(w);
}
}

/**
 * Represents part of speech objects.
 * 
 * @author Mark A. Finlayson
 * @version 2.2.3
 * @since JWI 2.0.0
 */
public enum POS {

  /** 
   * Object representing the Noun part of speech.
   *
   * @since JWI 2.0.0
   */
  NOUN("noun", 'n', 1, "noun"),

  /** 
   * Object representing the Verb part of speech.
   *
   * @since JWI 2.0.0
   */
  VERB("verb", 'v', 2, "verb"),

  /** 
   * Object representing the Adjective part of speech.
   *
   * @since JWI 2.0.0
   */
  ADJECTIVE("adjective", 'a', 3, "adj", "adjective"),

  /** 
   * Object representing the Adverb part of speech.
   *
   * @since JWI 2.0.0
   */
  ADVERB("adverb", 'r', 4, "adv", "adverb");

  // standard WordNet numbering scheme for parts of speech
  public static final int NUM_NOUN = 1;
  public static final int NUM_VERB = 2;
  public static final int NUM_ADJECTIVE = 3;
  public static final int NUM_ADVERB = 4;
  public static final int NUM_ADJECTIVE_SATELLITE = 5;

  // standard character tags for the parts of speech
  public static final char TAG_NOUN = 'n';
  public static final char TAG_VERB = 'v';
  public static final char TAG_ADJECTIVE = 'a';
  public static final char TAG_ADVERB = 'r';
  public static final char TAG_ADJECTIVE_SATELLITE = 's';

  // final instance fields
  private final String name;
  private final char tag;
  private final int num;
  private final Set<String> filenameHints;

  // private constructor
  private POS(String name,char tag,int type,String... patterns) {
    this.name = name;
    this.tag = tag;
    this.num = type;
    this.filenameHints =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(patterns)));
  }

  /**
   * Returns a set of strings that can be used to identify resource
   * corresponding to objects with this part of speech.
   * 
   * @return an immutable set of resource name hints
   * @since JWI 2.2
   */
  public Set<String> getResourceNameHints(){
    return filenameHints;
  }

  /**
   * The tag that is used to indicate this part of speech in Wordnet data
   * files
   * 
   * @return the character representing this part of speech
   * @since JWI 2.0.0
   */
  public char getTag(){
    return tag;
  }

  /** 
   * Returns the standard WordNet number of this part of speech
   *
   * @return the standard WordNet number of this part of speech 
   * @since JWI 2.0.0
   */
  public int getNumber(){
    return num;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString(){
    return name;
  }

  /**
   * Returns <code>true</code> if the specified number represents an adjective
   * satellite, namely, if the number is 5; <code>false</code> otherwise
   * 
   * @param num
   *            the number to be checked
   * @return <code>true</code> if the specified number represents an adjective
   *         satellite, namely, if the number is 5; <code>false</code> otherwise
   * @since JWI 2.0.0
   */
  public static boolean isAdjectiveSatellite(int num){
    return num == 5;
  }

  /**
   * Returns <code>true</code> if the specified character represents an
   * adjective satellite, namely, if the number is 's' or 'S';
   * <code>false</code> otherwise
   * 
   * @param tag
   *            the character to be checked
   * @return <code>true</code> if the specified number represents an adjective
   *         satellite, namely, if the number is 's' or 'S';
   *         <code>false</code> otherwise
   * @since JWI 2.0.0
   */
  public static boolean isAdjectiveSatellite(char tag){
    return tag == 's' || tag == 'S';
  }

  /**
   * Retrieves the part of speech object given the number.
   * 
   * @param num
   *            the number for the part of speech
   * @return POS the part of speech object corresponding to the specified tag,
   *         or <code>null</code> if none is found
   * @since JWI 2.0.0
   */
  public static POS getPartOfSpeech(int num){
    switch (num) {
    case (1):
    return NOUN;
    case (2):
    return VERB;
    case (4):
    return ADVERB;
    case (5): // special case, '5' for adjective satellite, fall through
    case (3):
    return ADJECTIVE;
    }
    return null;
  }

  /**
   * Retrieves of the part of speech object given the tag. Accepts both lower
   * and upper case characters.
   * 
   * @param tag
   * @return POS the part of speech object corresponding to the specified tag,
   *         or null if none is found
   * @since JWI 2.0.0
   */
  public static POS getPartOfSpeech(char tag){
    switch (tag) {
    case ('N'): // capital, fall through
    case ('n'):
    return NOUN;
    case ('V'): // capital, fall through
    case ('v'):
    return VERB;
    case ('R'): // capital, fall through
    case ('r'):
    return ADVERB;
    case ('s'): // special case, 's' for adjective satellite, fall through
    case ('S'): // capital, fall through
    case ('A'): // capital, fall through
    case ('a'):
    return ADJECTIVE;
    }
    return null;
  }
}

}
