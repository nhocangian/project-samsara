#version 120

/* DRAWBUFFERS:0 */

#include "/lib/loi_di_nhap_sky.glsl"

// Ảnh phơi quá sáng: vùng sáng cháy về trắng, màu hơi nhạt.
// Đường cong giữ nguyên màu đen tuyệt đối.
// Khi mắt ở dưới nước: hình gợn méo, sương nước xanh, nhìn xuống chìm về đen.

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform sampler2D depthtex1;    // độ sâu không tính nước
uniform sampler2D colortex2;    // do gbuffers_water ghi: rg = độ nghiêng sóng, b = 1 là mặt nước
uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform float frameTimeCounter;
uniform int isEyeInWater;
uniform float aspectRatio;
uniform mat4 gbufferModelView;
uniform mat4 gbufferProjection;

varying vec2 texCoord;

const float EXPOSURE_CURVE = 1.4;
const float DESATURATE = 0.04;

const float UNDERWATER_VISIBILITY = 14.0;   // tăng thì nhìn được xa hơn dưới nước
const vec3  UNDERWATER_TINT   = vec3(0.55, 0.90, 0.95);
const vec3  UNDERWATER_ABYSS  = vec3(0.00, 0.03, 0.06);
const vec3  UNDERWATER_MID    = vec3(0.02, 0.35, 0.42);
const vec3  UNDERWATER_LIGHT  = vec3(0.35, 0.70, 0.70);

const int   GLOW_TAPS = 32;
const float GLOW_RADIUS = 0.09;       // độ rộng quầng, tính theo chiều cao màn hình
const float GLOW_THRESHOLD = 0.78;    // chỉ vùng sáng hơn mức này mới toả
const float GLOW_STRENGTH = 0.55;

const int   SHAFT_TAPS = 28;
const float SHAFT_STRENGTH = 0.85;    // độ mạnh tia nắng
const float HALO_STRENGTH = 0.30;     // độ mạnh quầng sáng lớn quanh mặt trời
const vec3  SUN_LIGHT = vec3(1.00, 0.80, 0.58);

// Khói sương trên mặt biển và ở chân trời: dày sát đường chân trời, loãng dần lên cao, chỉ phủ vật ở xa.
const float HAZE_STRENGTH = 0.92;
const float HAZE_HEIGHT = 0.060;      // độ dày dải sương, tính theo độ dốc tia nhìn
const float HAZE_NEAR = 70.0;         // bắt đầu có sương từ khoảng cách này (block)
const float HAZE_FAR = 260.0;         // từ đây sương đủ dày
const vec3  HAZE_COLOR = vec3(0.98, 0.70, 0.66);

bool isSky(vec2 p) {
    return p.x > 0.0 && p.x < 1.0 && p.y > 0.0 && p.y < 1.0 && texture2D(depthtex0, p).r >= 1.0;
}

// Quầng và tia nắng: đi từ điểm ảnh về phía mặt trời, chỗ nào vật thể che thì không có tia
vec3 sunRadiance(vec2 uv) {
    vec3 view = mat3(gbufferModelView) * normalize(LDN_SUN_POS);
    if (view.z >= 0.0) return vec3(0.0);

    vec4 clip = gbufferProjection * vec4(view, 1.0);
    vec2 sun = clip.xy / clip.w * 0.5 + 0.5;
    float fade = 1.0 - smoothstep(0.5, 1.0, max(abs(sun.x - 0.5), abs(sun.y - 0.5)));
    if (fade <= 0.0) return vec3(0.0);

    vec2 stretch = vec2(aspectRatio, 1.0);
    float jitter = ldnHash(gl_FragCoord.xy);
    float shafts = 0.0;
    for (int i = 0; i < SHAFT_TAPS; i++) {
        float t = (float(i) + jitter) / float(SHAFT_TAPS);
        vec2 p = mix(uv, sun, t);
        if (isSky(p)) shafts += exp(-length((p - sun) * stretch) * 6.0) * (1.0 - t * 0.5);
    }
    shafts /= float(SHAFT_TAPS);

    // Quầng lớn yếu đi khi mặt trời bị che
    float visible = 0.0;
    for (int i = 0; i < 8; i++) {
        float angle = float(i) * 0.7854;
        visible += isSky(sun + vec2(cos(angle) / aspectRatio, sin(angle)) * 0.02) ? 1.0 : 0.0;
    }
    float halo = exp(-length((uv - sun) * stretch) * 3.2) * visible / 8.0;

    return SUN_LIGHT * (shafts * SHAFT_STRENGTH + halo * HALO_STRENGTH) * fade;
}

