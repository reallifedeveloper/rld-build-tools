package com.reallifedeveloper.tools.test.database.inmemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.NullHandling;
import org.springframework.data.domain.Sort.Order;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;

import com.reallifedeveloper.tools.test.TestUtil;

/**
 * A helper class to assist with sorting lists in accordance with {@code org.springframework.data.domain.Sort} instances.
 *
 * @author RealLifeDeveloper
 */
public final class SortUtil {

    /**
     * Since this is a utility class that should not be instantiated, we hide the only constructor.
     */
    private SortUtil() {
    }

    /**
     * Sorts the given list according to the sort definition provided.
     *
     * @param <T>   the type of elements in the list
     * @param items the list of items to sort
     * @param sort  the {@code org.springframework.data.domain.Sort} instance defining how the list should be sorted
     *
     * @return a copy of {@code items}, sorted according to {@code sort}
     */
    public static <T> List<T> sort(List<T> items, Sort sort) {
        List<T> sortedItems = new ArrayList<>(items);
        // We reverse the Order instances so that we sort by the least important first and the most important last.
        List<Order> orders = reverseOrders(sort);
        for (Order order : orders) {
            sort(sortedItems, order);
        }
        return sortedItems;
    }

    private static List<Order> reverseOrders(Sort sort) {
        List<Order> orders = new ArrayList<>(sort.toList());
        Collections.reverse(orders);
        return orders;
    }

    private static <T> void sort(List<T> items, Order order) {
        Collections.sort(items, new FieldComparator<>(order));
    }

    @AllArgsConstructor
    @SuppressFBWarnings(value = "SE_COMPARATOR_SHOULD_BE_SERIALIZABLE", justification = "This class is only used interally when sorting")
    private static final class FieldComparator<T> implements Comparator<T> {

        private Order order;

        @Override
        @SuppressWarnings("unchecked")
        public int compare(T o1, T o2) {
            T fieldValue1 = (T) TestUtil.getFieldValue(o1, order.getProperty());
            T fieldValue2 = (T) TestUtil.getFieldValue(o2, order.getProperty());
            return nullSafeCompare(fieldValue1, fieldValue2);
        }

        @SuppressWarnings("unchecked")
        private int nullSafeCompare(T fieldValue1, T fieldValue2) {
            if (fieldValue1 == null) {
                if (fieldValue2 == null) {
                    return 0;
                } else if (order.getNullHandling() == NullHandling.NULLS_FIRST) {
                    return -1;
                } else {
                    return 1;
                }
            }
            if (fieldValue2 == null) {
                if (order.getNullHandling() == NullHandling.NULLS_FIRST) {
                    return 1;
                } else {
                    return -1;
                }
            }
            // We simply try to cast the field value to Comparable, it it fails we get a ClassCastException:
            int compareResult = ((Comparable<T>) fieldValue1).compareTo(fieldValue2);
            if (order.isAscending()) {
                return compareResult;
            } else {
                return invert(compareResult);
            }
        }

        private static int invert(int c) {
            if (c < 0) {
                return 1;
            } else if (c > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
