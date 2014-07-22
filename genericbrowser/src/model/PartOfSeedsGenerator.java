package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


import java.util.List;

import javax.management.timer.Timer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javatools.database.Database;


import util.DBConnector;
import util.FileUtil;

/**
 * Run this class to generate partOf seeds from database by joining
 * wn_mereonymy_m or wn_mereonymy_p or wn_mereonymy_s with wn_synsets.
 * Results will be stored as JSON file in WebContent/data/
 * (101801 seeds are generated from the database)
 * @author chariman
 */
public class PartOfSeedsGenerator {
	
	public static class PartOfSeeds {
		public String description;
		public List<PartOfSeed> seeds;
		
		public PartOfSeeds(String desc){
			this.description = desc;
			this.seeds = new ArrayList<>();
		}
		
		public void addSeed(PartOfSeed seed){
			this.seeds.add(seed);
		}
	}
	
	private static class PartOfSeed{
		private String fromId;
		private String fromWord;
		private String fromSenseNumber;
		private String toId;
		private String toWord;
		private String toSenseNumber;
			
		private PartOfSeed(String frId, String frWord, String frSenseNumber,
					String toId, String toWord, String toSenseNumber) {
				this.fromId = frId;
				this.fromSenseNumber = frSenseNumber;
				this.fromWord = frWord;
				this.toId = toId;
				this.toSenseNumber = toSenseNumber;
				this.toWord = toWord;
		}
	}
	
	private static Database db;
	private static PartOfSeeds seeds ;
    private static int count;
    private static  Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static String json;
	/**
	 * SQL to join tables wn_mereonymy_m or wn_mereonymy_p or wn_mereonymy_s with wn_synsets
	 * @param type : suffix of wn_mereonymy {m, p, s}
	 * @return
	 */
	public static String buildSql(String meronymType){
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT p.from_ss AS fr_synset_id, f.word AS fr_word, ")
			.append("f.sense_number AS fr_sense_number, ")
			.append("p.to_ss AS to_synset_id, t.word AS to_word, t.sense_number AS to_sense_number ")
			.append("FROM wordnet.wn_mereonymy_"+meronymType+" p " )
			.append("JOIN wordnet.wn_synsets f ON (p.from_ss = f.synset_id) ")
			.append("JOIN wordnet.wn_synsets t ON (p.to_ss = t.synset_id) ")
			.append("");
		return sql.toString();
	}
	
	public static void printSeeds(String meronymType, String description) throws SQLException{
		count = 0;
		String outFile = "WebContent/data/partOfSeeds_"+meronymType+".json";
		seeds = new PartOfSeeds(description);
		ResultSet rs = DBConnector.q(buildSql(meronymType));
	    rs.toString();
	    while (rs.next()){
	       seeds.addSeed(new PartOfSeed(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)));
	       count++;
	    }
	    json = gson.toJson(seeds);
	    FileUtil.writeFileUserDefinedObject(outFile, json, false);
	    System.out.println("Done generating "+count+" "+meronymType+" seeds.");
		 
	}
	public static void main(String[] args) throws SQLException{
	     
		 
	     if(db == null) db = DBConnector.getDB();
		 
	     printSeeds("m", "member meronym");
	     printSeeds("p", "part meronym");
	     printSeeds("s", "substance meronym");
	     
	     DBConnector.closeConnections();
	     //101801 seeds
	}
	
}
