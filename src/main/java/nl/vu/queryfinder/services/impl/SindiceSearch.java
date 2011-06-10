/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.sindice.Sindice;
import com.sindice.SindiceException;
import com.sindice.result.SearchResults;

import nl.vu.queryfinder.services.ResourceMatcher;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class SindiceSearch implements ResourceMatcher {
	static final Logger logger = LoggerFactory.getLogger(SindiceSearch.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.ResourceMatcher#getResources(java.lang.String)
	 */
	public Set<Resource> getResources(String keyword) {
		logger.info(String.format("Look for resources for \"%s\"", keyword));

		// Create a list of possible labels (mostly from
		// https://github.com/fadmaa/grefine-rdf-extension/blob/master/src/com/google/refine/org/deri/reconcile/rdf/factories/files/preview_properties.properties)
		List<String> labels = new ArrayList<String>();
		labels.add("http://www.w3.org/2000/01/rdf-schema#label");
		labels.add("http://www.w3.org/2004/02/skos/core#prefLabel");
		labels.add("http://www.w3.org/2004/02/skos/core#altLabel");
		labels.add("http://purl.org/dc/terms/title");
		labels.add("http://purl.org/dc/elements/1.1/title");
		labels.add("http://xmlns.com/foaf/0.1/name");
		labels.add("http://xmlns.com/foaf/0.1/givenName");
		labels.add("http://xmlns.com/foaf/0.1/familyName");
		labels.add("http://xmlns.com/foaf/0.1/nick");
		labels.add("http://www.geonames.org/ontology#name");
		labels.add("http://www.geonames.org/ontology#alternateName");

		Map<Resource, Integer> stats = new HashMap<Resource, Integer>();
		Model model = ModelFactory.createDefaultModel();
		Sindice query = new Sindice();
		try {
			for (String label : labels) {
				SearchResults searchResults = query.advancedSearch(label, keyword);
				for (int index = 0; index < Math.min(5, searchResults.size()); index++) {
					Resource resource = model.createResource(searchResults.get(index).getLink());
					Integer count = stats.get(resource);
					if (count == null)
						count = 0;
					stats.put(resource, count + 1);
				}
			}
		} catch (SindiceException e) {
			e.printStackTrace();
		} finally {
			model.close();
		}

		Set<Resource> resources = new LinkedHashSet<Resource>();
		for (Entry<Resource, Integer> stat : stats.entrySet())
			if (stat.getValue() > 1)
				resources.add(stat.getKey());
		logger.info(String.format("%d filtered results out of %d ", resources.size(), stats.size()));

		return resources;
	}

}
