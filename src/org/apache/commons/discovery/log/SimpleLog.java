/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.discovery.log;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;

/**
 * <p>
 * Simple implementation of Log that sends all enabled log messages, for all
 * defined loggers, to System.err.
 * </p>
 * 
 * <p>
 * Hacked from commons-logging SimpleLog for use in discovery. This is intended
 * to be enough of a Log implementation to bootstrap Discovery.
 * </p>
 * 
 * <p>
 * One property: <code>org.apache.commons.discovery.log.level</code>. valid
 * values: all, trace, debug, info, warn, error, fatal, off.
 * </p>
 * 
 * @author Richard A. Sitze
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author Rod Waldhoff
 * @author Robert Burrell Donkin
 * 
 * @version $Id: SimpleLog.java,v 1.1 2009/07/06 21:31:51 murkle Exp $
 */
class SimpleLog implements Log {
  // ---------------------------------------------------- Log Level Constants

  /** "Trace" level logging. */
  public static final int LOG_LEVEL_TRACE = 1;
  /** "Debug" level logging. */
  public static final int LOG_LEVEL_DEBUG = 2;
  /** "Info" level logging. */
  public static final int LOG_LEVEL_INFO = 3;
  /** "Warn" level logging. */
  private static final int LOG_LEVEL_WARN = 4;
  /** "Error" level logging. */
  public static final int LOG_LEVEL_ERROR = 5;
  /** "Fatal" level logging. */
  public static final int LOG_LEVEL_FATAL = 6;

  /** Enable all logging levels */
  public static final int LOG_LEVEL_ALL = LOG_LEVEL_TRACE - 1;

  /** Enable no logging levels */
  public static final int LOG_LEVEL_OFF = LOG_LEVEL_FATAL + 1;

  // ------------------------------------------------------- Class Attributes

  static protected final String PROP_LEVEL = "org.apache.commons.discovery.log.level";

  /** Include the instance name in the log message? */
  static private boolean showLogName = false;

  /**
   * Include the short name ( last component ) of the logger in the log message.
   * Default to true - otherwise we'll be lost in a flood of messages without
   * knowing who sends them.
   */
  static private boolean showShortName = true;

  /** Include the current time in the log message */
  static private boolean showDateTime = false;

  /** Used to format times */
  static private DateFormat dateFormatter = null;

  /** The current log level */
  static private int logLevel = LOG_LEVEL_INFO;

  /**
   * Use 'out' instead of 'err' for logging to keep in-sync with test messages.
   */
  static private PrintStream out = System.out;

  // ------------------------------------------------------------ Initializer

  // initialize class attributes
  static {
    if (showDateTime)
      dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS zzz");

    try {
      // set log level from properties
      String lvl = System.getProperty(PROP_LEVEL);

      if ("all".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_ALL);
      else if ("trace".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_TRACE);
      else if ("debug".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_DEBUG);
      else if ("info".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_INFO);
      else if ("warn".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_WARN);
      else if ("error".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_ERROR);
      else if ("fatal".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_FATAL);
      else if ("off".equalsIgnoreCase(lvl))
        setLevel(SimpleLog.LOG_LEVEL_OFF);
    } catch (SecurityException ignored) {
      // do nothing. We get here if running discovery
      // under a servlet with restricted security rights, and
      // cannot read the system property.
      // In which case, the default is what you get to keep.
    }

  }

  // -------------------------------------------------------- Properties

  /**
   * <p>
   * Get logging level.
   * </p>
   */
  private static int getLevel() {
    return logLevel;
  }

  /**
   * Is the given log level currently enabled?
   * 
   * @param level
   *          is this level enabled?
   */
  private static boolean isLevelEnabled(int level) {
    // log level are numerically ordered so can use simple numeric
    // comparison
    return level >= getLevel();
  }

  /**
   * <p>
   * Set logging level.
   * </p>
   * 
   * @param currentLogLevel
   *          new logging level
   */
  private static void setLevel(int currentLogLevel) {
    logLevel = currentLogLevel;
  }

  // ------------------------------------------------------------- Attributes

  /** The name of this simple log instance */
  private String logName = null;

  private String prefix = null;

  // ------------------------------------------------------------ Constructor

  /**
   * Construct a simple log with given name.
   * 
   * @param name
   *          log name
   */
  protected SimpleLog(String name) {
    logName = name;
  }

  // -------------------------------------------------------- Logging Methods

