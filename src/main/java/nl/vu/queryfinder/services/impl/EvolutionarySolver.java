/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import nl.erdf.constraints.impl.StatementPatternConstraint;
import nl.erdf.model.Request;
import nl.erdf.model.Solution;
import nl.erdf.model.impl.StatementPatternProvider;
import nl.erdf.optimizer.Optimizer;
import nl.erdf.util.Converter;
import nl.vu.queryfinder.model.Quad;
import nl.vu.queryfinder.model.Query;
import nl.vu.queryfinder.services.Service;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
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

	// List of optimal solutions
	private Collection<Solution> solutions = null;

	// Value factory
	protected final ValueFactory f = new ValueFactoryImpl();

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
		int nbConstraints = 0;
		// Map<String, StatementPatternSetConstraint> constraints = new
		// HashMap<String, StatementPatternSetConstraint>();
		for (Quad quad : inputQuery.getQuads()) {
			// Turn the triple into a statement pattern
			Var s = Converter.toVar(quad.getSubject());
			Var p = Converter.toVar(quad.getPredicate());
			Var o = Converter.toVar(quad.getObject());
			// String c = quad.getContext().stringValue();
			StatementPattern pattern = new StatementPattern(s, p, o);

			// Add a constraint
			/*
			 * nbConstraints++; if (constraints.containsKey(c)) {
			 * StatementPatternSetConstraint set = constraints.get(c);
			 * set.add(new StatementPatternConstraint(pattern)); } else {
			 * StatementPatternSetConstraint set = new
			 * StatementPatternSetConstraint(c); set.add(new
			 * StatementPatternConstraint(pattern)); constraints.put(c, set); }
			 */
			nbConstraints++;
			request.addConstraint(new StatementPatternConstraint(pattern));

			// If it has only one variable, use that pattern as a provider
			int nbVar = 0;
			nbVar += quad.getSubject().stringValue().startsWith("?") ? 1 : 0;
			nbVar += quad.getPredicate().stringValue().startsWith("?") ? 1 : 0;
			nbVar += quad.getObject().stringValue().startsWith("?") ? 1 : 0;
			if (nbVar == 1)
				request.addResourceProvider(new StatementPatternProvider(pattern));

			// Use it too to instantiate solutions
			request.addStatementPattern(pattern);
		}

		// Add the constraints
		// for (Entry<String, StatementPatternSetConstraint> set :
		// constraints.entrySet())
		// request.addConstraint(set.getValue());

		// logger.info("Number of constraint sets: " + constraints.size());
		logger.info("Number of constraints: " + request.getNbConstraints());
		logger.info("Number of variables: " + request.getNbVariables());

		// Create the optimiser
		Optimizer optimizer = new Optimizer(dataLayer, request, null);
		optimizer.addObserver(this);
		optimizer.run();

		Query output = new Query();

		// Copy the value of the description
		output.setDescription(inputQuery.getDescription());

		for (Solution s : solutions) {
			String solutionName = s.toString();
			logger.info("Result : " + solutionName);
			for (nl.erdf.model.Triple triple : request.getTripleSet(s)) {
				if (triple.getNumberNulls() == 0) {
					Quad quad = new Quad(triple.getSubject(), triple.getPredicate(), triple.getObject(),
							f.createLiteral(solutionName));
					output.addQuad(quad);
				}
			}
		}

		return output;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void update(Observable source, Object param) {
		// Check source
		if (!(source instanceof Optimizer))
			return;
		Optimizer optimizer = (Optimizer) source;
		solutions = (Collection<Solution>) param;

		// Check if all the solutions have the same fitness
		double[] fitnesses = new double[solutions.size()];
		int i = 0;
		for (Solution s : solutions) {
			logger.info("Candidate solution : " + s.toString());
			fitnesses[i] = s.getFitness();
			i++;
		}

		StandardDeviation dev = new StandardDeviation();
		logger.info("Dev " + dev.evaluate(fitnesses));
		boolean staled = (dev.evaluate(fitnesses) < 0.001) && (fitnesses[0] > 0.1);

		/*
		 * boolean stop = false; for (Solution s : solutions) {
		 * logger.info(s.toString()); for (nl.erdf.model.Triple triple :
		 * optimizer.getRequest().getTripleSet(s)) if (triple.getNumberNulls()
		 * == 0) logger.info(triple.toString()); if (s.isOptimal()) { stop =
		 * true; } }
		 */

		// If we should stop, do it
		if (staled) {
			logger.info("Evolution staled ");
			optimizer.terminate();
			dataLayer.shutdown();
		}

	}

}
