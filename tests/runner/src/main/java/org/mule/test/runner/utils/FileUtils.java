/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

/**
 * Utility for {@link File} classes.
 */
public final class FileUtils {

  /**
   * Converts the {@link List} of {@link File}s to {@link URL}s
   *
   * @param files {@link File} to get {@link URL}s
   * @return {@link List} of {@link URL}s for the files
   */
  public static List<URL> toUrl(Collection<File> files) {
    return files.stream().map(FileUtils::toUrl).collect(toList());
  }

  /**
   * Converts the {@link File} to {@link URL}
   *
   * @param file {@link File} to get its {@link URL}
   * @return {@link URL} for the file
   */
  public static URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Couldn't get URL", e);
    }
  }

}
