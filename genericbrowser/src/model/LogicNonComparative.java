package model;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import javatools.datatypes.Triple;
import model.WebchildBrowserOld.InputBean;
import util.IDHelper;
import util.IDHelper.IDSubjObj;
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

public class LogicNonComparative {

private static int tripleID;
private static Triple<String, String, String> disambiXAY;

/**
 * @param in e.g. car,bike
 * @param atoms <car,fast,bike>; <bike,efficient,bike>
 * @return 
 */
public String dbfetch(InputToModel in,List<Atom> atoms,List<ResultRow> rows){
  try{
    // if(rnormTriples == null) initRnormTriples();
    // similarToX = similarNoun(in.x.get(0));
    tripleID = 1;
    // TODO what if input is an adjective not noun?
    String inputtype = in.metadata.get(0).equals("a") ? "a" : "n";
    String inputDisambiguated =
      in.x.get(0).contains("#") || in.x.get(0).contains("::") ? in.x.get(0)
        : in.x.get(0) + "#" + inputtype + "#1";

    disambiXAY = new Triple<String, String, String>(inputDisambiguated, "", "");
    Timer timer = new Timer();
    // Fetch matching csynsets
    System.out.println("Fetch matching csynsets ...");

    LinkedHashMap<String, ArrayList<CKBResult>> sortedLeftAssertionRelMap =
      new LinkedHashMap<String, ArrayList<CKBResult>>();
    LinkedHashMap<String, ArrayList<CKBResult>> sortedRightAssertionRelMap =
      new LinkedHashMap<String, ArrayList<CKBResult>>();

    InputBean xBean = new InputBean(in.x.get(0));
    if(xBean.xid < 0){
      // Perhaps in plural form
      xBean = new InputBean(Util.applySingularizeRule(in.x.get(0)));
      // Perhaps an Ad hoc concept
      if(xBean.xid < 0) xBean = new InputBean(headWord(in.x.get(0)));
      // Lost hope!
      return "Word [" + in.x.get(0) + "] not found!";
    }
    timer.time("input bean created ");

    WebchildBrowserOld.formSortedResult(sortedLeftAssertionRelMap,
      sortedRightAssertionRelMap, xBean);

    // //////////////
    // For each csynset, construct UIAtoms from synset member ctriples.
    System.out.println("For each csynset, construct UIAtoms of ctriples ...");
    fillCSynsetAtomsOrdered(sortedLeftAssertionRelMap, rows, in);
    fillCSynsetAtomsOrdered(sortedRightAssertionRelMap, rows, in);
    timer.time("created GUI ");
  } catch (Exception e){
    e.printStackTrace();
  }

  return resultsDisambiguationSummary(disambiXAY, in);
}

private String headWord(String x){
  int lastSpace = x.indexOf(' ');
  return lastSpace > 0 ? x.substring(lastSpace) : x;
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

private String resultsDisambiguationSummary(
  Triple<String, String, String> disambiXAY2,InputToModel in){
  String formattedSummary = "<h4>";
  if(disambiXAY2 != null){
    try{
      IDSubjObj defaultIDSubj =
        new IDSubjObj(disambiXAY2.first, in.x.get(0), "could not fetch gloss");
      String inputtype = in.metadata.get(0);

      boolean isWordID = disambiXAY2.first.contains("::");

      int wordID = -1;
      if(isWordID)
        wordID =
          Integer.parseInt(disambiXAY2.first.substring(disambiXAY2.first
            .indexOf("::") + 2));// n_vigour::48746 -> 48746
      String tb = inputtype.equals("a") ? "adjid" : "nounid";
      IDSubjObj metaX =
        isWordID ? IDHelper.getTextMetaSlowly(wordID, tb) : (disambiXAY2.first
          .length() > 0 ? IDHelper.getTextMetaSlowly(disambiXAY2.first, tb)
          : defaultIDSubj);
      formattedSummary +=
        "<h4>" + (metaX.word == null ? "" : metaX.word) + ":&nbsp;\t&nbsp;"
          + shortenGloss((metaX.gloss == null ? "" : metaX.gloss));

    } catch (Exception e){
      System.out.println("Exception fetching gloss for " + disambiXAY2.first);
      /*formattedSummary +=
        "<h4>" + " inputword " + ":&nbsp;\t&nbsp;"
          + shortenGloss("could not fetch gloss") + "\n<br>" + " inputword "
          + ":&nbsp;\t&nbsp;" + shortenGloss("could not fetch gloss");*/
    }
    formattedSummary += "</h4>";

  } else{
    formattedSummary += in.x.get(0) + "&nbsp;";
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

/** man,woman --> fill important (high frequency/ size of csynset/
 if there is an oppose set.) vertical tabs
 
 * @param m temperature, list of temperature related attributes for input noun
 * @param atoms need to fill these for displaying in GUI
 * @param in 
 * @throws SQLException
 */
private void fillCSynsetAtomsOrdered(
  LinkedHashMap<String, ArrayList<CKBResult>> m,List<ResultRow> rows,
  InputToModel in) throws SQLException{

  // Foreach csynset; adj -> ctriples
  for(Entry<String, ArrayList<CKBResult>> e: m.entrySet()){
    // inrangedata in logicNC must be handled correctly
    List<DbTriple> triples =
      memberTriples(e.getValue(), in, e.getKey()
        .equalsIgnoreCase("inrangedata"));
    String repAdj = e.getKey(); // relation

    // Supporting triples
    for(DbTriple t: triples){
      fillARow(rows, t, repAdj);
      // fillAnAtom(color.black, repAdj, t, atoms);
    }
  }
}

private void fillARow(List<ResultRow> rows,DbTriple t,String clusterName){
  // Vertical tab labels do not accept spaces.
  clusterName = clusterName.replaceAll(" ", "_");
  tripleID++; // same tripleID for each rowElem
  List<Atom> rowElems = new ArrayList<>();
  rowElems.add(constructAnAtom(color.black, clusterName, t.href[0],
    t.tooltip[0], t.x[0]));
  if(t.tooltip.length == 2)
    rowElems.add(constructAnAtom(color.black, clusterName, t.href[1],
      t.tooltip[1] + "", t.x[1]));
  rows.add(new ResultRow(rowElems, clusterName));
}

private Atom constructAnAtom(color c,String clusterName,String href,
  String toolTip,String content){
  Decoration d =
    new Decoration(c, font.normal, boldface.normal, fontsize.normal,
      background.white);
  return new Atom(AtomType.content, tripleID, content, href, toolTip,
    clusterName, d); // t.adj contains sensified adj/noun
}

private List<DbTriple> memberTriples(ArrayList<CKBResult> results,
  InputToModel in,boolean valNotInXarg) throws SQLException{
  List<DbTriple> triples = new ArrayList<>();
  for(CKBResult r: results){
    if(r.y.contains(",")){
      triples.add(splitPairedResult(r));
    } else triples.add(ckbresultToHTMLTriple(r, in, valNotInXarg));
  }
  return triples;
}

private DbTriple splitPairedResult(CKBResult r){
  String[] splittedXIDs = r.y.split(",");
  String[] splittedXsRaw = r.y.split(",");
  splittedXsRaw[0] = joinedWAndIDToRaw(splittedXIDs[0]);
  splittedXsRaw[1] = joinedWAndIDToRaw(splittedXIDs[1]);

  CKBResult[] results = new CKBResult[2];
  results[0] =
    new CKBResult(r.x, splittedXsRaw[0], r.support, r.relation, splittedXIDs[0]);
  results[1] =
    new CKBResult(r.x, splittedXsRaw[1], r.support, r.relation, splittedXIDs[1]);
  return new DbTriple(r.relation, new String[] { splittedXsRaw[0],
    splittedXsRaw[1] }, new String[] { results[0].xHref, results[1].xHref },
    new String[] { results[0].support + "", results[1].support + "" });
}

// a_lumbering::15136 to lumbering
private String joinedWAndIDToRaw(String w){
  return w.substring(w.indexOf("_") + 1, w.indexOf("::"));
}

private DbTriple ckbresultToHTMLTriple(CKBResult r,InputToModel in,
  boolean valNotInXarg){
  // (valNotInXarg): // LogicNC ckbResult juxtaposed result args.
  boolean isAdj = in.metadata.get(0).equals("a");
  String mainSecondArg = isAdj ? r.x : r.y;
  if(valNotInXarg && isAdj) mainSecondArg = r.y;
  String[] y = mainSecondArg.split(InputBean.senseSeparator);
  String secondArg =
    y.length == 2 ? y[0].substring(y[0].indexOf('_') + 1) : mainSecondArg;

  DbTriple triple =
    new DbTriple(r.relation, new String[] { secondArg },
      new String[] { r.xHref }, new String[] { r.support + "" });
  return triple;
}

private static class DbTriple {
public String relation; // car#n#1, fast#a#1 has e.g. two x but one relation (speed)
public String[] x;
public String[] href;
public String[] tooltip;

public DbTriple(String relation,String[] x,String[] href,String[] tooltip) {
  super();
  this.relation = relation;
  this.x = x;
  this.href = href;
  this.tooltip = tooltip;
}

}

}
