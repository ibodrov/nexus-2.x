/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.hosted;

import org.sonatype.nexus.proxy.repository.AbstractRepositoryConfiguration;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
public class NpmHostedRepositoryConfiguration
    extends AbstractRepositoryConfiguration
{
  public NpmHostedRepositoryConfiguration(Xpp3Dom configuration) {
    super(configuration);
  }
}
