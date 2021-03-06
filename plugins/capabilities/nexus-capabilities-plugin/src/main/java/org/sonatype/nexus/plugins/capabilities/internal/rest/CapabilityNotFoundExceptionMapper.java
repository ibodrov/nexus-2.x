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
package org.sonatype.nexus.plugins.capabilities.internal.rest;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.plugins.capabilities.CapabilityNotFoundException;
import org.sonatype.sisu.siesta.common.error.ErrorXO;
import org.sonatype.sisu.siesta.common.error.ObjectNotFoundException;
import org.sonatype.sisu.siesta.server.ErrorExceptionMapperSupport;

/**
 * Maps {@link CapabilityNotFoundException} to 404 with a {@link ErrorXO} body.
 *
 * @since 2.7
 */
@Named
@Singleton
public class CapabilityNotFoundExceptionMapper
    extends ErrorExceptionMapperSupport<CapabilityNotFoundException>
{

  @Override
  protected int getStatusCode(final CapabilityNotFoundException exception) {
    return Response.Status.NOT_FOUND.getStatusCode();
  }

}
