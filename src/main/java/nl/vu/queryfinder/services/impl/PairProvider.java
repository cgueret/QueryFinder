/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.aggregate.AggCountVar;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;

import nl.erdf.datalayer.DataLayer;
import nl.erdf.model.ResourceProvider;
import nl.erdf.model.Solution;
import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.util.QueryEngineHTTPClient;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
class PairProvider implements ResourceProvider {
	private final Triple[] patterns;
	private final Random rand = new Random();
	private final EndPoint endPoint;

	/**
	 * @param first
	 * @param second
	 */
	public PairProvider(EndPoint endPoint, Triple first, Triple second) {
		this.endPoint = endPoint;
		patterns = new Triple[] { first, second };
	}

	/**
	 * @param initQuery
	 */
	private int getCount(Node_Variable variable, ElementGroup elg) {
		try {
			Query query = QueryFactory.create();
			query.setQuerySelectType();
			query.setQueryPattern(elg);
			query.addResultVar(query.allocAggregate(new AggCountVar(new ExprVar(variable))));
			// logger.info(query.serialize());

			// Exec and get the count
			QueryEngineHTTPClient queryExec = new QueryEngineHTTPClient(endPoint.getURI(), query);
			if (endPoint.getDefaultGraph() != null)
				queryExec.addDefaultGraph(endPoint.getDefaultGraph());
			ResultSet results = queryExec.execSelect();
			if (!results.hasNext())
				throw new Exception("No choice");

			QuerySolution a = results.next();
			int nb = Integer.parseInt(a.get(a.varNames().next()).asLiteral().getLexicalForm());
			queryExec.close();

			return nb;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * @param variable
	 * @param query
	 * @return
	 */
	private Node getOne(Node_Variable variable, ElementGroup elg, int nb) {
		try {
			Query query = QueryFactory.create();
			query.setQuerySelectType();
			query.addResultVar(variable);
			query.setQueryPattern(elg);
			query.setLimit(1);
			query.setOffset(rand.nextInt(nb));
			// logger.info(query.serialize());

			// Exec and get the count
			QueryEngineHTTPClient queryExec = new QueryEngineHTTPClient(endPoint.getURI(), query);
			if (endPoint.getDefaultGraph() != null)
				queryExec.addDefaultGraph(endPoint.getDefaultGraph());
			ResultSet results = queryExec.execSelect();
			if (!results.hasNext())
				throw new Exception("No choice");

			QuerySolution a = results.next();
			Node node = a.get(a.varNames().next()).asNode();
			queryExec.close();
			return node;
		} catch (Exception e) {
			return Node.NULL;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.erdf.model.ResourceProvider#getResource(com.hp.hpl.jena.graph.
	 * Node_Variable, nl.erdf.model.Solution, nl.erdf.datalayer.DataLayer)
	 */
	public Node getResource(Node_Variable variable, Solution solution, DataLayer dataLayer) {
		try {
			// Prepare a query
			ElementGroup elg = new ElementGroup();
			Map<Node, Node> replace = new HashMap<Node, Node>();
			for (Triple triple : patterns) {
				Node[] nodes = new Node[] { triple.getSubject(), triple.getPredicate(), triple.getObject() };
				for (int i = 0; i < nodes.length; i++) {
					if (nodes[i].isVariable() && !nodes[i].equals(variable)) {
						nodes[i] = solution.getBinding((Node_Variable) nodes[i]).getValue();
						if (nodes[i] == null || nodes[i].equals(Node.NULL) || nodes[i].equals(Node.ANY)) {
							Node n = replace.get(nodes[i]);
							if (n == null) {
								n = Node.createAnon();
								replace.put(nodes[i], n);
							}
							nodes[i] = n;
						}
					}
				}
				elg.addTriplePattern(Triple.create(nodes[0], nodes[1], nodes[2]));
			}
			// logger.info(elg.toString());

			// Get number of results
			int nb = getCount(variable, elg);
			if (nb == 0)
				throw new Exception("No choice");

			// Get one
			Node node = getOne(variable, elg, nb);
			return node;
		} catch (Exception e) {
			return Node.NULL;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.erdf.model.ResourceProvider#getVariables()
	 */
	public Set<Node_Variable> getVariables() {
		Set<Node_Variable> variables = new HashSet<Node_Variable>();
		for (int i = 0; i < 2; i++)
			for (Node n : new Node[] { patterns[i].getSubject(), patterns[i].getPredicate(), patterns[i].getObject() })
				if (n.isVariable())
					variables.add((Node_Variable) n);
		return variables;
	}
}

// Usage
/*
 * for (int i = 0; i < groups.length - 1; i++) { for (int j = i + 1; j <
 * groups.length; j++) { for (Triple t1 : groups[i]) { for (Triple t2 :
 * groups[j]) { Set<Node> v1 = getVars(t1); Set<Node> v2 = getVars(t2);
 * v1.retainAll(v2); if (!v1.isEmpty()) { ResourceProvider p = new
 * PairProvider(t1, t2); request.addResourceProvider(p); count++; } } } } }
 */

