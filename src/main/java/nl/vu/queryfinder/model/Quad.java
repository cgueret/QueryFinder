/**
 * 
 */
package nl.vu.queryfinder.model;

import org.openrdf.model.Value;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Quad {
	private final Value subject;
	private final Value predicate;
	private final Value object;
	private final Value context;

	/**
	 * @param subject
	 * @param predicate
	 * @param object
	 */
	public Quad(Value subject, Value predicate, Value object, Value context) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Triple [subject=" + subject + ", predicate=" + predicate + ", object=" + object + ", context="
				+ context + "]";
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

	/**
	 * @return the context
	 */
	public Value getContext() {
		return context;
	}
}
