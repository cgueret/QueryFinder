/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import nl.vu.queryfinder.services.PropertyMatcher;
import nl.vu.queryfinder.util.PaginatedQueryExec;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class SPARQLPropertyMatcher implements PropertyMatcher {
	private Node[] propertyTypes = { RDF.Property.asNode(), OWL.DatatypeProperty.asNode(), OWL.ObjectProperty.asNode() };
	private final String service;

	/**
	 * @param service
	 */
	public SPARQLPropertyMatcher(String service) {
		this.service = service;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.PropertyMatcher#getProperties(java.lang.String
	 * )
	 */
	public Set<Node> getProperties(String keyword) {
		Node var = Node.createVariable("r");
		Node label = Node.createVariable("l");
		Query query = QueryFactory.create();
		query.setQuerySelectType();
		query.setDistinct(true);
		query.addResultVar(var);

		Set<Node> results = new HashSet<Node>();
		for (Node propertyType : propertyTypes) {
			ElementGroup group = new ElementGroup();
			group.addTriplePattern(new Triple(Node.createAnon(), var, Node.createAnon()));
			group.addTriplePattern(new Triple(var, RDF.type.asNode(), propertyType));
			group.addTriplePattern(new Triple(var, RDFS.label.asNode(), label));
			group.addTriplePattern(new Triple(label, Node.createURI("bif:contains"), Node.createLiteral(keyword)));
			query.setQueryPattern(group);

			results.addAll(PaginatedQueryExec.process(service, query, var));
		}

		return results;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		SPARQLPropertyMatcher me = new SPARQLPropertyMatcher("http://dbpedia.org/sparql");
		for (Node resource : me.getProperties("field"))
			System.out.println(resource);
	}
}
