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

    //취소시 Payment Repository를 수정하기위해 저장소 추가
    @Autowired PaymentRepository PaymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCatchCancelled_PaymentCancel(@Payload CatchCancelled catchCancelled){

        if(catchCancelled.isMe()){
            System.out.println("################ 매칭 취소요청으로 인한 주문 취소 ################ ");
            System.out.println("##### listener PaymentCancel : " + catchCancelled.toJson());

            PaymentRepository.findById(catchCancelled.getId()).ifPresent(Payment ->{
                Payment.setPaymentAction("Cancel");
                PaymentRepository.save(Payment);
         });

        }
    }

}
