/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import nl.vu.queryfinder.services.ClassMatcher;
import nl.vu.queryfinder.util.PaginatedQueryExec;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class SPARQLClassMatcher implements ClassMatcher {
	private final String service;

	/**
	 * @param service
	 */
	public SPARQLClassMatcher(String service) {
		this.service = service;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.queryfinder.services.ClassMatcher#getClasses(java.lang.String)
	 */
	public Set<Node> getClasses(String keyword) {
		Node var = Node.createVariable("r");
		Node label = Node.createVariable("l");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(var);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(new Triple(Node.createAnon(), RDF.type.asNode(), var));
		group.addTriplePattern(new Triple(var, RDF.type.asNode(), OWL.Class.asNode()));
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
		SPARQLClassMatcher me = new SPARQLClassMatcher("http://dbpedia.org/sparql");
		for (Node resource : me.getClasses("artist"))
			System.out.println(resource);
	}

}
