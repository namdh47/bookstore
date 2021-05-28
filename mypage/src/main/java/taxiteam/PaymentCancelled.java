
package taxiteam;

public class PaymentCancelled extends AbstractEvent {


    private Long matchId;
    private String customer;


    public void setMatchId(Long id) {
        this.matchId = matchId;
    }
    public Long getMatchId() {
        return matchId;
    }

    public void setId(Long matchId) {
        this.matchId = matchId;
    }
    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }
}

