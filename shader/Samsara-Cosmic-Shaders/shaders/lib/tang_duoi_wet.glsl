// Đường Ống là cống: vách và gờ hơi ẩm, chỗ ướt tối đi một chút và bóng lên khi nhìn xiên.
// Không dựa vào vị trí ống hay đèn trong mod: độ bóng theo ánh sáng block thật tại điểm đó.

const float WET_DARKEN = 0.32;   // chỗ ướt tối đi bao nhiêu
const float WET_SHEEN  = 0.42;   // độ bóng khi nhìn xiên
const float WET_SPEC   = 0.30;   // vệt sáng từ phía trên

float wetHash(vec2 p) {
    vec3 a = fract(vec3(p.xyx) * 0.1031);
    a += dot(a, a.yzx + 33.33);
    return fract((a.x + a.y) * a.z);
}

float wetNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(wetHash(i), wetHash(i + vec2(1.0, 0.0)), f.x),
               mix(wetHash(i + vec2(0.0, 1.0)), wetHash(i + vec2(1.0, 1.0)), f.x), f.y);
}

// 0..1: mảng ẩm lớn, trên vách có thêm các vệt nước chảy dọc
float wetMask(vec3 worldPos, vec3 worldNormal) {
    bool isWall = abs(worldNormal.y) < 0.5;
    vec2 uv = isWall ? vec2(worldPos.x + worldPos.z, worldPos.y) : worldPos.xz;

    float patches = smoothstep(0.42, 0.62, wetNoise(uv * 0.33 + 7.0) * 0.7 + wetNoise(uv * 1.4 + 3.0) * 0.3);
    float streaks = isWall ? smoothstep(0.60, 0.74, wetNoise(vec2(uv.x * 3.4, uv.y * 0.20))) : 0.0;
    // Chỗ khô phải khô hẳn: độ tương phản giữa khô và ướt mới làm mắt nhận ra độ ẩm
    return clamp(0.04 + 0.80 * patches + 0.70 * streaks, 0.0, 1.0);
}

vec3 applyWetness(vec3 col, vec3 viewPos, vec3 viewNormal, vec3 viewUp, float wet, float lightLevel) {
    vec3 V = normalize(-viewPos);
    vec3 N = normalize(viewNormal);

    vec3 result = col * (1.0 - WET_DARKEN * wet);

    float cosView = clamp(dot(N, V), 0.0, 1.0);
    float sheen = pow(1.0 - cosView, 4.0);

    vec3 H = normalize(viewUp + V);
    float spec = pow(clamp(dot(N, H), 0.0, 1.0), 48.0);

    vec3 shine = vec3(0.74, 0.84, 0.92) * lightLevel;
    return result + shine * wet * (sheen * WET_SHEEN + spec * WET_SPEC);
}
