package edu.cmu.cs.lti.util;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/26/16
 * Time: 11:17 PM
 *
 * @author Zhengzhong Liu
 */
public class NuggetFormat {

    public static final String typeSubtypeJoiner = "_";

    public static String canonicalType(String... types) {
        StringBuilder sb = new StringBuilder();
        String joiner = "";
        for (String type : types) {
            sb.append(joiner).append(canonicalize(type));
            joiner = typeSubtypeJoiner;
        }

        return Joiner.on(typeSubtypeJoiner).join(
                Arrays.stream(sb.toString().split(typeSubtypeJoiner)).map(
                        p -> CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, p)
                ).collect(Collectors.toList())
        );
    }

    /**
     * Lowercase and replace all punctuations except underscore.
     *
     * @param s The input string
     * @return The canonicalized String.
     */
    private static String canonicalize(String s) {
        return s.toLowerCase().replaceAll("(?!_)\\p{Punct}", "");
    }
}
