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
    @Autowired CatchReqListRepository CatchReqListRepository;
    @Autowired PickUpRepository PickUpRepository;
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_PickupRequest(@Payload PaymentApproved paymentApproved){

        if(paymentApproved.isMe()){
            System.out.println("##### listener  : " + paymentApproved.toJson());


            //승인완료 시 승인완료된 리스트를 저장
            CatchReqList catchReqList = new CatchReqList();
            catchReqList.setId(paymentApproved.getMatchId());
            catchReqList.setDestination(paymentApproved.getDestination());
            catchReqList.setStartingPoint(paymentApproved.getStartingPoint());
            catchReqList.setPrice(paymentApproved.getPrice());
            CatchReqListRepository.save(catchReqList);
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_PickupCancel(@Payload PaymentCancelled paymentCancelled){

        if(paymentCancelled.isMe()){
            System.out.println("##### listener  : " + paymentCancelled.toJson());
            PickUpRepository.findById(paymentCancelled.getMatchId()).ifPresent(PickUp->{
                PickUpRepository.delete(PickUp);
            });

            //취소 요청 시, CatchReqList에서도 삭제 추가 05/30
            CatchReqListRepository.findById(paymentCancelled.getMatchId()).ifPresent(PickUp->{
                CatchReqListRepository.delete(PickUp);
            });
            
        }
    }

}
