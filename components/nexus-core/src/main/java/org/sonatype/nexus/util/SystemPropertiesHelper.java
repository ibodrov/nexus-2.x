/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.util;

public class SystemPropertiesHelper
{
  public final static int getInteger(final String key, final int defaultValue) {
    final String value = System.getProperty(key);

    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }

    try {
      return Integer.valueOf(value);
    }
    catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public final static long getLong(final String key, final long defaultValue) {
    final String value = System.getProperty(key);

    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }

    try {
      return Long.valueOf(value);
    }
    catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public final static boolean getBoolean(final String key, final boolean defaultValue) {
    final String value = System.getProperty(key);

    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }

    return Boolean.valueOf(value);
  }

  public final static String getString(final String key, final String defaultValue) {
    return System.getProperty(key, defaultValue);
  }
}
