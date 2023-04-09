package pro.aquaiot.aquablue.data.remote;


import pro.aquaiot.aquablue.data.model.AquaDeviceTwin;
import pro.aquaiot.aquablue.data.model.DesiredJSON;
import pro.aquaiot.aquablue.data.model.SensorsData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PUT;

public interface AquaService {
    @Headers({"Accept: application/json"})
    @GET("devices/7")
    Call<AquaDeviceTwin> getDeviceTwin();
    @Headers({"Accept: application/json"})
    @GET("sensordatas?deviceId[eq]=7&latest=true")
    Call<SensorsData> getLatestSensor();

    @Headers({"Accept: application/json"})
    @PUT("devices/7")
    Call<AquaDeviceTwin> putRequest(@Body DesiredJSON desiredJSON);
}
