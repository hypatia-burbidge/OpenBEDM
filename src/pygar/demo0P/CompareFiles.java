	/****************************************************************CopyrightNotice
	 * Copyright (c) 2010 WWN Software LLC 
	 * All rights reserved. This program and the accompanying materials
	 * are made available under the terms of the Pygar Public License v1.1
	 * which accompanies this distribution, and is available at
	 * http://ectn.typepad.com/pygar/pygar-public-license.html
	 *
	 * Contributors:
	 *    Paul Baker, WWN Software LLC
	 *    
	 * The blind-agent-mediated negotiation process implemented by this software
	 * is the subject of U.S. Patent 7,685,073. 
	 *******************************************************************************/

package pygar.demo0P;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.*;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import pygar.configuration.DocumentError;

/**
 * This class contains methods to locate similar documents contained in two files.
 * The similarity is based on word counts that are contained in the XML document. 
 * In fact, this class does not expect the actual document. The body of the document
 * is never sent to the BAN in Demonstration 0; instead, the team members send the 
 * accession number and word counts for each document. 
 * @author pbaker
 *
 */
public class CompareFiles {
	
	private static String dataDirectory;
	private static BAN ban;
	
	public CompareFiles(String fileDir) {
		dataDirectory = fileDir;
		
		documents = new HashMap<String, Map<String, DocStruct> >();

		wordSums = new HashMap<String, Integer>();
	}
	
	/** save a pointer of the BAN instance so that we
	 * can update the progress bar in the GUI panel.
	 * @param thisBAN
	 */
	public void setBAN(BAN thisBAN) {
		ban = thisBAN;
	}
		
	class CountStruct implements Comparable<CountStruct>{
		String word;
		Integer count;
		
		public CountStruct(String w, Integer c) {
			word = w;
			count = c;
		}
		public int compareTo(CountStruct o) {
			return count.compareTo(o.count);
		}
	}
	
	class DocStruct {
		String title;
		String accession;
		List<CountStruct> words;
		Map<String, Double> wordFrequencies;
		
		public DocStruct(String t, String a) {
			title = t;
			accession = a;
			words = new LinkedList<CountStruct>();
			wordFrequencies = new HashMap<String, Double>();
		}
	}
	
	class ResultStruct implements Comparable<ResultStruct> {
		double correl;
		String title1;
		String title2;
		String accession1;
		String accession2;
		
		public ResultStruct(double c, String t1, String t2, String acc1, String acc2) {
			correl = c;
			title1 = t1;
			title2 = t2;
			accession1 = acc1;
			accession2 = acc2;
		}
		
		public int compareTo(ResultStruct o) {
			return Double.compare(this.correl, o.correl);
		}
		
	}
	
	// document -- accession -> DocStruct --> words --> List<CountStruct>
//	static Map<String, DocStruct> document1;
//	static Map<String, DocStruct> document2;
	
	public Map<String, Map<String, DocStruct> > documents;
	
//	// accession -> List -> word -> count
//	
//	static Map<String, List<CountStruct> > document1;
//	static Map<String, List<CountStruct> > document2;
	
	// wordSums provide the number of occurrences of a word in all of the
	// text that is processed. 
	static Map<String, Integer> wordSums;
	// wordFreqs normalize the counts in 
	static Map<String, Double> wordFreqs;
	
	public List<ResultStruct> resultList;
	double maxCorrel;
	public int reportedResults;
	public List<String> titleList1;
	public Set<String> titleSet1;
	public List<String> titleList2;
	public Set<String> titleSet2;
	public Set<String> accessionSet1;
	public List<String> accessionList1;
	public Set<String> accessionSet2;
	public List<String> accessionList2;
	public int ndocs1;
	public int ndocs2;
	
	public MemoryImageSource imageSource;
	public ImageComponent imageComponent;
	public int[] pix;
	
	class ImageComponent extends JComponent {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Image image;
		
		public ImageComponent(int dataSize1, int dataSize2, int pixSize1, int pixSize2) {
			int index = 0;
			int w = pixSize1;
			int h = pixSize2;
			pix = new int[w * h];

			for (int y = h - 1; y >= 0; y--) {
	            for (int x = 0; x < w; x++) {
	            	// evidently, highest bits are transparency and then RGB
 	                pix[index++] =  (255 << 24);
	            }

			}
			imageSource = new MemoryImageSource(w, h, pix, 0, w);
			imageSource.setAnimated(true);
	        image = createImage(imageSource);
//	        bufImage = image;
		}
		
