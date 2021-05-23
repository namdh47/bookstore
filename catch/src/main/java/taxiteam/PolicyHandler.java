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
    //이벤트 생성 시 매칭 DB접근을 위해 MatchRepository 에노테이션 추가 
    @Autowired CatchRepository CatchRepository;
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }
    
    //PickUp이 됐을 경우
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPickupAssigned_StatusUpdate(@Payload PickupAssigned pickupAssigned){

        if(pickupAssigned.isMe()){

            System.out.println("##### listener wheneverPickupAssigned : " + pickupAssigned.toJson());

            CatchRepository.findById(pickupAssigned.getId()).ifPresent(Catch ->{
                System.out.println("##### wheneverPickupAssigned_MatchRepository.findById : exist" );
                Catch.setStatus(pickupAssigned.getEventType()); 
                CatchRepository.save(Catch);
            });
        }
    }

}
