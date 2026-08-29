package course.concurrency.m3_shared.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;
    private final AtomicReference<Bid> latestBid = new AtomicReference<>();

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid localLatestBid;

        do {
            localLatestBid = latestBid.get();

            if (localLatestBid != null && bid.getPrice() <= localLatestBid.getPrice())
                return false;
        } while (!latestBid.compareAndSet(localLatestBid, bid));

        if (localLatestBid != null)
            notifier.sendOutdatedMessage(localLatestBid);

        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
