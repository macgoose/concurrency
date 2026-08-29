package course.concurrency.m3_shared.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;
    private final AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(null, false);

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid currentBid;

        do {
            if (latestBid.isMarked()) return false;
            currentBid = latestBid.getReference();

            if (currentBid != null && bid.getPrice() <= currentBid.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(currentBid, bid, false, false));

        if (currentBid != null)
            notifier.sendOutdatedMessage(currentBid);

        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        Bid currentBid;

        do {
            currentBid = latestBid.getReference();
        } while (!latestBid.attemptMark(currentBid, true));

        return currentBid;
    }
}
