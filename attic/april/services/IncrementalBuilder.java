/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import nl.vu.queryfinder.model.EndPoint;
import nl.vu.queryfinder.model.MappedQuery;
import nl.vu.queryfinder.services.QueryGenerator;
import nl.vu.queryfinder.util.QueryEngineHTTPClient;
import nl.vu.queryfinder.util.TripleSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class IncrementalBuilder implements QueryGenerator {
	public class BuildingBlock extends HashSet<TripleSet> implements Comparable<BuildingBlock> {
		private static final long serialVersionUID = -4820888956735317884L;
		private String title;

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(BuildingBlock o) {
			return this.size() - o.size();
		}

		/**
		 * @param title
		 *           the title to set
		 */
		public void setTitle(String title) {
			this.title = title;
		}

		/**
		 * @return the title
		 */
		public String getTitle() {
			return title;
		}
	}

	static final Logger logger = LoggerFactory.getLogger(IncrementalBuilder.class);
	final EndPoint endPoint;

	private int numberCalls;

	/**
	 * @param endPoint
	 */
	public IncrementalBuilder(EndPoint endPoint) {
		this.endPoint = endPoint;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.queryfinder.services.QueryGenerator#getQuery(nl.vu.queryfinder.
	 * model.MappedQuery)
	 */
	public Set<Query> getQuery(MappedQuery mappedQuery) throws Exception {
		// Solve
		LinkedList<BuildingBlock> blocks = new LinkedList<BuildingBlock>();
		for (TripleSet triples : mappedQuery.getGroups()) {
			BuildingBlock block = new BuildingBlock();
			for (Triple t : triples) {
				TripleSet set = new TripleSet();
				set.add(t);
				block.add(set);
			}
			block.setTitle(triples.getPattern().toString());
			blocks.add(block);
		}

		// Get the result
		numberCalls = 0;
		BuildingBlock result = reduxBlocks(blocks);

		// Compose the query
		Set<Query> queries = new HashSet<Query>();
		for (TripleSet element : result) {
			if (isValid(element)) {
				Query query = QueryFactory.make();
				ElementGroup elg = new ElementGroup();
				for (Triple t : element) {
					for (Node n : new Node[] { t.getSubject(), t.getPredicate(), t.getObject() })
						if (n.isVariable())
							query.addResultVar(n);
					elg.addTriplePattern(t);
				}
				query.setQuerySelectType();
				query.setQueryPattern(elg);
				queries.add(query);
			}
		}

		logger.info(String.format("Found %d queries with %d calls to the end point", queries.size(), numberCalls));

		return queries;
	}

	/**
	 * @param first
	 * @param blocks
	 * @return
	 * @throws Exception
	 */
	private BuildingBlock getOther(BuildingBlock block, LinkedList<BuildingBlock> blocks) throws Exception {
		// Get a list of the variables we have to find
		Set<Node> variables = getVars(block);

		// Find the biggest other block with different var
		int index = blocks.size() - 1;
		int self = blocks.indexOf(block);
		BuildingBlock match = null;
		while (index != self && match == null) {
			Set<Node> vars2 = getVars(blocks.get(index));
			vars2.retainAll(variables);
			if (!vars2.isEmpty()) {
				match = blocks.get(index);
				blocks.remove(index);
			}
			index--;
		}

		// No overlap between blocks
		if (match == null) {
			// fall back on the biggest
			match = blocks.pollLast();
		}

		return match;
	}

	/**
	 * @param b
	 * @return
	 */
	private Set<Node> getVars(final BuildingBlock block) {
		Set<Node> variables = new HashSet<Node>();
		for (TripleSet set : block)
			for (Triple t : set)
				for (Node n : new Node[] { t.getSubject(), t.getPredicate(), t.getObject() })
					if (n.isVariable())
						variables.add(n);
		return variables;
	}

	/**
	 * @param set
	 * @return
	 * @throws Exception
	 */
	private boolean isValid(TripleSet set) throws Exception {
		Query query = QueryFactory.make();
		query.setQueryAskType();
		ElementGroup elg = new ElementGroup();
		for (Triple triple : set)
			elg.addTriplePattern(triple);
		query.setQueryPattern(elg);
		QueryEngineHTTPClient queryExec = new QueryEngineHTTPClient(endPoint.getURI(), query);
		if (endPoint.getDefaultGraph() != null)
			queryExec.addDefaultGraph(endPoint.getDefaultGraph());
		numberCalls++;
		return queryExec.execAsk();
	}

	/**
	 * @param blocks
	 * @return
	 * @throws Exception
	 */
	private BuildingBlock reduxBlocks(LinkedList<BuildingBlock> blocks) throws Exception {
		// No more reduction possible
		if (blocks.size() == 1)
			return blocks.get(0);

		// Sort the blocks
		Collections.sort(blocks);

		// Print a status line
		StringBuffer buffer =  new StringBuffer();
		buffer.append("Blocks = [");
		for (BuildingBlock block: blocks)
			buffer.append(block.size()).append(',');
		buffer.setCharAt(buffer.length()-1, ']');
		logger.info(buffer.toString());
		
		// Sanity check (Bug)
		// if (getVariables(blocks).isEmpty())
		// throw new Exception("No overlap between blocks");

		// Get two blocks from the list and prepare a third one to replace them
		BuildingBlock first = blocks.pollFirst();
		BuildingBlock second = getOther(first, blocks);
		BuildingBlock newBlock = new BuildingBlock();
		logger.info("Match [" + first.getTitle() + "] and [" + second.getTitle() + "]");
		
		// Test combination of sets from the building blocks
		for (TripleSet firstSet : first) {
			for (TripleSet secondSet : second) {
				TripleSet newSet = new TripleSet();
				newSet.addAll(firstSet);
				newSet.addAll(secondSet);
				if (isValid(newSet))
					newBlock.add(newSet);
			}
		}

		// No matching pair found
		if (newBlock.isEmpty())
			throw new Exception("Can't merge [" + first.getTitle() + "] with [" + second.getTitle() + "]");

		blocks.addFirst(newBlock);
		newBlock.setTitle(first.getTitle() + " + " + second.getTitle());
		return reduxBlocks(blocks);
	}
}
