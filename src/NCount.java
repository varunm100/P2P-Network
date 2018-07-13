import java.io.Serializable;

/**
 * @author varun on 7/4/2018
 * @project P2P-Network
 */
class NCount implements Serializable {
    private static final long serialVersionUID = -2262288895514226561L;
    int count;
    int maxCount;
    Object object;

    NCount(int _count, int _maxCount, Object _object) {
        count = _count;
        maxCount = _maxCount;
        object = _object;
    }
}
