package util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** The commonly used file utilities like reading (txt, bz2..), writing (txt, graphviz images) and iterating over folder.
 * 
 * @author ntandon
 * @version1.0
 * @since 15.01.2010 */
public class FileUtil {

/**
 * Returns the top k elements from the array.
 * @param array of type T.
 * @param k e.g. 5 (top 5)
 * @param comparator
 <PRE> 
 comparator = new Comparator<Integer>() {
  @Override public int compare(Integer o1,Integer o2){
  return Integer.compare(o1, o2);
  }
  }
 </PRE>
 * @return
 */
public static <T> List<T>
  topK(T[] array,int k,Comparator<? super T> comparator){
  PriorityQueue<T> heap = new PriorityQueue<>(k, comparator);
  // Min Heap of k elements is maintained.
  for(int i = 0; i < k; i++)
    heap.add(array[i]);

  for(int i = k; i < array.length; i++){
    // We are sure that the root is not in topk: it has the smallest value.
    if(heap.comparator().compare(heap.peek(), array[i]) == -1){
      heap.remove();
      heap.add(array[i]);
    }
  }

  // The top k are fetched by popping seeds one by one in asc. order.
  List<T> resultAscending = new ArrayList<>(k);
  for(int i = 0; i < k; i++)
    resultAscending.add(heap.remove());

  // Get the top k in the right order (maximum first) by shifting.
  List<T> resultDescending = new ArrayList<>(k);
  for(int i = k - 1; i >= 0; i--)
    resultDescending.add(resultAscending.get(i));

  return resultDescending;
}

public static long usedMem(){
  System.gc();
  return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
}

/**
 * Updates environment variable for the currently running processes.
 * @param newenv
 * 
 */
public static void setEnv(Map<String, String> newenv){
  try{
    Class<?> processEnvironmentClass =
      Class.forName("java.lang.ProcessEnvironment");
    Field theEnvironmentField =
      processEnvironmentClass.getDeclaredField("theEnvironment");
    theEnvironmentField.setAccessible(true);
    Map<String, String> env =
      (Map<String, String>) theEnvironmentField.get(null);
    env.putAll(newenv);
    Field theCaseInsensitiveEnvironmentField =
      processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
    theCaseInsensitiveEnvironmentField.setAccessible(true);
    Map<String, String> cienv =
      (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
    cienv.putAll(newenv);
  } catch (NoSuchFieldException e){
    try{
      Class[] classes = Collections.class.getDeclaredClasses();
      Map<String, String> env = System.getenv();
      for(Class cl: classes){
        if("java.util.Collections$UnmodifiableMap".equals(cl.getName())){
          Field field = cl.getDeclaredField("m");
          field.setAccessible(true);
          Object obj = field.get(env);
          Map<String, String> map = (Map<String, String>) obj;
          map.clear();
          map.putAll(newenv);
        }
      }
    } catch (Exception e2){
      e2.printStackTrace();
    }
  } catch (Exception e1){
    e1.printStackTrace();
  }
}

public static double idf(int totDocs,int myfreq){
  int antilog = totDocs == 0 || myfreq == 0 ? 0 : totDocs / myfreq;
  return antilog <= 0 ? -1.0 : Math.log10(antilog);
}

public static double nChooseKLog(int n,int k){

  if(n < k){ throw new IllegalArgumentException(
    "must have n >= k for binomial coefficient (n,k)"); }
  if(n < 0){ throw new IllegalArgumentException(
    "must have n >= 0 for binomial coefficient (n,k)"); }
  k = Math.min(k, n - k);

  if((n == k) || (k == 0)){ return 0; }
  if((k == 1) || (k == n - 1)){ return Math.log((double) n); }

  double logSum = 0;

  // n!/(n-k)!
  for(int i = 0; i < k; i++){
    logSum += Math.log((double) (n - i));
  }

  // divide by k!
  for(int i = 2; i <= k; i++){
    logSum -= Math.log((double) i);
  }

  return logSum;
}

/** create table if not exists using the tbSchema and load TSV data 
 * @param tbSchemaNoBracket x varchar, xid int, y varchar, score real
 * @param tb new table name
 * @param fileToLoad TSV file values to upload
 * @throws Exception if cannot create table or upload file to Db
 */
public static void dumpToTable(String tbSchemaNoBracket,String tb,
  String fileToLoad) throws Exception{
  String createQ =
    "create table if not exists " + tb + " ( " + tbSchemaNoBracket + " )";
 // DBConnector.getDB().executeUpdate(createQ);
 // DBConnector.load(tb, fileToLoad, false, '\t');
}

/** for (T v: myCollection){} fails with nulls.
 *  for (T v: nullableIter(myCollection)){} would not fail with nulls.
 */
public static <T> Iterable<T> nullableIter(Iterable<T> it){
  return it != null ? it : Collections.<T> emptySet();
}

/** Computes the Wilson Interval (fp+tp, tp) 
 * 
 *  
 * (see http://en.wikipedia.org/wiki/Binomial_proportion_confidence_interval#Wilson_score_interval)
 * Given the total number of events and the number of "correct" events, returns in a double-array
 * in the first component the center of the Wilson interval and in the second component the
 * width of the interval. alpha=95%.
 *  
 */
public static double[] wilson(int total,int correct){
  if(total == 0) return (new double[] { 0, 0 });
  double z = 1.96;
  double p = (double) correct / total;
  double center = (p + 1 / 2.0 / total * z * z) / (1 + 1.0 / total * z * z);
  double d =
    z * Math.sqrt((p * (1 - p) + 1 / 4.0 / total * z * z) / total)
      / (1 + 1.0 / total * z * z);
  return (new double[] { center, d });
}

public static <F> String join(F[] arr,String separator){
  StringBuilder s = new StringBuilder();
  if(arr == null || arr.length == 0) return s.toString();
  for(int i = 0; i < arr.length; i++){
    if(arr[i] != null)
      s.append(i > 0 ? separator : "").append(arr[i].toString());
  }
  return s.toString();
}

public static void writeObjects(String outFile,List<Object> l) throws Exception{
  FileOutputStream fos = new FileOutputStream(new File(outFile));
  ObjectOutputStream streamedWriter = new ObjectOutputStream(fos);
  streamedWriter.writeObject(l);
  streamedWriter.close();
}

public static void printMap(AutoMap<Integer, Set<String>> map,String outFile,
  boolean append){
  ArrayList<String> mapSerialized = new ArrayList<String>(100002);
  int counter = 0;
  Util.writeFile(outFile, mapSerialized, append);
  for(Entry<Integer, Set<String>> e: map.entrySet()){
    StringBuilder sb = new StringBuilder(1000);
    sb.append(e.getKey());
    for(String v: e.getValue())
      sb.append("\t").append(v);
    if(++counter % 100000 == 0){
      Util.writeFile(outFile, mapSerialized, true);
      mapSerialized = new ArrayList<String>(100002);
    }
    mapSerialized.add(sb.toString());
  }
  if(mapSerialized.size() > 0) Util.writeFile(outFile, mapSerialized, true);
}

public static void writeObjects(String outFile,Object l) throws Exception{
  FileOutputStream fos = new FileOutputStream(new File(outFile));
  ObjectOutputStream streamedWriter = new ObjectOutputStream(fos);
  streamedWriter.writeObject(l);
  streamedWriter.close();
}

public static Object readObject(String inFile) throws Exception{
  FileInputStream fis = new FileInputStream(new File(inFile));
  ObjectInputStream streamedReader = new ObjectInputStream(fis);
  Object obj = streamedReader.readObject();
  streamedReader.close();
  return obj;
}

/**  File folderHandle = new File(folderPath); */
public static void iterateOverAllSubFolders(File folderHandle,
  ArrayList<File> children){
  if(!folderHandle.isDirectory()){
    children.add(folderHandle);
    return;
  } else{
    children.add(folderHandle);
    for(File child: folderHandle.listFiles())
      iterateOverAllSubFolders(child, children);
  }
}

public static int percentileIndex(double percentile,int sizeOfSortedArrAsc){
  double a = 0.5 + percentile / 100.0 * sizeOfSortedArrAsc;
  int index = (int) Math.round(a);
  return index;
}

/** Wait n milliseconds */
public static void waiting(double n){
  long t0,t1;
  t0 = System.currentTimeMillis();
  do{
    t1 = System.currentTimeMillis();
  } while (t1 - t0 < n);
}

/** Wait n seconds */
public static void waiting(int n){
  long t0,t1;
  t0 = System.currentTimeMillis();
  do{
    t1 = System.currentTimeMillis();
  } while (t1 - t0 < n * 1000);
}

/** Does not modify the collection itself, returns the intersection Collection */
public static <ColType> Collection<ColType> intersect(Collection<ColType> c1,
  Collection<ColType> c2){
  int l1 = c1 == null ? 0 : c1.size();
  int l2 = c2 == null ? 0 : c2.size();
  if(l1 <= l2) return intersect(c1, c2, true);
  else return intersect(c2, c1, true);
}

/**
 * c1 is smaller than c2
 * @param c1
 * @param c2
 * @param inOrder dummy
 * @return
 */
private static <ColType> Collection<ColType> intersect(Collection<ColType> c1,
  Collection<ColType> c2,boolean inOrder){
  Collection<ColType> returnC = new HashSet<ColType>();
  for(ColType c: nullableIter(c1)){
    if(c2.contains(c)) returnC.add(c);
  }
  return returnC;
}

/** 
 * Regex helper to fill out wildcard(s).
 * @param regex * cars are much * than * 
 *        (other regex possible e.g. cars are much * than trucks)
 * @param input Sports cars are much faster than SUV trucks
 * @return [Sports] [faster] [SUV trucks]
 */
public static List<String> stringsBetween(String regex,String input){
  List<String> o = new ArrayList<String>();
  if(!regex.contains(".*?")) regex = regex.replaceAll("\\(?\\*\\)?", "(.*?)");
  Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
  Matcher m = p.matcher(input);
  int gid = 1;
  int numHits = 0;
  if(m.find()) numHits = m.groupCount();
  if(numHits > 0){
    while (gid <= numHits){
      if(m.group(gid).length() > 0) o.add(m.group(gid));
      gid++;
    }
  }

  // a portion of starting and ending text should be matched when there are
  // trailing *.
  if(o.size() > 1){
    String starting = substring(1, false, o.get(0));
    o.set(0, starting);

    String ending = substring(2, true, o.get(numHits - 1));
    o.set(o.size() - 1, ending);
  }

  return o;
}

/**
 * Regex helper to fill out wildcard.
 * @param regex cars are much * than trucks
 * @param input Sports cars are much faster than SUV trucks
 * @return [faster]
 */
public static String stringBetween(String regex,String input){
  String o = "";
  if(!regex.contains(".*?")) regex = regex.replaceAll("\\(?\\*\\)?", "(.*?)");
  Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
  Matcher m = p.matcher(input);
  int gid = 0;
  if(m.find()){
    o = m.group(1);
  }
  return o;
}

/**
 * Select first few/ last few words from a string.
 * @param howMany e.g. 2
 * @param fromBeginning e.g. true
 * @param str e.g. One two three four
 * @return e.g. One two
 */
public static String substring(int howMany,boolean fromBeginning,String str){
  String[] s = str.split(" ");
  StringBuilder builder = new StringBuilder();
  int counter = 0;
  int startIndex = fromBeginning ? 0 : s.length - howMany;
  if(startIndex < 0) startIndex = 0;
  for(int k = startIndex; (counter++ < howMany) && k < s.length; k++){
    builder.append(" ").append(s[k]);
  }
  return builder.toString().trim();
}

public static void splitList(Collection input,Collection part1,
  Collection part2,double percentInFirst){
  if(percentInFirst > 1 || percentInFirst < 0){
    System.out
      .println("Exception: Cannot split(percent of first list) in the given ratio "
        + percentInFirst + " , which is not between 0 and 1");
    return;
  }
  if(part1 == null) part1 = new ArrayList();
  if(part2 == null) part2 = new ArrayList();
  int aMax = (int) (input.size() * percentInFirst);
  int counter = 0;
  for(Object o: input){
    if(counter < aMax){
      part1.add(o);
      counter++;
    } else{
      part2.add(o);
    }
  }
}

private static <V> void checkProportions(Collection<V> input,
  double[] proportions) throws Exception{
  // Checks
  if(proportions == null || proportions.length == 0)
    throw new Exception("Cannot split list: no proportion vector provided ");
  for(int i = 0; i < proportions.length; i++){
    if(proportions[i] > 1 || proportions[i] < 0)
      throw new Exception(
        "Cannot split(percent of first list) in the given ratio "
          + proportions[i] + " , which is not between 0 and 1");
  }
  if(sumVec(proportions) > 1.0)
    throw new Exception("Cannot split list: proportions add up "
      + " to more than 1 ");
  if(input == null || input.size() < proportions.length)
    throw new Exception("Cannot split list: input.size (" + input.size()
      + ") < proportions.length (" + proportions.length + ")");
}

public static double sumVec(double[] v){
  double sum = 0;
  for(int i = 0; i < v.length; i++)
    sum += v[i];
  return sum;
}

public static int sumVec(int[] v){
  int sum = 0;
  for(int i = 0; i < v.length; i++)
    sum += v[i];
  return sum;
}

public static <V> List<Collection<V>> splitList(List<V> input,
  double[] proportions) throws Exception{
  checkProportions(input, proportions);

  // Size of each splitted list.
  int[] sizes = new int[proportions.length];
  int inputsize = input.size();
  for(int i = 0; i < sizes.length; i++){
    int roundedSize = (int) Math.round(proportions[i] * inputsize);
    sizes[i] = roundedSize;
  }
  if(sumVec(sizes) < input.size()){
    // input= {2,3,4,5,6,7}, prop = {0.4,0.4,0.2}
    // due to size rounding 2+2+1 =5 elements returned
    int lastIdxVal = sizes[sizes.length - 1];
    while (sumVec(sizes) < input.size()){
      lastIdxVal++;
      sizes[sizes.length - 1] = lastIdxVal;
    }
  }

  // Store in lists.
  List<Collection<V>> o = new ArrayList<Collection<V>>();
  int offset = 0;
  for(int i = 0; i < sizes.length; i++){
    for(int v = offset; v < offset + sizes[i] && v < input.size(); v++){
      // Reach the right offset (--1,2,-- 3,4,-- 5)
      if(o.size() <= i) o.add(new ArrayList<V>());
      o.get(i).add(input.get(v));
    }
    offset += sizes[i];
  }

  return o;
}

public static void main(String[] args) throws IOException{
  //Timer timer = new Timer();

  // <html> returns ""
  // <u>abcd</u> returns abcd
/*  System.out.println(Util.htmlDataBtwTag("<u>abc d </u>"));
  System.out.println(Util.htmlDataBtwTag("<html>"));
  System.out.println(Util.htmlDataBtwTag("<uabcd</u>"));
  System.out.println(Util.htmlDataBtwTag(""));
  System.out.println(Util.htmlDataBtwTag("<html>"));
  System.out.println(Util.htmlDataBtwTag("html"));*/

  /*Map<String, String> envVarsToAdd = new HashMap<>();
  envVarsToAdd
    .put("GUROBI_HOME", "/var/tmp/important-gurobi/gurobi560/linux64");
  envVarsToAdd.put("LD_LIBRARY_PATH",
    "/var/tmp/important-gurobi/gurobi560/linux64/lib");
  envVarsToAdd.put("GRB_LICENSE_FILE",
    "/var/tmp/important-gurobi/gurobi560/gurobi.lic");
  Util.setEnv(envVarsToAdd);*/

  // System.out.println(removeStopWords("lrb come on go to his lrb place today"));
  // System.out.println(Util.percentNonWords("this12 good."));

  /*String input =
    "<P><FONT color=#996633><BR><STRONG>ARTICLE IV: Elections and Resignations</STRONG><BR>Section 1<SPAN class=GramE>)</SPAN><BR>The President and Vice-President of South Asia shall hold office for a term of two years, and may be re-elected. The Secretary and Treasurer shall hold office for two years, and may be re-elected. The other elected members of the executive committee shall serve staggered two-year terms. Elections are to be held in odd and even calendar years, at which time all officers with expiring terms and two/three (roughly half) of the members of the executive committee shall stand for election. No member of the executive committee (the Editor and President are exempt they may serve three consecutive terms) may serve more than two consecutive terms in office.<BR><BR>Section 2<SPAN class=GramE>)</SPAN><BR>The Editor shall be appointed for a term agreed upon by majority vote of the governing executive board of the ASAEH, and shall serve at the pleasure of the board and President. In the event that the Editor is unable to perform his/her duties, the President of ASAEH, in consultation with the executive board, shall make the necessary temporary arrangements for a replacement. Prior to the expiration of the Editor's term, a new Editor shall be selected by a joint search committee of the organization for approval as provided in this section.<BR><BR>Section 3)<BR>One hundred and twenty days prior to the annual meetings, when Association elections shall normally take place, a Nominating Committee, elected by the membership, shall meet or shall communicate in some other fashion and shall select a slate of officers and elected members of the executive committee. The candidate for President shall normally be the standing Vice-President of South Asia. The Nominating Committee shall consist of four members to serve six years each, with two members being elected from the executive committee. The slate they select shall consist of one nominee for Vice-President of South Asia, the next President-elect, two/three nominees for other officers with expiring terms, the International Vice-President, and two nominees for each vacancy on the executive and Nominating Committee, and shall be communicated to the Secretary, who shall in turn communicate it to the membership by mail as provided in Article II, Section 4 of this constitution. Elections shall take place during the annual <SPAN class=GramE>meetings,</SPAN> or by mail (or email) ballot prior to the meeting. The candidates receiving the highest number of votes are elected. All ballots shall have provisions for write-in nominees.<BR><BR>Section 4<SPAN class=GramE>)</SPAN><BR>Elections may be held by mail ballot if circumstances warrant. A decision to do so may be made by the President and any four other members of the executive committee. This same group may make such other alterations in the election procedure as they deem necessary, except that <SPAN class=GramE>an</SPAN> slate of nominees must be selected by the Nominating Committee, and the Secretary must provide notice of the election to the membership in the usual fashion. All terms of office shall begin and end the day after the stated and scheduled annual meeting election.</FONT></P>";
  Pattern p = Pattern.compile("^(.*?) more (.*?) than (.*?)$");
  List<String> o = new ArrayList<String>();
  Matcher m = p.matcher(input);
  int gid = 1;
  int numHits = 0;
  if(m.find()) numHits = m.groupCount();
  if(numHits > 0){
    while (gid <= numHits){
      if(m.group(gid).length() > 0) o.add(m.group(gid));
      gid++;
    }
  }
  // a portion of starting and ending text should be matched when there are trailing *.
  if(o.size() > 1){
    String starting = Util.substring(10, false, o.get(0));
    o.set(0, starting);

    String ending = Util.substring(10, true, o.get(o.size() - 1));
    o.set(o.size() - 1, ending);
  }*/

  //timer.time();
}

public static <T extends Number> double l2norm(Collection<T> values){
  double v = 0;
  for(T value: values){
    v += Math.pow((Double) value, 2);
  }
  return Math.pow(v, 0.5);
}

public static double mean(ArrayList<Double> numbers){
  double sum = 0;
  for(double x: numbers)
    sum += x;
  return sum / numbers.size();
}

/**
 * Retrive the quartile value from an array 
 * @param values The array of data
 * @param lowerPercent The percent cut off. For the lower quartile use 25,
 *      for the upper-quartile use 75, for median use 50
 * @return
 */
public static double quartile(double[] values,double lowerPercent){
  if(values == null || values.length == 0){ throw new IllegalArgumentException(
    "The data array either is null or does not contain any data."); }
  if(values.length == 1) return values[0];
  double[] v = new double[values.length];
  System.arraycopy(values, 0, v, 0, values.length);
  Arrays.sort(v);
  double actualN = v.length * lowerPercent / 100 - 1;
  int roundedN = (int) Math.round(actualN);
  // Note: Element 13 would be at index v[12] since v starts from 0.
  // Thought of: 13.2 and mimic http://www.miniwebtool.com/outlier-calculator/;
  // then take 13 and 14
  // ==> dropped the
  // idea;its not logical.
  return v[roundedN];
}

public static double interQuartile(double[] values){
  if(values == null || values.length == 0){ throw new IllegalArgumentException(
    "The data array either is null or does not contain any data."); }
  double lQR = quartile(values, 25);
  double uQR = quartile(values, 75);
  return uQR - lQR;
}

public static ArrayList<Double> findOutliers(double[] values){
  if(values == null || values.length == 0){ throw new IllegalArgumentException(
    "The data array either is null or does not contain any data."); }
  ArrayList<Double> o = new ArrayList<Double>();
  double q1 = quartile(values, 25);
  // TODO remove Q3 relaxation
  // double q3 = quartile(values, 75);
  double q3 = quartile(values, 75);
  double iqr = q3 - q1;
  double q1minus1_5IQR = q1 - 1.5 * iqr;
  double q3plus1_5IQR = q3 + 1.5 * iqr;
  // System.out.println("Q1: " + q1 + "  Q3: " + q3 + "  IQR: " + iqr + "\n <" +
  // q1minus1_5IQR +
  // " | > " +
  // q3plus1_5IQR);
  for(int i = 0; i < values.length; i++){
    // TODO drop >= and <=
    if(values[i] >= q3plus1_5IQR || values[i] <= q1minus1_5IQR)
      o.add(values[i]);
  }
  return o;
}

public static ArrayList<Double> findOutliers(List<Double> valuesList){
  if(valuesList == null || valuesList.size() == 0){ throw new IllegalArgumentException(
    "The data array either is null or does not contain any data."); }
  double[] values = new double[valuesList.size()];
  int index = 0;
  for(double v: valuesList){
    values[index++] = v;
  }
  return findOutliers(values);
}

public static <K, V> String prettyPrintMap(Map<K, V> content,
  String kvSeparator,String twoKvSeparator){
  StringBuilder s = new StringBuilder();
  if(content.size() < 1) return s.toString();
  for(Entry<K, V> e: content.entrySet()){
    s.append(twoKvSeparator).append(e.getKey()).append(kvSeparator).append(
      e.getValue());
  }
  return s.substring(twoKvSeparator.length());
}

public static void mainMapPrint(String[] args){
  try{
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);
    System.out.println(prettyPrintMap(map, " ", " "));
    // String ab = "a nice happy cat who is slim";
    // System.out.println(getAdjectives(ab));
    // mainForChunking();
  } catch (Exception e){
    e.printStackTrace();
  }
}

public static void mainForOutliers(String[] args){
  // double[] values = new double[] { 18, 9, 8, 8, 8, 7, 4, 4, 3, 3, 2, 2, 2, 2,
  // 1 };
  // double[] values = new double[] { 38, 24, 22, 20, 19, 14, 11, 9, 5, 5, 5, 4,
  // 4, 2, 2, 2, 2, 1, 1
  // };
  double[] values =
    new double[] { 32, 19, 16, 14, 10, 7, 7, 7, 4, 2, 2, 2, 2, 1, 1 };
  // double[] values = new double[] { 69, 47, 38, 34, 19, 18, 9, 7, 7, 6, 5, 4,
  // 4, 4, 1, 1, 1 };
  HashMap<String, Double> valuesmap = new HashMap<String, Double>();
  valuesmap.put("a", 24.0);
  valuesmap.put("b", 18.0);
  valuesmap.put("c", 12.0);
  valuesmap.put("d", 8.0);
  valuesmap.put("e", 5.0);
  valuesmap.put("f", 5.0);
  valuesmap.put("g", 5.0);
  valuesmap.put("h", 4.0);
  valuesmap.put("i", 2.0);
  valuesmap.put("j", 2.0);
  valuesmap.put("k", 2.0);
  valuesmap.put("l", 2.0);
  valuesmap.put("m", 2.0);
  valuesmap.put("n", 2.0);
  valuesmap.put("o", 2.0);
  valuesmap.put("p", 1.0);
  valuesmap.put("q", 1.0);
  valuesmap.put("r", 1.0);
  System.out.println("Quartiles: \n\t" + quartile(values, 25) + "\t"
    + quartile(values, 50) + "\t" + quartile(values, 75));
  System.out.println("Outliers: " + findOutliers(values));
}

public static double variance(ArrayList<Double> numbers,double mean,
  boolean isPopulationCompletelyKnown){
  long n = 0;
  double s = 0.0;
  for(double x: numbers){
    n++;
    double delta = x - mean;
    mean += delta / n;
    s += delta * (x - mean);
  }
  // Population std deviation (i.e. divide by n not n-1.. all population known
  // exactly hence there
  // exists no bias)
  if(isPopulationCompletelyKnown) return s / n;
  else return s / (n - 1);
}

/** Longest common subsequence (not substr that wouldn't consider gaps)
 * @param a new String[] { "abcd", "abkde", "man", "woman" },
 * @param b new String[] { "abkde", "abkde", "lion", "woman", "chump" }
 * @return abkde   woman (tsv)
 */
public static String lcsSeq(String[] a,String[] b,String outStrSeparator){
  int[][] lengths = new int[a.length + 1][b.length + 1];

  // row 0 and column 0 are initialized to 0 already

  for(int i = 0; i < a.length; i++)
    for(int j = 0; j < b.length; j++)
      if(a[i].equals(b[j])) lengths[i + 1][j + 1] = lengths[i][j] + 1;
      else lengths[i + 1][j + 1] =
        Math.max(lengths[i + 1][j], lengths[i][j + 1]);

  // read the substring out from the matrix
  StringBuilder sb = new StringBuilder();
  for(int x = a.length,y = b.length; x != 0 && y != 0;){
    if(lengths[x][y] == lengths[x - 1][y]) x--;
    else if(lengths[x][y] == lengths[x][y - 1]) y--;
    else{
      assert a[x - 1].equals(b[y - 1]);
      sb.append(a[x - 1]).append(outStrSeparator);
      x--;
      y--;
    }
  }

  StringBuilder result = new StringBuilder();
  String[] sbArr = sb.toString().split(outStrSeparator);
  for(int i = sbArr.length - 1; i >= 0; i--){
    result.append(i == sbArr.length - 1 ? "" : outStrSeparator)
      .append(sbArr[i]);
  }

  return result.toString();
}

/** Longest common subsequence (not substr that wouldn't consider gaps)
 * @param a abcd
 * @param b abkde
 * @return abd
 */
public static String lcsSeq(String a,String b){
  int[][] lengths = new int[a.length() + 1][b.length() + 1];

  // row 0 and column 0 are initialized to 0 already

  for(int i = 0; i < a.length(); i++)
    for(int j = 0; j < b.length(); j++)
      if(a.charAt(i) == b.charAt(j)) lengths[i + 1][j + 1] = lengths[i][j] + 1;
      else lengths[i + 1][j + 1] =
        Math.max(lengths[i + 1][j], lengths[i][j + 1]);

  // read the substring out from the matrix
  StringBuilder sb = new StringBuilder();
  for(int x = a.length(),y = b.length(); x != 0 && y != 0;){
    if(lengths[x][y] == lengths[x - 1][y]) x--;
    else if(lengths[x][y] == lengths[x][y - 1]) y--;
    else{
      assert a.charAt(x - 1) == b.charAt(y - 1);
      sb.append(a.charAt(x - 1));
      x--;
      y--;
    }
  }

  return sb.reverse().toString();
}

/** LCSubarray (not longest common subsequence) at string level, not character level 
<pre>
function LCSubstr(S[1..m], T[1..n])
   L := array(1..m, 1..n)
   z := 0
   ret := {}
   for i := 1..m
       for j := 1..n
           if S[i] = T[j]
               if i = 1 or j = 1
                   L[i,j] := 1
               else
                   L[i,j] := L[i-1,j-1] + 1
               if L[i,j] > z
                   z := L[i,j]
                   ret := []
               if L[i,j] = z
                   ret := ret union {S[i-z+1..i]}
           else L[i,j]=0;
   return ret
   </pre>
  <pre>
   For the example strings "BABA" (source) and  "ABAB" (target) :
        A   B   A   B
    0   0   0   0   0
B   0   0   1   0   1
A   0   1   0   2   0
B   0   0   2   0   3
A   0   1   0   3   0

</pre>
   The variable z is used to hold the length of the longest common substring found so far. 
   The set ret is used to hold the set of strings which are of length z. 
   The set ret can be saved efficiently by just storing the index i, 
   which is the last character of the longest common substring (of size z) instead of S[i-z+1..z]. 
   Thus all the longest common substrings would be, for each i in ret, S[(ret[i]-z)..(ret[i])].
*/
private static String wordsFrom(int i,int j,String[] s){
  StringBuilder builder = new StringBuilder();
  builder.append(s[i]);
  for(int k = i + 1; k <= j; k++){
    builder.append(" ").append(s[k]);
  }
  return builder.toString();
}

public static int countChar(String s,char ec,boolean shouldTrim){
  int count = 0;
  if(shouldTrim) s = s.trim();
  for(char c: s.toCharArray()){
    if(c == ec) count++;
  }
  return count;
}

/**  [NP The/DT building/NN] [VP is/VBZ] [ADJP dark/JJ] ./.
1. 's (possessives), of(merged with prev. chunk) 
2. non-breaking whitespaces handled on top of Apache openNLPChunker.
3. Maintains a list of tokens (surface forms), pos tags, and, types of layers (e.g. np)
The layers are a bit too specific to reverb and not generalizable.  

* @return the tokens, POS tags, and NP chunk tags of this string in Open
*         NLP format (square brackets around chunks, then token/tag).
*/
/*public static List<Pair<String, String>> chunkFormatter(ChunkedSentence c){
  List<Pair<String, String>> posChunks = new ArrayList<>();
  String currentPOS = "";
  StringBuffer sb = new StringBuffer();
  for(int i = 0; i < c.getLength(); i++){
    String tt = c.getToken(i);
    String npChunkTag = c.getChunkTag(i);
    if(i > 0 && !npChunkTag.startsWith("I-")
      && !c.getChunkTag(i - 1).equals("O")){
      posChunks.add(new Pair<>(currentPOS, sb.toString().trim()));
      sb = new StringBuffer();
    }
    if(npChunkTag.startsWith("B-")){
      // ignores ./. that has currentPOS="0"
      currentPOS = npChunkTag.substring(2);
    }
    sb.append(tt).append(" ");
  }

  return posChunks;
}*/

private static BreakIterator boundary;

public static List<String> splitSentence(String text){
  if(boundary == null) boundary = BreakIterator.getSentenceInstance(Locale.US);
  List<String> sentences = new ArrayList<>();
  boundary.setText(text);
  int start = boundary.first();
  for(int end = boundary.next(); end != BreakIterator.DONE; start = end,end =
    boundary.next()){
    sentences.add(text.substring(start, end));
  }
  return sentences;
}

public static String headWord(String s){
  return s.substring(s.lastIndexOf(' ') + 1);
  /*StringBuilder sb = new StringBuilder();
  char[] chs = s.toCharArray();
  for(int i = 0; i < chs.length; i++){
    if(chs[i] == ' ')
  } 
  return count;*/
}

public static int countStr(String s,String es,boolean shouldTrim){
  int count = 0;
  if(shouldTrim) s = s.trim();
  for(String st: constructNgms(s, 1)){
    if(st.equals(es)) count++;
  }
  return count;
}

public static String getArg(String[] args,int index,String defaultVal){
  String val = "";
  if(args != null && args.length > index && args[index] != null
    && args[index].trim().length() > 0) val = args[index];
  else val = defaultVal;
  return val;
}

public static String applyPluralizeRule(String wordToPluralize){
  String pluralForm = "";
  if(wordToPluralize.endsWith("y")){
    pluralForm = wordToPluralize.replaceFirst("^(.*?)y$", "$1ies");
  } else if(wordToPluralize.endsWith("man")){
    pluralForm = wordToPluralize.replaceFirst("^(.*?)man$", "$1men");
  } else if(wordToPluralize.endsWith("sh")){
    pluralForm = wordToPluralize.replaceFirst("^(.*?)sh$", "$1shes");
  } else if(wordToPluralize.endsWith("ch")){
    pluralForm = wordToPluralize.replaceFirst("^(.*?)ch$", "$1ches");
  } else if(wordToPluralize.endsWith("z")){
    pluralForm = wordToPluralize.replaceFirst("^(.*?)z$", "$1zes");
  } else if(wordToPluralize.endsWith("x")){
    pluralForm = wordToPluralize.replaceFirst("^(.*?)x$", "$1xes");
  } else if(wordToPluralize.endsWith("s")){
    pluralForm = wordToPluralize.replaceFirst("^(.*?)s$", "$1ses");
  } else pluralForm = wordToPluralize + "s";
  return pluralForm;
}

//private static Morpha lexer;

/**
 * That is, it only does noun
 * plurals, pronoun case, and verb endings, and not things like comparative adjectives
 * or derived nominals.  It is based on a finite-state
 * transducer implemented by John Carroll et al., written in flex and publicly
 * available.
 * See: http://www.informatics.susx.ac.uk/research/nlp/carroll/morph.html .
 * @param w e.g. fighter jets
 * @param morphaPOSNum (Use Morpha. static members) e.g. 2 (for noun), 3 (for any)
 * @return fighter jet (note that fighters jets returns fighters jet
 * @usage Util.stem("goes", Morpha.verb);
 */
/*public static String stem(String w,int morphaPOSNum){
  try{
    if(w == null || w.length() == 0) return w;
    int numWords = Util.countChar(w, ' ', false) + 1;
    String[] ws = null;
    if(numWords > 1) ws = w.split(" ");
    if(lexer == null) lexer = new Morpha(System.in);
    lexer.yyreset(new StringReader(numWords == 1 ? w : ws[ws.length - 1]));
    lexer.yybegin(morphaPOSNum);
    if(numWords == 1) return lexer.next();
    else{
      StringBuilder sb = new StringBuilder();
      for(int i = 0; i < ws.length - 1; i++)
        sb.append(ws[i]).append(" ");
      sb.append(lexer.next());
      return sb.toString();
    }
  } catch (Exception e){
    e.printStackTrace();
  }
  return w;
}*/

// |s=|ses=s|xes=x|zes=z|ches=ch|shes=sh|men=man|ies=y
//public static String applySingularizeRule(String wordToSingularize){
  //return stem(wordToSingularize, Morpha.noun);
  /*// Exceptions
  if(wordToSingularize.equalsIgnoreCase("sizes")) return "size";
  if(wordToSingularize.equalsIgnoreCase("delicious")) return "delicious";
  if(wordToSingularize.equalsIgnoreCase("lens")) return "lens";
  String singForm = wordToSingularize;
  if(wordToSingularize.endsWith("ies")){
    singForm = wordToSingularize.replaceFirst("^(.*?)ies$", "$1y");
  } else if(wordToSingularize.endsWith("men")){
    singForm = wordToSingularize.replaceFirst("^(.*?)men$", "$1man");
  } else if(wordToSingularize.endsWith("shes")){
    singForm = wordToSingularize.replaceFirst("^(.*?)shes$", "$1sh");
  } else if(wordToSingularize.endsWith("ches")){
    singForm = wordToSingularize.replaceFirst("^(.*?)ches$", "$1ch");
  } else if(wordToSingularize.endsWith("zes")){
    singForm = wordToSingularize.replaceFirst("^(.*?)zes$", "$1z");
  } else if(wordToSingularize.endsWith("xes")){
    singForm = wordToSingularize.replaceFirst("^(.*?)xes$", "$1x");
  } else if(wordToSingularize.endsWith("ses")){
    singForm = wordToSingularize.replaceFirst("^(.*?)ses$", "$1s");
  } else if(wordToSingularize.endsWith("s")){
    singForm = wordToSingularize.replaceFirst("^(.*?)s$", "$1");
  }
  return singForm;*/
//}

public static String capitalizeFirstChar(String s){
  StringBuilder tokenCleaned = new StringBuilder("");
  char[] tokenArray = s.toCharArray();
  if(tokenArray[0] >= 97 && tokenArray[0] <= 122) tokenCleaned
    .append((char) (tokenArray[0] - 32));
  else tokenCleaned.append(tokenArray[0]);
  for(int i = 1; i < tokenArray.length; i++){
    tokenCleaned.append(tokenArray[i]);
  }
  return tokenCleaned.toString();
}

/** @description Does this file Exist */
public static boolean exists(String filePath){
  return new File(filePath).exists();
}

/** lowers case and returns only a->z. add space in list of ignore characters if needed.
 * 
 * @param token
 *            e.g. niket#1
 * @param ignoreCharacters
 *            e.g. #
 * @return niket */
public static String cleanToken(String token,List<Character> ignoreCharacters){
  StringBuilder tokenCleaned = new StringBuilder("");
  char[] tokenArray = token.toLowerCase().toCharArray();
  for(int i = 0; i < tokenArray.length; i++){
    if(tokenArray[i] >= 97 && tokenArray[i] <= 122
      || ignoreCharacters.contains(tokenArray[i])){
      tokenCleaned.append(tokenArray[i]);
    }
  }
  return tokenCleaned.toString();
}

public static String cleanTokenIgnoreSpace(String token,boolean lowerTheCase){
  StringBuilder tokenCleaned = new StringBuilder("");
  char[] tokenArray =
    lowerTheCase ? token.toLowerCase().toCharArray() : token.toCharArray();
  for(int i = 0; i < tokenArray.length; i++){
    // 32 is space, 46 is dot (full stop)
    if(tokenArray[i] >= 97 && tokenArray[i] <= 122 || tokenArray[i] >= 65
      && tokenArray[i] <= 90 || 32 == tokenArray[i]){
      tokenCleaned.append(tokenArray[i]);
    }
  }
  return tokenCleaned.toString();
}

public static String
  cleanTokenIgnoreSpaceDot(String token,boolean lowerTheCase){
  StringBuilder tokenCleaned = new StringBuilder("");
  char[] tokenArray =
    lowerTheCase ? token.toLowerCase().toCharArray() : token.toCharArray();
  for(int i = 0; i < tokenArray.length; i++){
    // 32 is space, 46 is dot (full stop)
    if(tokenArray[i] >= 97 && tokenArray[i] <= 122 || tokenArray[i] >= 65
      && tokenArray[i] <= 90 || 32 == tokenArray[i] || 46 == tokenArray[i]){
      tokenCleaned.append(tokenArray[i]);
    }
  }
  return tokenCleaned.toString();
}


public static String removeStopWords(String in){
  StringBuilder sb = new StringBuilder();
  for(String i: nullableIter(split(in, ' '))){
    if(isStopWord(i)) continue;
    if(sb.length() > 0) sb.append(' ');
    sb.append(i);
  }
  return sb.toString();
}

public static boolean hasSpecialChars(String token){
  return hasSpecialChars(token, null);
}

/** good (returns false) : good1 (returns true)
 * NOTE: works for ONLY ONE WORD
 * [65-90], [97-122] 
 * @param token
 * @param ignoreCharacters
 * @return yes/no
 */
public static boolean hasSpecialChars(String token,
  ArrayList<Character> ignoreCharacters){
  char[] tokenArray = token.toCharArray();
  for(int i = 0; i < tokenArray.length; i++){
    if((tokenArray[i] >= 97 && tokenArray[i] <= 122)
      || (tokenArray[i] >= 65 && tokenArray[i] <= 90)){
      continue;
    } else{
      if(ignoreCharacters != null && ignoreCharacters.size() > 0
        && ignoreCharacters.contains(tokenArray[i])) continue;
      else return true;
    }
  }
  return false;
}

/** lowers case and returns only a->z.
 * 
 * @param token
 *            e.g. niket#1
 * @param ignoreCharacters
 *            e.g. #
 * @return niket */
public static String cleanToken(String token){
  return cleanToken(token, new ArrayList<Character>());
}
public static ArrayList<String> englishStopWords;

public static boolean isStopWord(String word,List<String> stopList){
  if(word == null){ return false; }
  if(stopList.contains(word)){ return true; }
  return false;
}

public static boolean isStopWord(String word,String stopFile){
  if(word == null){ return false; }
  if(englishStopWords == null) initializeStopWordList(",", stopFile);
  if(englishStopWords.contains(word)){ return true; }
  return false;
}

public static boolean isStopWord(String word){
  String defaultStopFilePath = "./data/english-stop-words.txt";
  if(englishStopWords != null
    || (englishStopWords == null && !StopWordRemover.getInstance().initialized && Util
      .exists(defaultStopFilePath))) return isStopWord(word,
    defaultStopFilePath);
  else return StopWordRemover.getInstance().isStopWord(word);
}

/** Load stop words
 * 
 * 
 * 
 * 
 * 
 * 
 * @param separator ","
 * @param stopfile "./data/english-stop-words.txt"
 */
public static void initializeStopWordList(String separator,String stopfile){
  englishStopWords =
    new ArrayList<String>(Arrays.asList(Util.readFile(stopfile).toString()
      .split(separator)));
}

public static boolean isStopWordOrEmpty(String word){
  if(isStopWord(word)) return true;
  String cleaned = cleanToken(word);
  // All numbers of special characters, no real alphabet.
  if(cleaned.length() == 0) return true;
  if(isStopWord(cleaned)) return true;
  return false;
}

public static String getCharsFromUntil(int fromPosition,char untilChar,
  String inputStr){
  String posVal = "";
  for(int i = fromPosition; inputStr.charAt(i) != ' '; i++){
    posVal += inputStr.charAt(i);
  }
  return posVal;
}

public static List<String> parseString(String phrase,char delim,int maxWords)
  throws IOException{
  StringBuilder word = new StringBuilder();
  List<String> words = new ArrayList<String>(maxWords);
  char[] charArr = phrase.toCharArray();
  for(int i = 0; i < charArr.length; i++){
    if(charArr[i] != delim) word.append(charArr[i]);
    else{
      words.add(word.toString());
      word = new StringBuilder(maxWords);
    }
  }
  if(word.length() > 0) words.add(word.toString());
  return words;
}

/** The utility function doesn't assume subfolders which involves additional complexity. 
 * @param folderPath
 * @return */
public static File[] iterateOverFolder(String folderPath){
  File folderHandle = new File(folderPath);
  if(folderHandle.isDirectory()){ return folderHandle.listFiles(); }
  /* Its a file. */
  return new File[] { folderHandle };
}

public static int writeGraphToFile(byte[] img,File to){
  try{
    FileOutputStream fos = new FileOutputStream(to);
    fos.write(img);
    fos.close();
  } catch (java.io.IOException ioe){
    return -1;
  }
  return 1;
}

public static double sigmoid(double x){
  return 1 / (1 + Math.exp(-x));
}
private static DecimalFormat decim;

/** 32.535534534534; after formatting = 32.536 */
public static String format(double x){
  if(decim == null) decim = new DecimalFormat("#.###");
  return decim.format(x);
}

public static int readNumberFromUser() throws NumberFormatException,IOException{
  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  int s = Integer.parseInt(br.readLine());
  return s;
}

public static String readStringFromUser(String msg) throws IOException{
  System.out.print(msg + " ");
  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  return br.readLine();
}

public static String readStringFromUser() throws IOException{
  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  return br.readLine();
}

public static void
  writeFile(String filePath,String content,Boolean shouldAppend){
  BufferedWriter out = null;
  try{
    out = new BufferedWriter(new FileWriter(filePath, shouldAppend));
    out.write(content);
    out.close();
  } catch (IOException e){}
}

public static void writeFileUserDefinedObject(String filePath,Object content,
  Boolean shouldAppend){
  BufferedWriter out = null;
  try{
    out = new BufferedWriter(new FileWriter(filePath, shouldAppend));
    out.write(content + "\n");
    out.close();
  } catch (IOException e){}
}

public static void writeFile(String filePath,Collection content,
  Boolean shouldAppend){
  BufferedWriter out = null;
  try{
    out = new BufferedWriter(new FileWriter(filePath, shouldAppend));
    for(Object line: nullableIter(content))
      out.write(line + "\n");
    out.close();
  } catch (IOException e){}
}

/**
 * Default kvSeparator is TAB
 * @param filePath
 * @param map
 * @param shouldAppend
 * @throws IOException
 */
public static <T, V> void writeFile(String filePath,Map<T, V> map,
  boolean shouldAppend) throws IOException{
  writeFile(filePath, map, shouldAppend, "\t");
}

public static <T, V> void writeFile(String filePath,Map<T, V> map,
  boolean shouldAppend,String kvSeparator) throws IOException{
  BufferedWriter out =
    new BufferedWriter(new FileWriter(filePath, shouldAppend));
  for(Entry<T, V> e: map.entrySet())
    out.write((T) e.getKey() + kvSeparator + (V) e.getValue() + "\n");
  out.close();
}

public static void writeFile(String filePath,Collection<String> content,
  boolean shouldAppend){
  BufferedWriter out = null;
  try{
    out = new BufferedWriter(new FileWriter(filePath, shouldAppend));
    for(String line: nullableIter(content))
      out.write(line + "\n");
    out.close();
  } catch (IOException e){}
}

public static TreeMap<String, String> readCSVFileIntoMap(String filePath,
  String valueSeparator){
  TreeMap<String, String> allValues = new TreeMap<String, String>();
  if(valueSeparator == null || valueSeparator.trim().length() < 1){
    valueSeparator = "\\s+";
  }
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(UTF8Str != null && UTF8Str.trim().length() >= 1){
        String[] values = UTF8Str.split(valueSeparator);
        allValues.put(values[0], values[1]);
      }
    }
    in.close();
    // dispose all the resources after using them.
  } catch (FileNotFoundException e){
    e.printStackTrace();
  } catch (IOException e){
    e.printStackTrace();
  }
  return allValues;
}

