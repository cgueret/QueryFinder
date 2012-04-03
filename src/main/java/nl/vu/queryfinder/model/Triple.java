/**
 * 
 */
package nl.vu.queryfinder.model;

import org.openrdf.model.Value;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Triple {
	private final Value subject;
	private final Value predicate;
	private final Value object;

	/**
	 * @param subject
	 * @param predicate
	 * @param object
	 */
	public Triple(Value subject, Value predicate, Value object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Triple [subject=" + subject + ", predicate=" + predicate + ", object=" + object + "]";
	}

	/**
	 * @return the subject
	 */
	public Value getSubject() {
		return subject;
	}

	/**
	 * @return the predicate
	 */
	public Value getPredicate() {
		return predicate;
	}

	/**
	 * @return the object
	 */
	public Value getObject() {
		return object;
	}
}
