package pro.aquaiot.aquablue.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Fish {
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("fishName")
    @Expose
    private String fishName;
    @SerializedName("fishMinpH")
    @Expose
    private Float fishMinpH;
    @SerializedName("fishMaxpH")
    @Expose
    private Float fishMaxpH;
    @SerializedName("fishMinTds")
    @Expose
    private Integer fishMinTds;
    @SerializedName("fishMaxTds")
    @Expose
    private Integer fishMaxTds;
    @SerializedName("fishMinTemperature")
    @Expose
    private Integer fishMinTemperature;
    @SerializedName("fishMaxTemperature")
    @Expose
    private Integer fishMaxTemperature;
    @SerializedName("pictureUrl")
    @Expose
    private String pictureUrl;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFishName() {
        return fishName;
    }

    public void setFishName(String fishName) {
        this.fishName = fishName;
    }

    public Float getFishMinpH() {
        return fishMinpH;
    }

    public void setFishMinpH(Float fishMinpH) {
        this.fishMinpH = fishMinpH;
    }

    public Float getFishMaxpH() {
        return fishMaxpH;
    }

    public void setFishMaxpH(Float fishMaxpH) {
        this.fishMaxpH = fishMaxpH;
    }

    public Integer getFishMinTds() {
        return fishMinTds;
    }

    public void setFishMinTds(Integer fishMinTds) {
        this.fishMinTds = fishMinTds;
    }

    public Integer getFishMaxTds() {
        return fishMaxTds;
    }

    public void setFishMaxTds(Integer fishMaxTds) {
        this.fishMaxTds = fishMaxTds;
    }

    public Integer getFishMinTemperature() {
        return fishMinTemperature;
    }

    public void setFishMinTemperature(Integer fishMinTemperature) {
        this.fishMinTemperature = fishMinTemperature;
    }

    public Integer getFishMaxTemperature() {
        return fishMaxTemperature;
    }

    public void setFishMaxTemperature(Integer fishMaxTemperature) {
        this.fishMaxTemperature = fishMaxTemperature;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    @Override
    public String toString() {
        return "Fish{" +
                "id=" + id +
                ", fishName='" + fishName + '\'' +
                ", fishMinpH=" + fishMinpH +
                ", fishMaxpH=" + fishMaxpH +
                ", fishMinTds=" + fishMinTds +
                ", fishMaxTds=" + fishMaxTds +
                ", fishMinTemperature=" + fishMinTemperature +
                ", fishMaxTemperature=" + fishMaxTemperature +
                ", pictureUrl='" + pictureUrl + '\'' +
                '}';
    }

    public String getStringPh(){
        return  "pH: " + fishMinpH + " đến " + fishMaxpH;


    }

    public String getStringTds(){
        return "Tds: " + fishMinTds + " đến " + fishMaxTds;
    }
    public String getStringTemp(){
        return "Nhiệt Độ: " + fishMinTemperature + " đến " + fishMaxTemperature;
    }

}
