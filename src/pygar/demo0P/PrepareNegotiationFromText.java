package pygar.demo0P;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Off-line program that prepares text for demo0.
 * Read a document containing essays each of which has a title beginning with "Of".
 * Divide the essays into two parts on arbitrary boundaries so that half the text of 
 * each essay is found in one output document and half in the other. 
 * @author pbaker
 *
 */
public class PrepareNegotiationFromText {
	
	private static String dataDirPath;
	
	/** 
	 * On the first look over the document, we derive word counts for all words
	 * in the document. Subsequently, we merge single and plural words with a 
	 * heuristic that works often but not always. 
	 */
	private static HashMap<String, Integer> wordCountsPass1;
	
	/**
	 * For each plural found by the heuristic, the following map
	 * stores the singular. 
	 */
	private static HashMap<String, String> pluralsSingular;
	
	/** 
	 * splitTextFiles does a first pass over a document dividing it into 
	 * two text file in the TextData subdirectory. 
	 * @throws IOException
	 */
	private static void splitTextFiles() throws IOException {
		String dirPath = dataDirPath + "TextData/";
		InputStream inStream = new FileInputStream(dirPath +
		"FrancisBaconEssays.txt");
		FileWriter outFile1 = null;
		FileWriter outFile2 = null;
		try {
			outFile1 = new FileWriter(dirPath + "BlueEssays.txt");
			outFile2 = new FileWriter(dirPath + "GreenEssays.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}

		String topic = null;
		// document will be divided into word blocks containing divisionCount. 
		final int  divisionCount = 50;
		int n = 0;
		int block;
		int pblock1 = 0;
		int pblock2 = 1;
		Pattern p = Pattern.compile("\\w+");
		Matcher m;
		String word;

		Scanner in = new Scanner(inStream);
		String line;
		while (in.hasNextLine()) {
			line = in.nextLine();
			if (line.length() > 2) {
//				System.out.printf("<%s>%n", line.substring(0, 3));
				if ( line.substring(0, 3).equals("Of ") &&
						line.length() < 50) {
					topic = line;
//					System.out.printf("Topic: %s%n", topic);
					outFile1.write("\n" + topic + "\n");
					outFile2.write("\n" + topic + "\n");
				} else {
					if ( topic != null) {
						m = p.matcher(line);
						while (m.find()) {
							word =  m.group().toLowerCase();
							if (wordCountsPass1.containsKey(word)) {
								wordCountsPass1.put( word, wordCountsPass1.get(word) + 1);
							} else {
								wordCountsPass1.put(word, 1);
							}
							block = (n++) / divisionCount;
							if ( block % 2 == 0) { 
								if (pblock1 != block) {
									outFile1.write(" ... \n");
									pblock1 = block;
									pblock2 = block;
								}
								outFile1.write(word + " ");
							} else {
								if (pblock2 != block) {
									outFile2.write(" ... \n");
									pblock1 = block;
									pblock2 = block;
								}
								outFile2.write(word + " ");
								
							}
						}
					}
//					System.out.println(line);
				}
			}
		}
		outFile1.write("\n");
		outFile1.close();
		outFile2.write("\n");
		outFile2.close();
		
//		printWordCounts();

		
	}
	private static void printWordCounts() { 
		String key;
		Set<String> keys = wordCountsPass1.keySet();
		Iterator<String> ikeys = keys.iterator();
		while (ikeys.hasNext()) {
			key = ikeys.next();
			System.out.printf("Word %s Count %d%n", key, wordCountsPass1.get(key));
		}

	}

	/**
	 * mergePlurals works on nouns that have a plural ending in s and both the singular
	 * and the plural occur in the document. This misses Latin words, obviously. Also it
	 * messes up comparing verbs and nouns, e.g. mars is considered the plural of mar!
	 */
	private static void mergePlurals() {	
		String plural;
		String key;
		System.out.printf("Begin mergePlurals with %d words", wordCountsPass1.size());
		Set<String> keys = wordCountsPass1.keySet();
		Iterator<String> ikeys = keys.iterator();
		while (ikeys.hasNext()) {
			key = ikeys.next();
			plural = key + "s";
			if (wordCountsPass1.containsKey(plural)) {
				int total = wordCountsPass1.get(key) + wordCountsPass1.get(plural);
				wordCountsPass1.put(key, total);
				pluralsSingular.put(plural, key);
//				System.out.printf("Merge %s with %s for total %d%n", plural, key, total);
			}
		}
		ikeys = pluralsSingular.keySet().iterator();
		while (ikeys.hasNext()) {
			plural = ikeys.next();
			wordCountsPass1.remove(plural);
		}
		System.out.printf("End mergePlurals with %d words", wordCountsPass1.size());


	}
	
	private static void writeXmlFile(String dir, String file) throws IOException, XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = null;
		
		String dirPath = dataDirPath + dir + "/";
		InputStream inStream = new FileInputStream(dataDirPath + "TextData/" + file);
		FileWriter outFile = null;
		try {
			outFile = new FileWriter(dirPath + "Essays.xml");
			writer = factory.createXMLStreamWriter(outFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			System.exit(-1);
			e.printStackTrace();
		}
		String topic = null;
		HashMap<String, Integer> wordCounts = null;;

		Pattern p = Pattern.compile("\\w+");
		Matcher m;
		String word;
		boolean inBody = false;
		boolean inEssay = false;
		int accession;

		Scanner in = new Scanner(inStream);
		String line;
//		outFile.write("<documents>\n");
		writer.writeStartDocument();
		writer.writeStartElement("documents");
		writer.writeCharacters("\n");
		while (in.hasNextLine()) {
			line = in.nextLine();
			if (line.length() > 2) {
//				System.out.printf("<%s>%n", line.substring(0, 3));
				if ( line.substring(0, 3).equals("Of ") &&
						line.length() < 50) {
					topic = line;
					
//					System.out.printf("Topic2: %s%n", topic);
					if (inEssay) {
						closeDocument(writer, wordCounts);
					}
					// start new essay xml record
					wordCounts = new HashMap<String, Integer>();
					accession = (int)( 10000000 * Math.random());
//					outFile.write("<document>\n<title>" + topic + "</title>\n<accession>" +
//							accession + "</accession>\n<body>\n");
					writer.writeStartElement("document");
					writer.writeCharacters("\n");
					writer.writeStartElement("title");
					writer.writeCharacters(topic);
					writer.writeEndElement();
					writer.writeCharacters("\n");
					writer.writeStartElement("accession");
					writer.writeCharacters(Integer.toString(accession));
					writer.writeEndElement();
					writer.writeCharacters("\n");
					writer.writeStartElement("body");
					writer.writeCharacters("\n");
					inBody = true;
					inEssay = true;;
				} else {
					if ( topic != null) {
						m = p.matcher(line);
						while (m.find()) {
							word =  m.group().toLowerCase();
							if (pluralsSingular.containsKey(word)) {
								word = pluralsSingular.get(word);
							}
							
							if (wordCounts.containsKey(word)) {
								wordCounts.put( word, wordCounts.get(word) + 1);
							} else {
								wordCounts.put(word, 1);
							}
						}
						if (inBody) {
//							outFile.write();
							writer.writeCharacters(line + "\n");
						}
					}
//					System.out.println(line);
				}
			}
		}
//		outFile.write("\n</documents>\n");
		writer.writeEndElement();
		writer.writeEndDocument();
		outFile.close();
	}
	
	private static void closeDocument(XMLStreamWriter f, 
			HashMap<String, Integer> counts) throws IOException, XMLStreamException {
		// TODO finish previous by closing body and writing word counts
		f.writeEndElement();
		f.writeCharacters("\n");
		f.writeStartElement("wordcounts");
//		f.write("</body>\n<wordcounts>\n");
		Iterator<String> ikeys = counts.keySet().iterator();
		String key;
		while (ikeys.hasNext()) {
			key = ikeys.next();
//			f.write("<wordcount><word>" + key + "</word><count>" +
//					Integer.toString(counts.get(key).intValue()) + "</count></wordcount>\n" );
			f.writeStartElement("wordcount");
			f.writeStartElement("word");
			f.writeCharacters(key);
			f.writeEndElement();
			f.writeStartElement("count");
			f.writeCharacters(Integer.toString(counts.get(key).intValue()));
			f.writeEndElement();
			f.writeEndElement();
			f.writeCharacters("\n");
			
		}
//		f.write("</wordcounts>\n</document>\n\n");
		f.writeEndElement();
		f.writeEndElement();
		f.writeCharacters("\n");
		
		
	}
	
	private static void compareFiles(String fname1, String fname2) {
		return;
	}

	/**
	 * @param args
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public static void main(String[] args) throws IOException, XMLStreamException {
		// TODO Auto-generated method stub
		dataDirPath = "/Users/pbaker/Coding/eclipse_workspace/demo0/data/";
		wordCountsPass1 = new HashMap<String, Integer>();
		pluralsSingular = new HashMap<String, String>(); 
		splitTextFiles();
		mergePlurals();
//		printWordCounts();
		writeXmlFile("BlueTeam" , "BlueEssays.txt");
		writeXmlFile("GreenTeam" , "GreenEssays.txt");
		System.out.println("...midway");
		compareFiles("BlueTeam" , "GreenTeam");
		System.out.println("...finished");

	}
	

}
