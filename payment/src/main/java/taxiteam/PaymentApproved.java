
package taxiteam;

public class PaymentApproved extends AbstractEvent {

    private Long id;
    private Long matchId;
    private Integer price;
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
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getStudent() {
        return customer;
    }

    public void setStudent(String customer) {
        this.customer = customer;
    }
}

