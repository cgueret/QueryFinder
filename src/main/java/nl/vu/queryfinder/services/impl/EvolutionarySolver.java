/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import nl.erdf.constraints.impl.StatementPatternConstraint;
import nl.erdf.constraints.impl.StatementPatternSetConstraint;
import nl.erdf.datalayer.DataLayer;
import nl.erdf.datalayer.hbase.NativeHBaseDataLayer;
import nl.erdf.datalayer.sparql.SPARQLDataLayer;
import nl.erdf.model.Directory;
import nl.erdf.model.Request;
import nl.erdf.model.Solution;
import nl.erdf.model.impl.StatementPatternProvider;
import nl.erdf.model.impl.StatementPatternSetProvider;
import nl.erdf.optimizer.Optimizer;
import nl.erdf.util.Converter;
import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;

import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class EvolutionarySolver extends Service implements Observer {
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(EvolutionarySolver.class);

	private final DataLayer dataLayer;

	/**
	 * @param directory
	 */
	public EvolutionarySolver(Directory directory) {
		if (directory != null) {
			// Create the SPARQL data layer
			dataLayer = new SPARQLDataLayer(directory);
		} else {
			dataLayer = new NativeHBaseDataLayer();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.Service#process(nl.vu.queryfinder.model.Query)
	 */
	@Override
	public Query process(Query inputQuery) {
		// Create the request
		Request request = new Request(dataLayer);

		// Fill up the request
		Map<String, StatementPatternSetConstraint> constraints = new HashMap<String, StatementPatternSetConstraint>();
		Map<String, StatementPatternSetProvider> providers = new HashMap<String, StatementPatternSetProvider>();
		for (Quad quad : inputQuery.getTriples()) {
			// Turn the triple into a statement pattern
			Var s = Converter.toVar(quad.getSubject());
			Var p = Converter.toVar(quad.getPredicate());
			Var o = Converter.toVar(quad.getObject());
			String c = quad.getContext().stringValue();
			StatementPattern pattern = new StatementPattern(s, p, o);

			// Add a constraint
			if (constraints.containsKey(c)) {
				StatementPatternSetConstraint set = constraints.get(c);
				set.add(new StatementPatternConstraint(pattern));
			} else {
				StatementPatternSetConstraint set = new StatementPatternSetConstraint(c);
				set.add(new StatementPatternConstraint(pattern));
				constraints.put(c, set);
			}

			// Use that pattern as a provider
			if (providers.containsKey(c)) {
				StatementPatternSetProvider set = providers.get(c);
				set.add(new StatementPatternProvider(pattern));
			} else {
				StatementPatternSetProvider set = new StatementPatternSetProvider(c);
				set.add(new StatementPatternProvider(pattern));
				providers.put(c, set);
			}

			// Use it too to instantiate solutions
			request.addStatementPattern(pattern);
		}

		// Add the constraints
		for (Entry<String, StatementPatternSetConstraint> set : constraints.entrySet())
			request.addConstraint(set.getValue());

		// Add the providers
		for (Entry<String, StatementPatternSetProvider> set : providers.entrySet())
			request.addResourceProvider(set.getValue());

		// Create the optimiser
		Optimizer optimizer = new Optimizer(dataLayer, request, null);
		optimizer.addObserver(this);
		optimizer.run();

		Query output = new Query();
		return output;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable source, Object param) {
		// Check source
		if (!(source instanceof Optimizer))
			return;
		Optimizer optimizer = (Optimizer) source;

		// Get the best solution
		@SuppressWarnings("unchecked")
		Collection<Solution> solutions = (Collection<Solution>) param;
		boolean stop = false;
		for (Solution s : solutions) {
			logger.info(s.toString());
			for (nl.erdf.model.Triple triple : optimizer.getRequest().getTripleSet(s))
				if (triple.getNumberNulls() == 0)
					logger.info(triple.toString());
			if (s.isOptimal()) {
				stop = true;
			}
		}

		// If we should stop, do it
		if (stop) {
			optimizer.terminate();
			dataLayer.shutdown();
		}

	}

}
