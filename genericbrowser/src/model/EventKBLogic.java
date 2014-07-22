package model;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import util.AutoMap;
import visualcsk.moviescripts.EventKB;
import visualcsk.moviescripts.gsp.SeqInterestingness;

public class EventKBLogic {

private static final String countsOfLen1Cache =
  "/var/tmp/git-repository/genericbrowser/WebContent/data/counts1.cache";
private static final String countsOfLen2Cache =
  "/var/tmp/git-repository/genericbrowser/WebContent/data/counts2.cache";
private static final String eventTb = "___eventseq";
private static SeqInterestingness si;

public static class PrevOrNextEvent {
public String forEvent;
public Map<String, Double> eventsPMI;

public PrevOrNextEvent(String forEvent,String output) {
  eventsPMI = new LinkedHashMap<String, Double>();
  this.forEvent = forEvent;
  String[] outputLines = output.split("\n");
  // i=0 is a header saying PrevOrNextEvent.
  for(int i = 1; i < outputLines.length; i++){
    String[] wordPMI = outputLines[i].split("\t");
    if(wordPMI == null || wordPMI.length != 2) continue;
    if(wordPMI[0].contains(forEvent) || forEvent.contains(wordPMI[0]))
      continue;
    eventsPMI.put(wordPMI[0], Double.parseDouble(wordPMI[1]));
  }
}
}

/**
 * @usage  PrevOrNextEvent nextEvent = EventKBLogic.getNextEvent("cook");

 * @param s1 cook
 * @return top pmi sorted
 * @throws Exception
 */
public static PrevOrNextEvent getNextEvent(String s1) throws Exception{
  initSi();
  return new PrevOrNextEvent(s1, si.analyze(s1, ""));
}

public static PrevOrNextEvent getPrevEvent(String s1) throws Exception{
  initSi();
  return new PrevOrNextEvent(s1, si.analyze("", s1));
}

private static void initSi(){
  if(si == null) try{
    si = new SeqInterestingness(eventTb, countsOfLen1Cache, countsOfLen2Cache);
  } catch (SQLException | IOException e){
    e.printStackTrace();
  }
}

public static String modifiedToString(EventKB event){
  StringBuilder sb = new StringBuilder();
  sb.append("Activity: ").append(event.activity).append("\n");
  sb.append("Parent: ").append(event.parent).append("\n");

  sb.append("Agents:\t");
  for(String s: agentsModified(event))
    sb.append("\t").append(s);

  sb.append("\nInt/Ext: ").append(event.printTopKElems(event.locIntExt, 2))
    .append("\n");
  sb.append("Location: ").append(event.printTopKElems(event.locHead, 3))
    .append("\n");
  sb.append("Time: ").append(event.printTopKElems(event.time, 3));

  return sb.toString();
}

private static List<String> agentsModified(EventKB event){
  int maxCandidates = 10;
  AutoMap<String, Integer> agentFreq = new AutoMap<String, Integer>();
  for(String s: event.printTopKElems(event.subjs, maxCandidates).toString()
    .split("\t"))
    agentFreq.addNumericValueInt(s, 1);
  for(String s: event.printTopKElems(event.objs, maxCandidates).toString()
    .split("\t"))
    agentFreq.addNumericValueInt(s, 1);
  for(String s: event.printTopKElems(event.agents, maxCandidates).toString()
    .split("\t"))
    agentFreq.addNumericValueInt(s, 1);

  List<String> agentsModified = new ArrayList<>();
  for(Entry<String, Integer> e: agentFreq.sortByValue().entrySet()){
    if(agentsModified.size() > maxCandidates) break;
    if(e.getValue() > 1) agentsModified.add(e.getKey());
  }

  int maxToDisplay = 5;
  if(agentsModified.size() == 0){
    for(Entry<String, Integer> e: agentFreq.sortByValue().entrySet()){
      if(agentsModified.size() > maxToDisplay) break;
      if(e.getValue() > 0) agentsModified.add(e.getKey());
    }
  }

  return agentsModified;
}
}
