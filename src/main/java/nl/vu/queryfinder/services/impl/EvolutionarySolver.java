/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import nl.erdf.constraints.TripleBlockConstraint;
import nl.erdf.constraints.TripleConstraint;
import nl.erdf.datalayer.sparql.SPARQLRequest;
import nl.erdf.datalayer.sparql.orig.Directory;
import nl.erdf.datalayer.sparql.orig.SPARQLDataLayer;
import nl.erdf.model.Solution;
import nl.erdf.optimizer.Optimizer;
import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.MappedQuery;
import nl.vu.queryfinder.services.QueryGenerator;
import nl.vu.queryfinder.util.TripleSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class EvolutionarySolver implements Observer, QueryGenerator {
	static final Logger logger = LoggerFactory.getLogger(EvolutionarySolver.class);
	private Optimizer optimizer;
	private SPARQLRequest request;
	private SPARQLDataLayer datalayer;
	private Directory directory;
	private Set<Query> queries = new HashSet<Query>();
	private Set<Set<Triple>> solutions = new HashSet<Set<Triple>>();
	private int solutionsSize = 0;

	/**
	 * @param endPoints
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public EvolutionarySolver(List<EndPoint> endPoints) throws FileNotFoundException, IOException {
		// Create a directory
		directory = new Directory();
		for (EndPoint endPoint : endPoints)
			directory.add("End point", endPoint.getURI());

		// Create a data layer
		datalayer = new SPARQLDataLayer(directory);
	}

	/**
	 * 
	 */
	public void terminate() {
		// Close the directory (stops the connections to the end points)
		directory.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public void update(Observable source, Object arg) {
		// Check source
		if (!(source instanceof Optimizer))
			return;

		// Get the best solution
		boolean stop = false;
		for (Solution s : (Collection<Solution>) arg) {
			Set<Triple> triples = request.getTripleSet(s);
			if (triples.size() > solutionsSize) {
				solutionsSize = triples.size();
				solutions.clear();
			}
			if (triples.size() == solutionsSize) {
				solutions.add(triples);
				if (s.isOptimal())
					stop = true;
			}
		}

		// If we should stop, do it
		if (stop)
			optimizer.terminate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.QueryGenerator#getQuery(nl.vu.queryfinder.
	 * model.MappedQuery)
	 */
	public Set<Query> getQuery(MappedQuery mappedQuery) throws Exception {
		// Reset the data from previous calls
		solutions.clear();
		solutionsSize = 0;

		// Create the request
		boolean oneTriplePerBlock = true;
		request = new SPARQLRequest(datalayer);
		for (TripleSet triples : mappedQuery.getGroups()) {
			if (oneTriplePerBlock) {
				for (Triple triple : triples)
					request.addConstraint(new TripleConstraint(triple));
			} else {
				TripleBlockConstraint block = new TripleBlockConstraint();
				for (Triple triple : triples)
					block.add(new TripleConstraint(triple));
				request.addConstraint(block);
			}
		}

		// Create the providers
		int count = 0;
		TripleSet[] groups = mappedQuery.getGroups().toArray(new TripleSet[mappedQuery.getGroups().size()]);
		for (int i = 0; i < groups.length; i++) {
			for (Triple t : groups[i]) {
				request.addResourceProvider(new TripleConstraint(t));
				count++;
			}
		}
		logger.info(count + " providers");

		// Create the optimiser
		optimizer = new Optimizer(datalayer, request, null);
		optimizer.addObserver(this);

		// Wait for completion
		logger.info("Start search");
		optimizer.run();
		logger.info("Search completed");

		// Print the queries and reset
		Model model = ModelFactory.createDefaultModel();
		for (Set<Triple> triples : solutions) {
			logger.info("Found solution");
			for (Triple triple : triples)
				logger.info(model.asStatement(triple).toString());
		}

		return queries;
	}

	/*
	 * protected Set<Node> getVars(final Triple t) { Set<Node> variables = new
	 * HashSet<Node>(); for (Node n : new Node[] { t.getSubject(),
	 * t.getPredicate(), t.getObject() }) if (n.isVariable()) variables.add(n);
	 * return variables; }
	 */

	/*
	 * protected boolean isValid(Triple[] set) throws Exception { Query query =
	 * QueryFactory.make(); query.setQueryAskType(); ElementGroup elg = new
	 * ElementGroup(); for (Triple triple : set) elg.addTriplePattern(triple);
	 * query.setQueryPattern(elg); QueryEngineHTTPClient queryExec = new
	 * QueryEngineHTTPClient(endPoint.getURI(), query); if
	 * (endPoint.getDefaultGraph() != null)
	 * queryExec.addDefaultGraph(endPoint.getDefaultGraph()); return
	 * queryExec.execAsk(); }
	 */
}
