
package taxiteam;

public class PickupAssigned extends AbstractEvent {

    private Long matchId;
    private String driver;
    private String custmoer;

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
    public String getCustmoer() {
        return custmoer;
    }

    public void setCustmoer(String custmoer) {
        this.custmoer = custmoer;
    }
}