public static TreeMap<String, Integer> readCSVFileIntoCountMap(String filePath,
  String valueSeparator){
  TreeMap<String, Integer> allValues = new TreeMap<String, Integer>();
  if(valueSeparator == null
    || (!valueSeparator.equals("\t") && valueSeparator.trim().length() < 1)){
    valueSeparator = "\\s+";
  }
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(UTF8Str != null && UTF8Str.trim().length() >= 1){
        String[] values = UTF8Str.split(valueSeparator);
        allValues.put(values[0], Integer.parseInt(values[1]));
      }
    }
    in.close();
    // dispose all the resources after using them.
  } catch (FileNotFoundException e){
    e.printStackTrace();
  } catch (IOException e){
    e.printStackTrace();
  }
  return allValues;
}

public static void readFileAsMap(String filePath,String valueSeparator,
  HashMap<String, Integer> cache){
  if(valueSeparator == null || valueSeparator.trim().length() < 1
    && !valueSeparator.equals("\t")){
    valueSeparator = "\\s+";
  }
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(UTF8Str != null && UTF8Str.trim().length() >= 1){
        String[] values = UTF8Str.split(valueSeparator);
        cache.put(values[0], Integer.parseInt(values[1]));
      }
    }
    in.close();
  } catch (IOException e){
    System.out.println("No data to read from " + filePath + " , as "
      + e.getMessage());
  }
}

