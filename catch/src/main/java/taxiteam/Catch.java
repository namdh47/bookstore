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
    //고객의 명령 상태(승인 or 취소)를 표현하기위한 변수 추가
    private String status;

    @PostPersist
    public void onPostPersist(){

        CatchRequested catchRequested = new CatchRequested();
        BeanUtils.copyProperties(this, catchRequested);
        catchRequested.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
        taxiteam.external.Payment payment = new taxiteam.external.Payment();
        // mappings goes here 
        payment.setMatchId(Long.valueOf(this.getId()));
        payment.setPrice(Integer.valueOf(this.getPrice()));
        payment.setPaymentAction("Approved");
        payment.setCustomer(String.valueOf(this.getCustomer()));

        CatchApplication.applicationContext.getBean(taxiteam.external.PaymentService.class)
            .paymentRequest(payment);

    }
    @PreUpdate
    public void onPreupdate(){
        if("cancel".equals(status)){
            CatchCancelled catchCancelled = new CatchCancelled();
            BeanUtils.copyProperties(this, catchCancelled);
            catchCancelled.publishAfterCommit();
        }
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

    public String getStatus() { 
        return status; 
    }
    public void setStatus(String status) {
        this.status = status;
    }
}




