package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * The binding of {@link org.slf4j.LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Thorsten M&ouml;ler
 * @version $Rev:$; $Author:$; $Date:$
 */
public class StaticLoggerBinder implements LoggerFactoryBinder
{

	/**
	 * The unique instance of this class.
	 */
	private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

	/**
	 * Return the singleton of this class.
	 *
	 * @return the StaticLoggerBinder singleton
	 */
	public static final StaticLoggerBinder getSingleton()
	{
		return SINGLETON;
	}

	/**
	 * Declare the version of the SLF4J API this implementation is compiled
	 * against. The value of this field is usually modified with each release.
	 */
	// to avoid constant folding by the compiler, this field must *not* be final
	public static String REQUESTED_API_VERSION = "1.6"; // !final

	private static final String loggerFactoryClassStr = ALoggerFactory.class.getName();

	/**
	 * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
	 * method should always be the same object
	 */
	private final ILoggerFactory loggerFactory;

	private StaticLoggerBinder()
	{
		loggerFactory = new ALoggerFactory();
	}

	public ILoggerFactory getLoggerFactory()
	{
		return loggerFactory;
	}

	public String getLoggerFactoryClassStr()
	{
		return loggerFactoryClassStr;
	}
}
