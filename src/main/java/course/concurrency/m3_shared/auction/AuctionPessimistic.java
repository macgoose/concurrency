package course.concurrency.m3_shared.auction;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;
    private volatile Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid outdatedBid;

        synchronized (this) {
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
}
