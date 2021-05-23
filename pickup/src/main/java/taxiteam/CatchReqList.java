package taxiteam;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="CatchReqList_table")
public class CatchReqList {

        @Id
        private Long id;    
        public Integer price;
        private String startingPoint;
        private String destination;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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
        
        public Integer getPrice() {
            return price;
        }
    
        public void setPrice(Integer price) {
            this.price = price;
        }
}

