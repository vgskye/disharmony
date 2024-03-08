package vg.skye.disharmony.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.util.crash.CrashReport;
import okhttp3.*;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vg.skye.disharmony.Disharmony;

import java.io.File;
import java.util.HashMap;

@Mixin(CrashReport.class)
public abstract class CrashReportMixin {
    @Shadow public abstract String asString();

    @Shadow @Final private static Logger LOGGER;
    @Unique
    private final OkHttpClient client = new OkHttpClient();
    @Unique
    private static final Gson gson = new Gson();

    @Inject(method = "writeToFile", at = @At("RETURN"))
    private void writeToFile(File file, CallbackInfoReturnable<Boolean> cir) {
        try {
            RequestBody formBody = new FormBody.Builder().add("content", this.asString()).build();
            Request request = new Request.Builder().url("https://api.mclo.gs/1/log").post(formBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.body().contentType().toString().equals("application/json")) {
                    throw new RuntimeException("mclo.gs didn't return JSON as response!");
                }
                JsonObject resp = gson.fromJson(response.body().charStream(), JsonObject.class);
                if (resp.getAsJsonPrimitive("success").getAsBoolean()) {
                    String reportLink = resp.getAsJsonPrimitive("url").getAsString();
                    System.err.println("Crash report uploaded to: " + reportLink);
                    String url = Disharmony.INSTANCE.getWebhook().getUrl();
                    var map = new HashMap<String, String>();
                    map.put("content", reportLink);
                    map.put("username", "Server Crash Report");
                    map.put("avatar_url", "https://mclo.gs/img/favicon.ico");
                    RequestBody jsonBody = RequestBody.create(gson.toJson(map), MediaType.parse("application/json"));
                    Request webhookRequest = new Request.Builder().url(url).post(jsonBody).build();
                    client.newCall(webhookRequest).execute().close();
                } else {
                    throw new RuntimeException(resp.getAsJsonPrimitive("error").getAsString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not upload report to mclo.gs because: {}", e.getLocalizedMessage());
        }
    }
}
