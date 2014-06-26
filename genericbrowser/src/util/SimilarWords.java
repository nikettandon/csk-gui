package util;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javatools.filehandlers.TSVFile;

public class SimilarWords {

/**
 * wna_181, {ripe, unripe}
 */
private AutoMap<String, Set<String>> cA,cN;

/**
 * ripe,wna_181
 */
private AutoMap<String, Set<String>> wA,wN;

private static SimilarWords proxy;
private static SimilarWords proxyMFS;

public static SimilarWords getInstance(boolean shouldRestrictToMFS)
  throws Exception{
  int restrictToMFS = shouldRestrictToMFS ? 1 : 30;
  if(restrictToMFS == 1){
    if(proxyMFS == null) proxyMFS = new SimilarWords(restrictToMFS);
    return proxyMFS;
  } else{
    if(proxy == null) proxy = new SimilarWords(restrictToMFS);
    return proxy;
  }
}

public static SimilarWords getInstance(int restrictToSenseNum) throws Exception{
  if(restrictToSenseNum == 1){
    if(proxyMFS == null) proxyMFS = new SimilarWords(restrictToSenseNum);
    return proxyMFS;
  } else{
    if(proxy == null) proxy = new SimilarWords(restrictToSenseNum);
    return proxy;
  }
}

private SimilarWords(int restrictToMFS) throws Exception {

  cA = new AutoMap<String, Set<String>>();
  cN = new AutoMap<String, Set<String>>();
  wA = new AutoMap<String, Set<String>>();
  wN = new AutoMap<String, Set<String>>();
  loadMaps(restrictToMFS);
}

public Set<String> getSimilar(String wordNo_,boolean isNoun){
  Set<String> similars = new HashSet<String>();
  if(wordNo_.contains("_")) wordNo_.replaceAll("_", " ");
  if(isNoun){
    if(!wN.containsKey(wordNo_)) return similars;
    for(String clusterID: Util.nullableIter(wN.get(wordNo_)))
      similars.addAll(cN.get(clusterID));
  } else{
    if(!wA.containsKey(wordNo_)) return similars;
    for(String clusterID: Util.nullableIter(wA.get(wordNo_)))
      similars.addAll(cA.get(clusterID));
  }
  return similars;
}

/** 
 * fill map <good,bad> (and also return it)
 * @param filePath ./data/antonyms/expanded.antonyms
 * @param restrictToMFS 
 * @return 
 * @throws IOException
 */
private void noSenses(int maxMFS,String dbTb,boolean isNoun) throws Exception{

  String sql = "select subx,y,ywps from " + dbTb;
  ResultSet rs = DBConnector.q(sql);
  while (rs.next()){
    String subx = rs.getString(1);
    String y = rs.getString(2);
    String ywps = rs.getString(3);
    int ysensenum = Integer.parseInt(ywps.substring(ywps.lastIndexOf('#') + 1));

    // if(restrictToMFS && !ywps.endsWith("#1")) continue;
    if(ysensenum > maxMFS) continue;

    if(isNoun){
      cN.addSetValue(subx, y);
      wN.addSetValue(y, subx);
    } else{
      cA.addSetValue(subx, y);
      wA.addSetValue(y, subx);
    }
  }
  // System.out.println("  [done]");
}

/** 
 * fill map <good,bad> (and also return it)
 * @param filePath ./data/antonyms/expanded.antonyms
 * @param restrictToMFS 
 * @return 
 * @throws IOException
 */
private AutoMap<String, Set<String>> noSenses(String filePath,
  AutoMap<String, Set<String>> m,boolean restrictToMFS) throws Exception{
  System.out.print("Loading map from " + filePath + " ...");
  for(List<String> l: new TSVFile(new File(filePath))){
    if(l == null || l.size() != 2) continue;
    // interesting#a#2 to slow#a#4 are ignored (low recall, high accuracy)
    if(restrictToMFS && (!l.get(0).endsWith("#1") || !l.get(1).endsWith("#1")))
      continue;
    // reasonable#a#1 to reasonable
    String word = l.get(0).substring(0, l.get(0).indexOf('#'));
    String synonym = l.get(1).substring(0, l.get(1).indexOf('#'));
    m.addSetValue(word, synonym);
    m.addSetValue(synonym, word);

    // Expand hyphenated words e.g. low-grade; also underscored low_grade.
    String expandedWord = word.contains("-") ? word.replaceAll("-", " ") : word;
    expandedWord = word.contains("_") ? word.replaceAll("_", " ") : word;
    String expandedAnt =
      synonym.contains("-") ? synonym.replaceAll("-", " ") : synonym;
    expandedAnt =
      synonym.contains("_") ? synonym.replaceAll("_", " ") : synonym;
    m.addSetValue(expandedWord, expandedAnt);
    m.addSetValue(expandedAnt, expandedWord);
  }
  System.out.println("  [done]");
  return m;
}

private void loadMaps(int maxMFS) throws Exception{
  noSenses(maxMFS, "__clusteredwna", false);
  noSenses(maxMFS, "__clusteredwnn", true);

  /*noSenses("./data/antonyms/expanded.adjectives.antonyms", mA, restrictToMFS);
  noSenses("./data/synonyms/expanded.adjectives.synonyms", mA, restrictToMFS);
  noSenses("./data/antonyms/expanded.nouns.antonyms", mN, restrictToMFS);
  noSenses("./data/synonyms/expanded.nouns.synonyms", mN, restrictToMFS);*/
}

public static void main(String[] args) throws Exception{
  SimilarWords proxy = SimilarWords.getInstance(false);
  System.out.println(proxy.getSimilar("electropositive", false));
  DBConnector.closeConnections();

}
}
