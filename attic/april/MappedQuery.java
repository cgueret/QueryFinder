package nl.vu.queryfinder.model;

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vu.queryfinder.util.TripleSet;

/**
 * @author cgueret
 * 
 */
public class MappedQuery {
	static final Logger logger = LoggerFactory.getLogger(MappedQuery.class);
	private Set<TripleSet> groups = new TreeSet<TripleSet>();

	/**
	 * @param group
	 */
	public void addGroup(TripleSet group) {
		groups.add(group);
	}

	/**
	 * @return
	 */
	public Set<TripleSet> getGroups() {
		return groups;
	}

	/**
	 * 
	 */
	public void printContent() {
		logger.info("Mapped query contains " + groups.size() + " groups");
		for (TripleSet triples : groups)
			logger.info("Group [" + triples.getPattern() + "] -> " + triples.size() + " options");
	}

}
