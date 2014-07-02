package model;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import javatools.database.Database;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.WebchildBrowserOld.InputBean.Inputtype;
import util.AutoMap;
import util.DBConnector;
import util.IDHelper;
import util.IDHelper.IDSubjObj;
import util.IDHelper.wps;
import util.Util;
import util.Util.Timer;

/**
 * Servlet implementation class Controller
 */
public class WebchildBrowserOld extends HttpServlet {
private static final long serialVersionUID = 1L;
public static Database db;
private static String hasPropertyUnclassifiedTb = "hasproperty2sdisambiguated";
private static String hasPropertyClassifiedTb = "_tupletempwithfreqgms"; // _tupletemp
public static Map<String, Integer> neededRelations;
private static String rangeTb = "_range";
private static String domainTb = "_domain";
private static int minPatternSupport = 1;
private static double minTupleScore = 0.0; // score-dummyscore
private static double minRangeScore = 0.06; // 0.06
private static double minDomainScore = 0.0;
private static String evokesEmotionRelName = "evokesEmotion";
private static String evokesEmotionRelNameFake = "candidateEvokesEmotion";
private static String ckbBrowserName,ckbProjectName;
private static AutoMap<String, ArrayList<CKBResult>> cachedRelationMap;

public void init(ServletConfig config) throws ServletException{
  super.init(config);
  try{
    variablesInit(false);
  } catch (Exception e){
    e.printStackTrace();
  }
}

public static double getNAFreq(String n,String a,double naFreq){
  // Avoid spam:
  if(n.equals("sex") || n.equals("babe") || n.contains("fuck")
    || n.equals("anal") || n.equals("ass") || n.equals("babe")
    || n.equals("cum") || n.equals("come") || n.equals("system")
    || n.equals("female") || n.equals("blow") || n.equals("boobs")
    || n.startsWith("girl") || n.equals("cock") || n.equals("porn")
    || n.equals("suck") || n.equals("pics") || n.equals("tits")
    || n.startsWith("web") || n.equals("semen") || n.equals("ejaculate")
    || n.equals("screw") || n.equals("search") || n.equals("once again")
    || a.equals("pussy"))
    return naFreq * -1.0;
  else return naFreq;
}

public static void variablesInit(boolean xHrefNoProject) throws SQLException{ 
  xHrefNoProj = xHrefNoProject;
  if(db == null || neededRelations == null) db = DBConnector.getDB();
  if(neededRelations == null) neededRelations = getAllAttrs();
  ckbBrowserName = "webchild";
  ckbProjectName = "webchildBrowser";

  cachedRelationMap = new AutoMap<>();
  for(Entry<String, Integer> e: neededRelations.entrySet())
    cachedRelationMap.addArrayValue("all_relations", new CKBResult("n_"
      + InputBean.joinWID(e.getKey(), e.getValue()), "r_"
      + InputBean.joinWID(e.getKey(), e.getValue()), 1, "all_relations",
      formHref(formMouseOver(e.getValue(), true), "r_"
        + InputBean.joinWID(e.getKey(), e.getValue()))));
}

/** For an input word, find out the WN entries (hyponyms, gloss..). 
 * too slow but manageable because only called once per query.
 * 
 * @param x e.g. car
 * @return
 * @throws Exception
 */
private static WNResult formWNQuery(int xid,boolean isNoun) throws Exception{
  WNResult wnr = null;
  if(!isNoun){
    String yGloss = "";
    // Adj requires only gloss.
    ResultSet ysynrs = db.query("select gloss from adjid where wordid=" + xid);
    if(ysynrs.next()) yGloss = ysynrs.getString(1);
    wnr = new WNResult(xid, -1, null, null, yGloss);
  } else{
    // Noun requires hypernyms and hyponyms.
    IDSubjObj m = IDHelper.idMeta(IDHelper.getwps(xid, true, isNoun));
    int synsetID = m.synset;
    String wnGloss = IDHelper.getTextMetaSlowly(m.wordID, "nounid").gloss;

    // TODO commented because takes very long time..
    Stack<Integer> hypernyms = new Stack<>();
    hypernyms = WordnetHelper.getInheritedHyperynymsUptoLevel(synsetID, db);
    HashMap<String, Integer> hyponymsMap =
      WordnetHelper.getFirstLevelHyponyms(synsetID, db);
    Set<Integer> hyponyms = new HashSet<Integer>();
    for(Integer v: hyponymsMap.values())
      hyponyms.add(v);

    wnr = new WNResult(xid, synsetID, hypernyms, hyponyms, wnGloss);
  }
  return wnr;
}

/**
 * Added support for wps.
 * @param w
 * @return attributes relevant for w.
 * @throws Exception
 */
static Map<String, Double> partOfAttr(String w,boolean isNoun,int wid)
  throws Exception{
  String query =
    "select x,scoresds from " + (isNoun ? domainTb : rangeTb) + " where "
      + (wid >= 0 ? "yid = " + wid : "y" + "='" + w + "'");
  ResultSet rs = db.query(query);
  Map<String, Double> rels = new HashMap<>();
  while (rs.next()){
    String attr = rs.getString(1);
    rels.put(getSingular(attr), rs.getDouble(2));
  }
  return rels;
}

static HashMap<String, String> plural2SingularMap;
private static boolean xHrefNoProj;

static String getSingular(String attrName){
  // plural singular merge.
  if(plural2SingularMap == null){
    String allATTRSingular =
      "sensitivity|ability|feeling|appearance|taste|motion|sound|length|beauty|shape|color|weight|size|strength|temperature|quality|smell|emotion|state|";
    String allATTRPlural =
      "sensitivities|abilities|feelings|appearances|tastes|motions|sounds|lengths|beauties|shapes|colors|weights|sizes|strengths|temperatures|qualities|smells|emotions|states";
    String[] singulars = allATTRSingular.split("\\|");
    String[] plurals = allATTRPlural.split("\\|");
    plural2SingularMap = new HashMap<String, String>();
    for(int i = 0; i < plurals.length; i++)
      plural2SingularMap.put(plurals[i], singulars[i]);
  }
  if(plural2SingularMap.containsKey(attrName))
    return plural2SingularMap.get(attrName);
  else return attrName;
}

public static String retrieveWithSubRelDirectlyFromDb(String x){

  StringBuilder details = new StringBuilder();
  Timer timer = new Timer();
  LinkedHashMap<String, ArrayList<CKBResult>> sortedLeftAssertionRelMap =
    new LinkedHashMap<String, ArrayList<CKBResult>>();
  LinkedHashMap<String, ArrayList<CKBResult>> sortedRightAssertionRelMap =
    new LinkedHashMap<String, ArrayList<CKBResult>>();

  InputBean xBean = new InputBean(x);
  if(xBean.xid < 0){
    // tupletemp has no plurals. perhaps convert to singular?
    xBean = new InputBean(Util.applySingularizeRule(x));
    // Lost hope!
    if(xBean.xid < 0) return "Word [" + x + "] not found!";
  }
  timer.time("input bean created ");

  formSortedResult(sortedLeftAssertionRelMap, sortedRightAssertionRelMap,
    xBean);

  /*details.append(GUIHelper.htmlStart());
  details.append(GUIHelper.htmlForm());
  details.append(GUIHelper.resultStart(inputPrintName(xBean)));
  details.append(GUIHelper.result(sortedLeftAssertionRelMap,
    sortedRightAssertionRelMap));
  details.append(GUIHelper.htmlEnd());*/

  timer.time("created GUI ");

  return details.toString();
}

public static void formSortedResult(
  LinkedHashMap<String, ArrayList<CKBResult>> sortedLeftAssertionRelMap,
  LinkedHashMap<String, ArrayList<CKBResult>> sortedRightAssertionRelMap,
  InputBean xBean){
  Timer timer = new Timer();
  AutoMap<String, ArrayList<CKBResult>> leftAssertionRelMap =
    new AutoMap<String, ArrayList<CKBResult>>();
  AutoMap<String, ArrayList<CKBResult>> rightAssertionRelMap =
    new AutoMap<String, ArrayList<CKBResult>>();

  try{
    // ////////////////////////////////////////////////////////////////////////
    // Fill left map: wn metadata, domain/range, attributes list, unclassified tuples
    // ////////////////////////////////////////////////////////////////////////
    leftCommonMeta(xBean, leftAssertionRelMap);
    timer.time("left map WN metadata ");
    leftSpecificMeta(xBean, leftAssertionRelMap);
    timer.time("left map specific data ");

    // Fill other senses
    if(xBean.inType.equals(Inputtype.a) || xBean.inType.equals(Inputtype.n))
      fillOtherSenses(xBean, leftAssertionRelMap);
    timer.time("left map other senses ");

    // Add more relations on left map here
    String[] orderedLeftRel =
      new String[] { "wn_gloss", "wn_hypernyms", "wn_hyponyms", "other_senses",
        "all_relations", "hasproperty", "rangedata", "domaindata",
        "inrangedata" }; // ,"indomaindata"
    // , "tupledata" };

    // ////////////////////////////////////////////////////////////////////////
    // Fill right map: sub-relations , auto add new relation that cannot be accommodated
    // on left.
    // ////////////////////////////////////////////////////////////////////////
    if(!xBean.inType.equals(Inputtype.r))
      rightSubRelations(xBean, rightAssertionRelMap);
    List<String> orderedLeftRelList = new ArrayList<>();
    for(int i = 0; i < orderedLeftRel.length; i++)
      orderedLeftRelList.add(orderedLeftRel[i]);
    for(String transferRel: leftAssertionRelMap.keySet()){
      if(!orderedLeftRelList.contains(transferRel))
        rightAssertionRelMap.put(transferRel, leftAssertionRelMap
          .get(transferRel));
    }
    timer.time("right map subrelation ");

    // Fill unclassified tuples.
    if(xBean.inType.equals(Inputtype.n)){
      fillUnclassifiedTuples(xBean, rightAssertionRelMap);
    }
    timer.time("right map unclassified tuples ");

    // order the maps, starting with left map.
    for(int i = 0; i < orderedLeftRel.length; i++){
      if(leftAssertionRelMap.containsKey(orderedLeftRel[i]))
        sortedLeftAssertionRelMap.put(orderedLeftRel[i], leftAssertionRelMap
          .get(orderedLeftRel[i]));
    }

    // Push these to the bottom of right map, other relations are at top.
    List<String> demotedRightSideRelations = new ArrayList<String>();
    demotedRightSideRelations.add("quality");
    demotedRightSideRelations.add("state");
    demotedRightSideRelations.add("unclassified");

    for(Entry<String, ArrayList<CKBResult>> e: rightAssertionRelMap.entrySet()){
      if((e.getValue().size() > 0)) // Atleast one adjective in relation
        // relations (to ensure we have enough information about this noun).
        if(!demotedRightSideRelations.contains(e.getKey()))
          sortedRightAssertionRelMap.put(e.getKey(), e.getValue());
    }

    // Place unclassified hasproperties at the last.
    for(String demotedR: demotedRightSideRelations)
      if(rightAssertionRelMap.containsKey(demotedR)){
        sortedRightAssertionRelMap.put(demotedR, rightAssertionRelMap
          .get(demotedR));
      }

    timer.time("ordered for GUI ");

  } catch (Exception e){
    e.printStackTrace();
  }

}

private static String inputPrintName(InputBean xBean){

  String typeFullName = "";
  switch (xBean.inType) {
  case a:
  typeFullName = "adjective";
  break;
  case n:
  typeFullName = "noun";
  break;
  case r:
  typeFullName = "relation";
  break;
  }
  return typeFullName + " : " + xBean.x;
}

private static void fillOtherSenses(InputBean xBean,
  AutoMap<String, ArrayList<CKBResult>> leftAssertionRelMap)
  throws SQLException{
  boolean isNoun = !xBean.inType.equals(Inputtype.a); // relation is also a noun.
  String type = xBean.inType.name();
  // Display all except the current word sense
  String sql =
    "select wordid from " + (isNoun ? "nounid" : "adjid") + " where word='"
      + xBean.x + "' and wordid!=" + xBean.xid + " order by sensenum";

  ResultSet rrs = db.query(sql);

  while (rrs.next()){
    leftAssertionRelMap.addArrayValue("other_senses", new CKBResult(type + "_"
      + InputBean.joinWID(xBean.x, xBean.xid), type + "_"
      + InputBean.joinWID(xBean.x, rrs.getInt(1)), 1, "other_senses", formHref(
      formMouseOver(rrs.getInt(1), isNoun), type + "_"
        + InputBean.joinWID(xBean.x, rrs.getInt(1)))));
  }
}

private static void fillUnclassifiedTuples(InputBean xBean,
  AutoMap<String, ArrayList<CKBResult>> rightAssertionRelMap)
  throws SQLException{
  boolean isMultiworded = xBean.x.contains("_");
  String x = isMultiworded ? xBean.x.replaceAll("_", " ") : xBean.x;

  String sql =
    "select y,numpatterns from " + hasPropertyUnclassifiedTb + " where (x='"
      + xBean.x + "' " + (isMultiworded ? " or x='" + x + "'" : "") + ") and "
      + "  y not in (select y from " + hasPropertyClassifiedTb + " where x='"
      + xBean.x + "'" + " ) order by numsources desc";

  ResultSet unclassRS = db.query(sql + " limit 100");
  // <A STYLE="text-decoration:none;" HREF=yellow
  // <A STYLE="text-decoration:none;" HREF="">yellow</a>
  // "<A STYLE=\"text-decoration:none;\"HREF=\"#\">" + unclassRS.getString(1) + "</a>"
  while (unclassRS.next())
    rightAssertionRelMap.addArrayValue("unclassified", new CKBResult(xBean.x,
      unclassRS.getString(1), Util.sigmoid(unclassRS.getInt(2) - 2),
      "unclassified", null));
}

private static void rightSubRelations(InputBean xBean,
  AutoMap<String, ArrayList<CKBResult>> rightAssertionRelMap)
  throws SQLException{
  String typeSpecificQuery = formTypeSpecificTupleQuery(xBean);

  // TODO tuple data not getting updated with x,y : rather only y
  String query =
    "select * from " + hasPropertyClassifiedTb + " where " + typeSpecificQuery
      + " and rangescore>=" + minRangeScore + " and scoresds>=" + minTupleScore
      + " order by freq desc";
  // + " order by rangescore desc, scoresds desc, numpatterns desc";
  ResultSet subPropRS = db.query(query + " limit 5000");

  // Allow repetitions across sentences to demonstrate the hierarchy problem of WebChild.
  // (x,y classified as appearance and beauty both)
  HashSet<String> alreadyAddedY = new HashSet<String>();

  while (subPropRS.next()){
    if(alreadyAddedY.contains(subPropRS.getString("r") + "\t"
      + subPropRS.getString("y"))) continue;
    alreadyAddedY.add(subPropRS.getString("r") + "\t"
      + subPropRS.getString("y"));
    // Hardcoded unfortunately to avoid so much noise due to one.
    if(subPropRS.getString("r").equals("motion")
      && subPropRS.getString("y").equals("first")) continue;

    double nafreq = subPropRS.getDouble("freq");
    nafreq =
      getNAFreq(subPropRS.getString("x"), subPropRS.getString("y"), nafreq);
    if(nafreq < 0) continue; // TODO prune low freq exotic tuples.

    ResultSet ysynrs =
      db.query("select * from adjid where wordid=" + subPropRS.getInt("yid"));

    if(ysynrs.next()){
      int yid = ysynrs.getInt("wordid");
      boolean isSeedTuple =
        isTupleSeed(subPropRS.getString("r"), xBean.xid, yid);

      String yGloss = ysynrs.getString("gloss");
      // TODO for browser , require ywps for exact sense.
      String yRaw = ysynrs.getString("word");

      if(isSeedTuple) yRaw = "***" + yRaw; // TODO change color later on.
      String yForHref =
        yRaw + "_" + Util.format(subPropRS.getDouble("scoresds")) + "_" + yid
          + "_" + yGloss;

      rightAssertionRelMap.addArrayValue(subPropRS.getString("r"),
        new CKBResult("n_" + xBean.x + xBean.senseSeparator + xBean.xid, "a_"
          + yRaw + xBean.senseSeparator + yid, subPropRS.getDouble("scoresds"),
          subPropRS.getString("r"), formHref(yForHref, "a_" + yRaw
            + xBean.senseSeparator + yid, Double.toString(subPropRS
            .getDouble("scoresds"))
            + " , freq: "
            + Util.format(nafreq)
            + " , numpatterns: "
            + Integer.toString(subPropRS.getInt("numpatterns"))
            + " , sources: " + subPropRS.getString("sources")), null, yid));
    }
  }
}

private static boolean isTupleSeed(String r,int xid,int yid){
  String sql =
    "select r from _tuplegold where r='" + r + "' and yid=" + yid + " and xid="
      + xid;
  try{
    ResultSet rs = DBConnector.getDB().query(sql);
    if(rs.next()) return true;
  } catch (Exception e){
    // do nothing seed not found.
  }
  return false;
}

private static boolean isDomainSeed(String r,int yid){
  String sql =
    "select yid from _domaingold where x='" + r + "' and yid=" + yid
      + " and evaltype='seed'";
  try{
    ResultSet rs = DBConnector.getDB().query(sql);
    if(rs.next()) return true;
  } catch (Exception e){
    // do nothing seed not found.
  }
  return false;
}

private static boolean isRangeSeed(String r,int yid){
  String sql =
    "select yid from _rangegold where x='" + r + "' and yid=" + yid
      + " and evaltype='seed'";
  try{
    ResultSet rs = DBConnector.getDB().query(sql);
    if(rs.next()) return true;
  } catch (Exception e){
    // do nothing seed not found.
  }
  return false;
}

private static String formTypeSpecificTupleQuery(InputBean xBean){
  String typeSpecificQuery = "";
  switch (xBean.inType) {
  case r:
  typeSpecificQuery = "r='" + xBean.x + "'";
  break;
  case n:
  typeSpecificQuery = "xid=" + xBean.xid;
  break;
  case a:
  typeSpecificQuery = "yid=" + xBean.xid;
  break;

  default:
  break;
  }
  return typeSpecificQuery;
}

/**
 * TODO: left meta of a relation, r_wps.
 * @param xBean
 * @param leftAssertionRelMap
 * @throws Exception 
 */
private static void leftSpecificMeta(InputBean xBean,
  AutoMap<String, ArrayList<CKBResult>> leftAssertionRelMap) throws Exception{
  Timer timer = new Timer();
  leftAssertionRelMap.put("all_relations", cachedRelationMap
    .get("all_relations"));
  /*for(Entry<String, Integer> e: neededRelations.entrySet())
    leftAssertionRelMap.addArrayValue("all_relations", new CKBResult("n_"
      + InputBean.joinWID(xBean.x, xBean.xid), "r_"
      + InputBean.joinWID(e.getKey(), e.getValue()), 1, "all_relations",
      formHref(formMouseOver(e.getValue(), true), "r_"
        + InputBean.joinWID(e.getKey(), e.getValue()))));*/
  timer.time("    relations ");
  switch (xBean.inType) {

  case a:
  // inRange and some tuples.
  for(Entry<String, Double> e: partOfAttr(xBean.x, false, xBean.xid).entrySet())
    leftAssertionRelMap.addArrayValue("inrangedata", new CKBResult("n_"
      + InputBean.joinWID(xBean.x, xBean.xid), "r_"
      + InputBean.joinWID(e.getKey(), neededRelations.get(e.getKey())), e
      .getValue(), "inrangedata", formHref(formMouseOver(neededRelations.get(e
      .getKey()), true), "r_"
      + InputBean.joinWID(e.getKey(), neededRelations.get(e.getKey())))));
  timer.time("    adj inrange ");

  String tuplesYSQL =
    "select x,xid,r,scoresds,numpatterns,sources,freq from "
      + hasPropertyClassifiedTb + " where yid=" + xBean.xid
      + "  order by freq desc limit 500";
  // "  order by rangescore desc, scoresds desc, numpatterns desc limit 500";

  ResultSet tyrs = DBConnector.getDB().query(tuplesYSQL);
  timer.time("    adj tuples 1 (query )");
  // add tuple data (relation and nouns) unlike relation.tupledata (n,a))
  HashSet<String> alreadyAddedX = new HashSet<String>();
  while (tyrs.next()){
    if(alreadyAddedX.contains(tyrs.getString("r") + "\t" + tyrs.getString("x")))
      continue;
    alreadyAddedX.add(tyrs.getString("r") + "\t" + tyrs.getString("x"));

    double nafreq = tyrs.getDouble("freq");
    nafreq = getNAFreq(tyrs.getString(1), xBean.x, nafreq);
    if(nafreq < 0) continue; // TODO prune low freq exotic tuples.

    String mouseOverString =
      formMouseOver(tyrs.getInt(2), true, Util.format(tyrs.getDouble(4)));
    boolean isSeedTuple =
      isTupleSeed(tyrs.getString("r"), tyrs.getInt(2), xBean.xid);
    if(isSeedTuple) mouseOverString = "***" + mouseOverString;

    leftAssertionRelMap.addArrayValue(tyrs.getString(3), new CKBResult("n_"
      + InputBean.joinWID(tyrs.getString(1), tyrs.getInt(2)), "a_"
      + InputBean.joinWID(xBean.x, xBean.xid), tyrs.getDouble(4), tyrs
      .getString(3), formHref(mouseOverString, "n_"
      + InputBean.joinWID(tyrs.getString(1), tyrs.getInt(2)), Double
      .toString(tyrs.getDouble(4))
      + " , freq: "
      + Util.format(nafreq)
      + " , numpatterns: "
      + Integer.toString(tyrs.getInt(5))
      + " , sources: "
      + tyrs.getString("sources"))));
  }

  timer.time("    adj tuples 2 (obj creation )");
  break;

  case n:
  // inDomain and some tuples.
  /*for(Entry<String, Double> e: partOfAttr(xBean.x, true, xBean.xid).entrySet())
    leftAssertionRelMap.addArrayValue("indomaindata", new CKBResult("n_"
      + InputBean.joinWID(xBean.x, xBean.xid), "r_"
      + InputBean.joinWID(e.getKey(), neededRelations.get(e.getKey())), e.getValue(),
      "indomaindata", formHref(formMouseOver(neededRelations.get(e.getKey()), true), "r_"
        + InputBean.joinWID(e.getKey(), neededRelations.get(e.getKey())))));

  timer.time("    noun indomain ");*/

  break;

  case r:
  // some tuples
  // TODO adjust cutoff based on avg(range_score) of the relation.

  String orderByPredicate =
    " and rangescore>0.3 and numpatterns>1 and scoresds>0.2 "
      + " order by freq desc, rangescore desc, scoresds desc ";

  // Hardcoded unfortunately to avoid so much noise due to one or two words and different
  // style of ranking for these relations as their rangescore was below avg. Could have
  // achieved the same with avg(scoresds) from _range.
  if(xBean.x.equals("shape") || xBean.x.equals("feeling")
    || xBean.x.equals("beauty") || xBean.x.equals("smell")
    || xBean.x.equals("motion") || xBean.x.equals("appearance")
    || xBean.x.equals("temperature") || xBean.x.equals("size")
    || xBean.x.startsWith("taste")){
    orderByPredicate =
      " and rangescore>0.2 and numpatterns>1 and numsources>0 and scoresds>0.3 order by freq desc, scoresds desc ";
    if(xBean.x.equals("shape"))
      orderByPredicate = " and y!='high'" + orderByPredicate;
    if(xBean.x.equals("beauty"))
      orderByPredicate =
        " and y!='fair' and ysyn!=300751838 and ysyn!=300752110 and rangescore>=0.05 and numpatterns>1 and numsources>0 and scoresds>0.3 "
          + "order by freq desc, scoresds desc ";
    if(xBean.x.equals("appearance"))
      orderByPredicate =
        "and ysyn!=300751838 and ysyn!=300752110 and rangescore>0.05 and numpatterns>1 and numsources>0 and scoresds>0.3 "
          + "order by freq desc, scoresds desc, rangescore desc ";
    if(xBean.x.equals("feeling"))
      orderByPredicate =
        " and ysyn!=301890187 and rangescore>0.05 and numpatterns>1 and numsources>0 and scoresds>0.3 "
          + "order by freq desc, scoresds desc, rangescore desc ";
    if(xBean.x.equals("motion"))
      orderByPredicate =
        "  and y!='light' and y!='opening' and y!='first'  and rangescore>0 and numpatterns>1 and numsources>0 and scoresds>0.3 "
          + "order by freq desc, scoresds desc, rangescore desc ";
    if(xBean.x.equals("size"))
      orderByPredicate =
        "and rangescore>0.1 and numpatterns>1 and numsources>0 and scoresds>0.3 "
          + "order by freq desc,  rangescore desc, scoresds desc";
    if(xBean.x.equals("temperature"))
      orderByPredicate =
        " and y!='raw' and  y!='sensitive' and rangescore>0.03 and numpatterns>2 and numsources>0 and scoresds>0.3 "
          + "order by freq desc, scoresds desc ";
    if(xBean.x.startsWith("taste"))
      orderByPredicate =
        " and xid!=88993 and xid!=88994 and rangescore>0.01 and numpatterns>2 and numsources>0 and scoresds>0.3 "
          + "order by  freq desc, scoresds desc, rangescore desc ";
  }

  String tuplesSQL =
    "select x,xid,y,yid,scoresds,numpatterns,sources,freq from "
      + hasPropertyClassifiedTb + " where r='" + xBean.x + "'  "
      + orderByPredicate + " limit 5000";
  ResultSet trs = DBConnector.getDB().query(tuplesSQL);
  timer.time("    relation tuples 1 (query )");
  HashSet<String> alreadyAddedXY = new HashSet<String>();
  while (trs.next()){
    if((xBean.x.equals(trs.getString(1)))
      || (xBean.x.startsWith("taste_property") && trs.getString(1).equals(
        "taste"))){
      continue;
    }

    if(alreadyAddedXY.contains(trs.getString("x") + "\t" + trs.getString("y")))
      continue;
    alreadyAddedXY.add(trs.getString("x") + "\t" + trs.getString("y"));

    double nafreq = trs.getDouble("freq");
    nafreq = getNAFreq(trs.getString(1), trs.getString(3), nafreq);
    if(nafreq < 0) continue; // TODO prune low freq exotic tuples.

    boolean isSeedTuple = isTupleSeed(xBean.x, trs.getInt(2), trs.getInt(4));
    String mouseOverStringX =
      formMouseOver(trs.getInt(2), true, Util.format(trs.getDouble(5)));
    String mouseOverStringY =
      formMouseOver(trs.getInt(4), false, Util.format(trs.getDouble(5)));

    if(isSeedTuple){
      mouseOverStringX = "***" + mouseOverStringX;
      mouseOverStringY = "***" + mouseOverStringY;
    }

    leftAssertionRelMap.addArrayValue("tupledata", new CKBResult("r_"
      + InputBean.joinWID(xBean.x, xBean.xid), "n_"
      + InputBean.joinWID(trs.getString(1), trs.getInt(2)) + "," + "a_"
      + InputBean.joinWID(trs.getString(3), trs.getInt(4)), trs.getDouble(5),
      "tupledata", formHref(mouseOverStringX, "n_"
        + InputBean.joinWID(trs.getString(1), trs.getInt(2)), Double
        .toString(trs.getDouble(5))
        + " , freq: "
        + Util.format(nafreq)
        + ", numpatterns: "
        + trs.getInt(6)
        + " , sources: " + trs.getString(7) + " , ")
        + ",<A STYLE=\"text-decoration:none;\"HREF="
        + formHref(mouseOverStringY, "a_"
          + InputBean.joinWID(trs.getString(3), trs.getInt(4)), Double
          .toString(trs.getDouble(5))
          + " , freq: "
          + Util.format(nafreq)
          + ", numpatterns: "
          + trs.getInt(6) + " , sources: " + trs.getString(7) + " , ")));
  }

  timer.time("    relation tuples 2 (obj creation )");

  // domain (here, taste is not written as taste_property)
  String dirtiedRelName = xBean.x;
  if(dirtiedRelName.equalsIgnoreCase("taste_property"))
    dirtiedRelName = "taste";
  if(dirtiedRelName.equalsIgnoreCase("smell")) dirtiedRelName = "olfactory";

  String domainSQL =
    "select y, yid, scoresds from " + domainTb + " where x='" + dirtiedRelName
      + "' order by scoresds desc limit 300";
  ResultSet drs = DBConnector.getDB().query(domainSQL);
  while (drs.next()){

    String mouseOverDomainX =
      formMouseOver(drs.getInt(2), true, Util.format(drs.getDouble(3)));
    if(isDomainSeed(dirtiedRelName, drs.getInt(2)))
      mouseOverDomainX = "***" + mouseOverDomainX;

    leftAssertionRelMap.addArrayValue("domaindata", new CKBResult("r_"
      + InputBean.joinWID(xBean.x, xBean.xid), "n_"
      + InputBean.joinWID(drs.getString(1), drs.getInt(2)), drs.getDouble(3),
      "domaindata", formHref(mouseOverDomainX, "n_"
        + InputBean.joinWID(drs.getString(1), drs.getInt(2)), Double
        .toString(drs.getDouble(3)))));
  }

  timer.time("    relation domain ");

  // range (here, taste is not written as taste_property)
  String rangeSQL =
    "select y, yid, scoresds from " + rangeTb + " where x='" + dirtiedRelName
      + "'  order by scoresds desc limit 300";
  ResultSet rrs = DBConnector.getDB().query(rangeSQL);
  while (rrs.next()){
    String mouseOverRangeX =
      formMouseOver(rrs.getInt(2), false, Util.format(rrs.getDouble(3)));
    if(isRangeSeed(dirtiedRelName, rrs.getInt(2)))
      mouseOverRangeX = "***" + mouseOverRangeX;
    leftAssertionRelMap.addArrayValue("rangedata", new CKBResult("r_"
      + InputBean.joinWID(xBean.x, xBean.xid), "a_"
      + InputBean.joinWID(rrs.getString(1), rrs.getInt(2)), rrs.getDouble(3),
      "rangedata", formHref(mouseOverRangeX, "a_"
        + InputBean.joinWID(rrs.getString(1), rrs.getInt(2)), Double
        .toString(rrs.getDouble(3)))));
  }
  timer.time("    relation range ");
  break;

  default:
  break;
  }
}

private static void leftCommonMeta(InputBean xBean,
  AutoMap<String, ArrayList<CKBResult>> leftAssertionRelMap) throws Exception{
  WNResult wnResult = formWNQuery(xBean.xid, !xBean.inType.equals(Inputtype.a));

  if(!xBean.inType.equals(Inputtype.a)){
    Set<wps> hypoList = new HashSet<wps>();
    for(int hypo: wnResult.hyponyms)
      hypoList.addAll(IDHelper.getwps(hypo));

    for(wps hypo: hypoList){
      leftAssertionRelMap.addArrayValue("wn_hyponyms", new CKBResult("n_"
        + InputBean.joinWID(xBean.x, xBean.xid), "n_" + hypo.w
        + InputBean.senseSeparator + IDHelper.idMeta(hypo).wordID, 1,
        "hyponyms", formHref(formMouseOver(hypo), "n_" + hypo.w
          + InputBean.senseSeparator + IDHelper.idMeta(hypo).wordID)));
    }

    Set<wps> hyperList = new HashSet<wps>();
    for(int hyper: wnResult.parents)
      hyperList.addAll(IDHelper.getwps(hyper));

    for(wps parent: hyperList)
      leftAssertionRelMap.addArrayValue("wn_hypernyms", new CKBResult("n_"
        + InputBean.joinWID(xBean.x, xBean.xid), "n_" + parent.w
        + InputBean.senseSeparator + IDHelper.idMeta(parent).wordID, 1,
        "hypernyms", formHref(formMouseOver(parent), "n_" + parent.w
          + InputBean.senseSeparator + IDHelper.idMeta(parent).wordID)));

  }
  String gloss = wnResult.gloss;
  // Make gloss shorter..
  StringBuilder glossBuilder = new StringBuilder();
  String[] glossWords = gloss.split(" ");
  int maxWords = 8; // 8 words wrap looks good on browser UI
  for(int i = 0; i < glossWords.length; i++){
    if((i + 1) % maxWords == 0) glossBuilder.append("<br>");
    glossBuilder.append(glossWords[i]).append(" ");
  }
  leftAssertionRelMap.addArrayValue("wn_gloss", new CKBResult(xBean.x,
    glossBuilder.toString(), 1, "gloss", null));
}

private static String formMouseOver(int wid,boolean isNoun,String score)
  throws SQLException{

  ResultSet ysynrs =
    db.query("select * from " + (isNoun ? "nounid" : "adjid")
      + " where wordid=" + wid);
  String yForHref = "";
  // sweet_patternSupport_synset_shortgloss
  if(ysynrs.next()){
    int yid = ysynrs.getInt("wordid");
    String yGloss = ysynrs.getString("gloss");
    String yRaw = ysynrs.getString("word");
    // Default score of 1 is given.
    yForHref = yRaw + "_" + score + "_" + yid + "_" + yGloss; // 1 replaced with ""
  }
  return yForHref;

}

private static String formMouseOver(int wid,boolean isNoun) throws SQLException{
  return formMouseOver(wid, isNoun, "");
}

private static String formMouseOver(wps wps) throws SQLException{
  return formMouseOver(IDHelper.idMeta(wps).wordID, wps.p.name()
    .equalsIgnoreCase("noun"));
}

public static class InputBean {
public final static String senseSeparator = "::"; // red::12387
public String xCleanedOrig;
public String x;
public int xid;
public Inputtype inType;
// input adj, noun or relation
public enum Inputtype {
  // removed t because t is just a,n; not a separate type
  a, n, r
};
public boolean isSensified;

public InputBean(String xRaw) {
  // this.xCleanedOrig = Util.applySingularizeRule(xRaw).toLowerCase().replaceAll(" ",
  // "_");
  this.xCleanedOrig = xRaw.toLowerCase().replaceAll(" ", "_");
  // Hardcoded relation name.
  if(this.xCleanedOrig.startsWith("r_olfactory::"))
    this.xCleanedOrig = this.xCleanedOrig.replace("olfactory", "smell");
  if(this.xCleanedOrig.startsWith("r_taste::"))
    this.xCleanedOrig = this.xCleanedOrig.replace("taste", "taste_property");

  if(this.xCleanedOrig.startsWith("a_")){
    inType = Inputtype.a;
    this.x = xCleanedOrig.substring(2); // a_ removed.
  } else if(this.xCleanedOrig.startsWith("n_")){
    inType = Inputtype.n;
    this.x = xCleanedOrig.substring(2);
  } else if(this.xCleanedOrig.startsWith("r_")){
    inType = Inputtype.r;
    this.x = xCleanedOrig.substring(2);
  } else{
    inType = Inputtype.n; // removed t @v:1.0
    this.x = xCleanedOrig;
  }
  this.isSensified = this.x.contains(senseSeparator);
  if(this.isSensified){
    String[] splits = this.x.split(senseSeparator);
    this.x = splits[0];
    this.xid = Integer.parseInt(splits[1]);
  } else{
    // this is a noun like car.. with no sense id, provide MFS.
    try{
      // fill xid of MFS of noun/adj
      xid =
        IDHelper.idMeta(new wps(this.x, this.inType.equals(Inputtype.a) ? 'a'
          : 'n', 1)).wordID;
    } catch (SQLException e){
      xid = -1;
      e.printStackTrace();
    }
  }
}

public static String joinWID(String w,int id){
  return new StringBuilder(w).append(senseSeparator).append(id).toString();
}

}

private static String formHref(String x,String xWithID){
  if(xHrefNoProj) return xWithID; // the new UI maker auto-adds project name
  if(x == null || x.trim().length() == 0){
    StringBuilder o = new StringBuilder();
    String hrefString =
      "/" + ckbProjectName + "/" + ckbBrowserName + "?x=" + xWithID;
    o.append("<a href =\"").append(hrefString).append("\">").append(x).append(
      "</a>");

    return o.toString();
  } else{
    return formHref(x, xWithID, "1.0");
  }
}

private static String formHref(String x,String xWithID,String support){
  StringBuilder o = new StringBuilder();
  String href = xWithID;
  if(xHrefNoProj) return href;

  String[] f = new String[2];
  // TODO bug: sweet_potato_patternSupport_synset_shortgloss
  f = x.split("_"); // sweet_patternSupport_synset_shortgloss
  int numEntries = f.length;

  String xWord = f[0];
  if(numEntries > 4) for(int i = 1; i <= numEntries - 4; i++)
    xWord += "_" + f[i];

  String patterSupStr = f[numEntries - 3];
  String glossStr = f[numEntries - 1];

  String supportPrunedDecimalPlaces =
    (patterSupStr.length() > 0) ? "score: " + support + ", " : "";

  supportPrunedDecimalPlaces +=
    (glossStr.length() > 0) ? " gloss: " + cleanGloss(glossStr) : "";

  String mouseOverStr =
    "title=\"" + supportPrunedDecimalPlaces + "\" onclick=\"return true\"";
  String hrefString =
    "/" + ckbProjectName + "/" + ckbBrowserName + "?x=" + href;
  // <a href ommitted because it will be added in the yagolikebrowser util..
  o.append(hrefString).append("\" ").append(mouseOverStr).append(
    " >").append(xWord).append("</a>");
  return o.toString();
}

private static String cleanGloss(String glossStr){
  String g = glossStr.replaceAll("\"", "-");
  return g;
}

protected void doGet(HttpServletRequest request,HttpServletResponse response)
  throws ServletException,IOException{
  String x = request.getParameter("x");
  System.out.println(request.getRequestedSessionId() == null ? x : request
    .getRequestedSessionId()
    + "\t" + x);
  if(x != null)
    x = x.trim();
  else x = "lily";
  if(x.length() == 0) x = "lily";
  String responseText = retrieveWithSubRelDirectlyFromDb(x);
  PrintWriter out = response.getWriter();
  response.setContentType("text/html");
  out.println(responseText);
}

private static Map<String, Integer> getAllAttrs() throws SQLException{
  Map<String, Integer> relations = new HashMap<String, Integer>();
  String sql = "select distinct(x,xid) from _range";
  ResultSet rs = db.query(sql);
  while (rs.next()){
    String relID = rs.getString(1);
    int endIdx = relID.indexOf(',');
    String relName = relID.substring(1, endIdx);
    int relID1 =
      Integer.parseInt(relID.substring(endIdx + 1, relID.length() - 1));
    relations.put(relName, relID1);
  }
  return relations;
}

public static void main(String[] args) throws SQLException{
  WebchildBrowserOld runner = new WebchildBrowserOld();
  runner.variablesInit(false);
  // apple, n_lilium_philadelphicum::118488, r_weight::48711, news, rain forest
  System.out
    .println(runner.retrieveWithSubRelDirectlyFromDb("r_beauty::45336"));
}
}

