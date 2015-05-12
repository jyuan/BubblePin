package bubblepin.com.bubblepin.metaioSDKLocationModule;

import com.metaio.sdk.jni.IGeometry;

public class MetaioMemory {

    private final IGeometry iGeometry;
    private final String memoryObjectId;

    public MetaioMemory(String memoryObjectId, IGeometry iGeometry) {
        this.memoryObjectId = memoryObjectId;
        this.iGeometry = iGeometry;
    }

    public IGeometry getiGeometry() {
        return iGeometry;
    }

    public String getMemoryObjectId() {
        return memoryObjectId;
    }

}
