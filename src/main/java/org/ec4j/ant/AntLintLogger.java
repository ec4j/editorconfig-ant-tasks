/**
 * Copyright (c) 2018 EditorConfig Maven Plugin
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ec4j.ant;

import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.ec4j.maven.lint.api.Logger;

/**
 * A {@link AntLintLogger} that delegates to a SLF4J {@link org.slf4j.Logger}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class AntLintLogger implements Logger {

    /**
     * A formatter based on SLF4J's <a href=
     * "https://github.com/qos-ch/slf4j/blob/master/slf4j-api/src/main/java/org/slf4j/helpers/MessageFormatter.java">MessageFormatter</a>
     * under MIT license:
     *
     * Copyright (c) 2004-2011 QOS.ch All rights reserved.
     *
     * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
     * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
     * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
     * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
     *
     * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
     * the Software.
     */
    static class Slf4jFormatter {
        static final char DELIM_START = '{';
        static final char DELIM_STOP = '}';
        static final String DELIM_STR = "{}";
        private static final char ESCAPE_CHAR = '\\';

        private static void booleanArrayAppend(StringBuilder sbuf, boolean[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

        private static void byteArrayAppend(StringBuilder sbuf, byte[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

        private static void charArrayAppend(StringBuilder sbuf, char[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

        // special treatment of array values was suggested by 'lizongbo'
        private static void deeplyAppendParameter(StringBuilder sbuf, Object o, Map<Object[], Object> seenMap) {
            if (o == null) {
                sbuf.append("null");
                return;
            }
            if (!o.getClass().isArray()) {
                safeObjectAppend(sbuf, o);
            } else {
                // check for primitive array types because they
                // unfortunately cannot be cast to Object[]
                if (o instanceof boolean[]) {
                    booleanArrayAppend(sbuf, (boolean[]) o);
                } else if (o instanceof byte[]) {
                    byteArrayAppend(sbuf, (byte[]) o);
                } else if (o instanceof char[]) {
                    charArrayAppend(sbuf, (char[]) o);
                } else if (o instanceof short[]) {
                    shortArrayAppend(sbuf, (short[]) o);
                } else if (o instanceof int[]) {
                    intArrayAppend(sbuf, (int[]) o);
                } else if (o instanceof long[]) {
                    longArrayAppend(sbuf, (long[]) o);
                } else if (o instanceof float[]) {
                    floatArrayAppend(sbuf, (float[]) o);
                } else if (o instanceof double[]) {
                    doubleArrayAppend(sbuf, (double[]) o);
                } else {
                    objectArrayAppend(sbuf, (Object[]) o, seenMap);
                }
            }
        }

        private static void doubleArrayAppend(StringBuilder sbuf, double[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

        private static void floatArrayAppend(StringBuilder sbuf, float[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

        static String format(String message, Object... params) {

            int i = 0;
            int j;
            // use string builder for better multicore performance
            final StringBuilder result = new StringBuilder(message.length() + 50);

            int L;
            for (L = 0; L < params.length; L++) {

                j = message.indexOf(DELIM_STR, i);

                if (j == -1) {
                    // no more variables
                    if (i == 0) { // this is a simple string
                        return message;
                    } else { // add the tail string which contains no variables and return
                        // the result.
                        result.append(message, i, message.length());
                        return result.toString();
                    }
                } else {
                    if (isEscapedDelimeter(message, j)) {
                        if (!isDoubleEscaped(message, j)) {
                            L--; // DELIM_START was escaped, thus should not be incremented
                            result.append(message, i, j - 1);
                            result.append(DELIM_START);
                            i = j + 1;
                        } else {
                            // The escape character preceding the delimiter start is
                            // itself escaped: "abc x:\\{}"
                            // we have to consume one backward slash
                            result.append(message, i, j - 1);
                            deeplyAppendParameter(result, params[L], new HashMap<Object[], Object>());
                            i = j + 2;
                        }
                    } else {
                        // normal case
                        result.append(message, i, j);
                        deeplyAppendParameter(result, params[L], new HashMap<Object[], Object>());
                        i = j + 2;
                    }
                }
            }
            // append the characters following the last {} pair.
            result.append(message, i, message.length());
            return result.toString();
        }

        private static void intArrayAppend(StringBuilder sbuf, int[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

        private static boolean isDoubleEscaped(String messagePattern, int delimeterStartIndex) {
            if (delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR) {
                return true;
            } else {
                return false;
            }
        }

        private static boolean isEscapedDelimeter(String messagePattern, int delimeterStartIndex) {

            if (delimeterStartIndex == 0) {
                return false;
            }
            char potentialEscape = messagePattern.charAt(delimeterStartIndex - 1);
            if (potentialEscape == ESCAPE_CHAR) {
                return true;
            } else {
                return false;
            }
        }

        private static void longArrayAppend(StringBuilder sbuf, long[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

        private static void objectArrayAppend(StringBuilder sbuf, Object[] a, Map<Object[], Object> seenMap) {
            sbuf.append('[');
            if (!seenMap.containsKey(a)) {
                seenMap.put(a, null);
                final int len = a.length;
                for (int i = 0; i < len; i++) {
                    deeplyAppendParameter(sbuf, a[i], seenMap);
                    if (i != len - 1)
                        sbuf.append(", ");
                }
                // allow repeats in siblings
                seenMap.remove(a);
            } else {
                sbuf.append("...");
            }
            sbuf.append(']');
        }

        private static void safeObjectAppend(StringBuilder sbuf, Object o) {
            try {
                String oAsString = o.toString();
                sbuf.append(oAsString);
            } catch (Throwable t) {
                sbuf.append("[FAILED toString()]");
            }

        }

        private static void shortArrayAppend(StringBuilder sbuf, short[] a) {
            sbuf.append('[');
            final int len = a.length;
            for (int i = 0; i < len; i++) {
                sbuf.append(a[i]);
                if (i != len - 1)
                    sbuf.append(", ");
            }
            sbuf.append(']');
        }

    }

    private final ProjectComponent delegate;
    private Level level;

    public enum Level {
        ERROR(Project.MSG_ERR), WARN(Project.MSG_WARN), INFO(Project.MSG_INFO), DEBUG(Project.MSG_INFO), TRACE(Project.MSG_INFO);
        private final int antLevel;
        Level(int antLevel) {
            this.antLevel = antLevel;
        }
    }

    public AntLintLogger(ProjectComponent delegate, Level level) {
        super();
        this.delegate = delegate;
        this.level = level;
    }

    public void debug(String message, Object... params) {
        log(Level.DEBUG, message, params);
    }

    void log(Level level, String message, Object[] params) {
        if (this.level.ordinal() >= level.ordinal()) {
            delegate.log(Slf4jFormatter.format(message, params), level.antLevel);
        }
    }

    public void error(String message, Object... params) {
        log(Level.ERROR, message, params);
    }

    public void info(String message, Object... params) {
        log(Level.INFO, message, params);
    }

    /** @return always {@code true} */
    @Override
    public boolean isDebugEnabled() {
        return this.level.ordinal() >= Level.DEBUG.ordinal();
    }

    /** @return always {@code true} */
    @Override
    public boolean isErrorEnabled() {
        return this.level.ordinal() >= Level.ERROR.ordinal();
    }

    /** @return always {@code true} */
    @Override
    public boolean isInfoEnabled() {
        return this.level.ordinal() >= Level.INFO.ordinal();
    }

    /** @return always {@code true} */
    @Override
    public boolean isTraceEnabled() {
        return this.level.ordinal() >= Level.TRACE.ordinal();
    }

    /** @return always {@code true} */
    @Override
    public boolean isWarnEnabled() {
        return this.level.ordinal() >= Level.WARN.ordinal();
    }

    public void trace(String message, Object... params) {
        log(Level.TRACE, message, params);
    }

    public void warn(String message, Object... params) {
        log(Level.WARN, message, params);
    }

}
