package nl.vu.queryfinder.tests;

import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.util.FileManager;

// Bad news: http://tech.groups.yahoo.com/group/jena-dev/message/45370
public class ParserTest {

	/**
	 * @throws IOException
	 * 
	 */
	public ParserTest() throws IOException {
		InputStream in = this.getClass().getResourceAsStream("query.sparql");
		String queryString = FileManager.get().readWholeFileAsUTF8(in);
		in.close();
		Query query = QueryFactory.create(queryString);
		System.out.println(query.serialize());
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		@SuppressWarnings("unused")
		ParserTest test = new ParserTest();
	}

}