public static void readFileSwappedAsMapIntStr(String filePath,
  String valueSeparator,Map<Integer, String> cache){
  if(valueSeparator == null || valueSeparator.trim().length() < 1
    && !valueSeparator.equals("\t")){
    valueSeparator = "\\s+";
  }
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(UTF8Str != null && UTF8Str.trim().length() >= 1){
        String[] values = UTF8Str.split(valueSeparator);
        cache.put(Integer.parseInt(values[1]), values[0]);
      }
    }
    in.close();
  } catch (IOException e){
    System.out.println("No data to read from " + filePath + " , as "
      + e.getMessage());
  }
}

public static void readFileAsMap(String filePath,String valueSeparator,
  Map<Integer, Integer> cache){
  if(valueSeparator == null || valueSeparator.trim().length() < 1
    && !valueSeparator.equals("\t")){
    valueSeparator = "\\s+";
  }
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(UTF8Str != null && UTF8Str.trim().length() >= 1){
        String[] values = UTF8Str.split(valueSeparator);
        cache.put(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
      }
    }
    in.close();
  } catch (IOException e){
    System.out.println("No data to read from " + filePath + " , as "
      + e.getMessage());
  }
}

public static <K, V> void readFileAsMap(String filePath,String valueSeparator,
  Map<K, V> cache,boolean isValueNumber) throws ParseException{
  readFileAsMap(filePath, valueSeparator, cache, isValueNumber, false);
}

