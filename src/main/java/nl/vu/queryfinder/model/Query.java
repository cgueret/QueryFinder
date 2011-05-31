/**
 * 
 */
package nl.vu.queryfinder.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Query extends HashSet<QueryPattern> {
	/** For serialization */
	private static final long serialVersionUID = -718582275488380974L;
	static final Logger logger = LoggerFactory.getLogger(Query.class);
	private final Map<String, Set<Resource>> mappings = new HashMap<String, Set<Resource>>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.HashSet#add(java.lang.Object)
	 */
	public boolean add(QueryPattern pattern) {
		// Check if already in there
		if (this.contains(pattern))
			return false;

		// Add the pattern
		super.add(pattern);

		// Initiate the mappings sets
		for (String part : pattern.getElements())
			if (!mappings.containsKey(part))
				mappings.put(part, new HashSet<Resource>());

		return true;
	}

	/**
	 * @param predicate2
	 */
	public void addBinding(String keyword, Resource resource) {
		mappings.get(keyword).add(resource);
	}

	/**
	 * 
	 */
	public void printBindings() {
		logger.info("List of mappings");
		for (Entry<String, Set<Resource>> mapping : mappings.entrySet())
			for (Resource r : mapping.getValue())
				logger.info(String.format("%s -> %s ", mapping.getKey(), r));
	}

	/**
	 * @return
	 */
	public Set<String> getStatements() {
		Set<String> statements = new TreeSet<String>();
		for (QueryPattern pattern : this) {
			// Get the list of all possible s
			Set<String> s = new HashSet<String>();
			for (Resource r : mappings.get(pattern.getSubject()))
				s.add("<" + r.toString() + ">");
			if (s.isEmpty())
				s.add(pattern.getSubject());

			// Get the list of all possible p
			Set<String> p = new HashSet<String>();
			for (Resource r : mappings.get(pattern.getPredicate()))
				p.add("<" + r.toString() + ">");
			if (p.isEmpty())
				p.add(pattern.getPredicate());

			// Get the list of all possible o
			Set<String> o = new HashSet<String>();
			for (Resource r : mappings.get(pattern.getObject()))
				o.add("<" + r.toString() + ">");
			if (o.isEmpty())
				o.add(pattern.getObject());

			for (String a : s)
				for (String b : p)
					for (String c : o)
						statements.add(a + " " + b + " " + c);
		}
		return statements;
	}
}