		public void paintComponent(Graphics g) {
			if (image == null) return;
			g.drawImage(image, 0, 0, null);
		}
		

	}

	class DrawFrame extends JFrame {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public int dataSize1, dataSize2, pixSize1, pixSize2;
		
		public DrawFrame(int dataSize1, int dataSize2, int pixSize1, int pixSize2) {
			this.dataSize1 = dataSize2;
			this.dataSize2 = dataSize2;
			this.pixSize1 = pixSize1;
			this.pixSize2 = pixSize2;
			setTitle("Similarity Measures between Two Document Sets");
			setSize(pixSize1, pixSize2);
			imageComponent = new ImageComponent(dataSize1, dataSize2, pixSize1, pixSize2);
			add(imageComponent);
		}
	}
	

	public void ingestFile(String teamName, String dirName, String fileName
			) throws FileNotFoundException, DocumentError {
		
		Map<String, DocStruct> document = new HashMap<String, DocStruct>();

		String path = dataDirectory +  
		File.separator + dirName + File.separator + fileName;
		
		System.out.printf("ingestFile %s%n", path);
		
		InputStream in = new FileInputStream(path);
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser;
		try {
			parser = factory.createXMLStreamReader(in);
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new DocumentError();
		}


		int event;
		int k;
		StringWriter bodyText = null;
		String text;
		String element = "";
		boolean inDocument = false;
		boolean inBody = false;
		boolean inAccession = false;
		boolean inWordCount = false;
		boolean inCounting = false;
		boolean inWord = false;
		boolean inCount = false;
		boolean inTitle = false;
		int currentCount = 0;
		String currentWord = null;
		String title = null;
		String currentAccession = null;
		boolean handled;
		Pattern regexp = Pattern.compile("\\S");
		Matcher match;
		CountStruct countStruct;
		
		try {
			while (parser.hasNext()) {
				event = parser.next();
				switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					element = parser.getLocalName();
					handled = false;
//					System.out.printf("Start:%s%n", element);
					// test for body
					if (element.equals("body")) {
						inBody = true;
						bodyText = new StringWriter();
						handled = true;
					} else 
						// test for accession
						if (element.equals("accession")) {
							inAccession = true;
							handled = true;
						} else
							// test for WordCount 
							if (element.equals("wordcount")) {
								inWordCount = true;
								currentCount = -1;
								currentWord = null;
								handled = true;
							} else
								if (element.equals("word")) {
									inWord = true;
									handled = true;
								} else
									if (element.equals("count")) {
										inCount = true;
										handled = true;
									} else
										if (element.equals("document")) {
//											System.out.println("Begin Document");
											inDocument = true;
											handled = true;
										} else
											if (element.equals("title")) {
												inTitle = true;
												handled = true;
											} else {
												if (element.equals("wordcounts")) {
//													countStruct = new CountStruct();
													// TODO start the handling of counts
													document.put(currentAccession, new DocStruct(title, currentAccession));
													inCounting = true;
													handled = true;
												}
											}
//					if ( !handled ) {
//						System.out.printf("unhandled start of %s!%n", element);
//					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					// test for body
					handled = false;
					if (inBody) {
//						System.out.printf("body<%s>%n", bodyText.toString());
						inBody = false;
						handled = true;
					} else
						// test for accession 
						if (inAccession) {
//							System.out.printf("accession<%s>%n", currentAccession);
							inAccession = false;
							handled = true;
						} else
							if (inCounting) {
								// store counts
								inCounting = false;
								handled = true;
							} else 
								if (inWord) {
									inWord = false;
									handled = true;
								} else 
									if (inCount) {
										inCount = false;
										handled = true;
									} else
										if (inWordCount) {
											countStruct = new CountStruct(currentWord, currentCount);
											document.get(currentAccession).words.add(countStruct);
//											System.out.printf("    count %d word %s%n", currentCount,
//													currentWord);
											inWordCount = false;
											handled = true;
										} else
											if (inTitle) {
//												System.out.printf("Title:%s%n", title);
												inTitle = false;
												handled = true;
											} else
												if (inCounting) {
													inCounting = false;
													handled = true;
												} else 
												if (inDocument) {
//													System.out.println("End Document");
													inDocument = false;
													handled = true;
												} 
//					if ( !handled ) {
//						System.out.println("Unhandled End!");
//					}
					break;
				case XMLStreamConstants.CHARACTERS:
					handled = false;
					text = parser.getText();
					match = regexp.matcher(text);
					if ( !match.find()) 
						break;
						
					// test accession
					if (inAccession) {
						currentAccession = text;
						handled = true;
					} else 
						// test body
						if (inBody) {
							bodyText.write(text);
							handled = true;
						}  else
							if (inWord) {
								currentWord = text;
								handled = true;
							} else 
								if (inCount) {
									currentCount = Integer.valueOf(text);
									handled = true;
								} else 
									if (inTitle) {
										title = text;
										handled = true;
									}
					if ( !handled ) {
						k = Math.min(40, text.length());
						System.out.printf("   %d unhandled content<%s>%n", text.length(),
								text.substring(0, k));
						//					System.out.printf("   content:%s%n", text);
					}
					break;
				default:
					break;
				}
			}
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		documents.put(teamName, document);
		
	}
	