public static <K, V> void readFileAsMap(String filePath,String valueSeparator,
  Map<K, V> cache,boolean isValueNumber,boolean toLowerCase)
  throws ParseException{
  if(valueSeparator == null || valueSeparator.trim().length() < 1
    && !valueSeparator.equals("\t")){
    valueSeparator = "\\s+";
  }
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(toLowerCase) UTF8Str = UTF8Str.toLowerCase();
      if(UTF8Str != null && UTF8Str.trim().length() >= 1){
        String[] values = UTF8Str.split(valueSeparator);
        cache.put((K) values[0], isValueNumber ? (V) NumberFormat.getInstance()
          .parse(values[1]) : (V) values[1]);
      }
    }
    in.close();
  } catch (IOException e){
    System.out.println("No data to read from " + filePath + " , as "
      + e.getMessage());
  }
}

public static <K, V> void readFileAsMapSwapped(String filePath,
  String valueSeparator,Map<K, V> cache,boolean isMapsValueANumber)
  throws ParseException{
  if(valueSeparator == null || valueSeparator.trim().length() < 1
    && !valueSeparator.equals("\t")){
    valueSeparator = "\\s+";
  }
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(UTF8Str != null && UTF8Str.trim().length() >= 1){
        String[] values = UTF8Str.split(valueSeparator);
        if(cache.containsKey(values[1]))
          System.out.println("Duplicate key found: " + values[1]);
        cache.put(isMapsValueANumber ? (K) NumberFormat.getInstance().parse(
          values[1]) : (K) values[1], (V) values[0]);
      }
    }
    // long l = 301506526;
    // System.out.println("Debug Niket readMapSwapped(should be non-null) " +
    // cache.get(l));
    in.close();
  } catch (IOException e){
    System.out.println("No data to read from " + filePath + " , as "
      + e.getMessage());
  }
}

