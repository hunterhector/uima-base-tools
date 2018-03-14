package edu.cmu.cs.lti.uima.util;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ReaderUtils {
    public static ArrayListMultimap<String, Span> getQuotesFromFile(File quotedAreaFile) throws IOException {
        ArrayListMultimap<String, Span> quotedSpans = ArrayListMultimap.create();

        for (String line : FileUtils.readLines(quotedAreaFile)) {
            String[] fields = line.split("\t");
            quotedSpans.put(fields[0], Span.of(Integer.valueOf(fields[1]), Integer.valueOf(fields[2])));
        }

        return quotedSpans;
    }
}
