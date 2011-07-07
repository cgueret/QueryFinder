package nl.vu.queryfinder.services.impl;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.vocabulary.RDF;

import nl.vu.queryfinder.model.MappedQuery;
import nl.vu.queryfinder.model.QueryPattern;
import nl.vu.queryfinder.model.StructuredQuery;
import nl.vu.queryfinder.services.ClassMatcher;
import nl.vu.queryfinder.services.MappedQueryGenerator;
import nl.vu.queryfinder.services.PropertyMatcher;
import nl.vu.queryfinder.services.ResourceMatcher;
import nl.vu.queryfinder.util.TripleSet;

public class DefaultMappedQueryGen implements MappedQueryGenerator {
	static final Logger logger = LoggerFactory.getLogger(DefaultMappedQueryGen.class);
	private PropertyMatcher propertyMatcher = null;
	private ResourceMatcher resourceMatcher = null;
	private ClassMatcher classMatcher = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.queryfinder.services.MappedQueryGenerator#getMappedQuery(nl.vu.
	 * queryfinder.model.StructuredQuery,
	 * nl.vu.queryfinder.services.PropertyMatcher,
	 * nl.vu.queryfinder.services.ResourceMatcher,
	 * nl.vu.queryfinder.services.ClassMatcher)
	 */
	public MappedQuery getMappedQuery(StructuredQuery structuredQuery, PropertyMatcher propertyMatcher,
			ResourceMatcher resourceMatcher, ClassMatcher classMatcher) {

		// Save local pointers
		this.propertyMatcher = propertyMatcher;
		this.resourceMatcher = resourceMatcher;
		this.classMatcher = classMatcher;

		MappedQuery mappedQuery = new MappedQuery();
		for (QueryPattern pattern : structuredQuery) {
			TripleSet triples = null;

			// Handle the special case when a type is asked
			if (pattern.getPredicate().equals(RDF.type.asNode()))
				triples = handleType(pattern.getSubject(), pattern.getObject());

			// Handle the special case when P is a variable
			else if (pattern.getPredicate().isVariable())
				triples = handleVariable(pattern.getSubject(), pattern.getObject());

			// Generic case
			else
				triples = handlePattern(pattern.getSubject(), pattern.getPredicate(), pattern.getObject());

			// Add the group to the query
			if (triples != null && !triples.isEmpty())  {
				triples.setPattern(pattern);
				mappedQuery.addGroup(triples);
			}
		}

		return mappedQuery;
	}

	/**
	 * Generate the set of triples using the mapping for the predicates as a
	 * pivot for constraining both subject and objects
	 * 
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 */
	private TripleSet handlePattern(Node subject, Node predicate, Node object) {
		logger.info("S,P,O");
		
		// Get all the predicates
		Set<Node> predicates = new HashSet<Node>();
		if (predicate.isURI())
			predicates.add(predicate);
		else if (predicate.isLiteral())
			predicates.addAll(propertyMatcher.getProperties(predicate.getLiteralLexicalForm()));

		// Create the set
		TripleSet set = new TripleSet();
		for (Node p : predicates) {
			
			// <?,P,O>
			if (subject.isVariable() && !object.isVariable()) {
				Triple context = Triple.create(Node.createAnon(), p, resourceMatcher.getVariable());
				Set<Node> objects = new HashSet<Node>();
				if (p.equals(RDF.type.asNode()))
					objects.addAll(classMatcher.getClasses(object.getLiteralLexicalForm()));
				else
					objects.addAll(resourceMatcher.getResources(object.getLiteralLexicalForm(), context));
				for (Node o:objects)
					set.add(Triple.create(subject, p, o));
			}
			
			// <S,P,?>
			if (!subject.isVariable() && object.isVariable()) {
				Triple context = Triple.create(resourceMatcher.getVariable(), p, Node.createAnon());
				Set<Node> subjects = resourceMatcher.getResources(subject.getLiteralLexicalForm(), context);
				for (Node s:subjects)
					set.add(Triple.create(s, p, object));
			}
			
			// <?,P,?>
			set.add(Triple.create(subject, p, object));
		}
		
		return set;
	}

	/**
	 * @param subject
	 * @param object
	 * @return
	 */
	private TripleSet handleVariable(Node subject, Node object) {
		// Get subjects
		Set<Node> subjects = new HashSet<Node>();
		if (subject.isLiteral())
			subjects.addAll(resourceMatcher.getResources(subject.getLiteralLexicalForm(), null));
		else if (subject.isURI())
			subjects.add(subject);

		// Get objects
		Set<Node> objects = new HashSet<Node>();
		if (object.isLiteral()) {
			objects.addAll(classMatcher.getClasses(object.getLiteralLexicalForm()));
			objects.addAll(resourceMatcher.getResources(object.getLiteralLexicalForm(), null));
		} else if (object.isURI())
			objects.add(object);

		// Create the set
		TripleSet set = new TripleSet();
		for (Node s : subjects)
			for (Node o : objects)
				set.add(Triple.create(s, RDF.type.asNode(), o));

		return set;
	}

	/**
	 * @param subject
	 * @param object
	 * @return
	 */
	private TripleSet handleType(Node subject, Node object) {
		logger.info("S,type,O");
		
		// Get subjects
		Set<Node> subjects = new HashSet<Node>();
		if (subject.isLiteral())
			subjects.addAll(resourceMatcher.getResources(subject.getLiteralLexicalForm(), null));
		else if (subject.isURI() || subject.isVariable())
			subjects.add(subject);

		// Get objects
		Set<Node> objects = new HashSet<Node>();
		if (object.isLiteral())
			objects.addAll(classMatcher.getClasses(object.getLiteralLexicalForm()));
		else if (object.isURI() || object.isVariable())
			objects.add(object);

		// Create the set
		TripleSet set = new TripleSet();
		for (Node s : subjects)
			for (Node o : objects)
				set.add(Triple.create(s, RDF.type.asNode(), o));
		
		return set;
	}
}