/**
 * Get index with maximum value (last index in case of equals.).
 * @param values V1: [1.5,1,4,2,3,1.9] , V2: [1.5,1,4,2,4,1.9]
 * @return argmax_V1=2, argmax_V2=4
 */
public static int argMax(double[] values){
  int maxPos = 0;
  double max = values[maxPos];
  for(int i = 0; i < values.length; i++){
    if(values[i] >= max){
      max = values[i];
      maxPos = i;
    }
  }
  return maxPos;
}

public static ArrayList<String> readFileAsList(String filePath){
  String temp = "";
  ArrayList<String> lines = new ArrayList<String>();
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String str = new String(temp.getBytes());
      lines.add(str);
    }
    in.close();
  } catch (FileNotFoundException e){
    System.out.println("File not found, in reading file as list: "
      + e.getMessage());
  } catch (IOException e){
    System.out
      .println("IOException in reading file as list: " + e.getMessage());
  }
  return lines;
}

public static ArrayList<String> readFileAsList(String filePath,
  boolean toLowerCase){
  String temp = "";
  ArrayList<String> lines = new ArrayList<String>();
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      if(temp.length() > 0){
        String UTF8Str = new String(temp.getBytes(), "UTF-8");
        lines.add(toLowerCase ? UTF8Str.toLowerCase() : UTF8Str);
      }
    }
    in.close();
  } catch (FileNotFoundException e){
    System.out.println("File not found, in reading file as list: "
      + e.getMessage());
  } catch (IOException e){
    System.out
      .println("IOException in reading file as list: " + e.getMessage());
  }
  return lines;
}

