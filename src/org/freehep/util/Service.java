// Copyright 2003, FreeHEP.
package org.freehep.util;

import geogebra.main.Application;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * This class does the same as sun.misc.Service, which may become public in some
 * java or javax package at some point. See Sun BUG# 4640520.
 * 
 * @author Mark Donszelmann
 * @version $Id: Service.java,v 1.5 2008/10/23 19:04:04 hohenwarter Exp $
 */
public class Service {

  public static Collection<?> installedProviders(Class<?> service) {
    ClassLoader loader = ClassLoader.getSystemClassLoader();
    ClassLoader previous = null;
    while (loader != null) {
      previous = loader;
      loader = loader.getParent();
    }
    return providers(service, previous);
  }

  public static Collection<?> providers(Class<?> service) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return Service.providers(service, loader);
  }

  public static Collection<?> providers(Class<?> service, ClassLoader loader) {
    List classList = new ArrayList<Object>();
    List<String> nameSet = new ArrayList<String>();
    String name = "META-INF/services/" + service.getName();
    Enumeration<URL> services;
    try {
      services = loader == null ? ClassLoader.getSystemResources(name) : loader
          .getResources(name);
    } catch (IOException ioe) {
      Application.debug("Service: cannot load " + name);
      return classList;
    }

    while (services.hasMoreElements()) {
      URL url = services.nextElement();
      // Application.debug(url);
      InputStream input = null;
      BufferedReader reader = null;
      try {
        input = url.openStream();
        reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
        String line = reader.readLine();
        while (line != null) {
          int ci = line.indexOf('#');
          if (ci >= 0)
            line = line.substring(0, ci);
          line = line.trim();
          int si = line.indexOf(' ');
          if (si >= 0)
            line = line.substring(0, si);
          line = line.trim();
          if (line.length() > 0)
            if (!nameSet.contains(line))
              nameSet.add(line);
          line = reader.readLine();
        }
      } catch (IOException ioe) {
        Application.debug("Service: problem with: " + url);
      } finally {
        try {
          if (input != null)
            input.close();
          if (reader != null)
            reader.close();
        } catch (IOException ioe2) {
          Application.debug("Service: problem with: " + url);
        }
      }
    }

    Iterator<String> names = nameSet.iterator();
    while (names.hasNext()) {
      String className = names.next();
      try {
        classList.add(Class.forName(className, true, loader).newInstance());
      } catch (ClassNotFoundException e) {
        Application.debug("Service: cannot find class: " + className);
      } catch (InstantiationException e) {
        Application.debug("Service: cannot instantiate: " + className);
      } catch (IllegalAccessException e) {
        Application.debug("Service: illegal access to: " + className);
      } catch (NoClassDefFoundError e) {
        Application.debug("Service: " + e + " for " + className);
      } catch (Exception e) {
        Application.debug("Service: exception for: " + className + " " + e);
      }
    }
    return classList;
  }

  private Service() {
  }
}