	/** Combine the word counts from each of a series of document. 
	 * 
	 */
	public void sumCounts() {
		String key;
		Iterator<String> i;
		DocStruct doc;
		Iterator<CountStruct> ics;
		CountStruct cs;
		int c;
		Map<String, DocStruct> document;

		Iterator<String> itm = documents.keySet().iterator();
		String tm;
		while (itm.hasNext()) {
			tm = itm.next();
			document = documents.get(tm);

			i = document.keySet().iterator();
			while (i.hasNext()) {
				key = i.next();
				// document -- accession -> DocStruct --> words --> List<CountStruct>
				doc = document.get(key);
				ics = doc.words.iterator();
				while (ics.hasNext()) {
					cs = ics.next();
					if ( !wordSums.containsKey(cs.word)) {
						wordSums.put(cs.word, 0);
						c = 0;
					} else {
						c = wordSums.get(cs.word);
					}
					wordSums.put(cs.word, c + cs.count);
				}
			}
		}
	}

	/** Remove the high frequency words from the word counts.
	 * 
	 * @param c the maximum count for the word. If greater, remove.
	 */
//	private static void trimWords(int c) {
//		Set<String> wList = new HashSet<String>(wordSums.keySet());
//		Iterator<String> i = wList.iterator();
//		String s;
//		int k;
//		while (i.hasNext()) {
//			s = i.next();
//			k = wordSums.get(s);
//			if (k >= c) {
////				System.out.printf("    trim %s of count %d%n", s, k);
//				wordSums.remove(s);
//			}
//		}
//	}
	
	/** reduce the size of the word list by eliminating the most frequent
	 * words. 
	 * @param ndelete number of words to delete
	 */
	public void trimSums(int ndelete) {
		// first we trim the list to the given fraction. 
		int numWords = wordSums.size();
//		System.out.printf("Aim to trim %d words out of %d%n", ndelete, numWords);
		if (ndelete >= numWords) {
			System.out.println("  !!!bypass trim, list is strangely short");
		}
		
		List<CountStruct> lcs = new LinkedList<CountStruct>();
		
		Iterator<String> i = wordSums.keySet().iterator();
		String key;
//		Set<Integer> countValues = new HashSet<Integer>();
		while ( i.hasNext()) {
			key = i.next();
			lcs.add(new CountStruct(key, wordSums.get(key)));
		}
		
		Collections.sort(lcs);
		
		// step through the list and toss out the first ndelete.
		CountStruct cs;
		for (int i1 = 0; i1 < ndelete; i1++) {
			cs = lcs.get(i1);
			key = cs.word;
//			System.out.printf("   trim of word %s%n", key);
			wordSums.remove(key);
		}
	}
	
