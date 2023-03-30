package kim.figure.springssg;

import java.util.List;
import java.util.Map;

/**
 * The interface Path repository.
 */
public interface PathRepository {

    /**
     * Gets path map list.
     *
     * @return the path map list
     */
    public List<Map<String, String>> getPathMapList();

}
