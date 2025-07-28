package com.code972.hebmorph.hspell;

import com.code972.hebmorph.DictionaryLoader;
import com.code972.hebmorph.datastructures.DictHebMorph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashSet;

/**
 * DictionaryLoader implementation for loading hspell data files
 */
public class HSpellDictionaryLoader implements DictionaryLoader {
    @Override
    public String dictionaryLoaderName() {
        return "hspell";
    }

    @Override
    @Deprecated
    public String[] dictionaryPossiblePaths() {
        return getPossiblePaths();
    }

    @Override
    public String[] getPossiblePaths(final String ... basePaths) {
        final HashSet<String> paths = new HashSet<>();
        if (basePaths != null) {
            for (final String basePath : basePaths) {
                paths.add(Paths.get(basePath, "hspell-data-files").toAbsolutePath().toString());
            }
        }
        paths.add("/var/lib/hspell-data-files/");
        return paths.toArray(new String[paths.size()]);
    }

    @Override
    public DictHebMorph loadDictionary(final InputStream stream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DictHebMorph loadDictionaryFromPath(String path) throws IOException {
        if (!path.endsWith("/")) {
            path += "/";
        }

        final File file = new File(path);
        if (file.isDirectory()) {
            HSpellLoader loader = new HSpellLoader(new File(path), true);
            return loader.loadDictionaryFromHSpellData(new FileInputStream(new File(path, HSpellLoader.PREFIX_H)));
        } else {
            throw new IOException("Expected a folder. Cannot load dictionary from HSpell files.");
        }
    }

    @Override
    public DictHebMorph loadDictionaryFromDefaultPath() throws IOException {
        final String resourcePath = "hspell-data-files/";
        final ClassLoader classLoader = HSpellDictionaryLoader.class.getClassLoader();
        
        // Use the constructor that loads from classpath resources
        HSpellLoader loader = new HSpellLoader(classLoader, resourcePath, true);
        
        // The prefixes file is also loaded from the classpath now
        InputStream prefixesStream = classLoader.getResourceAsStream(resourcePath + HSpellLoader.PREFIX_NOH);
        if (prefixesStream == null) {
            throw new IOException("Could not find prefixes file in classpath: " + resourcePath + HSpellLoader.PREFIX_NOH);
        }

        return loader.loadDictionaryFromHSpellData(prefixesStream);
    }
}
