# Creating new inline functions

New functions can be added to the core application, and current ones are stored in the `com.workiva.eva.service.client.functions` package. Function collections require an `@com.workiva.eva.service.client.PeerFunctionCollection` annotation to be discoverable and each function requires an `@com.workiva.eva.service.client.PeerFunction` annotation with the `value` populated with the function name.

## Example

```java
import com.workiva.eva.service.client.PeerFunction;
import com.workiva.eva.service.client.PeerFunctionCollection;
import com.workiva.eva.service.client.PeerRepository;

/**
 * Holds my functions.
 */
@PeerFunctionCollection
public class MyFunctions {

    /**
     * Does something useful.
     * @param repo The repository to examine.
     * @return Returns some value.
     */
    @PeerFunction("my-operation")
    public static Object myOp(
            PeerRepository repo // NOTE: The PeerRepository is always required as the first parameter.
    ) {
        
        // Usage would be something like this:
        //   #eva.client.service/inline { :fn my-operation }
        
        // ... The contents of my operation ...
        
        return value;
    }

    /**
     * Does something useful.
     * @param repo The repository to examine.
     * @return Returns the entity id.
     */
    @PeerFunction("my-function")
    public static Object myOp(
            PeerRepository repo, // NOTE: The PeerRepository is always required as the first parameter.
            
            // All the parameters you require are after the peer repository.
            Object param1,
            Object param2
    ) {
        
        // Usage would be something like this:
        //   #eva.client.service/inline { :fn my-function :params [{param1} [{param2}] }

        // ... The contents of my function ...
        
        return value;
    }
}

```