	/** calculate the frequency of the words in the aggregate of all documents
	 */
	public void computeFreqs() {
		wordFreqs = new HashMap<String, Double>();
		int totalWords = 0;
		double total;
		Iterator<String> i = wordSums.keySet().iterator();
		String key;
		double f;
		while (i.hasNext()) {
			totalWords += wordSums.get(i.next());
		}
		total = (double)totalWords;
		
		i = wordSums.keySet().iterator();
		while (i.hasNext()) {
			key = i.next();
			f = wordSums.get(key) / total;
			wordFreqs.put(key, f);
		}
		
	}
	/** compute word frequency profiles for individual documents in a set
	 * of documents. Frequencies are computed for all the words on the global
	 * word list, even if they do not appear in a particular document. Note also
	 * that the frequency is relative to all words in the document that are also on the global
	 * word list. Thus, common words do not count in the total, nor do we calculate
	 * a frequency for them.
	 * Steps:
	 * 1. total all words across the individual document except for the very common words.
	 * 2. for all words in the local list of words, which are also on the global list,
	 *    compute the frequency as the word count divided by the words in the document.
	 * 3. for all words on the global list, verify if there is a frequency in the profile.
	 *    if none is found, set the frequency for that word to zero. 
	 */
	public void computeProfileFreqs() {
		if (wordFreqs == null) {
			System.err.println("Program error: computing profile freqs before word freqs.");
			// let it go forward and crash with system error messages
		}
		Iterator<String> w;
		String word;
		Iterator<String> a;
		String acc;
		DocStruct d;
		List<CountStruct> lcs;
		Iterator<CountStruct> ics;
		CountStruct cs;
		Map<String, DocStruct> doc;

		Iterator<String> itm = documents.keySet().iterator();
		String tm;
		while (itm.hasNext()) {
			tm = itm.next();
			doc = documents.get(tm);

			// iterate over all individual documents by accession number
			int totalWordsInDoc;
			a = doc.keySet().iterator();
			while (a.hasNext()) {
				// step 1
				totalWordsInDoc = 0;
				acc = a.next();
				d = doc.get(acc);
				lcs = d.words;
				ics = lcs.iterator();
				while (ics.hasNext()) {
					cs = ics.next();
					if (wordSums.containsKey(cs.word)) {
						totalWordsInDoc += cs.count;
					}
				}
				// step 2
				ics = lcs.iterator();
				while (ics.hasNext()) {
					cs = ics.next();
					if (wordSums.containsKey(cs.word)) {
						d.wordFrequencies.put(cs.word, ( (double)cs.count ) / totalWordsInDoc);
					}
				}
				// step 3
				w = wordSums.keySet().iterator();
				while (w.hasNext()) {
					word = w.next();
					if ( ! d.wordFrequencies.containsKey(word) ) {
						d.wordFrequencies.put(word, 0.0);
					}
				}

			}
		}

	}
	
	/** Compare documents for similarity based on word counts. This is not an ideal search method,
	 * but it is sufficient to illustrate document matching. The comparison is actually between
	 * two files each containing a series of short documents labeled by accession and 
	 * title. 
	 * @param doc1
	 * @param doc2
	 */
	public void compareDocuments( Map<String, DocStruct> doc1, Map<String, DocStruct> doc2) {
		Iterator<String> a1;
		String acc1;
		DocStruct d1;
		Iterator<String> a2;
		String acc2;
		DocStruct d2;
		double coeff;

		resultList = new LinkedList<ResultStruct>();
		
		titleSet1 = new HashSet<String>();
		titleList1 = new LinkedList<String>();
		titleSet2 = new HashSet<String>();
		titleList2 = new LinkedList<String>();
		accessionList1 = new LinkedList<String>();
		accessionSet1 = new HashSet<String>();
		accessionList2 = new LinkedList<String>();
		accessionSet2 = new HashSet<String>();
		
		int progress = 0;
		int goal = doc1.size();

		a1 = doc1.keySet().iterator();
		while (a1.hasNext()) {
			acc1 = a1.next();
			d1 = doc1.get(acc1);
			a2 = doc2.keySet().iterator();
			
			if ( !titleSet1.contains(d1.title)) {
				titleSet1.add(d1.title);
				titleList1.add(d1.title);
			}
			
			if ( !accessionSet1.contains(acc1)) {
				accessionSet1.add(acc1);
				accessionList1.add(acc1);
			}
			
			while (a2.hasNext()) {
				acc2 = a2.next();
				d2 = doc2.get(acc2);
				coeff = xcorrelate(d1, d2);
				//				System.out.printf(" %f correlation of %s, %s%n       with %s, %s%n%n", 
				//						coeff,
				//						acc1, d1.title, acc2, d2.title);
				resultList.add(new ResultStruct(coeff, d1.title, d2.title, acc1, acc2));

				if ( !titleSet2.contains(d2.title)) {
					titleSet2.add(d2.title);
					titleList2.add(d2.title);
				}
				
				if (!accessionSet2.contains(acc2)) {
					accessionSet2.add(acc2);
					accessionList2.add(acc2);
				}
			}
//			if (ban != null ) {
				ban.updatePBarPercent((100 * progress++)/goal, "matching...");
//			}
		}

		Collections.sort(resultList);
		Collections.sort(titleList1);
		Collections.sort(titleList2);
		Collections.sort(accessionList1);
		Collections.sort(accessionList2);
		maxCorrel = resultList.get(resultList.size() - 1).correl;
		ndocs1 = accessionList1.size();
		ndocs2 = accessionList2.size();
		
	}
	

