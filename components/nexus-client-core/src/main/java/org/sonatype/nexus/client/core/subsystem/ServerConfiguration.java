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
package org.sonatype.nexus.client.core.subsystem;

import org.sonatype.nexus.client.core.subsystem.config.RemoteConnection;
import org.sonatype.nexus.client.core.subsystem.config.RemoteProxy;
import org.sonatype.nexus.client.core.subsystem.config.RestApi;
import org.sonatype.nexus.client.core.subsystem.config.Security;

/**
 * Server configuration subsystem.
 *
 * @since 2.2
 */
public interface ServerConfiguration
{
  /**
   * @return Remote connection configuration segment.
   * @since 2.14
   */
  RemoteConnection remoteConnection();

  /**
   * @return Remote Proxy configuration segment.
   * @since 2.6
   */
  RemoteProxy remoteProxySettings();

  /**
   * @return Rest API configuration segment.
   * @since 2.6.1
   */
  RestApi restApi();

  /**
   * Returns {@link Security} configuration segment.
   *
   * @since 2.7
   */
  Security security();

}
