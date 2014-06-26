package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Stack;
import javatools.database.Database;
import javatools.datatypes.Pair;

public class WordnetHelper {
// private static Database db;
public static final int entity_synsetID = 100001740;
public static final int physical_entity_synsetID = 100001930;
private static String wordnetWordsFile = "./data/wordnet_words.all";

/**
 * lookup DBWordnet.., from_ss = child, to_ss = parent to find all parents.
 * 
 * @param synset_id
 *            e.g. int edible_fruit_synset_id = 107705931;
 * @return most likely(1st) direct inherited hypernym synset_id.
 * @throws Exception
 */
public static Stack<Integer> getInheritedHyperynymsUptoLevel(int synset_id,
  Database db) throws Exception{
  // Stack<Pair<Integer, String>> parents = new Stack<Pair<Integer,
  // String>>();
  Stack<Integer> parents = new Stack<Integer>();
  int child = synset_id;
  int parent = getHyperynym(child, db);
  while ((parent = getHyperynym(child, db)) != -1){
    // Check not found.
    parents.add(parent);
    if(parent == entity_synsetID) return parents;
    child = parent;
  }
  System.out.println("No links exist from child: " + synset_id + " to parent: "
    + entity_synsetID);
  return parents;
}

/**
 * lookup DBWordnet.., from_ss = child, to_ss = parent to find all parents.
 * 
 * @param synset_id
 *            e.g. int edible_fruit_synset_id = 107705931;
 * @return most likely(1st) direct inherited hypernym synset_id.
 * @throws Exception
 */
public static Stack<Pair<Integer, String>> getInheritedHyperynymsUptoLevel(
  int synset_id,int upto_synset_id,Database db) throws Exception{
  Stack<Pair<Integer, String>> parents = new Stack<Pair<Integer, String>>();
  int child = synset_id;
  int parent = getHyperynym(child, db);
  while ((parent = getHyperynym(child, db)) != -1){
    // Check not found.
    parents.add(new Pair<Integer, String>(parent, getWord(parent, db)));
    if(parent == upto_synset_id) return parents;
    child = parent;
  }
  // System.out.println("No links exist from child: " + synset_id +
  // " to parent: " + upto_synset_id);
  return parents;
}

/**
 * lookup DBWordnet.., from_ss = child, to_ss = parent
 * 
 * @param synset_id
 *            e.g. int edible_fruit_synset_id = 107705931;
 * @return most likely(1st) direct hypernym synset_id.
 * @throws Exception
 */
public static int getHyperynym(int synset_id,Database db) throws Exception{
  int parent = -1;
  ResultSet s =
    db.query("select wordnet.wn_hyponymy.to_ss from wordnet.wn_hyponymy"
      + " where wordnet.wn_hyponymy.from_ss=" + synset_id + "");
  if(s.next()){
    parent = s.getInt("to_ss");
  }
  return parent;
}

public static HashSet<Integer> getHypoynym(int synset_id,Database db)
  throws Exception{
  HashSet<Integer> child = new HashSet<Integer>();
  ResultSet s =
    db.query("select wordnet.wn_hyponymy.from_ss from wordnet.wn_hyponymy"
      + " where wordnet.wn_hyponymy.to_ss=" + synset_id + "");
  while (s.next()){
    child.add(s.getInt("from_ss"));
  }
  return child;
}

/**
 * lookup DBWordnet.., from_ss = child, to_ss = parent
 * 
 * @param synset_id
 *            e.g. int edible_fruit_synset_id = 107705931;
 * @return list of all direct hyponyms
 * @throws Exception
 */
public static HashMap<String, Integer> getFirstLevelHyponyms(int synset_id,
  Database db) throws Exception{
  HashMap<String, Integer> r = new HashMap<String, Integer>();
  ResultSet s =
    db.query("select wordnet.wn_hyponymy.from_ss from wordnet.wn_hyponymy"
      + " where wordnet.wn_hyponymy.to_ss=" + synset_id + "");
  while (s.next()){
    int children = s.getInt("from_ss");
    for(Entry<String, Integer> child: getSynsetWords(children, db).entrySet()){
      r.put(child.getKey(), child.getValue());
    }
  }
  return r;
}

/**
 * @param synset_id
 *            e.g. 107705931
 * @return 1st most likely word e.g. edible_fruit
 * @throws Exception
 *             database lookup failure
 */
public static String getWord(int synset_id,Database db) throws Exception{
  String word = "";
  ResultSet synset =
    db.query("select wordnet.wn_synsets.word from wordnet.wn_synsets"
      + " where wordnet.wn_synsets.synset_id=" + synset_id + "");
  if(synset.next()){
    word = synset.getString("word");
  }
  return word;
}

public static String getGloss(int synset_id,Database db) throws Exception{
  String gloss = "";
  ResultSet synset =
    db.query("select wordnet.wn_glosses.gloss from wordnet.wn_glosses"
      + " where wordnet.wn_glosses.synset_id=" + synset_id);
  if(synset.next()){
    gloss = synset.getString("gloss");
  }
  return gloss;
}

public static ArrayList<String> getWords(int synset_id,Database db)
  throws Exception{
  ArrayList<String> words = new ArrayList<String>();
  ResultSet synset =
    db.query("select wordnet.wn_synsets.word from wordnet.wn_synsets"
      + " where wordnet.wn_synsets.synset_id=" + synset_id + "");
  while (synset.next()){
    words.add(synset.getString("word"));
  }
  return words;
}

/**
 * @param synset_id
 *            e.g. 107705931
 * @return e.g. [edible_fruit, eatable fruit]
 * @throws Exception
 *             database lookup failure
 */
public static HashMap<String, Integer>
  getSynsetWords(int synset_id,Database db) throws Exception{
  HashMap<String, Integer> r = new HashMap<String, Integer>();
  ResultSet synset =
    db.query("select wordnet.wn_synsets.word from wordnet.wn_synsets"
      + " where wordnet.wn_synsets.synset_id=" + synset_id + "");
  while (synset.next()){
    r.put(synset.getString("word"), synset_id);
  }
  return r;
}

public static int getSynset_id(String word,Database db) throws Exception{
  ResultSet synset =
    db.query("select wordnet.wn_synsets.synset_id from wordnet.wn_synsets"
      + " where wordnet.wn_synsets.word='" + word
      + "' order by wordnet.wn_synsets.tag_count desc");
  int id = -1;
  while (synset.next()){
    id = synset.getInt("synset_id"); // Return the first
    break;
  }
  return id;
}

public static boolean isAdjective(int getySynsetID,Database db)
  throws Exception{
  ResultSet synset =
    db.query("select wordnet.wn_synsets.ss_type from wordnet.wn_synsets"
      + " where wordnet.wn_synsets.synset_id='" + getySynsetID + "'");
  if(synset.next()){
    String ss_type = synset.getString("ss_type");
    if(ss_type.equals("a") || ss_type.equals("s")) return true;
  }
  return false;
}
}