public static HashSet<String>
  readFileAsSet(String filePath,boolean toLowerCase){
  String temp = "";
  HashSet<String> lines = new HashSet<String>();
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      if(temp.length() > 0){
        String UTF8Str = new String(temp.getBytes(), "UTF-8");
        lines.add(toLowerCase ? UTF8Str.toLowerCase() : UTF8Str);
      }
    }
    in.close();
  } catch (FileNotFoundException e){
    System.out.println("File not found, in reading file as list: "
      + e.getMessage());
  } catch (IOException e){
    System.out
      .println("IOException in reading file as list: " + e.getMessage());
  }
  return lines;
}

public static ArrayList<String> readFileAsList(String filePath,
  boolean toLowerCase,boolean trim){
  String temp = "";
  ArrayList<String> lines = new ArrayList<String>();
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      if(trim) UTF8Str = UTF8Str.trim();
      lines.add(toLowerCase ? UTF8Str.toLowerCase() : UTF8Str);
    }
    in.close();
  } catch (FileNotFoundException e){
    System.out.println("File not found, in reading file as list: "
      + e.getMessage());
  } catch (IOException e){
    System.out
      .println("IOException in reading file as list: " + e.getMessage());
  }
  return lines;
}

private static void readFileZipped(String filePath) throws IOException{
  BufferedInputStream in =
    new BufferedInputStream(new FileInputStream(new File(filePath)));
  GZIPInputStream gzin = new GZIPInputStream(in);
  BufferedReader br = new BufferedReader(new InputStreamReader(gzin));
  String line = "";
  int ctr = 0;
  while ((line = br.readLine()) != null){
    if(++ctr <= 1) System.out.println("read: " + line);
  }
  in.close();
}

