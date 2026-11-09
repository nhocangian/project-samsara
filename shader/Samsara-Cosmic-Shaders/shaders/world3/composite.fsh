#version 120

/* DRAWBUFFERS:3 */

#include "/lib/tang_duoi_water.glsl"

// Đường Ống: phản chiếu không gian màn hình trên mặt nước phẳng, như sàn gạch ướt.
// Nước luôn nằm ngang nên pháp tuyến cố định là (0, 1, 0).
// Pass này chỉ tính colortex3: rgb = màu phản chiếu, a = độ mờ.
// composite1.fsh làm mờ phản chiếu rồi mới hoà vào ảnh, để chỗ nối giữa phản chiếu thật và màu nền không lộ.

uniform sampler2D colortex0;
uniform sampler2D colortex2;   // r = 1: pixel là mặt trên của nước (gbuffers_water)
uniform sampler2D depthtex0;   // có nước
uniform sampler2D depthtex1;   // không có nước
uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

varying vec2 texCoord;

const int   RAY_STEPS   = 72;
const float RAY_FIRST   = 0.12;   // bước đầu (block)
const float RAY_GROWTH  = 1.045;   // mỗi bước dài thêm
const int   REFINE      = 6;

// Vách và gờ ẩm phản chiếu nhẹ. Xoá dòng #define này để tắt hẳn.
#define WET_REFLECTIONS
const int   WET_RAY_STEPS = 32;       // khoảng 8 block, đủ tới vách đối diện
const float WET_REFLECT   = 0.60;
const float WET_MAX_DIST  = 8.0;
const float HAND_DEPTH  = 0.56;   // tay cầm đồ được vẽ với depth nhỏ hơn mức này

// Màu nền khi tia không có dữ liệu trên màn hình. Không dựa vào vị trí ống hay đèn:
// độ sáng lấy từ ánh sáng block thật tại mặt nước, nên sửa bản đồ trong mod không cần sửa shader.
const vec3  FALLBACK_COLOR = vec3(0.60, 0.66, 0.70);

