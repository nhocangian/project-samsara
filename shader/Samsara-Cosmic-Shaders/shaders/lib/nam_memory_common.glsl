// Dùng chung cho chiều Ký Ức Của Nam (world2). CHỈ được include từ shaders/world2/.
// File này chỉ chứa hằng số và hàm (không khai báo uniform) để include ở bất kỳ chương trình nào
// mà không bị trùng khai báo.
//
// Mục tiêu hình ảnh: buổi trưa nắng vàng ấm, trời xanh đậm trong vắt, bóng đổ rõ nét,
// sương vàng nhạt ở xa, viền tối mảnh quanh khối, bloom mềm trên hoa hướng dương.
// Mọi màu tính trong "HDR" (có thể > 1), sau đó composite3 nén lại bằng tonemap.
#ifndef NAM_MEMORY_COMMON_GLSL
#define NAM_MEMORY_COMMON_GLSL

// ---- Ánh sáng ----
#define NM_SUN_COLOR    vec3(1.00, 0.86, 0.64)   // nắng vàng ấm
#define NM_SUN_STRENGTH 2.35                      // cường độ nắng trực tiếp [1.5 2.0 2.35 2.8 3.2]
#define NM_AMBIENT      vec3(0.54, 0.60, 0.74)    // ánh trời tán xạ trong bóng râm (hơi lạnh)
#define NM_AMBIENT_STRENGTH 0.78                  // [0.5 0.65 0.78 0.9 1.1]
#define NM_BLOCKLIGHT   vec3(1.00, 0.62, 0.30)    // đuốc / đèn lồng
#define NM_SHADOW_TINT  vec3(0.92, 0.95, 1.08)    // ám màu nhẹ trong vùng tối

// ---- Bóng đổ ----
// Phải khớp với const trong world2/shadow.vsh / shadow.fsh / composite.fsh.
#define NM_SHADOW_RES      2048.0
#define NM_SHADOW_DISTANCE 112.0
#define NM_SHADOW_DISTORT  0.86   // 0 = không méo, gần 1 = dồn độ phân giải về gần camera

// ---- Sương / bầu trời ----
#define NM_HAZE_COLOR   vec3(1.00, 0.80, 0.34)    // sương vàng mật ong phủ cánh đồng xa (đậm, ít trắng)
#define NM_HAZE_DENSITY 0.0070                    // [0.004 0.006 0.0070 0.0085 0.012]
#define NM_HAZE_MAX     0.32

// Méo bản đồ bóng: tăng độ phân giải gần camera.
float nmShadowDistortFactor(vec2 clipXY) {
    return length(clipXY) * NM_SHADOW_DISTORT + (1.0 - NM_SHADOW_DISTORT);
}

vec3 nmDistortShadow(vec3 clip) {
    float f = nmShadowDistortFactor(clip.xy);
    return vec3(clip.xy / f, clip.z * 0.25);
}

// Lightmap Iris (0.03..0.97) -> 0..1. x = ánh sáng block, y = ánh sáng trời.
vec2 nmLightmap(vec2 lm) {
    return clamp((lm - 1.0 / 32.0) * (32.0 / 30.0), 0.0, 1.0);
}

// Bầu trời: xanh đậm ở thiên đỉnh, xanh nhạt trong veo gần chân trời,
// dải nắng vàng mỏng sát chân trời và quầng sáng quanh mặt trời.
// dir, sunDir: vector đơn vị trong không gian người chơi (y hướng lên).
vec3 nmSkyColor(vec3 dir, vec3 sunDir) {
    float up = dir.y;
    float h = clamp(up, 0.0, 1.0);

    vec3 zenith  = vec3(0.10, 0.28, 1.70);
    vec3 mid     = vec3(0.30, 0.62, 1.55);
    vec3 horizon = vec3(0.78, 0.92, 1.30);

    vec3 col = mix(horizon, mid, smoothstep(0.0, 0.22, h));
    col = mix(col, zenith, smoothstep(0.18, 0.85, h));

    // Dưới chân trời: sương vàng (cánh đồng hướng dương phản chiếu lên)
    col = mix(col, NM_HAZE_COLOR * 1.35, smoothstep(0.04, -0.20, up) * 0.75);
    // Dải sáng ấm ôm sát chân trời
    col += vec3(1.00, 0.80, 0.42) * exp(-abs(up) * 12.0) * 0.30;

    // Quầng mặt trời (rộng + lõi)
    float sd = max(dot(dir, sunDir), 0.0);
    col += vec3(1.00, 0.86, 0.62) * (pow(sd, 6.0) * 0.30 + pow(sd, 48.0) * 0.90);

    return col;
}

