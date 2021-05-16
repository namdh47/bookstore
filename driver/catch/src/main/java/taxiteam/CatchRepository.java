package taxiteam;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="catches", path="catches")
public interface CatchRepository extends PagingAndSortingRepository<Catch, Long>{


}
