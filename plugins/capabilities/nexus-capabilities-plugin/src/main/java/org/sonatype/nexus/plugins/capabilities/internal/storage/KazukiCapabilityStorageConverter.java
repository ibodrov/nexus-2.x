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
package org.sonatype.nexus.plugins.capabilities.internal.storage;

import javax.inject.Inject;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Converts, if existing, {@link KazukiCapabilityStorage} back to default {@link CapabilityStorage}.
 *
 * @since 2.8
 */
public class KazukiCapabilityStorageConverter
    extends ComponentSupport
    implements CapabilityStorageConverter
{
  private final KazukiCapabilityStorage kazukiCapabilityStorage;

  private final CapabilityStorage capabilityStorage;

  @Inject
  public KazukiCapabilityStorageConverter(final KazukiCapabilityStorage kazukiCapabilityStorage,
                                          final CapabilityStorage capabilityStorage)
  {
    this.kazukiCapabilityStorage = checkNotNull(kazukiCapabilityStorage);
    this.capabilityStorage = checkNotNull(capabilityStorage);
  }

  @Override
  public void maybeConvert() throws Exception {
    if (kazukiCapabilityStorage.exists()) {
      log.info("Converting KZ-based storage to default implementation");
      kazukiCapabilityStorage.start();

      int count = 0;
      for (CapabilityStorageItem item : kazukiCapabilityStorage.getAll().values()) {
        capabilityStorage.add(item);
        count++;
      }

      log.info("Converted {} capability entities", count);

      kazukiCapabilityStorage.stop();
      kazukiCapabilityStorage.drop();

      log.info("Dropped KZ-based storage");
    }
  }
}
