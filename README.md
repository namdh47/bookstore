![image](https://user-images.githubusercontent.com/45971330/118398020-b948c800-b691-11eb-84f0-f69f29b29017.png)

# 택시 호출 서비스


# Table of contents

- [택시 호출 서비스](#---)
  - [서비스 시나리오 및 요구사항 분석](#서비스-시나리오-및-요구사항-분석)
    - 서비스 개요 및 시나리오
    - 요구사항
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
    - 개요 및 구성 목표
    - 서비스 설계를 위한 Event Storming
    - 헥사고날 아키텍처 다이어그램 도출
  - [구현방안 및 검증](#구현방안-및-검증)
    - DDD 의 적용
    - 폴리글랏 퍼시스턴스
    - 동기식 호출 과 Fallback 처리
    - 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성
    - SAGA / Correlation
    - CQRS
  - [베포 및 운영](#베포-및-운영)
    - CI/CD 설정
    - 동기식 호출 / 서킷 브레이킹 / 장애격리
    - 오토스케일 아웃
    - 무정지 재배포
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)


# 서비스 시나리오 및 요구사항 분석


## 서비스 개요 및 시나리오 

- 택시 호출 및 배정 시스템
- 상용 택시 서비스와 달리 사용자가 자신의 목적지와 비용을 정하여 요청하면 택시 기사가 매칭하여 확정하는 방식


## 요구사항

- 기능적 요구사항
 1. 고객이 자신의 위치, 목적지, 비용을 기입하여 택시 배정을 요청한다.
 2. 고객이 비용을 결제한다.
 3. 고객이 결제하면 택시기사들은 요청목록을 확인할 수 있다. 
 4. 기사는 매칭을 선택하여 확정한다. 
 5. 기사가 매칭을 확정하면 고객은 알람을 받는다. 
 6. 고객은 매칭을 취소할 수 있다. 
 7. 고객은 마이페이지에서 택시 배정 요청 상태를 확인할 수 있다. 
 8. 고객이 요청 상태를 수시로 조회할 수 있다

- 비기능적 요구사항
 1. 트랜잭션
    1. 결제가 되지 않은 요청 건은 요청목록에서 확인할 수 없다. (Sync 호출)
 1. 장애격리
    1. 기사 매칭기능이 수행되지 않더라도 고객 요청은 이상없이 받고 장애가 복구된 후 요청목록에 입력될 수 있어야 한다. (Async(Event-driven), Eventual Consistency)
    1. 결제시스템에 과부하가 걸리면 사용자를 잠시동안 받지 않고 부하가 해소된 후에 다시 시도하도록 유도한다. (Circuit breaker, fallback)
 1. 성능
    1. 시스템 부하에 무관하게 고객이 수시로 자신의 요청상태를 확인할 수 있어야 한다. (CQRS)
    1. 요청상태가 바뀔때마다 알림을 줄 수 있어야 한다. (Event-driven)


# 체크포인트


## 평가항목
- Saga
- CQRS
- Correlation
- Req/Resp
- Gateway
- Deploy/ Pipeline
- Circuit Breaker
- Autoscale (HPA)
- Zero-downtime deploy (Readiness Probe)
- Config Map/ Persistence Volume
- Polyglot
- Self-healing (Liveness Probe)


# 분석/설계


## 개요 및 구성 목표
- 구성원 개인 역할 중심의 Horizontally-Aligned 조직에서 서비스 중심의 Vertically-Aligned 조직으로 전환되면서
각 서비스를 분리하여 Domain-driven한 마이크로서비스 아키텍쳐를 클라우드 네이티브하게 구현한다.


### AS-IS 조직 (Horizontally-Aligned)
![image](https://user-images.githubusercontent.com/81601230/119945548-90e59580-bfd0-11eb-9d67-8743ff8133e1.png)


### TO-BE 조직 (Vertically-Aligned)
![image](https://user-images.githubusercontent.com/81601230/119945590-9f33b180-bfd0-11eb-9ef7-1abd5afb5b2b.png)


## 서비스 설계를 위한 Event Storming
- MSAEz를 이용하여 Event Storming을 진행하였다.
- 결과 링크:  http://www.msaez.io/#/storming/PF1pfRP3vaXhLUEUMVj8lFZ1FIi2/every/3c1d852da14f8f3c6a2f41b8cc54eecd
- 도출 순서는 다음과 같다.
- **이벤트 도출 - 부적격 이벤트 탈락 - 액터/커맨드 - 어그리게잇 - 바인디드 컨텍스트 - 폴리시 - 컨텍스트 매핑 - 요구사항 검증 및 보완**

### 이벤트 도출
![image](https://user-images.githubusercontent.com/81601230/119945899-00f41b80-bfd1-11eb-8914-bac35185b49f.png)


### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/81601230/119945921-0487a280-bfd1-11eb-953e-54f03a32d3ca.png)

    - 과정 중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함 (UI 이벤트. 시스템 동작 등)


### 액터, 커맨드 부착하여 가독성개선
![image](https://user-images.githubusercontent.com/81601230/119946050-25e88e80-bfd1-11eb-9638-9e9430331f7d.png)


### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/81601230/119946089-33057d80-bfd1-11eb-83fb-450864090077.png)

    - 택시 요청, 결제 이력, 택시 매칭은 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그룹핑함


### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/81601230/119946195-55979680-bfd1-11eb-9ac7-8f8c2e2826a9.png)

    - 도메인 서열 분리 
        - Core Domain: 택시 요청, 택시 매칭 - 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 app 의 경우 1주일 1회 미만, store 의 경우 1개월 1회 미만
        - Supporting Domain: 고객/기사 지원 서비스(미포함) - 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain: 결제 - 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (향후 전환 예정)


### 폴리시 부착
![image](https://user-images.githubusercontent.com/81601230/119946436-97284180-bfd1-11eb-98f8-6089c93a5520.png)


### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![image](https://user-images.githubusercontent.com/81601230/119946458-9becf580-bfd1-11eb-8312-f25f84fe7a22.png)


### 완성된 1차 모형
![image](https://user-images.githubusercontent.com/81601230/119946474-a0b1a980-bfd1-11eb-9580-939e3a8c5288.png)

    - 도출 이후 기사 택시 매칭 확정 필요에 의해 View Model 추가함


### 1차 완성본에 대한 기능적 요구사항 검증
![image](https://user-images.githubusercontent.com/81601230/119946533-b1621f80-bfd1-11eb-930e-b52cf4eb241c.png)

    - 고객이 출발지,목적지,비용을 기입한다 (ok)
    - 고객이 비용을 결제한다 (ok)
    - 고객이 결제하면 택시기사들은 요청목록을 확인하고 매칭을 확정할 수 있다 (ok)
    - 택시기사가 출발지로 출발한다 (ok)
    

![image](https://user-images.githubusercontent.com/81601230/119946576-bc1cb480-bfd1-11eb-96f4-84252a9e4452.png)
    - 고객이 주문을 취소할 수 있다 (ok)

    - 주문이 취소되면 택시 매칭이 취소된다 (ok)
    
    - 고객이 주문상태를 중간중간 조회한다 (View-green sticker 의 추가로 ok)
    
    - 주문, 매칭 상태가 바뀔 때 마다 mypage를 통해 확인한다 (?)
    
    


### 모델 수정
![image](https://user-images.githubusercontent.com/81601230/119946599-c2ab2c00-bfd1-11eb-8bf5-6d90947261a6.png)
    
    - 수정된 모델은 모든 요구사항을 커버함.


### 비기능 요구사항 검증
![image](https://user-images.githubusercontent.com/81601230/119946668-d787bf80-bfd1-11eb-9928-2d6610a9b84a.png)

    - 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
    - 고객 주문 시 결제처리:  결제가 완료되지 않은 주문은 절대 받지 않는다는 경영자의 오랜 신념(?) 에 따라, ACID 트랜잭션 적용. 주문완료 시 결제처리에 대해서는 Request-Response 방식 처리
    - 결제 완료 시 기사연결 매칭처리:  catch 에서 pickup 마이크로서비스로 매칭요청이 전달되는 과정에 있어서 pickup 마이크로 서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
    - 나머지 모든 inter-microservice 트랜잭션: 결재상태, 매칭상태 등 모든 이벤트에 대해 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.


## 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/11955597/120057954-46245600-c082-11eb-8873-c5345b3d1348.png)


    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐
    

# 구현방안 및 검증

- 분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 **Spring Boot**로 구현하였다.
- 구현한 각 서비스  로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8084 이다)

```
cd catch
mvn spring-boot:run

cd pickup
mvn spring-boot:run  

cd payment
mvn spring-boot:run 

cd mypage
mvn spring-boot:run  
```


## DDD 의 적용
- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 Catch 마이크로 서비스).
- Event Storming을 통한 아키텍쳐와 Domain 구조로 DDD의 적용을 확인 며, 각 Domain에 해당하는 Entity는 다음과 같다.
- **Catch(택시 요청) / Payment(결제 이력) / Pickup(택시 매칭)**

```
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

    //최초 결재 승인 요청 액션
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
        payment.setStartingPoint(String.valueOf(this.getStartingPoint()));
        payment.setDestination(String.valueOf(this.getDestination()));
        CatchApplication.applicationContext.getBean(taxiteam.external.PaymentService.class)
            .paymentRequest(payment);

    }

    //사용자가 해당 결재건 취소 했을 경우. (status를 Cancel로 업데이트 보냄) 
    @PreUpdate
    public void onPreupdate(){
        if("Cancel".equals(status)){
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
```
- **Entity Pattern** 과 **Repository Pattern** 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록
데이터 접근 어댑터를 자동 생성하기 위하여 **Spring Data REST** 의 RestRepository 를 적용하였다
```
package taxiteam;

import org.springframework.data.repository.PagingAndSortingRepository;
//import org.springframework.data.rest.core.annotation.RepositoryRestResource;

//@RepositoryRestResource(collectionResourceRel="catches", path="catches")
public interface CatchRepository extends PagingAndSortingRepository<Catch, Long>{

}

```
---
#### 검증 및 테스트
- 적용 후 REST API 의 테스트
```
# catch 서비스의 요청처리
http POST http://localhost:8081/catches price=250000 startingPoint=Busan destination=Seoul customer=Peter  status=approve
```
![image](https://user-images.githubusercontent.com/11955597/120089011-5b0bf280-c131-11eb-81c6-196659a8e810.png)
```
# catch 서비스의 상태확인 
http http://localhost:8081/catches/2
```
![image](https://user-images.githubusercontent.com/11955597/120089031-855db000-c131-11eb-96ca-e59c52f02afb.png)
```
# catch 서비스에 대한 pickup 서비스의 응답
http http://localhost:8082/pickUps matchId=2 custmoer=Peter driver=Speedmate
```
![image](https://user-images.githubusercontent.com/11955597/120089073-eb4a3780-c131-11eb-9d74-c4367ce94426.png)


## 폴리글랏 퍼시스턴스
- catch 서비스는 다른 서비스와 구별을 위해 hsqldb를 사용 하였다.
이를 위해 catchh내 pom.xml에 dependency를 h2database에서 hsqldb로 변경 하였다.

```
# catch service pom.xml

		<!--
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
		-->

		<dependency>
			<groupId>org.hsqldb</groupId>
			<artifactId>hsqldb</artifactId>
			<version>2.4.0</version>
			<scope>runtime</scope>
		</dependency>
```


## 동기식 호출 과 Fallback 처리
- 분석단계에서의 조건 중 하나로 콜요청(catch)->결제(payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다.
호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 **FeignClient** 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# (catch) PaymentService.java

package taxiteam.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

//url 수정
@FeignClient(name="payment", url="http://localhost:8088")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void paymentRequest(@RequestBody Payment payment);

}
```
- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Catch.java (Entity)


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
        payment.setStartingPoint(String.valueOf(this.getStartingPoint()));
        payment.setDestination(String.valueOf(this.getDestination()));
        CatchApplication.applicationContext.getBean(taxiteam.external.PaymentService.class)
            .paymentRequest(payment);

    }
```
---
#### 검증 및 테스트
- 서비스를 임의로 정지하여 
- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:
```
# 결제 (payment) 서비스를 잠시 내려놓음 (ctrl+c)

#주문처리
http POST http://localhost:8081/catches price=200000 startingPoint=Daejeon destination=Seoul customer=David  status=approve   # Fail
```
![image](https://user-images.githubusercontent.com/11955597/120089415-08343a00-c135-11eb-9e81-c4daca7ce905.png)

```
#결제서비스 재기동
cd payment
mvn spring-boot:run

#주문처리
http POST http://localhost:8081/catches price=200000 startingPoint=Daejeon destination=Seoul customer=David  status=approve   # Success
```
![image](https://user-images.githubusercontent.com/11955597/120089493-d66fa300-c135-11eb-8c8e-27e6b0390282.png)
- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성


### 비동기식 호출
- 결제가 이루어진 후에 이를 pickup 시스템으로 알려주는 행위는 동기가 아닌 비동기 식으로 구현하여, 택시잡기/결제시스템에 블로킹을 주지않는다. 
- 이를 위하여 결제이력에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 **Apache Kafka**로 송출한다(Publish)
 
```
package taxiteam;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    private Long matchId;
    private Integer price;
    private String paymentAction;
    private String customer;
    private String startingPoint;
    private String destination;
    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();


    }
    
```
- pickup 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:
```

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

```


### 시간적 디커플링 / 장애격리
- pickup 서비스와 payment 시스템은 전혀 결합성이 없어, 만약 pickup시스템이 에러가 생기더라도 고객이 차 배차(catch 시스템) 결제(payment)시스템이 정상적으로 되야한다.
---
#### 검증 및 테스트
- pick up 서비스를 잠시 종료한 후 배차/결제 요청

```
#배차처리
http POST http://localhost:8081/catches price=150000 startingPoint=Kwangjoo destination=Chooncheon customer=Steve  status=approve   #Success
```
![image](https://user-images.githubusercontent.com/11955597/120089890-59dec380-c139-11eb-8eeb-46e957b35d05.png)

-결제서비스(payment)가 정상적으로 동작했는지 조회

```
http http://localhost:8083/payments/4
```
![image](https://user-images.githubusercontent.com/11955597/120089925-afb36b80-c139-11eb-94ff-0496e1f16e64.png)

-pickup 서비스 재가동
```
cd pickup
mvn spring-boot:run
```
-pickup service 요청 목록 확인

```
http http://localhost:8082/catchReqLists/4     # 정상적으로 요청이 들어온 것 확인
```
![image](https://user-images.githubusercontent.com/11955597/120089986-ff923280-c139-11eb-911d-29540007037c.png)

-pickup 기능 확인
```
http http://localhost:8082/pickUps matchId=4 custmoer=Steve driver=Safemate   #정상적으로 매핑
```
![image](https://user-images.githubusercontent.com/11955597/120090101-e938a680-c13a-11eb-9561-34ac7e434e2d.png)


## SAGA / Correlation
- 픽업(pickup) 시스템에서 상태가 매칭으로 변경되면 매치(catch) 시스템 원천데이터의 상태(status) 정보가 update된다
```
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

```


## CQRS
- status가 변경될때마다 event를 수신하여 조회하도록 별도의 view를 구현하여 명령과 조회를 분리했다. 
```
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

```
mypage view조회

![image](https://user-images.githubusercontent.com/11955597/120090295-48e38180-c13c-11eb-9903-a845049e6862.png)


# 베포 및 운영


## CI/CD 설정
- 각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 Azure를 사용하였으며,
pipeline build script 는 각 프로젝트 폴더 이하에 Dockerfile 과 deployment.yml/service.yaml 에 포함되었다.

* Continuos Integration Pipeline (Azure Cloud Devops)
![image](https://user-images.githubusercontent.com/11955597/120958041-cee17700-c791-11eb-8dc8-81466f578aca.png)

* Continuos Deployment Pipeline (Azure Cloud Devops)
![image](https://user-images.githubusercontent.com/11955597/120959115-0ea95e00-c794-11eb-812c-c916d3dba919.png)


## 동기식 호출 / 서킷 브레이킹 / 장애격리
* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
```
# PaymentService.java (catch external 서비스)

//@FeignClient(name="payment", url="http://localhost:8083")
@FeignClient(name="payment", url="${api.payment.url}")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void paymentRequest(@RequestBody Payment payment);

}

```

- 시나리오는 택시 요청(catch)-->결제(payment) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB(Circuit Breaker) 를 통하여 장애격리.
- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml (catch 서비스)

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```
![image](https://user-images.githubusercontent.com/11955597/120107288-7b709700-c19b-11eb-9aa5-e89f60ab9537.png)


---
#### 검증 및 테스트
- 피호출 서비스(결제:payment) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
# Payment.java (Entity)

    @PostPersist
    public void onPostPersist(){  //결제이력을 저장한 후 적당한 시간 끌기

        ...
        
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```
* seige 툴 사용법
```
siege가 설치된 apexacme/nginx 이미지로 default namespace 에 siege 란 이름으로 pod 생성
$ kubectl run siege --image=apexacme/siege-nginx -n default

siege pod 에 접속
$ kubectl exec -it siege -c siege -n default -- /bin/bash
```

* 부하테스터 **siege** 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 10번 반복하여 실시
```
$ siege -c100 -t60S -r10 -v --content-type "application/json" 'http://catch:8080/catches POST {"price":"250000", "startingPoint":"Kwangjoo", "destination":"Chooncheon", "customer":"SteveOh", "status":"approve"}'
```

* 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 pay에서 처리되면서 다시 order를 받기 시작
![image](https://user-images.githubusercontent.com/11955597/120107709-1027c480-c19d-11eb-90fd-7cda05bbb08b.png)


* report

![image](https://user-images.githubusercontent.com/11955597/120108116-b32d0e00-c19e-11eb-9ca5-684a9f32590b.png)


- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌.
하지만, 64.29% 가 성공하였고, 35.71%가 실패했다는 것은 고객 사용성에 있어 좋지 않기 때문에 Retry 설정과
동적 Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.


## 오토스케일 아웃
- 앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- 요청(catch) 및 결제(payment)서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:

```
# auto scale-out 설정 (catch/kubernetes/deployment.yml, payment/kubernetes/deployment.yml)
```
![image](https://user-images.githubusercontent.com/11955597/120108512-69ddbe00-c1a0-11eb-882e-4584d932db3b.png)


```
$ kubectl autoscale deploy catch --min=1 --max=10 --cpu-percent=15
$ kubectl autoscale deploy payment --min=1 --max=10 --cpu-percent=15
```
![image](https://user-images.githubusercontent.com/11955597/120108569-a4475b00-c1a0-11eb-9aac-69a5f36b3451.png)



---
#### 검증 및 테스트
- CB 에서 했던 방식대로 워크로드를 2분 동안 걸어준다.
```
$ siege -c100 -t60S -r10 -v --content-type "application/json" 'http://catch:8080/catches POST {"price":"250000", "startingPoint":"Kwangjoo", "destination":"Chooncheon", "customer":"SteveOh", "status":"approve"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
$ kubectl get deploy catch -w
$ kubectl get deploy payment -w
$ kubectl get pod -w
```
- 어느정도 시간이 흐른 후 (약 30초 간격) 스케일 아웃이 벌어지는 것을 확인할 수 있다:

![image](https://user-images.githubusercontent.com/11955597/120108854-e7ee9480-c1a1-11eb-822e-b8fe208af184.png)

![image](https://user-images.githubusercontent.com/11955597/120108848-dc9b6900-c1a1-11eb-9b06-4dd815932a23.png)



- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다. 
![image](https://user-images.githubusercontent.com/11955597/120111684-dc08cf80-c1ad-11eb-910e-b47f00e2c03f.png)



## 무정지 재배포
* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
- seige 로 배포작업 직전에 워크로드를 모니터링 함.

```
$ kubectl apply -f catch/kubernetes/deployment_readiness.yml
```

* readiness 옵션이 없는 경우 배포 중 서비스 요청처리 실패

![image](https://user-images.githubusercontent.com/11955597/120112926-014c0c80-c1b3-11eb-93c3-f418209b69c8.png)


* readiness 옵션이 추가된 deployment.yml을 적용
```
$ kubectl apply -f catch/kubernetes/deployment.yml
```
![image](https://user-images.githubusercontent.com/11955597/120112818-7c60f300-c1b2-11eb-951b-1514648b01ac.png)


* 새버전으로의 배포 시작
```
kubectl set image deploy catch catch=cnateam4.azurecr.io/catch:v2 -n default
```

* 기존 버전과 새 버전의 catch pod 공존 중

![image](https://user-images.githubusercontent.com/11955597/120113839-525dff80-c1b7-11eb-97ec-6ff76ae07b4c.png)

* Availability : 100% 확인

![image](https://user-images.githubusercontent.com/11955597/120113867-6f92ce00-c1b7-11eb-848c-e773d98f9ea4.png)


## Config Map
* application.yml 설정 (catch 서비스)

- default 부분

![image](https://user-images.githubusercontent.com/11955597/120114255-45daa680-c1b9-11eb-9d09-d3417b5df42f.png)

- docker 부분

![image](https://user-images.githubusercontent.com/11955597/120114260-525eff00-c1b9-11eb-9bea-c95c0ae59d05.png)

* deployment.yml 설정 (catch 서비스)

![image](https://user-images.githubusercontent.com/11955597/120114310-94884080-c1b9-11eb-8d10-8f5205b1f500.png)

* config map 생성 후 조회
```
$ kubectl create configmap apiurl --from-literal=url=http://payment:8080 --from-literal=fluentd-server-ip=10.xxx.xxx.xxx -n default
```
![image](https://user-images.githubusercontent.com/11955597/120114378-edf06f80-c1b9-11eb-8585-dac97058bb1b.png)

* 위 config map으로 배포한 서비스에서 설정한 URL로 택시요청 호출
```
http POST http://catch:8080/catches price=250000 startingPoint=Busan destination=Seoul customer=Peter  status=approve
```
![image](https://user-images.githubusercontent.com/11955597/120114562-a3bbbe00-c1ba-11eb-8c04-4c52713c4abd.png)

* config map 삭제 후 catch 서비스 재시작
```
$ kubectl delete configmap apiurl -n default
$ kubectl get pod/catch-574665c7bc-n6dn2 -n default -o yaml | kubectl replace --force -f- 
```
![image](https://user-images.githubusercontent.com/11955597/120114992-b931e780-c1bc-11eb-888b-887d8f16027c.png)

![image](https://user-images.githubusercontent.com/11955597/120115006-c6e76d00-c1bc-11eb-9200-2dd35d2f9ed5.png)

* config map 삭제된 상태에서 주문 호출
```
http POST http://catch:8080/catches price=250000 startingPoint=Busan destination=Seoul customer=Peter  status=approve
```
![image](https://user-images.githubusercontent.com/11955597/120115086-175eca80-c1bd-11eb-9653-31213696b36e.png)

![image](https://user-images.githubusercontent.com/11955597/120115125-44ab7880-c1bd-11eb-82ff-496c62732d08.png)

```
$ kubectl get pod/catch-574665c7bc-z2tzj -o yaml
$ kubectl describe pod/catch-574665c7bc-z2tzj
```
![image](https://user-images.githubusercontent.com/11955597/120115219-a23fc500-c1bd-11eb-84ec-f8acf0fd2bf3.png)
![image](https://user-images.githubusercontent.com/11955597/120115282-ecc14180-c1bd-11eb-947e-179722c287b9.png)


## Self-healing (Liveness Probe)

* catch 서비스 정상 확인

![image](https://user-images.githubusercontent.com/11955597/120116102-71fa2580-c1c1-11eb-8ca0-08adf9f6a34d.png)

* deployment.yml (catch 서비스)에 Liveness Probe 옵션 추가
```
          livenessProbe:
            tcpSocket:
              port: 8081
            initialDelaySeconds: 5
            periodSeconds: 5
```
![image](https://user-images.githubusercontent.com/11955597/120116153-b1287680-c1c1-11eb-992d-db264a3c1f86.png)

* catch deploy 재배포 후 liveness 가 적용된 부분 확인

![image](https://user-images.githubusercontent.com/11955597/120116296-7c68ef00-c1c2-11eb-8d32-eeadd9eb555d.png)


* catch 서비스의 liveness 가 발동되어 5번 retry 시도한 부분 확인

![image](https://user-images.githubusercontent.com/11955597/120116360-c2be4e00-c1c2-11eb-9e28-04d84b06f6bd.png)


# 신규 개발 조직의 추가
![image](https://user-images.githubusercontent.com/81601230/120946690-46a2a800-c778-11eb-99be-a15fd88c0a73.png)


## 마케팅팀의 추가
    - KPI: 신규 고객의 유입률 증대와 기존 고객의 충성도 향상
    - 구현계획 마이크로 서비스: 기존 mypage 마이크로 서비스를 인수하며, 고객에 경로 및 호출 추천 서비스 등을 제공할 예정 (구글 맵 기능과 유사)


## 이벤트 스토밍 
![image](https://user-images.githubusercontent.com/81601230/120946698-4e624c80-c778-11eb-91f7-c71940f269d7.png)


## 헥사고날 아키텍처 변화 
![image](https://user-images.githubusercontent.com/81601230/120946684-40acc700-c778-11eb-85ca-a7da54831f12.png)


## 구현  
기존 마이크로서비스에 수정을 발생시키지 않도록 Inbound 요청을 REST 가 아닌 Event Pub/Sub 방식으로 구현.
기존 마이크로서비스의 아키텍쳐나 데이터베이스 변동 없이 추가함. 


## 운영과 Retirement
Request/Response 방식으로 구현하지 않았기 때문에 서비스가 더이상 불필요해져도 Deployment 에서 제거되면 기존 마이크로서비스에 어떤 영향도 주지 않음.

* 결제(payment) 마이크로서비스의 경우 API 변화나 Retire 시에 택시 호출(catch) 마이크로서비스의 변경을 초래함:

```
# Catch.java (Entity)

        @PostPersist
    	public void onPostPersist(){
	
	...
        
        taxiteam.external.Payment payment = new taxiteam.external.Payment();
	
	...

        CatchApplication.applicationContext.getBean(taxiteam.external.PaymentService.class)
            .paymentRequest(payment);

    }
```


![image](https://user-images.githubusercontent.com/45971330/118398020-b948c800-b691-11eb-84f0-f69f29b29017.png)




