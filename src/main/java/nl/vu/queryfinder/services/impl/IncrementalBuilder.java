/**
 * 
 */
package nl.vu.queryfinder.services.impl;

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
	static final Logger logger = LoggerFactory.getLogger(IncrementalBuilder.class);
	final EndPoint endPoint;
	private int numberCalls;

	public class Block extends HashSet<TripleSet> {
		private static final long serialVersionUID = -4820888956735317884L;
	}

	/**
	 * @param endPoint
	 */
	public IncrementalBuilder(EndPoint endPoint) {
		this.endPoint = endPoint;
	}

	/**
	 * @param blocks
	 * @return
	 */
	protected Block reduxBlocks(LinkedList<Block> blocks) {
		// No more reduction possible
		if (blocks.size() == 1) {
			logger.info("Done");
			return blocks.get(0);
		}

		Block first = blocks.pollFirst();
		Block second = blocks.pollLast();
		Block newBlock = new Block();

		for (TripleSet firstSet : first) {
			for (TripleSet secondSet : second) {
				boolean valid = false;
				TripleSet newSet = new TripleSet();
				newSet.addAll(firstSet);
				newSet.addAll(secondSet);
				try {
					Query query = QueryFactory.make();
					query.setQueryAskType();
					ElementGroup elg = new ElementGroup();
					for (Triple triple : newSet)
						elg.addTriplePattern(triple);
					query.setQueryPattern(elg);
					QueryEngineHTTPClient queryExec = new QueryEngineHTTPClient(endPoint.getURI(), query);
					queryExec.addDefaultGraph(endPoint.getDefaultGraph());
					numberCalls++;
					valid = queryExec.execAsk();
				} catch (Exception e) {
				}
				if (valid)
					newBlock.add(newSet);
			}
		}
		
		blocks.addFirst(newBlock);
		FIXME sort the list
		TODO Check that combines triples have an overlap in variables!
		return reduxBlocks(blocks);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.queryfinder.services.QueryGenerator#getQuery(nl.vu.queryfinder.
	 * model.MappedQuery)
	 */
	public Set<Query> getQuery(MappedQuery mappedQuery) {
		// Solve
		LinkedList<Block> blocks = new LinkedList<Block>();
		for (TripleSet triples : mappedQuery.getGroups()) {
			Block block = new Block();
			for (Triple t : triples) {
				TripleSet set = new TripleSet();
				set.add(t);
				block.add(set);
			}
			blocks.add(block);
		}

		// Get the result
		numberCalls = 0;
		Block result = reduxBlocks(blocks);
		logger.info(numberCalls + " requests sent to the end point");

		// Compose the query
		Set<Query> queries = new HashSet<Query>();
		for (TripleSet element : result) {
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

		return queries;
	}
}

/*
 * protected Block combineBlocks(List<Block> blocks, int queries) { //
 * logger.info(blocks.toString()); logger.info("-"); for (Block block : blocks)
 * { logger.info(block.size() + ""); }
 * 
 * if (blocks.size() == 0) { logger.warn("No solution found !"); return null; }
 * 
 * if (blocks.size() == 1) { logger.info("Solution found with " + queries +
 * " queries"); for (TripleSet element : blocks.get(0))
 * logger.info(element.toString()); return blocks.get(0); }
 * 
 * List<Block> newBlocks = new ArrayList<Block>();
 * 
 * for (int index = 0; index < blocks.size() - 1; index++) { // Get the two
 * blocks Block firstBlock = blocks.get(index); Block secondBlock =
 * blocks.get(index + 1); Block newBlock = new Block();
 * 
 * // Try to combine their content for (TripleSet firstGroup : firstBlock) { for
 * (TripleSet secondGroup : secondBlock) { boolean valid = false; // Prepare the
 * query TripleSet newSet = new TripleSet(); newSet.addAll(firstGroup);
 * newSet.addAll(secondGroup); try { // if (newSet.size() == 3) //
 * logger.info("Try " + newSet); Query query = QueryFactory.make();
 * query.setQueryAskType(); ElementGroup elg = new ElementGroup(); for (Triple
 * triple : newSet) elg.addTriplePattern(triple); query.setQueryPattern(elg);
 * QueryEngineHTTPClient queryExec = new
 * QueryEngineHTTPClient(endPoint.getURI(), query);
 * queryExec.addDefaultGraph(endPoint.getDefaultGraph()); valid =
 * queryExec.execAsk(); } catch (Exception e) { // logger.warn("Error"); } if
 * (valid) newBlock.add(newSet); queries++; } }
 * 
 * if (newBlock.size() > 0) newBlocks.add(newBlock); }
 * 
 * return combineBlocks(newBlocks, queries); }
 */

