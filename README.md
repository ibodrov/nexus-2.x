<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2008-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# Takari Enhanced Nexus Repository Manager OSS Codebase 

## Requirements

* Apache Maven 3+
* Java 8+
* Network access to https://repository.sonatype.org/content/groups/sonatype-public-grid

## Patches

Applied patches are listed [here](PATCHES.md).

## Building

To build the project and generate the template assembly use the included Maven wrapper:

    ./mvnw clean install

## Running

To run Nexus, after building, unzip the assembly and start the server:

    unzip -d target assemblies/nexus-bundle-template/target/nexus-bundle-template-*.zip
    ./target/nexus-bundle-template-*/bin/nexus console

The `nexus-bundle-template` assembly is used as the basis for the official Nexus Repository Manager distributions.
