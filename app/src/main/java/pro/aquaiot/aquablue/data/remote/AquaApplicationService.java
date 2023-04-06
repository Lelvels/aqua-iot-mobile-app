package pro.aquaiot.aquablue.data.remote;

import pro.aquaiot.aquablue.data.model.AquariumData;
import pro.aquaiot.aquablue.data.model.CreateAquariumRequest;
import pro.aquaiot.aquablue.data.model.SyncFishAquaRequest;
import pro.aquaiot.aquablue.data.model.FishData;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AquaApplicationService {
    @Headers({"Accept: application/json"})
    @GET("fishes")
    Call<FishData> getAllFishes();

    @Headers({"Accept: application/json"})
    @GET("fishaqua/{id}")
    Call<FishData> getFishesByAquariumId(@Path("id") Integer id);

    @Headers({"Accept: application/json"})
    @GET("aquariums/name/{name}")
    Call<AquariumData> getAquariumByName(@Path("name") String name);

    @Headers({"Accept: application/json"})
    @POST("fishaqua")
    Call<FishData> syncFishInAquarium(@Body SyncFishAquaRequest syncFishAquaRequest);

    @Headers({"Accept: application/json"})
    @POST("aquariums")
    Call<AquariumData> createNewAquarium(@Body CreateAquariumRequest createAquariumRequest);
}
