package course.concurrency.m3_shared.immutable;

import java.util.ArrayList;
import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.*;

public record Order(
    Long id,
    List<Item> items,
    PaymentInfo paymentInfo,
    boolean isPacked,
    Status status
) {

    public enum Status { NEW, IN_PROGRESS, DELIVERED }

    public static Order newOrder(long id, List<Item> items) {
        return new Order(id, new ArrayList<>(items), null, false, NEW);
    }

    public Order payed(PaymentInfo paymentInfo) {
        return new Order(id, new ArrayList<>(items), paymentInfo, isPacked, IN_PROGRESS);
    }

    public Order packed(boolean packed) {
        return new Order(id, new ArrayList<>(items), paymentInfo, packed, IN_PROGRESS);
    }

    public Order delivered() {
        return new Order(id, new ArrayList<>(items), paymentInfo, isPacked, DELIVERED);
    }

    public boolean checkStatus() {
        return items != null && !items.isEmpty() && paymentInfo != null && isPacked;
    }
}
