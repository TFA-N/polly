package de.skuzzle.test.polly.parsing.parser;

import org.junit.Ignore;

import de.skuzzle.polly.core.parser.InputParser;


/**
 * Base class for all {@link InputParser} tests.
 * 
 * @author Simon Taddiken
 */
@Ignore
public abstract class AbstractParserTest {

    public InputParser obtain(String input) {
        return new InputParser(input, new SimpleProblemReporter());
    }
}