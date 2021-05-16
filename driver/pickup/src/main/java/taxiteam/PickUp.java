package taxiteam;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="PickUp_table")
public class PickUp {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long matchId;
    private String driver;
    private String custmoer;

    @PostPersist
    public void onPostPersist(){
        PickupAssigned pickupAssigned = new PickupAssigned();
        BeanUtils.copyProperties(this, pickupAssigned);
        pickupAssigned.publishAfterCommit();


        PickupCancelled pickupCancelled = new PickupCancelled();
        BeanUtils.copyProperties(this, pickupCancelled);
        pickupCancelled.publishAfterCommit();


    }


    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }
    public String getCustmoer() {
        return custmoer;
    }

    public void setCustmoer(String custmoer) {
        this.custmoer = custmoer;
    }




}
