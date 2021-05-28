
package taxiteam;

public class PickupAssigned extends AbstractEvent {

    private Long matchId;
    private String driver;
    private String customer;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    public String getDrivers() {
        return driver;
    }

    public void setDrivers(String driver) {
        this.driver = driver;
    }
    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }
}

