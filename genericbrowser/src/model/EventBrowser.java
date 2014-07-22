package model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.xml.internal.fastinfoset.stax.EventLocation;
import model.Autocompletion.Trie;
import model.EventKBLogic.PrevOrNextEvent;
import util.AutoMap;
import util.FileLines;
import view.UI;
import visualcsk.moviescripts.EventKB;
import controller.InputToView;
import controller.InputToView.Atom;
import controller.InputToView.AtomType;
import controller.InputToView.Autocomplete;
import controller.InputToView.Decoration;
import controller.InputToView.Decoration.background;
import controller.InputToView.Decoration.boldface;
import controller.InputToView.Decoration.color;
import controller.InputToView.Decoration.font;
import controller.InputToView.Decoration.fontsize;
import controller.InputToView.ResultRow;
import controller.InputToView.SubmitForm;
import controller.InputToView.SubmitForm.buttonInfo;

public class EventBrowser extends HttpServlet {

private static final long serialVersionUID = 1L;

public static AutoMap<String, EventKB> m;

public void init(ServletConfig config) throws ServletException{
  super.init(config);
  try{
    m =
      loadJson("/var/tmp/git-repository/genericbrowser/WebContent/data/eventkb.json");

    if(Autocompletion.autoCompletionEvent == null){
      Autocompletion.autoCompletionEvent = new Trie();
      for(Entry<String, EventKB> e: m.entrySet()){
        if(e.getValue().freqCountInCorpus > 1)
          Autocompletion.autoCompletionEvent.insert(e.getKey());
      }
    }
  } catch (FileNotFoundException e){
    e.printStackTrace();
    m = new AutoMap<>();
  }
}

protected void doGet(HttpServletRequest request,HttpServletResponse response)
  throws ServletException,IOException{
  // Atoms
  List<Atom> atomsWithHeader = new ArrayList<>();
  atomsWithHeader.add(header());
  List<ResultRow> rows = new ArrayList<>();
  List<Atom> relatedWords = new ArrayList<>();
  /*
   * String resultsSummary = modelProxy.dbfetch(formInputToModel(request),
   * atomsWithHeader, rows);
   */
  String resultsSummary = getResult(request, rows, relatedWords);
  atomsWithHeader.add(new Atom(AtomType.resultSummary, -2, resultsSummary, "",
    "disambiguation", "resultssummary", new Decoration()));

  // UI
  SubmitForm form = formInputToUI("GET", "webchildevent");
  Autocomplete ac = new Autocomplete("x", "jsp/autocompleteEvent.jsp");
  UI uiProxy =
    new UI(new InputToView(form, atomsWithHeader, rows, relatedWords, ac));
  // Construct response
  StringBuilder responseText = uiProxy.buildHTMLCode();
  PrintWriter out = response.getWriter();
  response.setContentType("text/html");
  out.println(responseText.toString());

}

private SubmitForm formInputToUI(String getOrSet,String action){
  // Buttons and submit form.
  List<buttonInfo> buttons = new ArrayList<buttonInfo>();
  buttons.add(new buttonInfo("x", "text", "x", "", "e.g. cook"));
  buttons.add(new buttonInfo("go", "submit", "go", "search", ""));
  SubmitForm form = new SubmitForm(getOrSet, action, buttons);
  return form;
}

protected String getResult(HttpServletRequest request,List<ResultRow> rows,
  List<Atom> relatedWords){
  String x = request.getParameter("x");
  x = x.trim().toLowerCase();
  if(x.isEmpty()) x = "cook"; // Default value

  return getResult(x, rows, relatedWords);

}

private String getResult(String x,List<ResultRow> rows,List<Atom> relatedWords){
  String defaultKey = m.keySet().iterator().next();
  EventKB event = m.containsKey(x) ? m.get(x) : m.get(defaultKey);
  relatedWords.add(new Atom(event.parent.toString()));

  for(String e: EventKBLogic.modifiedToString(event).split("\n")){
    String[] clusterTitleAndContent = e.split(":");
    for(String s: clusterTitleAndContent[1].trim().split("\t")){
      List<Atom> rowElems = new ArrayList<>();
      rowElems.add(new Atom(s));
      ResultRow row = new ResultRow(rowElems, clusterTitleAndContent[0].trim());
      rows.add(row);
    }
  }

  // Add prev and next events.
  addPrevAndNextEvents(x, rows, true);
  addPrevAndNextEvents(x, rows, false);

  String resultSummary = "" + x;
  return resultSummary;
}

private void addPrevAndNextEvents(String x,List<ResultRow> rows,boolean prev){
  PrevOrNextEvent nextEvent = null;
  try{
    nextEvent =
      prev ? EventKBLogic.getPrevEvent(x) : EventKBLogic.getNextEvent(x);
  } catch (Exception e1){
    e1.printStackTrace();
  }
  if(nextEvent == null) return;

  for(Entry<String, Double> e: nextEvent.eventsPMI.entrySet()){
    List<Atom> rowElems = new ArrayList<>();

    rowElems.add(new Atom(e.getKey()));
    ResultRow row = new ResultRow(rowElems, prev ? "prev" : "next");
    rows.add(row);
  }
}

private Atom header(){
  return new Atom(AtomType.header, 0, "Commonsense Event Browser", "#",
    "Would be part of WebChild Browser", "header", new Decoration(color.black,
      font.normal, boldface.normal, fontsize.normal, background.yellow));
}

public static AutoMap<String, EventKB> loadJson(String eventJsonFile)
  throws FileNotFoundException{
  System.out.print("\nLoading json...");
  AutoMap<String, EventKB> m = new AutoMap<>();
  int maxLines = 10000000;
  for(String l: new FileLines(eventJsonFile)){
    EventKB eventKBObj = EventKB.fromJson(l);
    m.put(eventKBObj.activity, eventKBObj);
    if(maxLines-- < 0) break;
  }
  System.out.println(" [done]");
  return m;
}

}
