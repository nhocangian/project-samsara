// Quầng sáng cho mọi thứ vẽ bằng lớp "mắt phát sáng" (RenderLayer.getEyes -> chương trình gbuffers_spidereyes):
// mắt The Wanderer, khe nứt Quả Trứng, mắt Gargantua của bóng phản chiếu, mắt nhện / Enderman / Phantom...
// gbuffers_spidereyes ghi màu sáng vào colortex4 (bộ đệm riêng, không đụng bloom colortex1/2 của world2);
// final.fsh gọi eyeGlow() để làm loang thành quầng mềm và cộng lên ảnh.
uniform sampler2D colortex4;
uniform float viewWidth;
uniform float viewHeight;

const bool colortex4MipmapEnabled = true;

#define EYE_GLOW_STRENGTH 2.2 // [0.0 0.8 1.4 2.2 3.0 4.0]

vec3 eyeGlow(vec2 uv) {
    vec2 px = 1.0 / vec2(viewWidth, viewHeight);
    vec3 core = texture2D(colortex4, uv).rgb;
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    // Hai vòng mẫu thưa trên mipmap (rẻ mà mềm): vòng trong sắc, vòng ngoài loang rộng
    for (int i = 0; i < 12; i++) {
        float a = float(i) * 0.5235988;
        vec2 dir = vec2(cos(a), sin(a));
        sum += texture2D(colortex4, uv + dir * px * 6.0, 2.0).rgb * 1.0;
        sum += texture2D(colortex4, uv + dir * px * 16.0, 3.5).rgb * 0.6;
        wsum += 1.6;
    }
    vec3 halo = sum / wsum;
    // Nhuốm sắc lạnh như trong concept (#9FB4C8)
    vec3 tint = vec3(0.62, 0.71, 0.78);
    return core * 0.6 + halo * tint * EYE_GLOW_STRENGTH * 2.5;
}
