package taxiteam;

import org.springframework.data.repository.PagingAndSortingRepository;
//import org.springframework.data.rest.core.annotation.RepositoryRestResource;

//@RepositoryRestResource(collectionResourceRel="pickUps", path="pickUps")
public interface PickUpRepository extends PagingAndSortingRepository<PickUp, Long>{


}
