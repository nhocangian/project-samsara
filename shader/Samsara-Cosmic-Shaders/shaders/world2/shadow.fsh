#version 120

/* DRAWBUFFERS:0 */

// Bóng đổ: chỉ cần độ sâu. Cắt alpha để lá cỏ / hoa hướng dương đổ bóng đúng hình.

const int shadowMapResolution = 2048;      // [1024 2048 3072 4096] khớp NM_SHADOW_RES
const float shadowDistance = 112.0;        // khớp NM_SHADOW_DISTANCE
const float shadowDistanceRenderMul = 1.0;
const float shadowIntervalSize = 2.0;
const bool shadowHardwareFiltering = false;
const float sunPathRotation = -40.0;       // nghiêng mặt trời về phía nam -> bóng xiên, mặt tiền nhà được nắng

uniform sampler2D tex;

varying vec2 texCoord;
varying vec4 color;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;
    gl_FragData[0] = albedo * color;
}
