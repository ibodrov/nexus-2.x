# Collection of high-level changes against Nexus OSS

This branch is based on Nexus OSS published by Sonatype:
https://github.com/sonatype/nexus-public/tree/nexus-2.x

The branch contains several (non runtime source related) changes:
* POMs changed to deploy to `io.takari.nexus` groupId, to be able to publish to Central.
* Build overall updated and improved to perform well with latest Maven 3.3.x and Java 8.
  * as a consequence of that above, instead of [sonatype/nexus-plugin-bundle](https://github.com/sonatype/nexus-plugin-bundle/)
    build uses [takari/nexus-plugin-bundle-maven-plugin](https://github.com/takari/nexus-plugin-bundle-maven-plugin), a simplified
    and more compatible plugin to produce Nexus Plugin bundles.
  * some ITs were using ciphers that Java8 disabled, hence build was failing on Java 8. Those ITs are `@Ignore`d.
  * P2 bridge plugin updated to receive latest updates from [sonatype/p2-bridge](https://github.com/sonatype/p2-bridge),
    as original code was also dependant on `org.sonatype.aether`.
* npm related ITs (non-runtime code) are changed in a way to test scoped packages too, added some resources
* Some "unstable" ITs are set `@Ignore`d (they were always unstable)

Overall, no runtime source changes happened to Nexus in this repository.