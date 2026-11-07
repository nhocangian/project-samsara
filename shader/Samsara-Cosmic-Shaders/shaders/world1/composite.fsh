#version 120

/* DRAWBUFFERS:0 */

// Bầu không khí The Womb: sương thật nằm trong không gian (không phải lớp màu tô lên từng khối).
// Mỗi điểm ảnh: dựng lại tia nhìn từ depth, tính lượng sương tia đi xuyên qua (sương mũ theo độ cao, tích phân
// giải tích), rồi trộn màu theo độ xuyên thấu. Bầu trời (depth = 1) cũng bị phủ: nhìn ngang về chân trời tia đi
// xuyên rất nhiều sương nên thành một dải sáng mờ trước trời; ngước lên thì trời vẫn trong.
//  - Tán xạ: sương sáng rực khi nhìn về phía Gargantua, xám ấm khi quay lưng lại.
//  - Làn sương trôi: độ dày nhân với nhiễu 3D trôi chậm về phía Gargantua (bắc), nên có mảng dày mỏng.
//  - Tắt trong căn phòng cửa (trần Vách Hư Không chặn trời: eyeBrightnessSmooth.y ~ 0): phòng phải đen tuyệt đối.
// Không có gì nhấp nháy: mọi thứ liên tục theo vị trí và thời gian.

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform ivec2 eyeBrightnessSmooth;
uniform float frameTimeCounter;

varying vec2 texCoord;

const vec3 BH_DIR = vec3(0.0, 0.5406, -0.8413);   // khớp bhForward (gbuffers_skybasic) và GARGANTUA_DIR của mod

const float FOG_BASE_Y = 61.5;     // mặt nước
const float FOG_SCALE_H = 6.0;     // cứ lên cao chừng này block thì sương loãng đi e lần
const float FOG_DENSITY = 0.018;   // độ đặc ở mặt nước (mỗi block)
const float FOG_NEAR = 6.0;        // quanh người chơi không có sương
const float SKY_DISTANCE = 600.0;  // tia nhìn lên trời coi như đi xa chừng này

float hash3(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash3(i);
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 3; i++) {
        v += a * noise3(p);
        p = p * 2.03 + vec3(1.7, 9.2, 3.1);
        a *= 0.5;
    }
    return v;   // ~0..1
}

/** Lượng sương (optical depth) dọc tia từ độ cao y0, hướng đi lên dy, dài L (tích phân mũ theo độ cao). */
float heightFogDepth(float y0, float dy, float L) {
    float base = FOG_DENSITY * exp(-(y0 - FOG_BASE_Y) / FOG_SCALE_H);
    float k = dy / FOG_SCALE_H;
    if (abs(k * L) < 1.0e-3) return base * L;
    return base * (1.0 - exp(-k * L)) / k;
}

void main() {
    vec3 color = texture2D(colortex0, texCoord).rgb;
    float open = clamp(float(eyeBrightnessSmooth.y) / 200.0, 0.0, 1.0);
    if (open <= 0.001) {
        gl_FragData[0] = vec4(color, 1.0);
        return;
    }

    float depth = texture2D(depthtex0, texCoord).r;
    vec4 ndc = vec4(vec3(texCoord, depth) * 2.0 - 1.0, 1.0);
    vec4 view = gbufferProjectionInverse * ndc;
    view.xyz /= view.w;
    vec3 rel = mat3(gbufferModelViewInverse) * view.xyz;          // camera -> điểm (toạ độ thế giới tương đối)
    float L = depth >= 1.0 ? SKY_DISTANCE : length(rel);
    vec3 dir = normalize(rel);

    // Bỏ qua một khoảng ngắn quanh người chơi
    float Lf = max(L - FOG_NEAR, 0.0);
    float yStart = cameraPosition.y + dir.y * min(L, FOG_NEAR);
    float od = heightFogDepth(yStart, dir.y, Lf);

    // Làn sương trôi: lấy nhiễu ở hai khoảng cách dọc tia, trôi chậm về phía bắc và hơi lên trên
    vec3 drift = vec3(0.0, 0.08, -0.6) * frameTimeCounter;
    vec3 p1 = cameraPosition + dir * min(L, 22.0);
    vec3 p2 = cameraPosition + dir * min(L, 70.0);
    float n = 0.5 * (fbm((p1 + drift) * vec3(0.05, 0.09, 0.05)) + fbm((p2 + drift) * vec3(0.03, 0.06, 0.03)));
    od *= 0.45 + 1.1 * n;

    float transmittance = exp(-od);

    // Tán xạ: sáng rực về phía Gargantua, xám ấm khi quay lưng lại
    float cosTheta = dot(dir, BH_DIR);
    float forward = pow(max(cosTheta, 0.0), 5.0);
    vec3 mistColor = vec3(0.30, 0.235, 0.18) + vec3(0.80, 0.55, 0.28) * (0.25 + 1.1 * forward);

    vec3 fogged = color * transmittance + mistColor * (1.0 - transmittance);
    gl_FragData[0] = vec4(mix(color, fogged, open), 1.0);
}
