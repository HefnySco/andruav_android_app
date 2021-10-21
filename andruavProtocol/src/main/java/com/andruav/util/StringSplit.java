package com.andruav.util;

import java.util.ArrayList;
import java.util.List;

/**
 * This code was taken froom Greenbrobot String Utils
 * https://github.com/greenrobot/greenrobot-common/blob/master/java-common/src/main/java/de/greenrobot/common/StringUtils.java
 * Created by mhefny on 2/11/16.
 */
public class StringSplit {



    public static String[] fastSplit(String string, char delimiter) {
        final List<String> list = new ArrayList<String>();
        final int size = string.length();
        int start = 0;
        for (int i = 0; i < size; i++) {
            if (string.charAt(i) == delimiter) {
                if (start < i) {
                    list.add(string.substring(start, i));
                } else {
                    list.add("");
                }
                start = i + 1;
            } else if (i == size - 1) {
                list.add(string.substring(start, size));
            }
        }
        String[] elements = new String[list.size()];
        list.toArray(elements);
        return elements;
    }



}
