/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class WordNetExpander extends Service {
	// Logger
	protected static final Logger logger = LoggerFactory.getLogger(ModelExpander.class);

	// Dictionary
	protected final IDictionary dict;

	// Value factory
	protected final ValueFactory f = new ValueFactoryImpl();

	public WordNetExpander() throws IOException {
		// construct the URL to the Wordnet dictionary directory
		String wnhome = "/usr/share/wordnet/";
		String path = wnhome + File.separator + "dict";
		URL url = new URL("file", null, path);

		// construct the dictionary object and open it
		dict = new Dictionary(url);
		dict.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.Service#process(nl.vu.queryfinder.model.Query)
	 */
	@Override
	public Query process(Query inputQuery) {
		// Create the query
		Query outputQuery = new Query();

		// Copy the value of the description
		outputQuery.setDescription(inputQuery.getDescription());

		// Iterate over the triples
		for (Quad quad : inputQuery.getQuads()) {
			// Prepare a list of subjects
			List<Value> subjects = new ArrayList<Value>();
			Value subject = quad.getSubject();
			if (subject instanceof Literal && !subject.stringValue().startsWith("?")) {
				subjects.addAll(getWords(subject.stringValue()));
			} else {
				subjects.add(subject);
			}

			// Prepare a list of predicates
			List<Value> predicates = new ArrayList<Value>();
			Value predicate = quad.getPredicate();
			if (predicate instanceof Literal && !predicate.stringValue().startsWith("?")) {
				predicates.addAll(getWords(predicate.stringValue()));
			} else {
				predicates.add(predicate);
			}

			// Prepare a list of objects
			List<Value> objects = new ArrayList<Value>();
			Value object = quad.getObject();
			if (object instanceof Literal && !object.stringValue().startsWith("?")) {
				objects.addAll(getWords(object.stringValue()));
			} else {
				objects.add(object);
			}

			// Do the cartesian product of the lists
			for (Value s : subjects)
				for (Value p : predicates)
					for (Value o : objects)
						outputQuery.addQuad(new Quad(s, p, o, quad.getContext()));

			// Add the original quad
			outputQuery.addQuad(quad);
		}

		return outputQuery;
	}

	/**
	 * @param stringValue
	 * @return
	 */
	private Collection<Literal> getWords(String wordTxt) {
		// Prepare the output
		List<Literal> out = new ArrayList<Literal>();

		// look up first sense of the word
		IIndexWord idxWord = dict.getIndexWord(wordTxt, POS.NOUN);
		IWordID wordID = idxWord.getWordIDs().get(0); // 1 st meaning
		IWord word = dict.getWord(wordID);
		ISynset synset = word.getSynset();

		// iterate over words associated with the synset
		for (IWord w : synset.getWords())
			out.add(f.createLiteral(w.getLemma()));

		return out;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public static void main(String[] args) throws IOException, RepositoryException {
		WordNetExpander e = new WordNetExpander();
		e.getWords("dog");
	}
}
