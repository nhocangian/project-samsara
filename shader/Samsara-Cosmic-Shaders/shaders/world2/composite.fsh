#version 120

/* DRAWBUFFERS:01 */

// Pass 1/4 hậu kỳ Ký Ức Của Nam:
//  - colortex0: viền tối mảnh quanh hình khối (outline theo độ sâu), mờ dần theo khoảng cách
//  - colortex1: phần sáng (bright pass) làm nguồn cho bloom

/*
const int colortex0Format = RGBA16F;
const int colortex1Format = RGBA16F;
const int colortex2Format = RGBA16F;
*/
const float sunPathRotation = -40.0; // phải khớp shadow.fsh

#define NM_OUTLINE_STRENGTH 0.55   // [0.0 0.3 0.55 0.8 1.0]
#define NM_OUTLINE_NEAR     20.0   // bắt đầu mờ outline (block)
#define NM_OUTLINE_FAR      70.0   // tắt hẳn outline (block)
#define NM_BLOOM_THRESHOLD  0.85   // độ sáng HDR bắt đầu toả bloom

#include "/lib/nam_memory_common.glsl"

uniform sampler2D colortex0;
uniform sampler2D depthtex0;
uniform float near;
uniform float far;
uniform float viewWidth;
uniform float viewHeight;

varying vec2 texCoord;

float linearDepth(float d) {
    return 2.0 * near * far / (far + near - (2.0 * d - 1.0) * (far - near));
}

void main() {
    vec3 col = texture2D(colortex0, texCoord).rgb;
    vec2 px = 1.0 / vec2(viewWidth, viewHeight);

    float d0 = texture2D(depthtex0, texCoord).r;
    if (d0 < 1.0) {
        float z0 = linearDepth(d0);

        // Pixel thuộc VẬT Ở GẦN mà cạnh nó là vật ở xa (hoặc trời) -> nằm trên viền
        float zR = linearDepth(texture2D(depthtex0, texCoord + vec2(px.x, 0.0)).r);
        float zL = linearDepth(texture2D(depthtex0, texCoord - vec2(px.x, 0.0)).r);
        float zU = linearDepth(texture2D(depthtex0, texCoord + vec2(0.0, px.y)).r);
        float zD = linearDepth(texture2D(depthtex0, texCoord - vec2(0.0, px.y)).r);
        float gap = max(max(zR, zL), max(zU, zD)) - z0;

        // Ngưỡng tương đối theo độ sâu: mặt phẳng nghiêng xa không bị tô viền nhầm
        float edge = smoothstep(0.035, 0.09, gap / z0);
        edge *= 1.0 - smoothstep(NM_OUTLINE_NEAR, NM_OUTLINE_FAR, z0);

        // Viền nâu ấm thay vì đen tuyền -> hợp tông nắng
        vec3 lineCol = col * vec3(0.38, 0.32, 0.28);
        col = mix(col, lineCol, edge * NM_OUTLINE_STRENGTH);
    }

    float lum = nmLuma(col);
    float w = smoothstep(NM_BLOOM_THRESHOLD, NM_BLOOM_THRESHOLD + 1.4, lum);
    vec3 bright = col * w;

    gl_FragData[0] = vec4(col, 1.0);
    gl_FragData[1] = vec4(bright, 1.0);
}
