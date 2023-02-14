package pro.aquaiot.aquablue.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Data {

    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("enabled")
    @Expose
    private String enabled;
    @SerializedName("connectionState")
    @Expose
    private String connectionState;
    @SerializedName("deviceToCloudMessages")
    @Expose
    private Integer deviceToCloudMessages;
    @SerializedName("cloudToDeviceMessages")
    @Expose
    private Integer cloudToDeviceMessages;
    @SerializedName("desired")
    @Expose
    private String desired;
    @SerializedName("reported")
    @Expose
    private String reported;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(String connectionState) {
        this.connectionState = connectionState;
    }

    public Integer getDeviceToCloudMessages() {
        return deviceToCloudMessages;
    }

    public void setDeviceToCloudMessages(Integer deviceToCloudMessages) {
        this.deviceToCloudMessages = deviceToCloudMessages;
    }

    public Integer getCloudToDeviceMessages() {
        return cloudToDeviceMessages;
    }

    public void setCloudToDeviceMessages(Integer cloudToDeviceMessages) {
        this.cloudToDeviceMessages = cloudToDeviceMessages;
    }

    public String getDesired() {
        return desired;
    }

    public void setDesired(String desired) {
        this.desired = desired;
    }

    public String getReported() {
        return reported;
    }

    public void setReported(String reported) {
        this.reported = reported;
    }

}
