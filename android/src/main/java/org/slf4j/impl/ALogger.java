package org.slf4j.impl;

import android.util.Log;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * @author mriley
 */
public class ALogger extends MarkerIgnoringBase {

	private static final long serialVersionUID = -1227274521521287937L;
	private static int LEVEL = Log.INFO;

	private static boolean trace;
	private static boolean debug;
	private static boolean info;
	private static boolean warn;
	private static boolean error;
	private static boolean assrt;

	public static void setLevel(int level) {
		LEVEL = level;
		trace = debug = info = warn = error = assrt = false;
		switch (level) {
			case Log.VERBOSE: trace = true;
			case Log.DEBUG: debug = true;
			case Log.INFO: info = true;
			case Log.WARN: warn = true;
			case Log.ERROR: error = true;
			case Log.ASSERT: assrt = true;
		}
	}

	public static String getLevelName(int level) {
		switch (level) {
			case Log.VERBOSE: return "VERBOSE";
			case Log.DEBUG: return "DEBUG";
			case Log.INFO: return "INFO";
			case Log.WARN: return "WARN";
			case Log.ERROR: return "ERROR";
			case Log.ASSERT: return "ASSERT";
		}
		return "";
	}

	/**
	 * Package access allows only {@link ALoggerFactory} to instantiate
	 * SimpleLogger instances.
	 */
	ALogger(final String name)
	{
		this.name = name;
	}

	/* @see org.slf4j.Logger#isTraceEnabled() */
	public boolean isTraceEnabled()
	{
		return LEVEL <= Log.VERBOSE;
	}

	/* @see org.slf4j.Logger#trace(java.lang.String) */
	public void trace(final String msg)
	{
		if(trace) Log.v(name, msg);
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object) */
	public void trace(final String format, final Object param1)
	{
		if(trace) Log.v(name, format(format, param1, null));
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object, java.lang.Object) */
	public void trace(final String format, final Object param1, final Object param2)
	{
		if(trace) Log.v(name, format(format, param1, param2));
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Object[]) */
	public void trace(final String format, final Object ... argArray)
	{
		if(trace) Log.v(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#trace(java.lang.String, java.lang.Throwable) */
	public void trace(final String msg, final Throwable t)
	{
		if(trace) Log.v(name, msg, t);
	}

	/* @see org.slf4j.Logger#isDebugEnabled() */
	public boolean isDebugEnabled()
	{
		return LEVEL <= Log.DEBUG;
	}

	/* @see org.slf4j.Logger#debug(java.lang.String) */
	public void debug(final String msg)
	{
		if(debug) Log.d(name, msg);
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object) */
	public void debug(final String format, final Object arg1)
	{
		if(debug)Log.d(name, format(format, arg1, null));
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object, java.lang.Object) */
	public void debug(final String format, final Object param1, final Object param2)
	{
		if(debug)Log.d(name, format(format, param1, param2));
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Object[]) */
	public void debug(final String format, final Object ... argArray)
	{
		if(debug) Log.d(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#debug(java.lang.String, java.lang.Throwable) */
	public void debug(final String msg, final Throwable t)
	{
		if(debug)Log.d(name, msg, t);
	}

	/* @see org.slf4j.Logger#isInfoEnabled() */
	public boolean isInfoEnabled()
	{
		return LEVEL <= Log.INFO;
	}

	/* @see org.slf4j.Logger#info(java.lang.String) */
	public void info(final String msg)
	{
		if(info) Log.i(name, msg);
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Object) */
	public void info(final String format, final Object arg)
	{
		if(info) Log.i(name, format(format, arg, null));
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Object, java.lang.Object) */
	public void info(final String format, final Object arg1, final Object arg2)
	{
		if(info) Log.i(name, format(format, arg1, arg2));
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Object[]) */
	public void info(final String format, final Object ... argArray)
	{
		if(info) Log.i(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#info(java.lang.String, java.lang.Throwable) */
	public void info(final String msg, final Throwable t)
	{
		if(info) Log.i(name, msg, t);
	}

	/* @see org.slf4j.Logger#isWarnEnabled() */
	public boolean isWarnEnabled()
	{
		return LEVEL <= Log.WARN;
	}

	/* @see org.slf4j.Logger#warn(java.lang.String) */
	public void warn(final String msg)
	{
		if(warn) Log.w(name, msg);
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object) */
	public void warn(final String format, final Object arg)
	{
		if(warn) Log.w(name, format(format, arg, null));
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object, java.lang.Object) */
	public void warn(final String format, final Object arg1, final Object arg2)
	{
		if(warn) Log.w(name, format(format, arg1, arg2));
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Object[]) */
	public void warn(final String format, final Object ... argArray)
	{
		if(warn) Log.w(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#warn(java.lang.String, java.lang.Throwable) */
	public void warn(final String msg, final Throwable t)
	{
		if(warn) Log.w(name, msg, t);
	}

	/* @see org.slf4j.Logger#isErrorEnabled() */
	public boolean isErrorEnabled()
	{
		return LEVEL <= Log.ERROR;
	}

	/* @see org.slf4j.Logger#error(java.lang.String) */
	public void error(final String msg)
	{
		if(error) Log.e(name, msg);
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Object) */
	public void error(final String format, final Object arg)
	{
		if(error) Log.e(name, format(format, arg, null));
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Object, java.lang.Object) */
	public void error(final String format, final Object arg1, final Object arg2)
	{
		if(error) Log.e(name, format(format, arg1, arg2));
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Object[]) */
	public void error(final String format, final Object ... argArray)
	{
		if(error) Log.e(name, format(format, argArray));
	}

	/* @see org.slf4j.Logger#error(java.lang.String, java.lang.Throwable) */
	public void error(final String msg, final Throwable t)
	{
		if(error) Log.e(name, msg, t);
	}

	/**
	 * For formatted messages substitute arguments.
	 *
	 * @param format
	 * @param arg1
	 * @param arg2
	 */
	private static String format(final String format, final Object arg1, final Object arg2)
	{
		return MessageFormatter.format(format, arg1, arg2).getMessage();
	}

	/**
	 * For formatted messages substitute arguments.
	 *
	 * @param format
	 * @param args
	 */
	private static String format(final String format, final Object ... args)
	{
		return MessageFormatter.arrayFormat(format, args).getMessage();
	}
}