public static StringBuilder readFile(String filePath){
  StringBuilder fileContents = new StringBuilder("");
  String temp = "";
  try{
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      String UTF8Str = new String(temp.getBytes(), "UTF-8");
      fileContents.append(UTF8Str + "\n");
    }
    in.close();
    // dispose all the resources after using them.
  } catch (FileNotFoundException e){
    e.printStackTrace();
  } catch (IOException e){
    e.printStackTrace();
  }
  return fileContents;
}

public static ArrayList<String> readLineNumbersFromLine(String filePath,
  int low,int high){
  ArrayList<String> fileContents = new ArrayList<String>();
  String temp = "";
  try{
    int counter = 0;
    BufferedReader in = new BufferedReader(new FileReader(filePath));
    while ((temp = in.readLine()) != null){
      if(counter <= high && counter >= low)
        fileContents.add(new String(temp.getBytes(), "UTF-8"));
      counter++;
      if(counter > high) break;
    }
    in.close();
  } catch (FileNotFoundException e){
    e.printStackTrace();
  } catch (IOException e){
    e.printStackTrace();
  }
  return fileContents;
}

public static StringBuilder readFileThrowException(String filePath,
  String delimiterToSeparateLinesInFile) throws UnsupportedEncodingException,
  IOException{
  StringBuilder fileContents = new StringBuilder("");
  String temp = "";
  if(delimiterToSeparateLinesInFile == null
    || delimiterToSeparateLinesInFile.trim().length() < 1){
    delimiterToSeparateLinesInFile = "\n";
  }
  BufferedReader in = new BufferedReader(new FileReader(filePath));
  while ((temp = in.readLine()) != null){
    String UTF8Str = new String(temp.getBytes(), "UTF-8");
    fileContents.append(UTF8Str + delimiterToSeparateLinesInFile);
  }
  // dispose all the resources after using them.
  in.close();
  return fileContents;
}

public static StringBuilder getZipFileEntries(String filePath){
  StringBuilder zipFileNames = new StringBuilder("");
  try{
    FileInputStream fis = new FileInputStream("C:\\MyZip.zip");
    ZipInputStream zis = new ZipInputStream(fis);
    ZipEntry ze;
    while ((ze = zis.getNextEntry()) != null){
      zipFileNames.append(ze.getName());
      zis.closeEntry();
    }
    zis.close();
  } catch (FileNotFoundException e){
    e.printStackTrace();
  } catch (IOException e){
    e.printStackTrace();
  }
  return zipFileNames;
}

public static StringBuilder readZipFile(String filePath){
  StringBuilder zipFileContent = new StringBuilder("");
  try{
    FileInputStream fis = new FileInputStream("C:\\MyZip.zip");
    ZipInputStream zis = new ZipInputStream(fis);
    ZipEntry ze;
    while ((ze = zis.getNextEntry()) != null){
      zipFileContent.append(readFile(ze.getName()));
      zis.closeEntry();
    }
    zis.close();
  } catch (FileNotFoundException e){
    e.printStackTrace();
  } catch (IOException e){
    e.printStackTrace();
  }
  return zipFileContent;
}

/** Only exclude those files listed in exclusion list.
 * 
 * @param folderPath
 * @param exclusionList
 * @return file objects of all accepted files in the folder */
public static File[] iterateOverFolderWithExceptions(String folderPath,
  ArrayList<String> exclusionList){
  File folderHandle = new File(folderPath);
  if(folderHandle.isDirectory()){
    File[] allfiles = folderHandle.listFiles();
    ArrayList<File> toReturn = new ArrayList<File>();
    for(int i = 0; i < allfiles.length; i++){
      if(exclusionList == null
        || !exclusionList.contains(allfiles[i].getName())){
        toReturn.add(allfiles[i]);
      }
    }
    File[] returnArray = new File[toReturn.size()];
    int count = 0;
    for(File oneFile: toReturn){
      returnArray[count++] = oneFile;
    }
    return returnArray;
  }
  /* Its a file. */
  return new File[] { folderHandle };
}

public static File[] iterateOverFolderWithInclusions(String folderPath,
  ArrayList<String> exceptions){
  File folderHandle = new File(folderPath);
  if(folderHandle.isDirectory()){
    File[] allfiles = folderHandle.listFiles();
    ArrayList<File> toReturn = new ArrayList<File>();
    for(int i = 0; i < allfiles.length; i++){
      if(exceptions.contains(allfiles[i].getName())){
        toReturn.add(allfiles[i]);
      }
    }
    File[] returnArray = new File[toReturn.size()];
    int count = 0;
    for(File oneFile: toReturn){
      returnArray[count++] = oneFile;
    }
    return returnArray;
  }
  /* Its a file. */
  return new File[] { folderHandle };
}

public static void flushBufferedOutput(String outputPath,
  StringBuilder bufferedOutput){
  writeFile(outputPath, bufferedOutput.toString(), true);
}

public static void printList(List<String> lines){
  for(String line: lines){
    System.out.println(line);
  }
}

public static float dot(double[] x,float[] y){
  assert x.length == y.length;
  float dot = 0.0f;
  for(int i = 0; i < y.length; i++){
    dot += y[i] * x[i];
  }
  return dot;
}

public static int countLines(String filePath){
  return readFileAsList(filePath).size();
}

public static <F> String arrayToString(F[] arr,String separator){
  StringBuilder s = new StringBuilder();
  if(arr == null || arr.length == 0) return s.toString();
  s.append(arr[0].toString());
  for(int i = 1; i < arr.length; i++){
    s.append(separator).append(arr[i].toString());
  }
  return s.toString();
}

public static String getWordAtRelativePos(int i,String inputString,
  String pivotWord){
  int pivotBeginPos = inputString.indexOf(pivotWord);
  int pivotEndPos = pivotBeginPos + pivotWord.length() + 1;
  if(i > 0) // Following word.
  {
    int endIndex = inputString.indexOf(' ', pivotEndPos);
    if(endIndex > pivotEndPos) return inputString.substring(pivotEndPos,
      endIndex);
    else return "";
  } else{ // Previous word.
    if(pivotBeginPos <= 0) return "";
    // Till a space or beginning of inputString.. grab it.
    char[] chs = inputString.toCharArray();
    int prevSpacePos = 0;
    for(int j = pivotBeginPos - 2; j > 0; j--){
      if(chs[j] == ' '){
        prevSpacePos = j;
        break;
      }
    }
    return inputString.substring(prevSpacePos, pivotBeginPos - 1).trim();
  }
}

/**
 * @param string
 * @return number of digits in the string.
 */
public static int countDigits(String line){
  int count = 0;
  for(char c: line.toCharArray()){
    if(Character.isDigit(c)) count++;
  }
  return count;
}

