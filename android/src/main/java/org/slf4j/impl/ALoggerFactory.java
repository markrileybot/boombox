package org.slf4j.impl;

import org.slf4j.ILoggerFactory;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mriley
 */
public class ALoggerFactory implements ILoggerFactory {
	private final Map<String, WeakReference<ALogger>> loggerMap;

	static final int TAG_MAX_LENGTH = 32;

	public ALoggerFactory() {
		loggerMap = new HashMap<>(1);
	}

	/* @see org.slf4j.ILoggerFactory#getLogger(java.lang.String) */
	public ALogger getLogger(final String name)
	{
		final String actualName = trimName(name); // fix for bug #173

		WeakReference<ALogger> sloggerRef = null;
		// protect against concurrent access of the loggerMap
		synchronized (this)
		{
			sloggerRef = loggerMap.get(actualName);
			if (sloggerRef == null || sloggerRef.get() == null) {
				sloggerRef = new WeakReference<>(new ALogger(actualName));
				loggerMap.put(actualName, sloggerRef);
			}
		}
		return sloggerRef.get();
	}

	/**
	 * Trim name in case it exceeds maximum length of {@value #TAG_MAX_LENGTH} characters.
	 */
	private static String trimName(String name)
	{
		if (name != null && name.length() > TAG_MAX_LENGTH)
		{
			while( name.length() > TAG_MAX_LENGTH ) {
				int dotIndex = name.indexOf('.');
				boolean trimmed = false;
				if( dotIndex > -1 ) {
					int nextDotIndex = name.indexOf('.', dotIndex+1);
					if( nextDotIndex > -1 ) {
						name = name.substring(0,dotIndex) + name.substring(nextDotIndex);
						trimmed = true;
					}
				}
				if( !trimmed ) {
					name = name.substring(0, TAG_MAX_LENGTH);
				}
			}
		}
		return name;
	}
}
