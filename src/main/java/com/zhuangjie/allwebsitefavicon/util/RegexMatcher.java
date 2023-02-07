package com.zhuangjie.allwebsitefavicon.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatcher {
    public static List<String> matcher(String regex, String text) {
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(text);

        List<String> results = new ArrayList<>();
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                results.add(matcher.group(i));
            }
        }
        return results;
    }
}
