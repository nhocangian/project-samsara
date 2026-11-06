package com.samsara.client.sound;

import net.minecraft.client.sound.OggAudioStream;
import net.minecraft.client.sound.StaticSound;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Âm thanh tua ngược thật: đọc file .ogg vanilla rồi đảo ngược mẫu âm ngay trong bộ nhớ.
 *
 * Trong {@code sounds.json}, một âm tên {@code samsara:reversed/<namespace>/<đường_dẫn>} được hiểu là
 * file {@code <namespace>:sounds/<đường_dẫn>.ogg} phát ngược. Ví dụ
 * {@code "samsara:reversed/minecraft/mob/phantom/idle1"} = tiếng Phantom vanilla {@code idle1} tua ngược.
 * Không có file nào thật sự nằm ở {@code assets/samsara/sounds/reversed/}: mod không chép âm của Mojang.
 *
 * Không dùng được với {@code "stream": true} (âm stream được đọc dần, không đảo ngược được).
 * Móc vào game: {@code SoundLoaderReverseMixin} (nạp) và {@code SoundManagerReverseMixin} (kiểm tra file tồn tại).
 */
public final class ReversedSounds {
    private static final String PREFIX = "sounds/reversed/";

    private ReversedSounds() {}

    /** Đường dẫn file gốc cho một âm tua ngược, hoặc null nếu {@code location} không phải âm tua ngược. */
    @Nullable
    public static Identifier sourceOf(Identifier location) {
        if (!"samsara".equals(location.getNamespace())) return null;
        String path = location.getPath();
        if (!path.startsWith(PREFIX)) return null;
        String rest = path.substring(PREFIX.length());   // "minecraft/mob/phantom/idle1.ogg"
        int slash = rest.indexOf('/');
        if (slash <= 0) return null;
        return new Identifier(rest.substring(0, slash), "sounds/" + rest.substring(slash + 1));
    }

    /** Nạp file gốc, giải mã và trả về bản phát ngược. */
    public static StaticSound load(ResourceFactory resources, Identifier source) throws IOException {
        try (InputStream in = resources.open(source); OggAudioStream ogg = new OggAudioStream(in)) {
            ByteBuffer pcm = ogg.getBuffer();
            AudioFormat format = ogg.getFormat();
            return new StaticSound(reverse(pcm, format), format);
        }
    }

    /** Đảo thứ tự các khung mẫu (mỗi khung = mọi kênh của một thời điểm), giữ nguyên byte trong khung. */
    static ByteBuffer reverse(ByteBuffer pcm, AudioFormat format) {
        int frame = Math.max(1, format.getFrameSize());
        int start = pcm.position();
        int length = pcm.remaining() - pcm.remaining() % frame;
        ByteBuffer out = BufferUtils.createByteBuffer(length);
        for (int src = start + length - frame; src >= start; src -= frame) {
            for (int b = 0; b < frame; b++) out.put(pcm.get(src + b));
        }
        out.flip();
        return out;
    }
}
