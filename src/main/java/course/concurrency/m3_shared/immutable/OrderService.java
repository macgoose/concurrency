package course.concurrency.m3_shared.immutable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService {

    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private long nextId = 0L;

    private synchronized long nextId() {
        return nextId++;
    }

    public long createOrder(List<Item> items) {
        long id = nextId();
        Order order = Order.newOrder(id, items);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        currentOrders.computeIfPresent(orderId, (k, order) -> {
            if (order.status().equals(Order.Status.DELIVERED))
                return order;

            Order newOrder = order.payed(paymentInfo);

            if (newOrder.checkStatus())
                return deliver(order);

            return newOrder;
        });
    }

    public void setPacked(long orderId) {
        currentOrders.computeIfPresent(orderId, (k, order) -> {
            if (order.status().equals(Order.Status.DELIVERED))
                return order;

            Order newOrder = order.packed(true);

            if (newOrder.checkStatus())
                return deliver(order);

            return newOrder;
        });
    }

    private Order deliver(Order order) {
        return currentOrders.computeIfPresent(order.id(), (k, curr) -> curr.delivered());
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).status().equals(Order.Status.DELIVERED);
    }
}
