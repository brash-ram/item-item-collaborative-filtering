package com.brash.util;

import java.util.List;

public class PageableUtils {

    public static <T> boolean canGetPage(List<T> list, int offset, int limit) {
        return offset * limit < list.size();
    }

    public static <T> List<T> getPage(List<T> list, int offset, int limit) {
        return list.subList(offset, limit);
    }
}
