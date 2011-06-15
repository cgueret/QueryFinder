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

import com.hp.hpl.jena.graph.Node;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class StructuredQuery extends HashSet<QueryPattern> {
	/** For serialization */
	private static final long serialVersionUID = -718582275488380974L;
	static final Logger logger = LoggerFactory.getLogger(StructuredQuery.class);
	private final Map<Node, Set<Node>> mappings = new HashMap<Node, Set<Node>>();

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
		for (Node part : pattern.getElements())
			if (!mappings.containsKey(part))
				mappings.put(part, new HashSet<Node>());

		return true;
	}

	/**
	 * @param predicate2
	 */
	public void addBinding(String keyword, Node resource) {
		mappings.get(keyword).add(resource);
	}

	/**
	 * 
	 */
	public void printBindings() {
		logger.info("List of mappings");
		for (Entry<Node, Set<Node>> mapping : mappings.entrySet())
			for (Node r : mapping.getValue())
				logger.info(String.format("%s -> %s ", mapping.getKey(), r));
	}

	/**
	 * @return
	 */
	public Set<String> getStatements() {
		Set<String> statements = new TreeSet<String>();
		for (QueryPattern pattern : this) {
			// Get the list of all possible s
			Set<Node> s = new HashSet<Node>();
			for (Node r : mappings.get(pattern.getSubject()))
				s.add(r);
			if (s.isEmpty())
				s.add(pattern.getSubject());

			// Get the list of all possible p
			Set<Node> p = new HashSet<Node>();
			for (Node r : mappings.get(pattern.getPredicate()))
				p.add(r);
			if (p.isEmpty())
				p.add(pattern.getPredicate());

			// Get the list of all possible o
			Set<Node> o = new HashSet<Node>();
			for (Node r : mappings.get(pattern.getObject()))
				o.add(r);
			if (o.isEmpty())
				o.add(pattern.getObject());

			for (Node a : s)
				for (Node b : p)
					for (Node c : o)
						statements.add(a + " " + b + " " + c);
		}
		return statements;
	}
}
