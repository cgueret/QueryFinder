package nl.vu.queryfinder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.turtle.TurtleParserFactory;

public class ServiceExec implements RDFHandler {
	private String component;
	private Set<Statement> inputStatements = new HashSet<Statement>();
	private Set<Statement> outputStatements = new HashSet<Statement>();

	/**
	 * @param inputFile
	 * @throws RDFParseException
	 * @throws RDFHandlerException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void load(String inputFile) throws RDFParseException, RDFHandlerException, FileNotFoundException,
			IOException {
		inputStatements.clear();
		TurtleParserFactory f = new TurtleParserFactory();
		RDFParser parser = f.getParser();
		parser.setRDFHandler(this);
		parser.parse(new FileInputStream(inputFile), "http://example.org/");
	}

	/**
	 * @param exitCode
	 */
	public static void printHelpAndExit(Options options, int exitCode) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(QueryFinder.class.getName(), options);
		System.exit(exitCode);
	}

	/**
	 * @param args
	 * @throws ParseException
	 */
	public static void main(String[] args) throws ParseException {
		// Compose the options
		Options options = new Options();
		options.addOption("c", "component", true, "name of the processing component");
		options.addOption("i", "input", true, "input file with the patterns to process");
		options.addOption("o", "output", true, "output file for the new patterns");
		options.addOption("h", "help", false, "print help message");

		// Parse the command line
		CommandLineParser parser = new PosixParser();
		CommandLine line = parser.parse(options, args);

		// Handle request for help
		if (line.hasOption("h"))
			printHelpAndExit(options, 0);

		// Handle miss-use
		if (!line.hasOption("c") || !line.hasOption("o") || !line.hasOption("i"))
			printHelpAndExit(options, -1);

	}

	@Override
	public void endRDF() throws RDFHandlerException {
	}

	@Override
	public void handleComment(String arg0) throws RDFHandlerException {
	}

	@Override
	public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
	}

	@Override
	public void handleStatement(Statement statement) throws RDFHandlerException {
		inputStatements.add(statement);
	}

	@Override
	public void startRDF() throws RDFHandlerException {
	}
}
