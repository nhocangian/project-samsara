#version 120

/* DRAWBUFFERS:0 */

// Lượt vẽ bóng từ phía mặt trời: chỉ cần độ sâu, phần trong suốt của texture thì bỏ.

uniform sampler2D tex;

varying vec2 texCoord;

void main() {
    vec4 albedo = texture2D(tex, texCoord);
    if (albedo.a < 0.1) discard;
    gl_FragData[0] = albedo;
}
