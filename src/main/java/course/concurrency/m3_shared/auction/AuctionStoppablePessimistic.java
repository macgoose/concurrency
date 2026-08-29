package course.concurrency.m3_shared.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;
    private volatile Bid latestBid;
    private volatile boolean stopped = false;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid outdatedBid;

        synchronized (this) {
            if (stopped) return false;

            if (latestBid != null && bid.getPrice() <= latestBid.getPrice()) {
                return false;
            }

            outdatedBid = latestBid;
            latestBid = bid;
        }

        if (outdatedBid != null)
            notifier.sendOutdatedMessage(outdatedBid);

        return true;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public synchronized Bid stopAuction() {
        stopped = true;
        return latestBid;
    }
}