// Màu sương ở rìa tầm nhìn: lấy màu trời sát chân trời theo hướng nhìn ngang,
// để mặt đất cong lên hoà vào trời mà không thành vệt xanh đậm.
vec3 nmEdgeFogColor(vec3 dirPlayer, vec3 sunDir) {
    vec3 flatDir = normalize(vec3(dirPlayer.x, 0.03, dirPlayer.z) + vec3(0.0, 1e-4, 0.0));
    return nmSkyColor(flatDir, sunDir);
}

// Ánh sáng chính.
//  baseColor : albedo * màu đỉnh (đã gồm AO)
//  lm        : lightmap thô (lmCoord)
//  NdotL     : dot(pháp tuyến, hướng nắng)
//  shadow    : 0 = trong bóng, 1 = được nắng
//  foliage   : 1 = cây cỏ hai mặt (chiếu sáng xuyên qua lá)
vec3 nmLighting(vec3 baseColor, vec2 lm, float NdotL, float shadow, float foliage) {
    vec2 l = nmLightmap(lm);
    float sky = l.y;
    float blk = l.x;

    // Lá cỏ: sáng cả hai mặt, mặt sau hơi trong (subsurface giả)
    float diffuse = mix(max(NdotL, 0.0), 0.45 + 0.55 * abs(NdotL), foliage);

    // Không cho nắng lọt vào trong nhà (lightmap trời thấp)
    float skyExposure = smoothstep(0.55, 0.95, sky);

    vec3 sun = NM_SUN_COLOR * NM_SUN_STRENGTH * diffuse * shadow * skyExposure;
    vec3 amb = NM_AMBIENT * NM_AMBIENT_STRENGTH * (0.12 + 0.88 * sky * sky);
    amb *= mix(NM_SHADOW_TINT, vec3(1.0), shadow);
    vec3 torch = NM_BLOCKLIGHT * pow(blk, 2.6) * 1.6;

    // Ánh sáng tối thiểu để trong nhà không đen kịt
    vec3 minLight = vec3(0.045, 0.042, 0.05);

    return baseColor * (amb + sun + torch + minLight);
}

// Sương vàng theo khoảng cách + hoà vào trời ở rìa tầm nhìn.
vec3 nmApplyFog(vec3 col, vec3 playerPos, vec3 sunDir, float renderDist) {
    float dist = length(playerPos);
    vec3 dir = playerPos / max(dist, 1e-4);

    float haze = (1.0 - exp(-dist * NM_HAZE_DENSITY)) * NM_HAZE_MAX;
    col = mix(col, NM_HAZE_COLOR * 1.10, haze);

    float edge = smoothstep(renderDist * 0.72, renderDist * 1.05, dist);
    return mix(col, nmEdgeFogColor(dir, sunDir), edge);
}

// Nhận diện cây cỏ (model chữ thập, hoa hướng dương): pháp tuyến KHÔNG thẳng trục.
float nmFoliageMask(vec3 normalPlayer) {
    vec3 a = abs(normalPlayer);
    float m = max(a.x, max(a.y, a.z));
    return 1.0 - smoothstep(0.88, 0.97, m);
}

// ACES (Narkowicz) — nén HDR về 0..1, giữ màu nắng rực mà không cháy.
vec3 nmTonemap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

float nmLuma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

#endif