  /**
   * <p>
   * Log a message with debug log level.
   * </p>
   */
  public final void debug(Object message) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_DEBUG))
      log(SimpleLog.LOG_LEVEL_DEBUG, message, null);
  }

  // -------------------------------------------------------- Log Implementation

  /**
   * <p>
   * Log an error with debug log level.
   * </p>
   */
  public final void debug(Object message, Throwable t) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_DEBUG))
      log(SimpleLog.LOG_LEVEL_DEBUG, message, t);
  }

  /**
   * <p>
   * Log a message with error log level.
   * </p>
   */
  public final void error(Object message) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_ERROR))
      log(SimpleLog.LOG_LEVEL_ERROR, message, null);
  }

  /**
   * <p>
   * Log an error with error log level.
   * </p>
   */
  public final void error(Object message, Throwable t) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_ERROR))
      log(SimpleLog.LOG_LEVEL_ERROR, message, t);
  }

  /**
   * <p>
   * Log a message with fatal log level.
   * </p>
   */
  public final void fatal(Object message) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_FATAL))
      log(SimpleLog.LOG_LEVEL_FATAL, message, null);
  }

  /**
   * <p>
   * Log an error with fatal log level.
   * </p>
   */
  public final void fatal(Object message, Throwable t) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_FATAL))
      log(SimpleLog.LOG_LEVEL_FATAL, message, t);
  }

  /**
   * <p>
   * Log a message with info log level.
   * </p>
   */
  public final void info(Object message) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_INFO))
      log(SimpleLog.LOG_LEVEL_INFO, message, null);
  }

  /**
   * <p>
   * Log an error with info log level.
   * </p>
   */
  public final void info(Object message, Throwable t) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_INFO))
      log(SimpleLog.LOG_LEVEL_INFO, message, t);
  }

  /**
   * <p>
   * Are debug messages currently enabled?
   * </p>
   * 
   * <p>
   * This allows expensive operations such as <code>String</code> concatenation
   * to be avoided when the message will be ignored by the logger.
   * </p>
   */
  public final boolean isDebugEnabled() {
    return isLevelEnabled(SimpleLog.LOG_LEVEL_DEBUG);
  }

  /**
   * <p>
   * Are error messages currently enabled?
   * </p>
   * 
   * <p>
   * This allows expensive operations such as <code>String</code> concatenation
   * to be avoided when the message will be ignored by the logger.
   * </p>
   */
  public final boolean isErrorEnabled() {
    return isLevelEnabled(SimpleLog.LOG_LEVEL_ERROR);
  }

  /**
   * <p>
   * Are fatal messages currently enabled?
   * </p>
   * 
   * <p>
   * This allows expensive operations such as <code>String</code> concatenation
   * to be avoided when the message will be ignored by the logger.
   * </p>
   */
  public final boolean isFatalEnabled() {
    return isLevelEnabled(SimpleLog.LOG_LEVEL_FATAL);
  }

  /**
   * <p>
   * Are info messages currently enabled?
   * </p>
   * 
   * <p>
   * This allows expensive operations such as <code>String</code> concatenation
   * to be avoided when the message will be ignored by the logger.
   * </p>
   */
  public final boolean isInfoEnabled() {
    return isLevelEnabled(SimpleLog.LOG_LEVEL_INFO);
  }

  /**
   * <p>
   * Are trace messages currently enabled?
   * </p>
   * 
   * <p>
   * This allows expensive operations such as <code>String</code> concatenation
   * to be avoided when the message will be ignored by the logger.
   * </p>
   */
  public final boolean isTraceEnabled() {
    return isLevelEnabled(SimpleLog.LOG_LEVEL_TRACE);
  }

  /**
   * <p>
   * Are warn messages currently enabled?
   * </p>
   * 
   * <p>
   * This allows expensive operations such as <code>String</code> concatenation
   * to be avoided when the message will be ignored by the logger.
   * </p>
   */
  public final boolean isWarnEnabled() {
    return isLevelEnabled(SimpleLog.LOG_LEVEL_WARN);
  }

  /**
   * <p>
   * Do the actual logging. This method assembles the message and then prints to
   * <code>System.err</code>.
   * </p>
   */
  private void log(int type, Object message, Throwable t) {
    // use a string buffer for better performance
    StringBuffer buf = new StringBuffer();

    // append date-time if so configured
    if (showDateTime) {
      buf.append(dateFormatter.format(new Date()));
      buf.append(" ");
    }

    // append a readable representation of the log leve
    switch (type) {
      case SimpleLog.LOG_LEVEL_TRACE :
        buf.append("[TRACE] ");
        break;
      case SimpleLog.LOG_LEVEL_DEBUG :
        buf.append("[DEBUG] ");
        break;
      case SimpleLog.LOG_LEVEL_INFO :
        buf.append("[INFO ] ");
        break;
      case SimpleLog.LOG_LEVEL_WARN :
        buf.append("[WARN ] ");
        break;
      case SimpleLog.LOG_LEVEL_ERROR :
        buf.append("[ERROR] ");
        break;
      case SimpleLog.LOG_LEVEL_FATAL :
        buf.append("[FATAL] ");
        break;
    }

    // append the name of the log instance if so configured
    if (showShortName) {
      if (prefix == null) {
        // cut all but the last component of the name for both styles
        prefix = logName.substring(logName.lastIndexOf(".") + 1) + " - ";
        prefix = prefix.substring(prefix.lastIndexOf("/") + 1) + "-";
      }
      buf.append(prefix);
    } else if (showLogName)
      buf.append(String.valueOf(logName)).append(" - ");

    // append the message
    buf.append(String.valueOf(message));

    // append stack trace if not null
    if (t != null) {
      buf.append(" <");
      buf.append(t.toString());
      buf.append(">");
    }

    // print to System.err
    out.println(buf.toString());

    if (t != null)
      t.printStackTrace(System.err);
  }

  /**
   * <p>
   * Log a message with debug log level.
   * </p>
   */
  public final void trace(Object message) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_TRACE))
      log(SimpleLog.LOG_LEVEL_TRACE, message, null);
  }

  /**
   * <p>
   * Log an error with debug log level.
   * </p>
   */
  public final void trace(Object message, Throwable t) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_TRACE))
      log(SimpleLog.LOG_LEVEL_TRACE, message, t);
  }

  /**
   * <p>
   * Log a message with warn log level.
   * </p>
   */
  public final void warn(Object message) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_WARN))
      log(SimpleLog.LOG_LEVEL_WARN, message, null);
  }

  /**
   * <p>
   * Log an error with warn log level.
   * </p>
   */
  public final void warn(Object message, Throwable t) {
    if (isLevelEnabled(SimpleLog.LOG_LEVEL_WARN))
      log(SimpleLog.LOG_LEVEL_WARN, message, t);
  }
}
