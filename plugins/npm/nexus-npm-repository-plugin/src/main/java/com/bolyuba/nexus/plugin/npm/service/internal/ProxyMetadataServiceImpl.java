/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
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
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.ProxyMode;

import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;
import com.bolyuba.nexus.plugin.npm.service.ProxyMetadataService;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballRequest;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import static com.bolyuba.nexus.plugin.npm.service.PackageRoot.PROP_CACHED;
import static com.bolyuba.nexus.plugin.npm.service.PackageRoot.PROP_ETAG;
import static com.bolyuba.nexus.plugin.npm.service.PackageRoot.PROP_EXPIRED;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link ProxyMetadataService} implementation.
 */
public class ProxyMetadataServiceImpl
    extends GeneratorWithStoreSupport<NpmProxyRepository>
    implements ProxyMetadataService
{
  private static final String REGISTRY_ROOT_PACKAGE_NAME = "-";

  private final Object registryRootUpdateLock;

  private final ProxyMetadataTransport proxyMetadataTransport;

  public ProxyMetadataServiceImpl(final NpmProxyRepository npmProxyRepository,
                                  final MetadataStore metadataStore,
                                  final ProxyMetadataTransport proxyMetadataTransport,
                                  final MetadataParser metadataParser)
  {
    super(npmProxyRepository, metadataParser, metadataStore);
    this.registryRootUpdateLock = new Object();
    this.proxyMetadataTransport = checkNotNull(proxyMetadataTransport);
  }

  @Override
  public boolean deleteAllMetadata() {
    log.info("Deleting all proxied npm metadata from {}", getNpmRepository().getId());
    return metadataStore.deletePackages(getNpmRepository());
  }

  @Override
  public boolean deletePackage(final String packageName) {
    checkNotNull(packageName, "null package name");
    log.info("Deleting proxied npm package {} metadata from {}", packageName, getNpmRepository().getId());
    return metadataStore.deletePackageByName(getNpmRepository(), packageName);
  }

  @Override
  public PackageRoot consumeRawPackageRoot(final PackageRoot packageRoot) {
    checkNotNull(packageRoot);
    return metadataStore.updatePackage(getNpmRepository(), packageRoot);
  }

  private final static String SLASH_DASH_SLASH = "/-/";

  @Nullable
  @Override
  public TarballRequest createTarballRequest(final ResourceStoreRequest request) throws IOException {
    checkNotNull(request);
    TarballRequest tarballRequest = null;
    final String requestPath = request.getRequestPath();
    final int indexOfSlashDashSlash = requestPath.indexOf(SLASH_DASH_SLASH);
    if (indexOfSlashDashSlash > -1
        && requestPath.length() > indexOfSlashDashSlash + SLASH_DASH_SLASH.length()
        && requestPath.endsWith(".tgz")) {
      final String packageName = requestPath.substring(0, indexOfSlashDashSlash);
      final String tarballFilename = requestPath
          .substring(indexOfSlashDashSlash + SLASH_DASH_SLASH.length(), requestPath.length());

      try {
        PackageRequest.PackageCoordinates coordinates = PackageRequest.PackageCoordinates
            .coordinatesFromUrl(packageName);

        tarballRequest = requestTarball(request, mayUpdatePackageRoot(coordinates, true), tarballFilename);
        if (tarballRequest == null && !request.isRequestLocalOnly()) {
          // might be new package so check the upstream metadata
          tarballRequest = requestTarball(request, mayUpdatePackageRoot(coordinates, false), tarballFilename);
        }
      }
      catch (IllegalArgumentException e) {
        // handle it as non tarball request
      }
    }

    if (tarballRequest == null) {
      log.debug("Not a tarball request: {}", request.getRequestPath());
    }

    return tarballRequest;
  }

  @Override
  protected PackageRootIterator doGenerateRegistryRoot(final PackageRequest request) throws IOException {
    // TODO: ContentServlet sets isLocal to paths ending with "/", so registry root will be local!
    if (request.getCoordinates().getPath().endsWith("/") || !request.getStoreRequest().isRequestLocalOnly()) {
      // doing what NPM CLI does it's in own cache, using an invalid document (name "-" is invalid)
      PackageRoot registryRoot = metadataStore.getPackageByName(getNpmRepository(), REGISTRY_ROOT_PACKAGE_NAME);
      final long now = System.currentTimeMillis();
      if (registryRoot == null || isExpired(registryRoot, now)) {
        synchronized (registryRootUpdateLock) {
          // double checked locking, let's see again did some other thread update while we were blocked
          registryRoot = metadataStore.getPackageByName(getNpmRepository(), REGISTRY_ROOT_PACKAGE_NAME);
          if (registryRoot == null || isExpired(registryRoot, now)) {
            // fetch all from remote, this takes some time (currently 40MB JSON)
            if (registryRoot == null) {
              log.info("Registry root {} initial fetch", getNpmRepository().getId());
            }
            else {
              log.info("Registry root {} expired", getNpmRepository().getId());
            }
            if (isRemoteAccessAllowed()) {
              try (final PackageRootIterator packageRootIterator = proxyMetadataTransport
                  .fetchRegistryRoot(getNpmRepository())) {
                int count = metadataStore.updatePackages(getNpmRepository(), packageRootIterator);
                log.info("Registry root {} update successful ({} packages)", getNpmRepository().getId(), count);
              }
              catch (Exception e) {
                log.warn("Registry root {} update failed: ", getNpmRepository().getId(), e.toString());
                throw Throwables.propagate(e);
              }
              if (registryRoot == null) {
                // create a fluke package root
                final Map<String, Object> versions = Maps.newHashMap();
                versions.put("0.0.0", "latest");
                final Map<String, Object> distTags = Maps.newHashMap();
                distTags.put("latest", "0.0.0");
                final Map<String, Object> raw = Maps.newHashMap();
                raw.put("name", REGISTRY_ROOT_PACKAGE_NAME);
                raw.put("description", "NX registry root package");
                raw.put("versions", versions);
                raw.put("dist-tags", distTags);
                registryRoot = new PackageRoot(getNpmRepository().getId(), raw);
              }
              registryRoot.getProperties().put(PROP_EXPIRED, Boolean.FALSE.toString());
              registryRoot.getProperties().put(PROP_CACHED, Long.toString(now));
              metadataStore.updatePackage(getNpmRepository(), registryRoot);
            }
            else {
              log.info("Registry root {} update not allowed", getNpmRepository().getId());
            }
          }
        }
      }
    }
    return super.doGenerateRegistryRoot(request);
  }

  @Nullable
  @Override
  protected PackageRoot doGeneratePackageRoot(final PackageRequest request) throws IOException {
    return mayUpdatePackageRoot(request.getCoordinates(), request.getStoreRequest().isRequestLocalOnly());
  }

  // ==

  /**
   * Attempts to find the given tarball in the cached package metadata, returns null if not found.
   */
  private TarballRequest requestTarball(final ResourceStoreRequest request, final PackageRoot packageRoot,
                                        final String tarballFilename)
  {
    final String path = request.getRequestPath();
    if (packageRoot != null) {
      log.debug("Looking up package {} version for tarball request: {}", packageRoot.getName(), path);
      for (PackageVersion version : packageRoot.getVersions().values()) {
        // TODO: simpler regex with filename matching used for simplicity's sake. Version extracting might be less
        // robust due to complex regex
        if (version.getDistTarball().endsWith(tarballFilename)) {
          log.debug("Package {} version {} matched for tarball request: {}", packageRoot.getName(),
              version.getVersion(), path);
          return new TarballRequest(request, packageRoot, version);
        }
      }
      log.debug("Package not found in metadata: {}", path);
    }
    else {
      log.debug("Package metadata not available: {}", path);
    }
    return null;
  }

  /**
   * May fetch package root from remote if not found locally, or is found but is expired. The package root returned
   * document is NOT filtered, so this method should not be used to source documents sent downstream.
   */
  private PackageRoot mayUpdatePackageRoot(final PackageRequest.PackageCoordinates packageCoordinates,
                                           final boolean localOnly) throws IOException
  {
    final long now = System.currentTimeMillis();
    PackageRoot packageRoot = metadataStore.getPackageByName(getNpmRepository(), packageCoordinates.getPackageName());
    if (isRemoteAccessAllowed() && !localOnly && (packageRoot == null || isExpired(packageRoot, now))) {
      PackageRoot remotePackageRoot = proxyMetadataTransport.fetchPackageRoot(getNpmRepository(), packageCoordinates, packageRoot);
      if (remotePackageRoot == null) {
        return packageRoot;
      }
      // On remote fetch of metadata, evict /packageName and children from NFC
      getNpmRepository().getNotFoundCache().removeWithChildren("/" + packageCoordinates.getPackageName());
      remotePackageRoot.getProperties().put(PROP_EXPIRED, Boolean.FALSE.toString());
      remotePackageRoot.getProperties().put(PROP_CACHED, Long.toString(now));
      return metadataStore.replacePackage(getNpmRepository(), remotePackageRoot);
    }
    else {
      return packageRoot;
    }
  }

  /**
   * Returns {@code true} if passed in package root should be considered as expired in cache, so remote check for fresh
   * version is required.
   */
  private boolean isExpired(final PackageRoot packageRoot, final long now) {
    if (!REGISTRY_ROOT_PACKAGE_NAME.equals(packageRoot.getName()) && packageRoot.isIncomplete()) {
      // registry root is made incomplete for simplicity's sake
      log.trace("EXPIRED: package {} is incomplete", packageRoot.getName());
      // force full download of package metadata to remove incomplete elements
      packageRoot.getProperties().remove(PROP_ETAG);
      return true;
    }
    if (!getNpmRepository().isItemAgingActive()) {
      log.trace("EXPIRED: package {} owning repository item aging is inactive", packageRoot.getName());
      return true;
    }
    if (Boolean.TRUE.toString().equals(packageRoot.getProperties().get(PROP_EXPIRED))) {
      log.trace("EXPIRED: package {} flagged as expired", packageRoot.getName());
      return true;
    }
    if (getNpmRepository().getItemMaxAge() < 0) {
      log.trace("NOT-EXPIRED: package {} owning repository {} has negative item max age", packageRoot.getName(),
          getNpmRepository().getId());
      return false;
    }
    final long remoteCached = packageRoot.getProperties().containsKey(PROP_CACHED) ? Long
        .valueOf(packageRoot.getProperties().get(PROP_CACHED)) : now;
    final boolean result = ((now - remoteCached) > (getNpmRepository().getItemMaxAge() * 60L * 1000L));
    if (result) {
      log.trace("EXPIRED: package {} is too old", packageRoot.getName());
    }
    else {
      log.trace("NOT-EXPIRED: package {} is fresh", packageRoot.getName());
    }
    return result;
  }

  /**
   * Returns {@code true} if remote/proxy requests are allowed on behalf of this instance's repository.
   */
  private boolean isRemoteAccessAllowed() {
    final ProxyMode proxyMode = getNpmRepository().getProxyMode();
    if (proxyMode != null) {
      return proxyMode.shouldProxy();
    }
    return false;
  }
}
