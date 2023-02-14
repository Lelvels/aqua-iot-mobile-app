package pro.aquaiot.aquablue.data.remote;


import pro.aquaiot.aquablue.data.model.AquaDeviceTwin;
import pro.aquaiot.aquablue.data.model.Desired;
import pro.aquaiot.aquablue.data.model.DesiredJSON;
import pro.aquaiot.aquablue.data.model.SensorsData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AquaService {
    @Headers({"Accept: application/json"})
    @GET("devices/7")
    Call<AquaDeviceTwin> getAnswers();
    @Headers({"Accept: application/json"})
    @GET("sensordatas?deviceId[eq]=7&latest=true")
    Call<SensorsData> getLastestSensor();

    @Headers({"Accept: application/json"})
    @PUT("devices/7")
    Call<AquaDeviceTwin> putRequest(@Body DesiredJSON desiredJSON);
}
