package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import nl.vu.queryfinder.services.ResourceMatcher;
import nl.vu.queryfinder.util.PaginatedQueryExec;

public class SPARQLResourceMatcher implements ResourceMatcher {
	private final String service;

	/**
	 * @param service
	 */
	public SPARQLResourceMatcher(String service) {
		this.service = service;
	}

	public Set<Node> getResources(String keyword) {
		Node var = Node.createVariable("r");
		Node label = Node.createVariable("l");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(var);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(new Triple(var, RDF.type.asNode(), Node.createAnon()));
		group.addTriplePattern(new Triple(var, RDFS.label.asNode(), label));
		group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(keyword)));
		query.setQueryPattern(group);

		Set<Node> results = PaginatedQueryExec.process(service, query, var);
		return results;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		SPARQLResourceMatcher me = new SPARQLResourceMatcher("http://dbpedia.org/sparql");
		for (Node resource : me.getResources("amsterdam"))
			System.out.println(resource);
	}

}
