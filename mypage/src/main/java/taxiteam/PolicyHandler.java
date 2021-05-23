package taxiteam;

import taxiteam.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired MyPageRepository MyPageRepository;
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCatchCancelled_StatusUpdate(@Payload CatchCancelled catchCancelled){

       

        if(catchCancelled.isMe()){
        MyPageRepository.findById(catchCancelled.getId()).ifPresent(MyPage ->{
            System.out.println("##### listener  : " + catchCancelled.toJson());
            System.out.println("##### wheneverCatchCancelled_MyPageRepository.findById : exist" );
            MyPage.setStatus(catchCancelled.getEventType());
            MyPageRepository.save(MyPage);
        });
    }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_StatusUpdate(@Payload PaymentCancelled paymentCancelled){

      
        if(paymentCancelled.isMe()){
        MyPageRepository.findById(paymentCancelled.getId()).ifPresent(MyPage ->{
            System.out.println("##### listener  : " + paymentCancelled.toJson());
            System.out.println("##### wheneverPaymentCancelled_MyPageRepository.findById : exist" );
            MyPage.setStatus(paymentCancelled.getEventType());
            MyPageRepository.save(MyPage);
        });
    }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_StatusUpdate(@Payload PaymentApproved paymentApproved){

        if(paymentApproved.isMe()){
            System.out.println("##### listener  : " + paymentApproved.toJson());

            MyPage mypage = new MyPage();
            mypage.setId(paymentApproved.getMatchId());
            mypage.setPrice(paymentApproved.getPrice());
            mypage.setStatus(paymentApproved.getEventType());
            mypage.setDestination(paymentApproved.getDestination());
            mypage.setStartingPoint(paymentApproved.getStartingPoint());
            MyPageRepository.save(mypage);
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPickupAssigned_StatusUpdate(@Payload PickupAssigned pickupAssigned){

        System.out.println("this is wheneverPickupAssigned_StatusUpdate");
        if(pickupAssigned.isMe()){
        MyPageRepository.findById(pickupAssigned.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### listener  : " + pickupAssigned.toJson());
            System.out.println("##### wheneverPickupAssigned_MyPageRepository.findById : exist" );
            MyPage.setDriver(pickupAssigned.getDriver());
            MyPage.setStatus(pickupAssigned.getEventType());
            MyPageRepository.save(MyPage);
        });
    }

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPickupCancelled_StatusUpdate(@Payload PickupCancelled pickupCancelled){


        if(pickupCancelled.isMe()){
        MyPageRepository.findById(pickupCancelled.getId()).ifPresent(MyPage ->{
            System.out.println("##### listener  : " + pickupCancelled.toJson());
            System.out.println("##### wheneverPickupCancelled_MyPageRepository.findById : exist" );
            MyPage.setStatus(pickupCancelled.getEventType());
            MyPageRepository.save(MyPage);
        });
    }
    }
}