	/** Calculate the similarity index for a pair of documents. The function is
	 * named "xcorrelate" because it is loosely based on the cross correlation of
	 * two sets of statistical fluctuations. 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private double xcorrelate(DocStruct d1, DocStruct d2) {
		double coeff = 0.0;
		double f1, f2;
		int n = 0;
		String word;
		Iterator<String> words = wordSums.keySet().iterator();
		while (words.hasNext()) {
			word = words.next();	
			f1 = d1.wordFrequencies.get(word);
			f2 = d2.wordFrequencies.get(word);
			if (f1 > 0.0 && f2 > 0.0) {
				n++;
				coeff += ( f1/wordFreqs.get(word)) *
				( f2/wordFreqs.get(word)) ;
			}
		}
		
		if ( n == 0 ) {
			return 0.0;
		} else {
			return coeff / n;
		}
	}

	
	private int getShade(double x) {
		long y = Math.round(  (255.0 * x) / maxCorrel);
		int iy = (int) y;
		return ( iy << 8) |  (255 << 24);
	}
	
	private void displayResults(int top, boolean useAccession) {
		
		int xstart = 0;
		int ystart = 0;
		int xdelta;
		int ydelta;
		
		if (useAccession) {
			System.out.printf("displayResults using accession and %n top points%n", top);
			xdelta = frame1.pixSize1 / accessionList1.size();
			ydelta = frame1.pixSize2 / accessionList2.size();
		} else {		
			System.out.printf("displayResults using title and %n top points%n", top);
			xdelta = frame1.pixSize1 / titleList1.size();
			ydelta = frame1.pixSize2 / titleList2.size();
		}
		
		int p;
		int ip;
		int jp;
		ResultStruct rs;
		
		int color;
		int mask = (255 << 16) | (255 << 8) | (255);
		Iterator<ResultStruct> irs = resultList.iterator();
		while (irs.hasNext() ) { // && icount++ < 20
			rs = irs.next();
			color = getShade(rs.correl);
			if (useAccession) {
				ip = accessionList1.indexOf(rs.accession1);
				jp = accessionList2.indexOf(rs.accession2);;
			} else {
				ip = titleList1.indexOf(rs.title1);
				jp = titleList2.indexOf(rs.title2);
			}
			xstart = ip * xdelta;
			ystart = jp * ydelta;
			for (int k = 0; k < xdelta; k++) {
				for (int l = 0; l < ydelta; l++) {
					p = (l + ystart) * frame1.pixSize2 + xstart + k;
					this.pix[p] = color;
				}
			}
		}
		
		int rsSize = resultList.size();
		int color2;
		if (top < rsSize) {
			for (int i = 0; i < top; i++) {
				rs = resultList.get(rsSize - i - 1);
				color = getShade(rs.correl);
				color2 = mask & color;
				color2 = color2 << 8;
				color = color | color2;
				if (useAccession) {
					ip = accessionList1.indexOf(rs.accession1);
					jp = accessionList2.indexOf(rs.accession2);;
				} else {
					ip = titleList1.indexOf(rs.title1);
					jp = titleList2.indexOf(rs.title2);
				}
				xstart = ip * xdelta;
				ystart = jp * ydelta;
				for (int k = 0; k < xdelta; k++) {
					for (int l = 0; l < ydelta; l++) {
						p = (l + ystart) * frame1.pixSize2 + xstart + k;
						this.pix[p] = color;
					}
				}
				
			}
		}
			
		this.imageSource.newPixels();
		this.imageComponent.repaint();
	}
	
	public static DrawFrame frame1;
	
	public void writeResultsXML(String dir, String file, int nresults,
			String entity1, String entity2) throws IOException, XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = null;
		reportedResults = nresults;
		
		FileWriter outFile = null;
		String fullName = dataDirectory + 
		File.separator + dir + File.separator + file;
		try {
			outFile = new FileWriter(fullName);
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
		
		writer.writeStartDocument();
		  writer.writeCharacters("\n");
		writer.writeStartElement("matches");
		  writer.writeCharacters("\n");
		
		ResultStruct rs;
		int rsSize = resultList.size();
		int nlim = Math.min(nresults, rsSize);
		System.out.printf("Write %d matches in file:%s%n", nlim, fullName);
		for (int i = 0; i < nlim; i++) {
			rs = resultList.get(rsSize - i - 1);
			writer.writeStartElement("match");
			  writer.writeCharacters("\n");
			
			  writer.writeStartElement("dataOwner1");
			  writer.writeCharacters(entity1);
			  writer.writeEndElement();
			  writer.writeCharacters("\n");
			  
			  writer.writeStartElement("dataOwner2");
			  writer.writeCharacters(entity2);
			  writer.writeEndElement();
			  writer.writeCharacters("\n");
			  
			  writer.writeStartElement("accession1");
			  writer.writeCharacters(rs.accession1);
			  writer.writeEndElement();
			  writer.writeCharacters("\n");
			  
			  writer.writeStartElement("accession2");
			  writer.writeCharacters(rs.accession2);
			  writer.writeEndElement();
			  writer.writeCharacters("\n");
			  
			  writer.writeStartElement("title1");
			  writer.writeCharacters(rs.title1);
			  writer.writeEndElement();
			  writer.writeCharacters("\n");
			  
			  writer.writeStartElement("title2");
			  writer.writeCharacters(rs.title2);
			  writer.writeEndElement();
			  writer.writeCharacters("\n");
			  
			  writer.writeStartElement("similarity");
			  writer.writeCharacters( Double.toString(rs.correl));
			  writer.writeEndElement();
			  writer.writeCharacters("\n");
			  
			writer.writeEndElement();
			  writer.writeCharacters("\n");
			
		}
		
		writer.writeEndElement();
		outFile.write("\n");
		writer.writeEndDocument();
		outFile.write("\n");
		outFile.close();
		
	}
	
	public void initDisplay() throws InterruptedException, InvocationTargetException {
				DrawFrame frame = new DrawFrame(ndocs1, ndocs2, 600, 600);
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.setVisible(true);
				frame1 = frame;
	}
	
	public void displayResults() {
		displayResults(reportedResults, true);
	}
	
	/**
	 * @param args
	 * @throws DocumentError 
	 * @throws InvocationTargetException 
	 * @throws InterruptedException 
	 * @throws XMLStreamException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws DocumentError, InterruptedException, InvocationTargetException, IOException, XMLStreamException {
		
		final CompareFiles instance = new CompareFiles("/Users/pbaker/Coding/eclipse_workspace/demo0/");
		
		
		// read both document files
//		instance.ingestFile("GreenTeam", "frag.xml", document1);
//		instance.ingestFile("BlueTeam", "frag.xml", document2);
		instance.ingestFile("GreenTeam", "GreenTeam", "Essays.xml");
		instance.ingestFile("BlueTeam", "BlueTeam", "Essays.xml");
		
		// sum the occurrence of words in both document files
		instance.sumCounts();
		
		// eliminate the most common words from the sums of the word counts
		instance.trimSums(100);
		
		// compute word frequencies for the remaining words over the text
		// in both documents.
		instance.computeFreqs();
		
		// for each document file, calculate a word frequency profile for each
		// individual document using only the words in that file. 
		instance.computeProfileFreqs();
		
		// compare the documents
		instance.compareDocuments(instance.documents.get("GreenTeam"), 
				instance.documents.get("BlueTeam"));
		
	
		// display results
		instance.initDisplay();
//		instance.displayResults();
		instance.displayResults(5, false);
		instance.writeResultsXML("ban", "matches.xml", 5, "GreenTeam", "BlueTeam");

	}



}
