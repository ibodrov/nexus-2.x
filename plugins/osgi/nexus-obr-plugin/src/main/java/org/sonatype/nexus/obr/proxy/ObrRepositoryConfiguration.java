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
package org.sonatype.nexus.obr.proxy;

import org.sonatype.nexus.proxy.repository.AbstractProxyRepositoryConfiguration;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ObrRepositoryConfiguration
    extends AbstractProxyRepositoryConfiguration
{
  private final static String OBR_PATH_KEY = "obrPath";

  public ObrRepositoryConfiguration(final Xpp3Dom configuration) {
    super(configuration);
  }

  public boolean isObrPathSet() {
    return getRootNode().getChild(OBR_PATH_KEY) != null;
  }

  public String getObrPath() {
    return getNodeValue(getRootNode(), OBR_PATH_KEY, "/repository.xml");
  }

  public void setObrPath(final String val) {
    setNodeValue(getRootNode(), OBR_PATH_KEY, val);
  }
}
