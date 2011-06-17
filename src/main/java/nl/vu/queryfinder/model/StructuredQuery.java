/**
 * 
 */
package nl.vu.queryfinder.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class StructuredQuery extends HashSet<QueryPattern> {
	static final Logger logger = LoggerFactory.getLogger(StructuredQuery.class);
	/** For serialization */
	private static final long serialVersionUID = -718582275488380974L;
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
}
