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
package org.sonatype.nexus.proxy;

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.test.http.RemoteRepositories;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * The Class JettyTestsuiteEnvironment.
 *
 * @author cstamas
 */
public abstract class AbstractJettyEnvironmentBuilder
    implements EnvironmentBuilder
{
  private RemoteRepositories remoteRepositories;

  public AbstractJettyEnvironmentBuilder(RemoteRepositories remoteRepositories) {
    this.remoteRepositories = remoteRepositories;
  }

  public void startService()
      throws Exception
  {
    remoteRepositories.start();
  }

  public void stopService()
      throws Exception
  {
    remoteRepositories.stop();
  }

  protected RemoteRepositories getRemoteRepositories() {
    return remoteRepositories;
  }

  public final void buildEnvironment(AbstractProxyTestEnvironment env)
      throws Exception {
    if (!remoteRepositories.isStarted()) {
      remoteRepositories.start();
    }
    doBuildEnvironment(env);
  }

  protected abstract void doBuildEnvironment(AbstractProxyTestEnvironment env)
      throws Exception;
}
