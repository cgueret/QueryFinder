/**
 * 
 */
package nl.vu.queryfinder.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class QueryPattern {
	static final Logger logger = LoggerFactory.getLogger(QueryPattern.class);

	public static final String IS_A = "is_a";
	private final String subject;
	private final String predicate;
	private final String object;

	/**
	 * @param s
	 * @param p
	 * @param o
	 */
	public QueryPattern(String s, String p, String o) {
		this.subject = s;
		this.predicate = p;
		this.object = o;
	}

	/**
	 * @param s
	 * @param p
	 * @param o
	 * @return
	 */
	public static QueryPattern create(String s, String p, String o) {
		return new QueryPattern(s, p, o);
	}

	/**
	 * @return
	 */
	public String getPredicate() {
		return predicate;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @return the object
	 */
	public String getObject() {
		return object;
	}

	/**
	 * @return
	 */
	public String[] getElements() {
		return new String[] { subject, predicate, object };
	}

}
