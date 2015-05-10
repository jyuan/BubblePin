package bubblepin.com.bubblepin.metaioSDKLocationModule;

import com.metaio.sdk.jni.IGeometry;

public class MetaioSDKMemory {

    private final IGeometry iGeometry;
    private final String memoryObjectId;

    public MetaioSDKMemory(String memoryObjectId, IGeometry iGeometry) {
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
