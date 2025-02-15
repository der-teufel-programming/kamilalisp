/*
 * 12/14/2008
 *
 * DefaultTokenMakerFactory.java - The default TokenMaker factory.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea;


import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker;

/**
 * The default implementation of <code>TokenMakerFactory</code>.  This factory
 * can create {@link TokenMaker}s for all languages known to
 * {@link RSyntaxTextArea}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class DefaultTokenMakerFactory extends AbstractTokenMakerFactory
								implements SyntaxConstants {


	@Override
	protected void initTokenMakerMap() {
		putMapping(SYNTAX_STYLE_NONE, PlainTextTokenMaker.class);
	}


}
