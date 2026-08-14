package course.concurrency.m2_async.minPrice;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.lang.Double.NaN;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        ExecutorService executor = Executors.newFixedThreadPool(shopIds.size());

        try {
            List<CompletableFuture<Optional<Double>>> pricesFuture = shopIds.stream().map(shopId -> CompletableFuture
                .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                .thenApply(Optional::ofNullable)
                .orTimeout(2995, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> Optional.empty())
            ).toList();

            return pricesFuture.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .mapToDouble(Optional::get)
                .min()
                .orElse(NaN);
        } finally {
            executor.shutdownNow();
        }
    }
}