class CKBResult implements Comparator<CKBResult> {
public String x;
public String y;
public double support;
public String relation;
public String xHref;
public String styleColor;
public int ysynset;

public CKBResult(String x,String y,String relation) {
  this(x, y, 1, relation, y);
}

public CKBResult(String x,String y,double support,String relation,String href) {
  this(x, y, support, relation, href, null);
}

public CKBResult(String x,String y,double support,String relation,String href,
  String color) {
  this(x, y, support, relation, href, color, -1);
}

public CKBResult(String x,String y,double support,String relation,String href,
  String color,int synset) {
  this.x = x;
  this.y = y;
  this.support = support;
  this.relation = relation;
  this.xHref = href;
  this.styleColor = color;
  this.ysynset = synset;
}

@Override public String toString(){
  return "CKBAssertion [x=" + x + ", y=" + y + ", support=" + support
    + ", relation=" + relation + "]";
}

@Override public int compare(CKBResult o1,CKBResult o2){
  if(o1.support > o2.support)
    return 1;
  else if(o1.support < o2.support)
    return -1;
  // else if(o1.x.equals(o2.x) && o1.y.equals(o2.y) && o1.relation.equals(o2.relation)) return 0;
  else return 0;
}
}

class WNResult {
public int wordID;
public int synsetID;
public Stack<Integer> parents;
public Set<Integer> hyponyms;
public String gloss;

WNResult(int wordID,int synsetID,Stack<Integer> parents,Set<Integer> hyponyms,
  String gloss) {
  this.wordID = wordID;
  this.synsetID = synsetID;
  this.parents = parents;
  this.hyponyms = hyponyms;
  this.gloss = gloss;
}
}