// Phản chiếu vật thể trên mặt nước, tính trên màn hình như tầng 1 (world3).
// Trời vẫn do gbuffers_water vẽ; ở đây chỉ đè thêm những gì tia chạm phải.
const int   RAY_STEPS = 80;
const float RAY_FIRST = 0.3;          // bước đầu (block)
const float RAY_GROWTH = 1.05;        // mỗi bước dài thêm
const int   RAY_REFINE = 6;
const float HAND_DEPTH = 0.56;        // tay cầm đồ được vẽ với depth nhỏ hơn mức này
const float REFLECT_NEAR = 0.30;      // độ rõ của bóng khi nhìn dốc xuống
const float REFLECT_FAR  = 0.80;      // độ rõ của bóng khi nhìn xiên ra xa

vec3 toView(vec2 p, float depth) {
    vec4 v = gbufferProjectionInverse * vec4(vec3(p, depth) * 2.0 - 1.0, 1.0);
    return v.xyz / v.w;
}

vec2 toScreen(vec3 view) {
    vec4 c = gbufferProjection * vec4(view, 1.0);
    return c.xy / c.w * 0.5 + 0.5;
}

float rayBehind(vec3 pos, vec2 p) {
    return toView(p, texture2D(depthtex1, p).r).z - pos.z;
}

bool traceReflection(vec3 start, vec3 dir, out vec2 hitUV, out vec3 hitPos) {
    vec3 pos = start;
    float stepLen = RAY_FIRST;
    hitUV = vec2(0.0);
    hitPos = start;

    for (int i = 0; i < RAY_STEPS; i++) {
        vec3 prev = pos;
        pos += dir * stepLen;
        if (pos.z > -0.05) return false;

        vec2 p = toScreen(pos);
        if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return false;

        float behind = rayBehind(pos, p);
        if (behind > 0.0) {
            if (behind > stepLen * 2.0 + 0.5) return false;

            vec3 a = prev;
            vec3 b = pos;
            for (int j = 0; j < RAY_REFINE; j++) {
                vec3 mid = 0.5 * (a + b);
                if (rayBehind(mid, toScreen(mid)) > 0.0) b = mid; else a = mid;
            }
            hitPos = b;
            hitUV = toScreen(b);
            return true;
        }
        stepLen *= RAY_GROWTH;
    }
    return false;
}

// Làm mờ giãn theo chiều dọc, như ảnh in loang trên nước
vec3 sampleBlurred(vec2 p, vec2 radius) {
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int i = -4; i <= 4; i++) {
        float f = float(i) / 4.0;
        float side = (mod(float(i), 2.0) < 0.5) ? -0.5 : 0.5;
        float w = exp(-2.2 * f * f);
        sum += texture2D(colortex0, clamp(p + vec2(side * radius.x, f * radius.y), 0.001, 0.999)).rgb * w;
        total += w;
    }
    return sum / total;
}

vec3 applyReflection(vec3 col, vec2 p) {
    vec4 tag = texture2D(colortex2, p);
    if (tag.b < 0.5) return col;
    float depth = texture2D(depthtex0, p).r;
    if (depth < HAND_DEPTH || depth >= 1.0) return col;

    vec3 viewPos = toView(p, depth);
    vec3 viewDir = normalize(viewPos);
    vec2 wave = (tag.rg - 0.5) / 2.0;
    vec3 normal = normalize(mat3(gbufferModelView) * vec3(wave.x, 1.0, wave.y));
    vec3 up = mat3(gbufferModelView) * vec3(0.0, 1.0, 0.0);

    vec2 hitUV;
    vec3 hitPos;
    if (!traceReflection(viewPos + up * 0.02, reflect(viewDir, normal), hitUV, hitPos)) return col;

    float travel = length(hitPos - viewPos);
    float blur = clamp(0.002 + travel * 0.0005 + length(viewPos) * 0.0002, 0.0, 0.014);
    vec3 reflection = sampleBlurred(hitUV, vec2(blur * 0.8, blur * 3.0));

    vec2 edge = min(hitUV, 1.0 - hitUV);
    float keep = smoothstep(0.0, 0.10, min(edge.x, edge.y));
    float cosView = clamp(dot(-viewDir, normal), 0.0, 1.0);
    return mix(col, reflection, mix(REFLECT_NEAR, REFLECT_FAR, pow(1.0 - cosView, 2.0)) * keep);
}

