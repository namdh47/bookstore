
package taxiteam;

public class PickupCancelled extends AbstractEvent {

    private Long matchId;
    private String customer;

    public Long getId() {
        return matchId;
    }

    public void setId(Long matchId) {
        this.matchId = matchId;
    }
    public String getDrivers() {
        return customer;
    }

    public void setDrivers(String customer) {
        this.customer = customer;
    }
}

