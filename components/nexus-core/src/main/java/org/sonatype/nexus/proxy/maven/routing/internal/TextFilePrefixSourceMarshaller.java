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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.routing.Config;

import com.google.common.base.CharMatcher;
import com.google.common.io.Closer;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonatype.nexus.util.PathUtils.elementsOf;
import static org.sonatype.nexus.util.PathUtils.pathFrom;

/**
 * Marshals entries into raw streams and other way around. This is a simple text based file with prefixes with dead
 * simple syntax: Lines starting with '#' are comments, and any other non empty line is actually a prefix.
 *
 * @author cstamas
 * @since 2.4
 */
public class TextFilePrefixSourceMarshaller
{
  /**
   * The "magic" that has to be the first bytes of the prefixes file Nexus post version 2.6.0.
   *
   * @since 2.7.0
   */
  public static final String MAGIC = "## repository-prefixes/2.0";

  /**
   * Bytes that are expected to be and used as "legacy magic", this is basically the first line of the prefixes file
   * generated by Nexus since Automatic Routing feature were introduced.
   *
   * @since 2.7.0
   */
  protected static final String LEGACY_MAGIC = "# Prefix file generated by Sonatype Nexus";

  /**
   * The headers we write out.
   */
  protected static final String[] HEADERS =
      {"#", LEGACY_MAGIC, "# Do not edit, changes will be overwritten!"};

  protected static final Charset CHARSET = Charset.forName("UTF-8");

  /**
   * Directive that indicates that automatic routing is not supported for the repository. Must be the first line of
   * the prefix file.
   */
  protected static final String UNSUPPORTED = "@ unsupported";

  private final int prefixFileMaxLineLength;

  private final int prefixFileMaxEntryCount;

  public interface Result
  {
    boolean supported();

    List<String> entries();
  }

  private static final Result RESULT_UNSUPPORTED = new Result()
  {
    @Override
    public boolean supported() {
      return false;
    }

    @Override
    public List<String> entries() {
      return Collections.emptyList();
    }
  };

  /**
   * Constructor.
   *
   * @param config the autorouting config.
   */
  public TextFilePrefixSourceMarshaller(final Config config) {
    checkArgument(config.getPrefixFileMaxLineLength() > 0);
    checkArgument(config.getPrefixFileMaxEntriesCount() > 0);
    this.prefixFileMaxLineLength = config.getPrefixFileMaxLineLength();
    this.prefixFileMaxEntryCount = config.getPrefixFileMaxEntriesCount();
  }

  /**
   * Marshalls the prefix source into the output stream.
   */
  public void write(final List<String> entries, final OutputStream outputStream)
      throws IOException
  {
    final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, CHARSET));
    printWriter.println(MAGIC);
    for (String header : HEADERS) {
      printWriter.println(header);
    }
    for (String entry : entries) {
      printWriter.println(entry);
    }
    printWriter.flush();
  }

  /**
   * Unmarshalls the prefix source from input stream.
   *
   * @return prefix source
   */
  public final Result read(final StorageFileItem file)
      throws InvalidInputException, IOException
  {
    Closer closer = Closer.create();
    try {
      final BufferedReader reader =
          closer.register(new BufferedReader(new InputStreamReader(file.getInputStream(), CHARSET)));
      final ArrayList<String> entries = new ArrayList<String>();
      String line = reader.readLine();
      if (!MAGIC.equals(line) && !LEGACY_MAGIC.equals(line) && !UNSUPPORTED.equals(line)) {
        throw new InvalidInputException("Prefix file does not start with expected \"" + MAGIC
            + "\" header, refusing to load the file.");
      }
      while (line != null) {
        // check for unsupported. Before 2.7.0 it needed to be on 1st line, now it is obeyed if found on any
        // line, as Nexus 2.7.0+ will have it AFTER headers, whereas older Nexuses had it BEFORE headers (on 1st
        // line)
        if (UNSUPPORTED.equals(line)) {
          return RESULT_UNSUPPORTED;
        }
        // trim
        line = line.trim();
        if (!line.startsWith("#") && line.length() > 0) {
          // line length
          if (line.length() > prefixFileMaxLineLength) {
            throw new InvalidInputException("Prefix file contains line longer than allowed ("
                + prefixFileMaxLineLength + " characters), refusing to load the file.");
          }
          // line should be ASCII
          if (!CharMatcher.ASCII.matchesAllOf(line)) {
            throw new InvalidInputException(
                "Prefix file contains non-ASCII characters, refusing to load the file.");
          }
          // line should be actually a bit less than ASCII, filtering most certain characters
          if (line.contains(":") || line.contains("<") || line.contains(">")) {
            throw new InvalidInputException(
                "Prefix file contains forbidden characters (colon, less or greater signs), refusing to load the file.");
          }

          // Igor's find command makes path like "./org/apache/"
          while (line.startsWith(".")) {
            line = line.substring(1);
          }
          // win file separators? Highly unlikely but still...
          line = line.replace('\\', '/');
          // normalization
          entries.add(pathFrom(elementsOf(line)));
        }
        line = reader.readLine();

        // dump big files
        if (entries.size() > prefixFileMaxEntryCount) {
          throw new InvalidInputException("Prefix file entry count exceeds maximum allowed count ("
              + prefixFileMaxEntryCount + "), refusing to load it.");
        }
      }

      return new Result()
      {
        @Override
        public boolean supported() {
          return true;
        }

        @Override
        public List<String> entries() {
          return entries;
        }
      };
    }
    finally {
      closer.close();
    }
  }

  public void writeUnsupported(ByteArrayOutputStream outputStream) {
    final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, CHARSET));
    printWriter.println(MAGIC);
    for (String header : HEADERS) {
      printWriter.println(header);
    }
    printWriter.println(UNSUPPORTED);
    printWriter.flush();
  }
}
