/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.arquillian.common;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version $Rev$ $Date$
 */
public class ConfigurationOverrides {
    protected static final Logger LOGGER = Logger.getLogger(TomEEContainer.class.getName());

    public static void apply(Object configuration, final Properties systemProperties, String... prefixes) {
        final List<URL> urls = findPropertiesFiles(prefixes);

        apply(configuration, systemProperties, urls, prefixes);
    }

    public static void apply(final Object configuration, final Properties systemProperties, final List<URL> urls, final String... prefixes) {
        final List<Properties> propertiesList = read(urls);

        final Properties defaults = new Properties();

        // Merge all the properties
        for (Properties p : propertiesList) {
            defaults.putAll(p);
        }

        final ObjectMap map = new ObjectMap(configuration);
        for (Map.Entry<Object, Object> entry : defaults.entrySet()) {
            final String key = entry.getKey().toString();
            final String value = entry.getValue().toString();
            setProperty(map, key, key, value, Level.FINE);
        }

        //
        // Override the config with system properties
        //
        for (String key : map.keySet()) {
            for (String prefix : prefixes) {
                final String property = prefix + "." + key;
                final String value = systemProperties.getProperty(property);

                setProperty(map, key, property, value, Level.INFO);
            }
        }
    }

    private static List<Properties> read(List<URL> urls) {
        final List<Properties> propertiesList = new ArrayList<Properties>();
        for (URL url : urls) {
            try {
                propertiesList.add(IO.readProperties(url));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Cannot read : " + url, e);
            }
        }
        return propertiesList;
    }

    public static List<URL> findPropertiesFiles(String... prefixes) {
        final List<URL> urls = new ArrayList<URL>();

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        for (String prefix : prefixes) {
            final String resourceName = String.format("default.arquillian-%s.properties", prefix.replace('.', '-'));
            addResources(urls, loader, resourceName);
        }

        for (String prefix : prefixes) {
            final String resourceName = String.format("arquillian-%s.properties", prefix.replace('.', '-'));
            addResources(urls, loader, resourceName);
        }
        return urls;
    }

    private static void addResources(List<URL> urls, ClassLoader loader, String resourceName) {
        try {
            final Enumeration<URL> resources = loader.getResources(resourceName);
            while (resources.hasMoreElements()) {
                urls.add(resources.nextElement());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed getResources: " + resourceName, e);
        }
    }

    private static void setProperty(ObjectMap map, String key, String property, String value, final Level info) {
        if (value == null) {
            LOGGER.log(Level.FINE, String.format("Unset '%s'", property));
            return;
        }

        try {
            LOGGER.log(info, String.format("Applying override '%s=%s'", property, value));
            map.put(key, value);
        } catch (Exception e) {
            try {
                map.put(key, Integer.parseInt(value)); // we manage String and int and boolean so let's try an int
            } catch (Exception ignored) {
                try {
                    map.put(key, Boolean.parseBoolean(value)); // idem let's try a boolean
                } catch (Exception ignored2) {
                    LOGGER.log(Level.WARNING, String.format("Override failed '%s=%s'", property, value), e);
                }
            }
        }
    }
}
