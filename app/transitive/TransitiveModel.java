package transitive;



import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class TransitiveModel extends GenericTransitiveModel {

    @Id
	@GeneratedValue()
	public long id;


    public String toString() {
        return join(getClass().getName().replaceFirst(".*\\.", ""), "[", this.id, "]");
    }



}
