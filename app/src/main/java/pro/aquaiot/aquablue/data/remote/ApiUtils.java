package pro.aquaiot.aquablue.data.remote;

public class ApiUtils {
    public static final String AQUA_IOTHUB_BASE_URL = "https://aqua-iot.pro/api/v1/";
    public static AquaService getAquaService() {
        return RetrofitClient.getClient(AQUA_IOTHUB_BASE_URL).create(AquaService.class);
    }
}
