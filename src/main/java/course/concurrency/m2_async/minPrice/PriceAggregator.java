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
        double minPrice = NaN;
        List<CompletableFuture<Optional<Double>>> futures = new ArrayList<>();

        for (Long shopId : shopIds) {
            Supplier<Double> priceSupplier = () -> priceRetriever.getPrice(itemId, shopId);
            futures.add(CompletableFuture
                .supplyAsync(priceSupplier, executor)
                .thenApply(Optional::ofNullable)
                .orTimeout(2990, TimeUnit.MILLISECONDS)
                .exceptionally(error -> Optional.empty()));
        }

        for (CompletableFuture<Optional<Double>> future : futures) {
            try {
                Optional<Double> priceOptional = future.get();
                if (priceOptional.isPresent())
                    minPrice = Double.isNaN(minPrice) ? priceOptional.get() : Double.min(minPrice, priceOptional.get());
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }

        return minPrice;
    }
}
