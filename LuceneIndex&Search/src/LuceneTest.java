import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.naming.directory.SearchControls;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class LuceneTest
{
	public static String labelsfilePath = "E:\\Users\\Martin\\Desktop\\NT\\labels_en.nt";							      // File downloaded from: http://downloads.dbpedia.org/3.9/en/labels_en.nt.bz2
	public static String mbpropertiesFilePath = "E:\\Users\\Martin\\Desktop\\NT\\mappingbased_properties_cleaned_en.nt";  // File downloaded from: http://downloads.dbpedia.org/3.9/en/mappingbased_properties_cleaned_en.nt.bz2 
	public static String testingResourcesPath = "resources\\extractResources.txt";
	public static String keyword;
	
	public static void main(String[] args)
	{
		Lucene(args, labelsfilePath, testingResourcesPath, "resources\\labels_en_RESULTS");
		System.gc();
		Lucene(args, mbpropertiesFilePath, testingResourcesPath, "resources\\mappingbased_properties_cleaned_en_RESULTS");
		System.out.println("Done!");
	}
	
	public static void Lucene(String[] args, String NTFilePath, String resourceFilePath, String resultDirectory)
	{
		try
		{					
			// Kreiranje na analyzer za indeksiranje i prebaruvanje na tekst
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
			
			// Kreiranje na index-ot
			Directory index = new RAMDirectory();
			
			// Kreiranje na konfiguraciskiot objekt
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_44, analyzer);

			// Dodavanje na dokumenti
			addDocuments(index,config,NTFilePath);
				
			// Povlekuvanje na resursite potrebni za testiranje na searcher-ot
			ArrayList<Resource> resources = getResources(resourceFilePath);
			
			// == Proverka na tocnosta na Luceene vrz baza na poedinecni zborovi i parovi zborovi izvadeni
			//    od recenici za koi gi imame tocnite odgovori ==
			
			// Prebaruvanje po poedinecni zborovi
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(String.format("%s\\WordWeights.csv", resultDirectory))));
			bw.write("WORD,NUMBER OF HITS,NUMBER OF ANSWERS\n");
			for(int i = 0; i < resources.size(); i++)
			{
				for(int j = 0; j < resources.get(i).getSentenceWords().size(); j++)
				{
					keyword = resources.get(i).getSentenceWords().get(j).trim().toLowerCase();
					
					// Tekstot koja treba da se prebara
					String querystr = args.length > 0 ? args[0] : keyword;
					
					// Zemanje na rezultatite od izvrsenoto prebaruvanje
					ArrayList<ArrayList<String>> urls = getSearchResults(analyzer, index, querystr, keyword);
					
					// Presmetka na tezinata na tekovno prebaraniot zbor
					int wordWeight = calculateWordWeight(urls, resources.get(i).getAnswers());
					
					// Zapisuvanje na tezinata na soodvetniot zbor vo posebna datoteka
					bw.write(String.format("%s,%s,%s\n", keyword, wordWeight, resources.get(i).getAnswers().size()));
				}
				bw.write(",,\n,,\n");
			}
			bw.close();
	
			// Prebaruvanje po parovi od zborovi
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(String.format("%s\\WordPairWeights.csv", resultDirectory))));
			bw.write("WORD,NUMBER OF HITS,NUMBER OF ANSWERS\n");
			for(int i = 0; i < resources.size(); i++)
			{
				for(int j = 0; j < resources.get(i).getSentenceWords().size() - 1; j++)
				{
					keyword = String.format("%s %s",resources.get(i).getSentenceWords().get(j).trim().toLowerCase(),resources.get(i).getSentenceWords().get(j+1).trim().toLowerCase());
					
					// Tekstot koja treba da se prebara
					String querystr = args.length > 0 ? args[0] : keyword;
					
					// Zemanje na rezultatite od izvrsenoto prebaruvanje
					ArrayList<ArrayList<String>> urls = getSearchResults(analyzer, index, querystr, keyword);
					
					// Presmetka na tezinata na tekovno prebaraniot par od zborovi
					int wordWeight = calculateWordWeight(urls, resources.get(i).getAnswers());
					
					// Zapisuvanje na tezinata na soodvetniot par od zborovi vo posebna datoteka
					bw.write(String.format("%s,%s,%s\n", keyword, wordWeight, resources.get(i).getAnswers().size()));
				}
				bw.write(",,\n,,\n");
			}
			bw.close();
		
			System.out.println("Calculating word weights finished!\n==================================\n");
		}
		catch(Exception e)
		{
			System.out.println("===== KRAJ! =====");
		}
	}
	
	private static ArrayList<String> arrayToList(String[] array)
	{
		ArrayList<String> list = new ArrayList<String>();
		for(int i=0; i < array.length; i++)
			list.add(array[i]);
		
		return list;
	}
	
	private static void addDocuments(Directory index, IndexWriterConfig config, String filePath) throws Exception
	{
		BigFile file = null;
		IndexWriter w = null;
		try
		{
			w = new IndexWriter(index, config);	
			file = new BigFile(filePath);
			for (String line : file)
			{
				String[] fields = new String[3];
				for(int i=0; i < fields.length; i++)
					fields[i] = "";
				
				int numSpaces = 0;
				for(int i=0; i < line.length() - 2; i++)
				{
					if(line.charAt(i) == ' ')
						numSpaces++;
					
					if(numSpaces == 0)
						fields[0] += line.charAt(i);
					else if(numSpaces == 1)
						fields[1] += line.charAt(i);
					else
						fields[2] += line.charAt(i);
				}
				
				for(int i=0; i < fields.length; i++)
				{
					fields[i] = (fields[i].trim().replace("<", "")).replace(">", "");
					if(i == 2)
						fields[i] = fields[i].replace("@en", "");
				}

				addDoc(w, fields[0], fields[1], fields[2]);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		} 
		finally 
		{
			try 
			{
				w.close();
				file.Close();
				System.out.println("Indexing finished!\n==================\n");
			} 
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	private static void addDoc(IndexWriter w, String url1, String url2, String title) throws IOException
	{
		Document doc = new Document();
		
		// Tokenizacija na text field
		doc.add(new TextField("title", title, Field.Store.YES));
		// Koristime StringField za url1 bidejkji ne sakame da go tokenizirame i nego
		doc.add(new StringField("url1", url1, Field.Store.YES));
		// Koristime StringField za url2 bidejkji ne sakame da go tokenizirame i nego
		doc.add(new StringField("url2", url2, Field.Store.YES));
		w.addDocument(doc);
	}
	
	private static ArrayList<Resource> getResources(String path) throws IOException
	{
		ArrayList<Resource> resources = new ArrayList<Resource>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		String line = null;
		String[] sentenceWords = null;
		ArrayList<String> answers = null;
		while ((line = br.readLine()) != null)
		{
			if(!line.trim().contains("http://"))
			{
				if(answers != null)
					resources.add(new Resource(arrayToList(sentenceWords), answers));
				
				sentenceWords = line.split(" ");
				sentenceWords[sentenceWords.length - 1] = sentenceWords[sentenceWords.length - 1].replace(".", "");
				sentenceWords[sentenceWords.length - 1] = sentenceWords[sentenceWords.length - 1].replace("?", "");
				answers = new ArrayList<String>();
			}
			else
				answers.add(line.trim());
		}
		if(answers != null)
			resources.add(new Resource(arrayToList(sentenceWords), answers));
		
		br.close();
		return resources;
	}
	
	private static ArrayList<ArrayList<String>> getSearchResults(StandardAnalyzer analyzer, Directory index, String querystr, String keyword) throws Exception
	{
		ArrayList<ArrayList<String>> urls = new ArrayList<ArrayList<String>>();
		ArrayList<String> firstUrlColumn = new ArrayList<String>();
		ArrayList<String> secondUrlColumn = new ArrayList<String>();
		
		// "title" argumentot go oznacuva default-niot parametar koj se koristi koga nitu eden drug ne e explicitno specificiran za ova query
		Query q = new QueryParser(Version.LUCENE_44, "title", analyzer).parse(querystr);
		
		// Prebaruvanje
		int hitsPerPage = 10;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		
		// Prikazuvanje na rezultatite od izvrshenoto prebaruvanje
		for(int i=0; i < hits.length; i++)
		{
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			firstUrlColumn.add(d.get("url1"));
			secondUrlColumn.add(d.get("url2"));
		}
		urls.add(firstUrlColumn);
		urls.add(secondUrlColumn);
		
		// Koga nemame povekje potreba da gi pristapuvame dokumentite reader-ot se zatvora
		reader.close();
		
		return urls;
	}
	
	private static int calculateWordWeight(ArrayList<ArrayList<String>> urls, ArrayList<String> answers)
	{
		int wordWeight = 0;
		for(int i=0; i < urls.size(); i++)
			for(int j = 0; j < urls.get(i).size(); j++)
				for(int k=0; k < answers.size(); k++)
					if((urls.get(i).get(j).trim().toLowerCase()).equals(answers.get(k).trim().toLowerCase()))
						wordWeight++;
		
		return wordWeight;
	}
}