vec3 toView(vec2 uv, float depth) {
    vec4 p = gbufferProjectionInverse * vec4(vec3(uv, depth) * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

vec2 toScreen(vec3 view) {
    vec4 c = gbufferProjection * vec4(view, 1.0);
    return c.xy / c.w * 0.5 + 0.5;
}

bool onScreen(vec2 uv) {
    return uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0;
}

// Độ sâu của tia so với cảnh tại cùng điểm màn hình: > 0 là tia đã đi vào sau bề mặt
float rayBehind(vec3 pos, vec2 uv) {
    return toView(uv, texture2D(depthtex1, uv).r).z - pos.z;
}

// Dò tia phản chiếu. Trả về true và điểm trúng khi gặp vách, trần hoặc đèn.
bool traceReflection(vec3 start, vec3 dir, int steps, out vec2 hitUV, out vec3 hitPos) {
    vec3 pos = start;
    float stepLen = RAY_FIRST;

    for (int i = 0; i < steps; i++) {
        vec3 prev = pos;
        pos += dir * stepLen;
        if (pos.z > -0.05) return false;           // đi ra sau camera

        vec2 uv = toScreen(pos);
        if (!onScreen(uv)) return false;

        float behind = rayBehind(pos, uv);
        if (behind > 0.0) {
            if (behind > stepLen * 2.0 + 0.5) return false;  // chui sau vật ở gần: không có thông tin

            vec3 a = prev;
            vec3 b = pos;
            for (int j = 0; j < REFINE; j++) {
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

// Làm mờ theo khoảng cách, giãn theo chiều dọc như trên sàn ướt
vec3 sampleBlurred(vec2 uv, vec2 radius) {
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int i = -4; i <= 4; i++) {
        float f = float(i) / 4.0;
        float side = (mod(float(i), 2.0) < 0.5) ? -0.5 : 0.5;
        float w = exp(-2.2 * f * f);
        sum += texture2D(colortex0, clamp(uv + vec2(side * radius.x, f * radius.y), 0.001, 0.999)).rgb * w;
        total += w;
    }
    return sum / total;
}

// Vệt sáng dọc của đèn: chỉ lấy phần rất sáng, kéo về phía người nhìn nhiều hơn
vec3 lampStreak(vec2 uv, float len) {
    vec3 sum = vec3(0.0);
    for (int j = 1; j <= 8; j++) {
        float f = float(j) / 8.0;
        float w = (1.0 - f) * (1.0 - f);
        vec3 down = texture2D(colortex0, clamp(uv - vec2(0.0, f * len), 0.001, 0.999)).rgb;
        vec3 up   = texture2D(colortex0, clamp(uv + vec2(0.0, f * len * 0.45), 0.001, 0.999)).rgb;
        sum += down * lampMask(down) * w;
        sum += up   * lampMask(up) * w * 0.6;
    }
    return sum * 0.30;
}

// Nền phản chiếu: sáng lên ở vùng gần đèn, tối đi ở vùng thiếu đèn
vec3 fallbackReflection(float blockLight) {
    return FALLBACK_COLOR * mix(0.32, 1.15, smoothstep(0.30, 0.90, blockLight));
}

vec3 faceNormal(float encoded) {
    float id = floor(encoded * 6.0);
    if (id < 0.5) return vec3(1.0, 0.0, 0.0);
    if (id < 1.5) return vec3(-1.0, 0.0, 0.0);
    if (id < 2.5) return vec3(0.0, 1.0, 0.0);
    if (id < 3.5) return vec3(0.0, -1.0, 0.0);
    if (id < 4.5) return vec3(0.0, 0.0, 1.0);
    return vec3(0.0, 0.0, -1.0);
}

// Phản chiếu trên vách và gờ ẩm. a của kết quả là độ trộn; 0 = không có phản chiếu.
vec4 wetReflection(float depth, vec4 tag) {
    vec3 viewPos = toView(texCoord, depth);
    vec3 viewDir = normalize(viewPos);
    vec3 normal = normalize(mat3(gbufferModelView) * faceNormal(tag.g));
    float cosView = dot(-viewDir, normal);
    if (cosView <= 0.0) return vec4(0.0);

    float fresnel = 0.02 + 0.98 * pow(1.0 - cosView, 4.0);
    float strength = fresnel * tag.b * WET_REFLECT;
    if (strength < 0.01) return vec4(0.0);

    vec2 hitUV;
    vec3 hitPos;
    if (!traceReflection(viewPos + normal * 0.03, reflect(viewDir, normal), WET_RAY_STEPS, hitUV, hitPos)) return vec4(0.0);

    float travel = length(hitPos - viewPos);
    float blur = clamp(0.0015 + travel * 0.0009, 0.0, 0.010);
    vec3 color = sampleBlurred(hitUV, vec2(blur, blur * 1.6));

    vec2 edge = min(hitUV, 1.0 - hitUV);
    float keep = smoothstep(0.0, 0.08, min(edge.x, edge.y)) * (1.0 - smoothstep(WET_MAX_DIST * 0.6, WET_MAX_DIST, travel));
    return vec4(color, strength * keep);
}

void main() {
    gl_FragData[0] = vec4(0.0);

    float depth = texture2D(depthtex0, texCoord).r;
    if (depth < HAND_DEPTH || depth >= 1.0) return;
    vec4 tag = texture2D(colortex2, texCoord);
    if (tag.r < 0.5) {
#ifdef WET_REFLECTIONS
        if (tag.b > 0.05) gl_FragData[0] = wetReflection(depth, tag);
#endif
        return;
    }

    vec3 viewPos = toView(texCoord, depth);
    vec3 viewDir = normalize(viewPos);
    vec3 normal = normalize(mat3(gbufferModelView) * vec3(0.0, 1.0, 0.0));
    float cosView = dot(-viewDir, normal);
    if (cosView <= 0.0) return;

    float viewDist = length(viewPos);
    vec3 worldPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + cameraPosition;
    vec2 slope = waterSlope(worldPos.xz, frameTimeCounter);
    vec3 waveNormal = normalize(mat3(gbufferModelView) * vec3(-slope.x, 1.0, -slope.y));
    vec3 reflectDir = reflect(viewDir, waveNormal);

    // Nền theo ánh sáng thật; SSR đè lên ở nơi có dữ liệu trên màn hình
    vec3 reflection = fallbackReflection(texture2D(colortex2, texCoord).g);
    float blurAmount = 0.75;

    vec2 hitUV;
    vec3 hitPos;
    if (traceReflection(viewPos + normal * 0.02, reflectDir, RAY_STEPS, hitUV, hitPos)) {
        float travel = length(hitPos - viewPos);

        float blur = clamp(0.0012 + travel * 0.00045 + viewDist * 0.00022, 0.0, 0.012);
        vec3 ssr = sampleBlurred(hitUV, vec2(blur * 0.8, blur * 3.2));
        ssr += lampStreak(hitUV, clamp(0.05 + travel * 0.006, 0.05, 0.16));

        // Mờ dần về màu nền khi điểm trúng gần mép màn hình
        vec2 edge = min(hitUV, 1.0 - hitUV);
        float keep = smoothstep(0.0, 0.10, min(edge.x, edge.y));
        reflection = mix(reflection, ssr, keep);
        blurAmount = mix(0.75, clamp(0.15 + travel * 0.03, 0.15, 1.0), keep);
    }

    gl_FragData[0] = vec4(reflection, blurAmount);
}
