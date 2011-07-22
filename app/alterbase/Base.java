package alterbase;



import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class Base extends AlterBase {

    @Id
	@GeneratedValue()
	public long id;


    public String toString() {
        return join(getClass().getName().replaceFirst(".*\\.", ""), "[", this.id, "]");
    }



}
