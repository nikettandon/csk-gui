package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javatools.filehandlers.FileLines;
import util.AutoMap;
import util.DBConnector;
import util.IDHelper.WNWords;
import util.Util;

public class Autocompletion {

/** Like set expansion, fetch partner term from db (Input = CSV ; bike, works). 
 * For bike, return bike,car; bike,bus,..
   TODO autocompletion can be done over any field incl. relation or adjective
 * @param xCSVyOpt e.g. car,b ;   should be autocompleted with car,bike; car,bus..
 * @param max e.g. 20 maximum auto suggestions in dropdown.
 * @return autosuggestion y.
 */
public static List<String> y(String xCSVyOpt,int max){
  // System.out.print("\nAutocompletion for " + xCSVyOpt + " started ... ");
  List<String> y = new ArrayList<String>();
  // TODO adding dummy suggestion; GUI not clearing old suggestion
  // y.add(xCSVyOpt);
  y.add(" ");

  if(xCSVyOpt == null || xCSVyOpt.length() == 0) return y;
  // Generally input like: car,b; car,;car,bike are considered complete
  if(!isXComplete(xCSVyOpt)){
    x(y, xCSVyOpt, max);
    return y;
  }
  // System.out.print(" .. moving foward ... ");
  // Input = [car,] -> [car, ]
  xCSVyOpt += " ";
  String[] formatted = xCSVyOpt.split(",");
  if(formatted.length != 2) return y;
  String xIn = formatted[0].trim();
  String yIn = formatted[1].trim();

  // For bike, construct car,bus,..
  try{
    // car,bus,.. are already sorted by freq
    // TODO sort by #adj support.
    if(Autocompletion.autoCompletionXY.containsKey(xIn)){
      for(String aY: Autocompletion.autoCompletionXY.get(xIn)){
        if(--max < 0) break;
        // car,b .. should be autocompleted with car,bike; car,bus..
        if((yIn.length() > 0 && aY.startsWith(yIn)) || yIn.length() == 0)
          y.add(xIn + "," + aY);
      }
    }
  } catch (Exception e){
    System.out.println("Exception during autocomplete suggestions of "
      + xCSVyOpt + "\n\tdetail: " + e.getMessage());
  }
  // System.out.println(" .. returning  " + y);
  return y;
}

public static List<String> event(String x,int max){

  // System.out.print("\nAutocompletion for " + xCSVyOpt + " started ... ");
  List<String> y = new ArrayList<String>();
  // TODO adding dummy suggestion; GUI not clearing old suggestion
  // y.add(xCSVyOpt);
  y.add(" ");

  if(x == null || x.length() == 0) return y;

  try{

    for(String myX: Util.nullableIter(Autocompletion.autoCompletionEvent
      .autoComplete(x))){
      y.add(myX);
      if(--max < 0) break;
    }

  } catch (Exception e){
    System.out.println("Exception during autocomplete suggestions of " + x
      + "\n\tdetail: " + e.getMessage());
  }
  return y;
}

public static void x(List<String> y,String xCSVyOpt,int max){
  for(String myX: Util.nullableIter(Autocompletion.autoCompletionX
    .autoComplete(xCSVyOpt))){
    y.add(myX);
    if(--max < 0) break;
  }
}

private static boolean isXComplete(String x){
  int numCommas = Util.countChar(x, ',', false);
  return numCommas == 1;
}

public static void initAbsolCSKSortedByNumPatterns(String cachedNounFile)
  throws Exception{
  System.out.print("Loading autocompletion xHead pairs from _hasprop...");

  if(!Util.exists(cachedNounFile)){
    List<String> cache = new ArrayList<>();
    List<Character> ignorables =
      Arrays.asList(new Character[] { ' ', '-', '_' });
    String sql = "select distinct(x) from _hasprop ";
    ResultSet rs = DBConnector.q(sql);
    String nounPOS = "n";

    while (rs.next()){
      String noun = rs.getString(1);
      if(noun.length() > 2
        && !Util.hasSpecialChars(rs.getString(1), ignorables) && !isStop(noun)){
        if(WNWords.inWN(noun) != null){
          if(WNWords.inWN(noun).contains(nounPOS)) cache.add(noun);
        }
      }
    }
    Util.writeFile(cachedNounFile, cache, false);
  }
  if(Autocompletion.autoCompletionX == null)
    Autocompletion.autoCompletionX = new Trie();
  for(String l: new FileLines(cachedNounFile))
    autoCompletionX.insert(l);

  System.out.println("  [done]");
}

// TODO
public static void initACSortedByNumAdj(){
  // Prefetching for faster and cleaner set expansion.
  // Local prefetch map : car -> bike,10; bus,20; train,15...
  AutoMap<String, List<String>> mInit = new AutoMap<String, List<String>>();
  AutoMap<String, Double> mInitFreq = new AutoMap<String, Double>();
  System.out.print("Loading autocompletion xnorm,ynorm pairs ...");
  Autocompletion.autoCompletionX = new Trie();

  // Prefetch limited (xnorm=x;ynorm=y) suggestions for efficiency.
  String sqlSortByNumAdj =
    "select xnorm,ynorm,adj,freq from __csynsetinput "
      + "where xnorm=x and ynorm=y and freq>=1 order by freq desc";
  try{
    // Car,bus,.. are sorted by freq
    ResultSet rs = DBConnector.q(sqlSortByNumAdj);
    while (rs.next()){
      String a = rs.getString(1);
      String b = rs.getString(2);
      String adj = rs.getString(3);
      // Avoid car,this; this,that
      if(isStop(a) || isStop(b)) continue;

      mInit.addArrayValueNoRepeat(a + "\t" + b, adj);
      mInit.addArrayValueNoRepeat(b + "\t" + a, adj);

      mInitFreq.addNumericValue(a + "\t" + b, rs.getDouble(4));
      mInitFreq.addNumericValue(b + "\t" + a, rs.getDouble(4));

      Autocompletion.autoCompletionX.insert(a);
      Autocompletion.autoCompletionX.insert(b);
    }
  } catch (SQLException e){
    System.out.println("Exception during autocomplete list initialization"
      + "\n\tdetail: " + e.getMessage());
  }

  AutoMap<String, Integer> mTemp = new AutoMap<>();
  /*  for(Entry<String, List<String>> e: mInit.entrySet())
      mTemp.put(e.getKey(), e.getValue().size());
  */
  // LinkedHashMap<String, Integer> mTemp = new LinkedHashMap<>();

  for(Entry<String, List<String>> e: mInit.entrySet()){
    // LOGIC Min support = 1; weightage of 5 (adhoc) to adj support.
    int adjSup = e.getValue().size();
    if(adjSup >= 1){
      // It matters a lot if x,y occur with multiple adj contexts
      int boostedAdjSup = adjSup > 1 ? 5 * adjSup : adjSup;
      // It matters little if x,y occur with 1 adj contexts too many times
      // e.g. larger than "life".. // better than "sex".
      int loweredFreq = mInitFreq.get(e.getKey()).intValue();
      loweredFreq =
        (loweredFreq > 10 && boostedAdjSup == 1) ? loweredFreq / 5
          : loweredFreq;
      mTemp.put(e.getKey(), boostedAdjSup * loweredFreq);
    }
  }

  // car, bus->10.. man,woman,9.. ...
  TreeMap<String, Integer> m = mTemp.sortByValue();
  /* for(Entry<String, Integer> e: m.entrySet()){
     if(e.getKey().contains("tiger"))
       System.out.println(e.getKey() + "\t" + e.getValue());
   }*/

  Autocompletion.autoCompletionXY = new AutoMap<>();
  // Load WebchildBrowser.autoCompletionList with #Adj,freq sorted.
  // from: car -> bike,10; bus,20; train,15... to car -> bus,train,bike.
  for(Entry<String, Integer> e: m.entrySet()){
    String[] xy = e.getKey().split("\t");
    if(e.getValue() > 1) // Avoid cases where one adj matched once?
    {
      Autocompletion.autoCompletionXY.addArrayValueNoRepeat(xy[0], xy[1]);
      Autocompletion.autoCompletionXY.addArrayValueNoRepeat(xy[1], xy[0]);
    }
  }

  System.out.println("  loaded");
}

public static void initACSortedByFreq(){
  // Prefetching for faster and cleaner set expansion.
  // Local prefetch map : car -> bike,10; bus,20; train,15...
  AutoMap<String, AutoMap<String, Double>> m = new AutoMap<>();
  System.out.print("Loading autocompletion xnorm,ynorm pairs ...");
  Autocompletion.autoCompletionX = new Trie();

  // Prefetch limited suggestions for efficiency.
  String sql =
    "select xnorm,ynorm,freq from __csynsetinput "
      + "where xnorm=x and ynorm=y and freq>=1";
  try{
    // Car,bus,.. are sorted by freq
    ResultSet rs = DBConnector.q(sql);
    while (rs.next()){
      String a = rs.getString(1);
      String b = rs.getString(2);
      int freq = rs.getInt(3);
      // Avoid car,this; this,that
      if(isStop(a) || isStop(b)) continue;
      // Car,bus and bus,car
      AutoMap.addKeyKeyNumericValue(a, m, b, freq);
      AutoMap.addKeyKeyNumericValue(b, m, a, freq);

      Autocompletion.autoCompletionX.insert(a);
      Autocompletion.autoCompletionX.insert(b);
    }
  } catch (SQLException e){
    System.out.println("Exception during autocomplete list initialization"
      + "\n\tdetail: " + e.getMessage());
    // return;
  }

  Autocompletion.autoCompletionXY = new AutoMap<>();
  // Load WebchildBrowser.autoCompletionList with freq sorted comparables.
  // from: car -> bike,10; bus,20; train,15... to car -> bus,train,bike.
  for(Entry<String, AutoMap<String, Double>> e: m.entrySet()){
    // TODO store in a patricia tree for efficiency.
    // WebchildBrowser.autoCompletionX.insert(e.getKey()); // all x
    for(Entry<String, Double> k: e.getValue().sortByValue().entrySet()){
      // Only if observed with freq>=2 (TODO and in atleast 2 two triples?).
      if(k.getValue() >= 2.0)
        Autocompletion.autoCompletionXY.addArrayValueNoRepeat(e.getKey(), k
          .getKey());
    }
  }

  System.out.println("  loaded");
}

private static boolean isStop(String a){
  if(a.equals("one") || a.equals("none") || a.equals("nothing")
    || a.equals("mine") || a.equals("your") || a.equals("many")
    || Util.isStopWord(a))
    return true;
  else return false;
}

public static class Trie {

protected final Map<Character, Trie> children;
protected String value;
protected boolean terminal = false;

public Trie() {
  this(null);
}

private Trie(String value) {
  this.value = value;
  children = new HashMap<Character, Trie>();
}

protected void add(char c){
  String val;
  if(this.value == null){
    val = Character.toString(c);
  } else{
    val = this.value + c;
  }
  children.put(c, new Trie(val));
}

public void insert(String word){
  if(word == null){ throw new IllegalArgumentException(
    "Cannot add null to a Trie"); }
  Trie node = this;
  for(char c: word.toCharArray()){
    if(!node.children.containsKey(c)){
      node.add(c);
    }
    node = node.children.get(c);
  }
  node.terminal = true;
}

public String find(String word){
  Trie node = this;
  for(char c: word.toCharArray()){
    if(!node.children.containsKey(c)){ return ""; }
    node = node.children.get(c);
  }
  return node.value;
}

public Collection<String> autoComplete(String prefix){
  Trie node = this;
  for(char c: prefix.toCharArray()){
    if(!node.children.containsKey(c)){ return Collections.emptyList(); }
    node = node.children.get(c);
  }
  return node.allPrefixes();
}

protected Collection<String> allPrefixes(){
  List<String> results = new ArrayList<String>();
  if(this.terminal){
    results.add(this.value);
  }
  for(Entry<Character, Trie> entry: children.entrySet()){
    Trie child = entry.getValue();
    Collection<String> childPrefixes = child.allPrefixes();
    results.addAll(childPrefixes);
  }
  return results;
}
}

public static Trie autoCompletionX,autoCompletionEvent;
// TODO the Autocompletion class could not retain the static list, initialized everytime.
public static AutoMap<String, List<String>> autoCompletionXY;

public static void main(String[] args) throws Exception{
  Autocompletion.initAbsolCSKSortedByNumPatterns(WebchildBrowserInOne.context.getRealPath("/")
    + "WebContent/data/"
    + "preloaded.x.nouns");
  String input = Util.readStringFromUser("noun: ");
  while (input.length() > 0){
    System.out.println(Autocompletion.y(input, 10));
    input = Util.readStringFromUser("noun: ");
  }
  DBConnector.closeConnections();
}
}
