/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;

import nl.erdf.datalayer.DataLayer;
import nl.erdf.model.Constraint;
import nl.erdf.model.Solution;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class GraphSizeConstraint implements Constraint {
	private final List<Triple> patterns = new ArrayList<Triple>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.erdf.model.Constraint#getReward(nl.erdf.model.Solution,
	 * nl.erdf.datalayer.DataLayer)
	 */
	@Override
	public double getReward(Solution solution, DataLayer dataLayer) {
		// Returns the ratio of triples which are in the largest connected
		// component

		List<List<Triple>> components = new ArrayList<List<Triple>>();
		for (Triple pattern : patterns) {
			// Instantiate the triple
			Node subject = pattern.getSubject();
			if (subject.isVariable())
				subject = solution.getBinding((Node_Variable) subject).getValue();
			Node predicate = pattern.getPredicate();
			if (predicate.isVariable())
				predicate = solution.getBinding((Node_Variable) predicate).getValue();
			Node object = pattern.getObject();
			if (object.isVariable())
				object = solution.getBinding((Node_Variable) object).getValue();
			Triple triple = Triple.create(subject, predicate, object);

			if (dataLayer.isValid(triple)) {
				// Assign the triple to one of the component
				Iterator<List<Triple>> it = components.iterator();
				while (it.hasNext() && triple != null) {
					List<Triple> component = it.next();
					boolean match = false;
					for (Triple t : component) {
						match = match || (t.getSubject() == triple.getSubject());
						match = match || (t.getObject() == triple.getSubject());
						match = match || (t.getSubject() == triple.getObject());
						match = match || (t.getObject() == triple.getObject());
					}
					if (match) {
						component.add(triple);
						triple = null;
					}
				}

				// If no matching component found, create a new one
				if (triple != null) {
					List<Triple> component = new ArrayList<Triple>();
					component.add(triple);
					components.add(component);
				}
			}
		}

		// Get the size of largest component
		double largest = 0;
		for (List<Triple> component : components)
			if (component.size() > largest)
				largest = component.size();

		// Compute the ratio
		double res = largest / (double) (patterns.size());

		return res;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.erdf.model.Constraint#getVariables()
	 */
	@Override
	public Set<Node_Variable> getVariables() {
		Set<Node_Variable> vars = new HashSet<Node_Variable>();
		for (Triple pattern : patterns) {
			if (pattern.getSubject().isVariable())
				vars.add((Node_Variable) pattern.getSubject());
			if (pattern.getPredicate().isVariable())
				vars.add((Node_Variable) pattern.getPredicate());
			if (pattern.getObject().isVariable())
				vars.add((Node_Variable) pattern.getObject());
		}
		return vars;
	}

	/**
	 * @param triple
	 */
	public void add(Triple triple) {
		patterns.add(triple);
	}

	/**
	 * @return
	 */
	public int getSize() {
		return patterns.size();
	}
}
