#version 120

/* DRAWBUFFERS:0 */

#include "/lib/tang_duoi_water.glsl"

// Đường Ống: làm mờ phản chiếu theo chiều dọc rồi hoà vào ảnh.
// Tia phản chiếu ở các pixel cạnh nhau có thể rơi vào bậc trần khác nhau hoặc trượt khỏi màn hình,
// nên ảnh thô bị răng cưa thành khối. Mẫu cố định theo màn hình để mặt nước không nhấp nháy.

uniform sampler2D colortex0;
uniform sampler2D colortex2;   // r = 1: mặt trên của nước
uniform sampler2D colortex3;   // rgb = phản chiếu, a = độ mờ 0..1 (composite.fsh)
uniform sampler2D depthtex0;   // có nước
uniform sampler2D depthtex1;   // không có nước
uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

varying vec2 texCoord;

const int   TAPS = 32;
const vec2  MIN_RADIUS = vec2(0.0010, 0.0032);
const vec2  MAX_RADIUS = vec2(0.0050, 0.0190);
const float HAND_DEPTH = 0.56;

vec3 toView(vec2 uv, float depth) {
    vec4 p = gbufferProjectionInverse * vec4(vec3(uv, depth) * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// Nước liếm lên vách: pixel của khối đặc nằm ngay trên mặt nước được phủ màu nước tới độ cao của sóng.
// Độ cao mặt nước đọc từ chính pixel nước bên dưới, nên không phụ thuộc vị trí ống trong mod.
vec3 waterlineOnWall(vec3 col, float depth) {
    vec3 view = toView(texCoord, depth);
    vec3 relative = (gbufferModelViewInverse * vec4(view, 1.0)).xyz;

    vec3 loweredView = (gbufferModelView * vec4(relative - vec3(0.0, EDGE_HEIGHT, 0.0), 1.0)).xyz;
    vec4 clip = gbufferProjection * vec4(loweredView, 1.0);
    vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return col;
    if (texture2D(colortex2, uv).r < 0.5) return col;

    vec3 waterRelative = (gbufferModelViewInverse * vec4(toView(uv, texture2D(depthtex0, uv).r), 1.0)).xyz;
    float aboveWater = relative.y - waterRelative.y;
    if (aboveWater < -0.02) return col;

    float height = waterEdgeHeight(relative.xz + cameraPosition.xz, frameTimeCounter);
    float covered = 1.0 - smoothstep(height - 0.012, height + 0.012, aboveWater);
    // Phần bị nước phủ chỉ hơi lạnh và nhạt đi như nhìn qua lớp nước; tối đi sẽ trông như bóng đổ
    vec3 wet = mix(col * vec3(0.90, 0.97, 1.02), vec3(0.62, 0.70, 0.74), 0.22);
    // Đường viền sáng mảnh ở đỉnh sóng, chỗ mặt nước cong lên bắt ánh đèn
    float rim = exp(-pow((aboveWater - height) / 0.014, 2.0));
    return mix(col, wet, covered) + vec3(0.78, 0.90, 1.0) * rim * 0.32;
}

// 1 nếu điểm (toạ độ so với camera) nằm trên mặt nước, hoặc ngoài màn hình nên không biết
float waterAt(vec3 relative) {
    vec4 clip = gbufferProjection * vec4((gbufferModelView * vec4(relative, 1.0)).xyz, 1.0);
    if (clip.w <= 0.0) return 1.0;
    vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 1.0;
    return texture2D(colortex2, uv).r > 0.5 ? 1.0 : 0.0;
}

// Mức gần vách: thử các điểm xung quanh trên mặt nước, không cần biết hình dạng ống trong mod
float edgeCloseness(vec3 relative) {
    float water = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.78539816;
        float r = (mod(float(i), 2.0) < 0.5) ? 0.45 : 1.05;
        water += waterAt(relative + vec3(cos(a) * r, 0.0, sin(a) * r));
    }
    return 1.0 - water / 8.0;
}

void main() {
    vec3 base = texture2D(colortex0, texCoord).rgb;
    float depth = texture2D(depthtex0, texCoord).r;
    gl_FragData[0] = vec4(base, 1.0);
    if (depth < HAND_DEPTH || depth >= 1.0) return;
    if (texture2D(colortex2, texCoord).r < 0.5) {
        vec4 wetRefl = texture2D(colortex3, texCoord);
        if (wetRefl.a > 0.003) base = mix(base, wetRefl.rgb, wetRefl.a);
        gl_FragData[0] = vec4(waterlineOnWall(base, depth), 1.0);
        return;
    }

    vec3 viewPos = toView(texCoord, depth);
    vec3 normal = normalize(mat3(gbufferModelView) * vec3(0.0, 1.0, 0.0));
    float cosView = dot(-normalize(viewPos), normal);
    if (cosView <= 0.0) return;

    // Khúc xạ nhẹ: gạch đáy lay động theo sóng; gần mép nước thì giữ nguyên để không lấy nhầm vách
    vec3 worldPos = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + cameraPosition;
    vec2 slope = waterSlope(worldPos.xz, frameTimeCounter);
    vec2 bentUV = texCoord + slope * 0.16 / (1.0 + length(viewPos) * 0.22);
    if (texture2D(colortex2, bentUV).r > 0.5) {
        base = texture2D(colortex0, bentUV).rgb;

        // Vân sáng caustic trên đáy, mạnh theo ánh sáng block thật và tắt dần ở xa để khỏi răng cưa
        vec3 floorView = toView(bentUV, texture2D(depthtex1, bentUV).r);
        vec3 floorWorld = (gbufferModelViewInverse * vec4(floorView, 1.0)).xyz + cameraPosition;
        float fade = 1.0 - smoothstep(10.0, 34.0, length(floorView));
        float blockLight = texture2D(colortex2, texCoord).g;
        float glow = caustics(floorWorld.xz + slope * 6.0, frameTimeCounter) * mix(0.45, 1.0, smoothstep(0.25, 0.85, blockLight));
        base += vec3(0.78, 0.92, 1.0) * glow * fade * CAUSTIC_STRENGTH;
    }

    // Sườn sóng hướng về hay quay đi người nhìn thì sáng tối khác nhau, thấy nhấp nhô cả khi nhìn thẳng xuống
    vec2 viewXZ = normalize(worldPos.xz - cameraPosition.xz + vec2(1e-4, 0.0));
    base *= 1.0 + dot(slope, viewXZ) * 2.0;

    vec4 center = texture2D(colortex3, texCoord);
    vec2 radius = mix(MIN_RADIUS, MAX_RADIUS, center.a);

    float centerW = 1.0 + 5.0 * lampMask(center.rgb);
    vec3 sum = center.rgb * centerW;
    float total = centerW;
    for (int i = 1; i < TAPS; i++) {
        float f = float(i) / float(TAPS);
        float angle = float(i) * 2.39996323;   // góc vàng: mẫu rải đều, không thành hàng
        vec2 uv = texCoord + vec2(cos(angle), sin(angle)) * sqrt(f) * radius;
        // Chỉ lấy mẫu trên mặt nước, để vách và gờ không loang vào phản chiếu
        if (texture2D(colortex2, uv).r < 0.5) continue;
        vec3 tap = texture2D(colortex3, uv).rgb;
        // Đèn được ưu tiên nên giữ hình và loang thành vệt dọc thay vì bị làm mờ mất
        float w = (1.0 - 0.75 * f) * (1.0 + 5.0 * lampMask(tap));
        sum += tap * w;
        total += w;
    }
    vec3 reflection = sum / total;

    // Fresnel mạnh: sát mặt nước gần như gương, nhìn thẳng xuống thấy gạch đáy
    // Tính theo mặt sóng nên độ bóng chạy thành dải theo sóng
    vec3 waveNormal = normalize(mat3(gbufferModelView) * vec3(-slope.x, 1.0, -slope.y));
    float cosWave = clamp(dot(-normalize(viewPos), waveNormal), 0.0, 1.0);
    float fresnel = min(0.06 + 0.94 * pow(1.0 - cosWave, 2.2), 0.90);

    // Viền tối mảnh nơi nước chạm vách: lớp nước phía sau pixel rất mỏng
    float behindWater = length(toView(texCoord, texture2D(depthtex1, texCoord).r) - viewPos);
    float contact = 1.0 - smoothstep(0.03, 0.28, behindWater);

    vec3 col = mix(base, reflection, fresnel);
    col *= 1.0 - 0.14 * contact;

    // Bọt nằm trên cùng, không bị lớp phản chiếu che
    float foamLight = mix(0.70, 1.0, smoothstep(0.25, 0.85, texture2D(colortex2, texCoord).g));
    vec3 relative = worldPos - cameraPosition;
    float nearEdge = edgeCloseness(relative);
    vec2 foam = waterFoam(worldPos.xz, frameTimeCounter, length(viewPos), nearEdge);
    col = mix(col, vec3(0.80, 0.88, 0.92) * foamLight * foam.y, foam.x * FOAM_STRENGTH);

    gl_FragData[0] = vec4(col, 1.0);
}
