/**
 * 
 */
package nl.vu.queryfinder.services.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import nl.vu.queryfinder.model.MappedQuery;
import nl.vu.queryfinder.services.QueryGenerator;
import nl.vu.queryfinder.util.TripleSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class IncrementalBuilder implements QueryGenerator {
	static final Logger logger = LoggerFactory.getLogger(IncrementalBuilder.class);
	private final String endPoint;;

	public class Block extends HashSet<TripleSet> {
		private static final long serialVersionUID = -4820888956735317884L;
	}

	/**
	 * @param endPoint
	 */
	public IncrementalBuilder(String endPoint) {
		this.endPoint = endPoint;
	}

	/**
	 * @param blocks
	 */
	private Block combineBlocks(List<Block> blocks, int queries) {
		// logger.info(blocks.toString());

		if (blocks.size() == 0) {
			logger.warn("No solution found !");
			return null;
		}

		if (blocks.size() == 1) {
			logger.info("Solution found with " + queries + " queries");
			for (TripleSet element : blocks.get(0))
				logger.info(element.toString());
			return blocks.get(0);
		}

		List<Block> newBlocks = new ArrayList<Block>();

		for (int index = 0; index < blocks.size() - 1; index++) {
			// Get the two blocks
			Block firstBlock = blocks.get(index);
			Block secondBlock = blocks.get(index + 1);
			Block newBlock = new Block();

			// Try to combine their content
			for (TripleSet firstGroup : firstBlock) {
				for (TripleSet secondGroup : secondBlock) {
					boolean valid = false;
					// Prepare the query
					TripleSet newSet = new TripleSet();
					newSet.addAll(firstGroup);
					newSet.addAll(secondGroup);
					try {
						// logger.info("Try " + newSet);
						Query query = QueryFactory.make();
						query.setQueryAskType();
						ElementGroup elg = new ElementGroup();
						for (Triple triple : newSet)
							elg.addTriplePattern(triple);
						query.setQueryPattern(elg);
						QueryExecution queryExec = QueryExecutionFactory.sparqlService(endPoint, query);
						valid = queryExec.execAsk();
					} catch (Exception e) {
					}
					if (valid)
						newBlock.add(newSet);
					queries++;
				}
			}

			if (newBlock.size() > 0)
				newBlocks.add(newBlock);
		}

		return combineBlocks(newBlocks, queries);
	}

	public Query getQuery(MappedQuery mappedQuery) {
		List<Block> blocks = new ArrayList<Block>();
		for (TripleSet triples : mappedQuery.getGroups()) {
			Block block = new Block();
			block.add(triples);
			blocks.add(block);
		}
		Block result = combineBlocks(blocks, 0);
		System.out.println(result);
		return null;
	}
}
