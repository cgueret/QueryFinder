/**
 * 
 */
package nl.vu.queryfinder.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class QueryPattern {
	static final Logger logger = LoggerFactory.getLogger(QueryPattern.class);

	public static final String IS_A = "is_a";
	private final Node subject;
	private final Node predicate;
	private final Node object;

	/**
	 * @param s
	 * @param p
	 * @param o
	 */
	public QueryPattern(Node s, Node p, Node o) {
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
	public static QueryPattern create(Node s, Node p, Node o) {
		return new QueryPattern(s, p, o);
	}

	/**
	 * @return
	 */
	public Node getPredicate() {
		return predicate;
	}

	/**
	 * @return the subject
	 */
	public Node getSubject() {
		return subject;
	}

	/**
	 * @return the object
	 */
	public Node getObject() {
		return object;
	}

	/**
	 * @return
	 */
	public Node[] getElements() {
		return new Node[] { subject, predicate, object };
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Triple.create(subject, predicate, object).toString();
	}
}
