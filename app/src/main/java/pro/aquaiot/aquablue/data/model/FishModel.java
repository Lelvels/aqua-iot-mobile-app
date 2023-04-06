package pro.aquaiot.aquablue.data.model;

public class FishModel {
    private String fishName;
    private String fishOtherInformation;
    private int fish_image;
    private boolean fishIsChecked;

    public FishModel(String fishName, String fishOtherInformation, int fish_image, boolean fishIsChecked) {
        this.fishName = fishName;
        this.fishOtherInformation = fishOtherInformation;
        this.fish_image = fish_image;
        this.fishIsChecked = fishIsChecked;
    }

    public boolean isFishIsChecked() {
        return fishIsChecked;
    }

    public void setFishIsChecked(boolean fishIsChecked) {
        this.fishIsChecked = fishIsChecked;
    }

    public String getFishName() {
        return fishName;
    }

    public void setFishName(String fishName) {
        this.fishName = fishName;
    }

    public String getFishOtherInformation() {
        return fishOtherInformation;
    }

    public void setFishOtherInformation(String fishOtherInformation) {
        this.fishOtherInformation = fishOtherInformation;
    }

    public int getFish_image() {
        return fish_image;
    }

    public void setFish_image(int fish_image) {
        this.fish_image = fish_image;
    }
}
