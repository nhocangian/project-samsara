package com.samsara.client.overworld;

import com.samsara.system.overworld.DirectorCues;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Phím đạo diễn phía client ({@link DirectorCues}): mỗi cảnh một phím, mặc định numpad 1..9, 0 và dấu chấm, nằm trong nhóm "Samsara: đạo diễn" của Điều khiển. Bấm thì gửi tên cảnh lên server; không hiện gì trên
 * màn hình.
 */
public final class DirectorKeys {
    private DirectorKeys() {}

    private static final List<KeyBinding> KEYS = new ArrayList<>();
    /** Phím mặc định theo thứ tự {@link DirectorCues#CUES}: numpad 1..9, 0, dấu chấm. */
    private static final int[] DEFAULT_KEYS = {GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_4,
            GLFW.GLFW_KEY_KP_5, GLFW.GLFW_KEY_KP_6, GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_9,
            GLFW.GLFW_KEY_KP_0, GLFW.GLFW_KEY_KP_DECIMAL};

    public static void register() {
        for (int i = 0; i < DirectorCues.CUES.size(); i++) {
            KEYS.add(KeyBindingHelper.registerKeyBinding(new KeyBinding("key.samsara.cue." + DirectorCues.CUES.get(i),
                    InputUtil.Type.KEYSYM, i < DEFAULT_KEYS.length ? DEFAULT_KEYS[i] : GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories.samsara_director")));
        }
        // Lệnh chuẩn bị quay xoá khung chat (để chữ của lệnh không lọt vào hình)
        ClientPlayNetworking.registerGlobalReceiver(com.samsara.system.overworld.FilmingSetup.CLEAR_CHAT,
                (client, handler, buf, sender) -> client.execute(() -> client.inGameHud.getChatHud().clear(false)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (int i = 0; i < KEYS.size(); i++) {
                while (KEYS.get(i).wasPressed()) {
                    if (client.player == null || !ClientPlayNetworking.canSend(DirectorCues.PACKET)) continue;
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeString(DirectorCues.CUES.get(i));
                    ClientPlayNetworking.send(DirectorCues.PACKET, buf);
                }
            }
        });
    }
}