public static void createFolder(String folderPath){
  File folderHandle = new File(folderPath);
  if(!folderHandle.exists()) folderHandle.mkdirs();
}

/** File having three column format is read as a triples object. If any line/row doesn't have three columns it is ignored.
 * @param fileToRead must be separated by 'separator' and have exactly three columns. 
 * @param triples output <red, color, 2.0>
 * @param separator TAB: char c = 9;
 * @throws IOException
 */

public static List<String> split(String input,char delimChar){
  List<String> splits = new ArrayList<String>();
  if(input == null || delimChar < 0) return splits;
  StringBuilder sb = new StringBuilder();
  for(char c: input.toCharArray()){
    if(c == delimChar){
      splits.add(sb.toString());
      sb.setLength(0);
    } else{
      sb.append(c);
    }
  }
  // leftover chars or if input has no presence of delimChar.
  if(sb.length() > 0) splits.add(sb.toString());
  return splits;
}

public static StringBuilder readUrl(URL url) throws MalformedURLException,
  IOException{
  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
  StringBuilder sb = new StringBuilder();
  if(conn.getResponseCode() != 200){
    throw new MalformedURLException(
      "Exception: Server did not (or refused to) return results:\n"
        + conn.getResponseMessage());
  } else{
    BufferedReader rd =
      new BufferedReader(new InputStreamReader(conn.getInputStream()));

    String line;
    while ((line = rd.readLine()) != null){
      sb.append(line);
    }
    rd.close();
    conn.disconnect();
  }
  return sb;
}

public static List<String> constructNgms(String s,int n){
  List<String> ngrams = new ArrayList<String>();
  String[] splits = s.split(" ");
  for(int i = 0; i <= splits.length - n; i++){
    StringBuilder oneNgram = new StringBuilder();
    for(int j = i; j < i + n; j++){
      if(j != i) oneNgram.append(" ");
      oneNgram.append(splits[j]);
    }
    if(oneNgram.length() > 0) ngrams.add(oneNgram.toString());
  }
  return ngrams;
}

/*public static <T> List<Pair<T, T>> cross(List<T> l1,List<T> l2){
  List<Pair<T, T>> output = new ArrayList<Pair<T, T>>();
  for(T e1: l1)
    for(T e2: l2)
      output.add(new Pair<T, T>(e1, e2));
  return output;
}*/

public static class Progress {
int linePer;
int dotPer;
public int ctr;

/**
 * @param numWorkCtrToPrintADot e.g. 10,000 
 * (makes 1 dot every 10K, hence 1 line = 1mil.)
 */
public Progress(int numWorkCtrToPrintADot) {
  this.dotPer = numWorkCtrToPrintADot;
  this.linePer = this.dotPer * 100;
  this.ctr = 0;
}

private List<String> storageBuffer;
private String outFile;

public void storageInit(String outFile,boolean inAppendMode){
  // Initialize.
  this.outFile = outFile;
  if(storageBuffer == null){
    storageBuffer = new ArrayList<String>();
    if(!inAppendMode) Util.writeFile(outFile, "", inAppendMode);
  }
}

public void flush(){
  // Cleanup.
  Util.writeFile(outFile, storageBuffer, true);
  storageBuffer = null;
}

/**
 * if(s==null) considers ends of writing, clears internal buffer.
 * @param s string to store
 * @param outFile file to store
 * @throws SQLException
 * @throws IOException
 */
public void store(String s,int bufferSize) throws IOException{

  // What to store.
  storageBuffer.add(s);

  // Write but avoid FileIO too often.
  if(storageBuffer.size() == bufferSize){
    Util.writeFile(outFile, storageBuffer, true);
    storageBuffer = new ArrayList<String>();
  }
}

/**
 * if(s==null) considers ends of writing, clears internal buffer.
 * @param s string to store
 * @param outFile file to store
 * @throws SQLException
 * @throws IOException
 */
public void store(String s) throws IOException{

  // What to store.
  storageBuffer.add(s);

  // Write but avoid FileIO too often.
  if(storageBuffer.size() == 10000){
    Util.writeFile(outFile, storageBuffer, true);
    storageBuffer = new ArrayList<String>();
  }
}

public void next(){
  ctr++;
  printIfNeeded();
}

private void printIfNeeded(){
  if(ctr % linePer == 0) System.out.println("  " + ctr);
  else if(ctr % dotPer == 0) System.out.print(".");
}
}

public static <K, V> void printMultiMap(Map<K, Collection<V>> m,String outFile,
  boolean append) throws Exception{
  Progress p = new Progress(10000);
  p.storageInit(outFile, true);
  for(Entry<K, Collection<V>> e: nullableIter(m.entrySet())){
    for(V v: e.getValue()){
      p.store(new StringBuilder().append(e.getKey()).append("\t").append(v)
        .toString());
    }
  }
  p.flush();
}

public static <K, V> void printMap(Map<K, V> m,String outFile,boolean append)
  throws Exception{
  Progress p = new Progress(10000);
  p.storageInit(outFile, append);
  for(Entry<K, V> e: nullableIter(m.entrySet())){
    p.store(new StringBuilder().append(e.getKey()).append("\t").append(
      e.getValue()).toString());
  }
  p.flush();
}

public static boolean isUpperCasedString(String s){
  if(s == null || s.length() == 0) return false;
  for(char ch: s.toCharArray())
    if((ch >= 97 && ch <= 122)) return false;
  return true;
}

// <html> returns ""
// <u>abcd</u> returns abcd
public static String htmlDataBtwTag(String l){
  return Util.stringBetween("<.>(.*?)</.>", l);
}

//private static SentenceChunker sc;

/*public static List<Pair<String, String>> getPhrases(String text)
  throws IOException{
  if(sc == null){
    System.out.print("Initializing chunker... ");
    Timer timer = new Timer();
    sc = new OpenNlpSentenceChunker();
    timer.time();
  }

  return Util.chunkFormatter(sc.chunkSentence(text));
}*/

}

class StopWordRemover {

private static final StopWordRemover instance = new StopWordRemover();
private Set<String> stoplist;
public boolean initialized;

private static HashSet<String> stopwords;

/**
 * Private constructor 
 */
private StopWordRemover() {
  try{
    //loadStopList();
    initialized = true;
  } catch (Exception e){
    e.printStackTrace();
  }
}

/**
 * Singleton pattern
 * @return singleton object
 */
public static StopWordRemover getInstance(){
  return StopWordRemover.instance;
}

/*private synchronized void loadStopList() throws IOException{
  String stoplistFilename = WS4JConfiguration.getInstance().getStopList();

  stoplist = new LinkedHashSet<String>(256);
  InputStream stream =
    StopWordRemover.class.getResourceAsStream("/" + stoplistFilename);
  InputStreamReader isr = new InputStreamReader(stream);
  BufferedReader br = new BufferedReader(isr);
  String line = null;
  while ((line = br.readLine()) != null){
    String stopword = line.trim();
    if(stopword.length() > 0){
      stoplist.add(stopword);
    }
  }
  stopwords =
    new HashSet<String>(Arrays.asList(new String[] { "a", "about", "above",
      "after", "again", "against", "all", "am", "an", "and", "any", "are",
      "as", "at", "be", "because", "been", "before", "being", "below",
      "between", "both", "but", "by", "cannot", "could", "did", "do", "does",
      "doing", "down", "during", "each", "few", "for", "from", "further",
      "had", "has", "have", "having", "he", "her", "here", "hers", "herself",
      "him", "himself", "his", "how", "however", "i", "if", "in", "into", "is",
      "it", "its", "itself", "let", "lrb", "me", "more", "most", "must", "my",
      "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
      "other", "ought", "our", "ours ourselves", "out", "over", "own", "rrb",
      "same", "sha", "she", "should", "so", "some", "such", "than", "that",
      "the", "their", "theirs", "them", "themselves", "then", "there", "these",
      "they", "this", "those", "through", "to", "too", "under", "until", "up",
      "very", "was", "we", "were", "what", "when", "where", "which", "while",
      "who", "who", "whom", "why", "why", "with", "wo", "would", "would",
      "you", "you", "you", "you", "you", "your", "yours", "yourself",
      "yourselves" }));
  stoplist.addAll(stopwords);
  br.close();
  isr.close();
}*/

public boolean isStopWord(String w){
  return stoplist.contains(w);
}

// separate stop word class if needed
public String[] removeStopWords(String[] words){
  List<String> contents = new ArrayList<String>(words.length);
  for(String word: words){
    if(!stoplist.contains(word)){
      contents.add(word);
    }
  }
  String[] result = (String[]) contents.toArray(new String[contents.size()]);
  return result;
}
}
