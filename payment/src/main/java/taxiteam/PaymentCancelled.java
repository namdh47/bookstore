
package taxiteam;

public class PaymentCancelled extends AbstractEvent {

    private Long id;
    private Long matchId;
    private String customer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    public String getStudent() {
        return customer;
    }

    public void setStudent(String customer) {
        this.customer = customer;
    }
}

