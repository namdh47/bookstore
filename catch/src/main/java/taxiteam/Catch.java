package taxiteam;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Catch_table")
public class Catch {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Integer price;
    private String startingPoint;
    private String destination;
    private String customer;

    @PostPersist
    public void onPostPersist(){
        CatchCancelled catchCancelled = new CatchCancelled();
        BeanUtils.copyProperties(this, catchCancelled);
        catchCancelled.publishAfterCommit();


        CatchRequested catchRequested = new CatchRequested();
        BeanUtils.copyProperties(this, catchRequested);
        catchRequested.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        taxiteam.external.Payment payment = new taxiteam.external.Payment();
        // mappings goes here
        Application.applicationContext.getBean(taxiteam.external.PaymentService.class)
            .paymentRequest(payment);


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getStartingPoint() {
        return startingPoint;
    }

    public void setStartingPoint(String startingPoint) {
        this.startingPoint = startingPoint;
    }
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }




}