vec3 applyHaze(vec3 col, vec2 p) {
    float depth = texture2D(depthtex0, p).r;
    if (depth < HAND_DEPTH) return col;
    bool sky = depth >= 1.0;
    vec3 view = toView(p, min(depth, 0.99999));
    vec3 dir = normalize(mat3(gbufferModelViewInverse) * view);
    float dist = sky ? 100000.0 : length(view);

    // Dải sương mềm, lượn nhẹ theo phương ngang như khói
    float angle = atan(dir.x, dir.z) * 7.0;
    float t = frameTimeCounter * 0.015;
    float wisp = ldnNoise(vec2(angle, t * 3.0)) * 0.6 + ldnNoise(vec2(angle * 3.1 + 11.0, -t * 5.0)) * 0.4;
    float height = HAZE_HEIGHT * (0.65 + 0.7 * wisp);
    float band = exp(-max(dir.y + 0.006, 0.0) / height);

    float haze = band * smoothstep(HAZE_NEAR, HAZE_FAR, dist) * HAZE_STRENGTH;
    return mix(col, HAZE_COLOR, clamp(haze, 0.0, 1.0));
}

vec3 applyExposure(vec3 c) {
    return 1.0 - pow(1.0 - c, vec3(EXPOSURE_CURVE));
}

void main() {
    bool underwater = isEyeInWater == 1;
    vec2 uv = texCoord;
    if (underwater) {
        float t = frameTimeCounter;
        uv += vec2(sin(uv.y * 22.0 + t * 1.6), cos(uv.x * 18.0 + t * 1.3)) * 0.0025;
    }

    vec3 col = clamp(texture2D(colortex0, uv).rgb, 0.0, 1.0);

    if (underwater) {
        vec4 view = gbufferProjectionInverse * vec4(vec3(uv, texture2D(depthtex0, uv).r) * 2.0 - 1.0, 1.0);
        vec3 relative = (gbufferModelViewInverse * vec4(view.xyz / view.w, 0.0)).xyz;
        float up = normalize(relative).y;

        vec3 water = mix(UNDERWATER_ABYSS, UNDERWATER_MID, smoothstep(-0.7, 0.25, up));
        water = mix(water, UNDERWATER_LIGHT, smoothstep(0.3, 0.95, up));
        col = mix(col * UNDERWATER_TINT, water, 1.0 - exp(-length(relative) / UNDERWATER_VISIBILITY));
    }

    if (!underwater) {
        col = applyReflection(col, uv);
        col = applyHaze(col, uv);
    }

    col = applyExposure(col);

    // Glow: vùng sáng toả mờ ra xung quanh, cộng kiểu screen để không làm cháy thêm
    vec3 glow = vec3(0.0);
    for (int i = 0; i < GLOW_TAPS; i++) {
        float angle = float(i) * 2.39996;
        float radius = sqrt((float(i) + 0.5) / float(GLOW_TAPS)) * GLOW_RADIUS;
        vec2 offset = vec2(cos(angle) * radius / aspectRatio, sin(angle) * radius);
        vec3 tap = applyExposure(clamp(texture2D(colortex0, uv + offset).rgb, 0.0, 1.0));
        glow += tap * smoothstep(GLOW_THRESHOLD, 0.98, dot(tap, vec3(0.299, 0.587, 0.114)));
    }
    col = 1.0 - (1.0 - col) * (1.0 - glow / float(GLOW_TAPS) * GLOW_STRENGTH);

    if (!underwater) col = 1.0 - (1.0 - col) * (1.0 - clamp(sunRadiance(uv), 0.0, 1.0));

    col = mix(col, vec3(dot(col, vec3(0.299, 0.587, 0.114))), DESATURATE);
    gl_FragData[0] = vec4(col, 1.0);
}
