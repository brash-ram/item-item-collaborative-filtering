package com.brash.util;

import java.util.ArrayList;
import java.util.List;

public class PageableUtils {

    public static <T> boolean canGetPage(List<T> list, int offset, int limit) {
        return offset * limit < list.size();
    }

    public static <T> List<T> getPage(List<T> list, int offset, int limit) {
        offset = offset * limit;
        if (offset >= list.size()) {
            return new ArrayList<>();
        } else if (limit >= list.size()) {
            limit = list.size();
        }
        return list.subList(offset, limit);
    }
}
