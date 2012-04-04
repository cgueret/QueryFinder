package nl.vu.queryfinder.util;

import java.util.HashSet;

import nl.vu.queryfinder.model.QueryPattern;

import com.hp.hpl.jena.graph.Triple;

public class TripleSet extends HashSet<Triple> implements Comparable<TripleSet> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7890082691310811635L;

	private QueryPattern pattern;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(TripleSet o) {
		return this.size() - o.size();
	}

	public QueryPattern getPattern() {
		return pattern;
	}

	public void setPattern(QueryPattern pattern) {
		this.pattern = pattern;
	}

}
