package vg.skye.disharmony.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vg.skye.disharmony.Disharmony;

import java.net.SocketAddress;
import java.util.List;

@Mixin(PlayerManager.class)
public final class PlayerManagerMixin {
    @Shadow @Final private List<ServerPlayerEntity> players;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        try {
            Disharmony.INSTANCE.onPlayerCountChange(this.players.size());
        } catch (Exception ignored) {}
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void onDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
        try {
            Disharmony.INSTANCE.onPlayerCountChange(this.players.size());
        } catch (Exception ignored) {}
    }

    @Inject(method = "checkCanJoin", at = @At("RETURN"), cancellable = true)
    private void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() != null) return;
        try {
            Text text = Disharmony.INSTANCE.checkCanJoin(profile);
            if (text != null) cir.setReturnValue(text);
        } catch (Exception ignored) {}
    }
}
