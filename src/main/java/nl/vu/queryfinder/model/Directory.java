/**
 * 
 */
package nl.vu.queryfinder.model;

import java.util.ArrayList;

/**
 * A directory is a collection of end points
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Directory extends ArrayList<EndPoint> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7897336454073821165L;

	/**
	 * @param fileName
	 * @return
	 */
	public static Directory create(String fileName) {
		Directory directory = new Directory();

		return directory;
	}
}
